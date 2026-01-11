package com.mlengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.model.dto.PredictionDTO;
import com.mlengine.model.entity.ApiKey;
import com.mlengine.model.entity.ApiUsageStat;
import com.mlengine.model.entity.Model;
import com.mlengine.repository.ApiKeyRepository;
import com.mlengine.repository.ApiUsageStatRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for API Key management and usage statistics.
 * Supports the API Integration tab in the Predictions UI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiUsageStatRepository usageStatRepository;
    private final ModelRepository modelRepository;
    private final PredictionRepository predictionRepository;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "mlk_";
    private static final int KEY_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    // ========== API KEY MANAGEMENT ==========

    @Transactional
    public PredictionDTO.ApiKeyResponse createApiKey(PredictionDTO.ApiKeyRequest request) {
        log.info("Creating API key for model: {}", request.getModelId());

        String fullKey = generateApiKey();
        String keyPrefix = fullKey.substring(0, 12) + "...";

        ApiKey apiKey = ApiKey.builder()
                .name(request.getName() != null ? request.getName() : "API Key")
                .keyValue(fullKey)
                .keyPrefix(keyPrefix)
                .modelId(request.getModelId())
                .projectId(request.getProjectId())
                .rateLimitPerHour(request.getRateLimitPerHour() != null ? request.getRateLimitPerHour() : 1000)
                .rateLimitPerDay(10000)
                .isActive(true)
                .totalRequests(0L)
                .requestsToday(0L)
                .requestsThisHour(0L)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Created API key: {} for model: {}", apiKey.getId(), request.getModelId());

        return toApiKeyResponse(apiKey, fullKey);
    }

    @Transactional
    public PredictionDTO.ApiKeyResponse regenerateApiKey(String apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new RuntimeException("API key not found: " + apiKeyId));

        String newKey = generateApiKey();
        String keyPrefix = newKey.substring(0, 12) + "...";

        apiKey.setKeyValue(newKey);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setUpdatedAt(LocalDateTime.now());

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Regenerated API key: {}", apiKeyId);

        return toApiKeyResponse(apiKey, newKey);
    }

    public PredictionDTO.ApiKeyResponse getApiKey(String apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new RuntimeException("API key not found: " + apiKeyId));
        return toApiKeyResponse(apiKey, null);
    }

    public PredictionDTO.ApiKeyResponse getApiKeyForModel(String modelId) {
        List<ApiKey> keys = apiKeyRepository.findByModelIdAndIsActiveTrue(modelId);
        if (keys.isEmpty()) {
            return null;
        }
        return toApiKeyResponse(keys.get(0), null);
    }

    @Transactional
    public PredictionDTO.ApiKeyResponse getOrCreateApiKey(String modelId, String projectId) {
        List<ApiKey> keys = apiKeyRepository.findByModelIdAndIsActiveTrue(modelId);
        if (!keys.isEmpty()) {
            return toApiKeyResponse(keys.get(0), null);
        }

        PredictionDTO.ApiKeyRequest request = PredictionDTO.ApiKeyRequest.builder()
                .modelId(modelId)
                .projectId(projectId)
                .name("Default API Key")
                .rateLimitPerHour(1000)
                .build();
        return createApiKey(request);
    }

    @Transactional
    public void revokeApiKey(String apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new RuntimeException("API key not found: " + apiKeyId));
        
        apiKey.setIsActive(false);
        apiKey.setRevokedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key: {}", apiKeyId);
    }

    public boolean validateAndCheckRateLimit(String apiKeyValue) {
        Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyValue(apiKeyValue);
        if (keyOpt.isEmpty()) {
            return false;
        }

        ApiKey apiKey = keyOpt.get();
        if (!apiKey.getIsActive()) {
            return false;
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (apiKey.getRequestsThisHour() >= apiKey.getRateLimitPerHour()) {
            log.warn("Rate limit exceeded for API key: {}", apiKey.getId());
            return false;
        }

        return true;
    }

    @Transactional
    public void recordUsage(String apiKeyValue, long latencyMs, boolean success) {
        Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyValue(apiKeyValue);
        if (keyOpt.isEmpty()) return;

        ApiKey apiKey = keyOpt.get();

        apiKey.setTotalRequests(apiKey.getTotalRequests() + 1);
        apiKey.setRequestsToday(apiKey.getRequestsToday() + 1);
        apiKey.setRequestsThisHour(apiKey.getRequestsThisHour() + 1);
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        updateUsageStats(apiKey.getModelId(), apiKey.getProjectId(), latencyMs, success);
    }

    // ========== API USAGE STATISTICS ==========

    public PredictionDTO.ApiUsageStats getUsageStats(String modelId, String projectId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime monthStartTime = monthStart.atStartOfDay();

        Long todayRequests = predictionRepository.countApiCallsByModelSince(modelId, todayStart);
        Long monthRequests = predictionRepository.countApiCallsByModelSince(modelId, monthStartTime);
        Double avgLatency = predictionRepository.avgApiLatencyByModelSince(modelId, monthStartTime);

        if (todayRequests == null || todayRequests == 0) {
            todayRequests = usageStatRepository.sumRequestsByModelAndDate(modelId, today);
        }
        if (monthRequests == null || monthRequests == 0) {
            monthRequests = usageStatRepository.sumRequestsByModelAndDateRange(modelId, monthStart, today);
        }
        if (avgLatency == null) {
            avgLatency = usageStatRepository.avgLatencyByModelAndDateRange(modelId, monthStart, today);
        }

        List<PredictionDTO.DailyUsage> dailyUsage = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Long count = usageStatRepository.sumRequestsByModelAndDate(modelId, date);
            dailyUsage.add(PredictionDTO.DailyUsage.builder()
                    .date(date.format(DateTimeFormatter.ISO_DATE))
                    .requests(count != null ? count : 0L)
                    .avgLatency(0.0)
                    .build());
        }

        List<PredictionDTO.HourlyUsage> hourlyUsage = new ArrayList<>();
        List<ApiUsageStat> hourlyStats = usageStatRepository.findHourlyStatsByModelAndDate(modelId, today);
        for (int hour = 0; hour < 24; hour++) {
            int finalHour = hour;
            ApiUsageStat stat = hourlyStats.stream()
                    .filter(s -> s.getStatHour() != null && s.getStatHour() == finalHour)
                    .findFirst()
                    .orElse(null);
            hourlyUsage.add(PredictionDTO.HourlyUsage.builder()
                    .hour(hour)
                    .requests(stat != null ? stat.getRequestCount() : 0L)
                    .avgLatency(stat != null ? stat.getAvgLatencyMs() : 0.0)
                    .build());
        }

        return PredictionDTO.ApiUsageStats.builder()
                .todayRequests(todayRequests != null ? todayRequests : 0L)
                .todayLabel(formatNumber(todayRequests != null ? todayRequests : 0L))
                .monthRequests(monthRequests != null ? monthRequests : 0L)
                .monthLabel(formatNumber(monthRequests != null ? monthRequests : 0L))
                .avgLatencyMs(avgLatency != null ? avgLatency : 0.0)
                .avgLatencyLabel(avgLatency != null ? String.format("%.0fms", avgLatency) : "0ms")
                .successRate(100.0)
                .successRateLabel("100%")
                .dailyUsage(dailyUsage)
                .hourlyUsage(hourlyUsage)
                .build();
    }

    public PredictionDTO.RateLimitInfo getRateLimitInfo(String modelId) {
        List<ApiKey> keys = apiKeyRepository.findByModelIdAndIsActiveTrue(modelId);
        if (keys.isEmpty()) {
            return PredictionDTO.RateLimitInfo.builder()
                    .limitPerHour(1000)
                    .usedThisHour(0)
                    .remainingThisHour(1000)
                    .usagePercentage(0.0)
                    .usageLabel("0 / 1,000 requests/hour")
                    .build();
        }

        ApiKey apiKey = keys.get(0);
        int used = apiKey.getRequestsThisHour() != null ? apiKey.getRequestsThisHour().intValue() : 0;
        int limit = apiKey.getRateLimitPerHour() != null ? apiKey.getRateLimitPerHour() : 1000;
        int remaining = Math.max(0, limit - used);
        double percentage = limit > 0 ? (used * 100.0 / limit) : 0;

        return PredictionDTO.RateLimitInfo.builder()
                .limitPerHour(limit)
                .usedThisHour(used)
                .remainingThisHour(remaining)
                .usagePercentage(percentage)
                .usageLabel(String.format("%,d / %,d requests/hour", used, limit))
                .build();
    }

    // ========== API INTEGRATION INFO ==========

    public PredictionDTO.ApiIntegrationInfo getApiIntegrationInfo(String modelId, String projectId, String baseUrl) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));

        PredictionDTO.ApiKeyResponse apiKeyResponse = getOrCreateApiKey(modelId, projectId);

        String endpoint = baseUrl + "/api/predictions/v1/models/" + modelId + "/predict";

        PredictionDTO.ApiUsageStats usageStats = getUsageStats(modelId, projectId);
        PredictionDTO.RateLimitInfo rateLimitInfo = getRateLimitInfo(modelId);

        Map<String, String> codeExamples = generateCodeExamples(endpoint, apiKeyResponse.getKeyPrefix(), model);
        Map<String, Object> sampleRequest = buildSampleRequest(model);
        Map<String, Object> sampleResponse = buildSampleResponse(model);

        return PredictionDTO.ApiIntegrationInfo.builder()
                .modelId(modelId)
                .modelName(model.getName())
                .endpoint(endpoint)
                .method("POST")
                .apiKeyId(apiKeyResponse.getId())
                .apiKeyPrefix(apiKeyResponse.getKeyPrefix())
                .apiKeyCreatedAt(apiKeyResponse.getCreatedAt())
                .usageStats(usageStats)
                .rateLimit(rateLimitInfo)
                .codeExamples(codeExamples)
                .sampleRequest(sampleRequest)
                .sampleResponse(sampleResponse)
                .build();
    }

    // ========== HELPER METHODS ==========

    private String generateApiKey() {
        byte[] randomBytes = new byte[KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder(KEY_PREFIX);
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().substring(0, KEY_PREFIX.length() + KEY_LENGTH);
    }

    private void updateUsageStats(String modelId, String projectId, long latencyMs, boolean success) {
        LocalDate today = LocalDate.now();
        int currentHour = LocalDateTime.now().getHour();

        Optional<ApiUsageStat> statOpt = usageStatRepository.findByModelIdAndStatDateAndStatHour(
                modelId, today, currentHour);

        ApiUsageStat stat;
        if (statOpt.isPresent()) {
            stat = statOpt.get();
            stat.setRequestCount(stat.getRequestCount() + 1);
            if (success) {
                stat.setSuccessCount(stat.getSuccessCount() + 1);
            } else {
                stat.setErrorCount(stat.getErrorCount() + 1);
            }
            stat.setTotalLatencyMs(stat.getTotalLatencyMs() + latencyMs);
            stat.setAvgLatencyMs((double) stat.getTotalLatencyMs() / stat.getRequestCount());
            if (stat.getMinLatencyMs() == null || latencyMs < stat.getMinLatencyMs()) {
                stat.setMinLatencyMs(latencyMs);
            }
            if (stat.getMaxLatencyMs() == null || latencyMs > stat.getMaxLatencyMs()) {
                stat.setMaxLatencyMs(latencyMs);
            }
        } else {
            stat = ApiUsageStat.builder()
                    .modelId(modelId)
                    .projectId(projectId)
                    .statDate(today)
                    .statHour(currentHour)
                    .requestCount(1L)
                    .successCount(success ? 1L : 0L)
                    .errorCount(success ? 0L : 1L)
                    .totalLatencyMs(latencyMs)
                    .minLatencyMs(latencyMs)
                    .maxLatencyMs(latencyMs)
                    .avgLatencyMs((double) latencyMs)
                    .build();
        }
        stat.setUpdatedAt(LocalDateTime.now());
        usageStatRepository.save(stat);
    }

    private Map<String, String> generateCodeExamples(String endpoint, String apiKeyPrefix, Model model) {
        Map<String, String> examples = new LinkedHashMap<>();

        String sampleFeatures = "{\n" +
                "    \"age\": 35,\n" +
                "    \"annual_income\": 75000,\n" +
                "    \"credit_score\": 720,\n" +
                "    \"loan_amount\": 250000,\n" +
                "    \"employment_years\": 8,\n" +
                "    \"existing_loans\": 1\n" +
                "  }";

        // Python
        examples.put("python", 
                "import requests\n\n" +
                "response = requests.post(\n" +
                "    \"" + endpoint + "\",\n" +
                "    headers={\"Authorization\": \"Bearer YOUR_API_KEY\"},\n" +
                "    json={\n" +
                "        \"features\": " + sampleFeatures.replace("\n", "\n        ") + "\n" +
                "    }\n" +
                ")\n\n" +
                "result = response.json()\n" +
                "print(f\"Prediction: {result['prediction']}\")\n" +
                "print(f\"Confidence: {result['confidence']}\")");

        // JavaScript
        examples.put("javascript",
                "const response = await fetch('" + endpoint + "', {\n" +
                "  method: 'POST',\n" +
                "  headers: {\n" +
                "    'Content-Type': 'application/json',\n" +
                "    'Authorization': 'Bearer YOUR_API_KEY'\n" +
                "  },\n" +
                "  body: JSON.stringify({\n" +
                "    features: " + sampleFeatures.replace("\n", "\n    ") + "\n" +
                "  })\n" +
                "});\n\n" +
                "const result = await response.json();\n" +
                "console.log(`Prediction: ${result.prediction}`);\n" +
                "console.log(`Confidence: ${result.confidence}`);");

        // cURL
        examples.put("curl",
                "curl -X POST \"" + endpoint + "\" \\\n" +
                "  -H \"Content-Type: application/json\" \\\n" +
                "  -H \"Authorization: Bearer YOUR_API_KEY\" \\\n" +
                "  -d '{\n" +
                "    \"features\": " + sampleFeatures.replace("\n", "\n    ") + "\n" +
                "  }'");

        // Java
        examples.put("java",
                "HttpClient client = HttpClient.newHttpClient();\n\n" +
                "String json = \"{\\\"features\\\": {\\\"age\\\": 35, ...}}\";\n\n" +
                "HttpRequest request = HttpRequest.newBuilder()\n" +
                "    .uri(URI.create(\"" + endpoint + "\"))\n" +
                "    .header(\"Content-Type\", \"application/json\")\n" +
                "    .header(\"Authorization\", \"Bearer YOUR_API_KEY\")\n" +
                "    .POST(HttpRequest.BodyPublishers.ofString(json))\n" +
                "    .build();\n\n" +
                "HttpResponse<String> response = client.send(request,\n" +
                "    HttpResponse.BodyHandlers.ofString());\n\n" +
                "System.out.println(response.body());");

        return examples;
    }

    private Map<String, Object> buildSampleRequest(Model model) {
        Map<String, Object> request = new LinkedHashMap<>();
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("age", 35);
        features.put("annual_income", 75000);
        features.put("credit_score", 720);
        features.put("loan_amount", 250000);
        features.put("employment_years", 8);
        features.put("existing_loans", 1);
        request.put("features", features);
        return request;
    }

    private Map<String, Object> buildSampleResponse(Model model) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("prediction", "approved");
        response.put("confidence", 0.873);
        
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("approved", 0.873);
        probabilities.put("rejected", 0.127);
        response.put("probabilities", probabilities);
        
        return response;
    }

    private PredictionDTO.ApiKeyResponse toApiKeyResponse(ApiKey apiKey, String fullKey) {
        return PredictionDTO.ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .keyFull(fullKey)
                .modelId(apiKey.getModelId())
                .projectId(apiKey.getProjectId())
                .isActive(apiKey.getIsActive())
                .rateLimitPerHour(apiKey.getRateLimitPerHour())
                .totalRequests(apiKey.getTotalRequests())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .expiresAt(apiKey.getExpiresAt())
                .build();
    }

    private String formatNumber(long number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.format("%,d", number);
    }
}
