package com.mlengine.model.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Prediction operations.
 * Supports ALL features in the Predictions UI screens.
 */
public class PredictionDTO {

    // ========== SINGLE PREDICTION ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleRequest {
        @NotBlank(message = "Model ID is required")
        private String modelId;

        @NotNull(message = "Features are required")
        private Map<String, Object> features;

        private Boolean includeExplanation;
        private String projectId;
        private String source;  // "UI", "API"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleResponse {
        private String predictionId;
        private String modelId;
        private String modelName;

        // Main prediction
        private String predictedClass;      // "Approved", "Rejected"
        private String predictedLabel;      // Display label
        private Double probability;
        private String probabilityLabel;    // "87.3%"
        private Double confidence;
        private String confidenceLabel;     // "High Confidence"

        // Probabilities for all classes
        private Map<String, Double> probabilities;  // {"Approved": 0.873, "Rejected": 0.127}

        // Risk level
        private String riskLevel;           // "High Risk", "Medium Risk", "Low Risk"
        private String riskColor;           // "red", "yellow", "green"

        // Regression result
        private Double predictedValue;
        private String predictedValueLabel;

        // Input features echo
        private Map<String, Object> inputFeatures;

        // Optional explanation
        private List<FeatureContribution> topContributions;
        private String explanation;

        // Metadata
        private Long processingTimeMs;
        private LocalDateTime timestamp;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureContribution {
        private String feature;
        private Object value;
        private Double contribution;
        private String contributionLabel;
        private String direction;
        private String impact;
    }

    // ========== BATCH PREDICTION ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRequest {
        @NotBlank(message = "Model ID is required")
        private String modelId;
        private String jobName;
        private Boolean includeConfidence;
        private Boolean includeProbabilities;
        private Boolean includeExplanations;
        private String projectId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchResponse {
        private String jobId;
        private String jobName;
        private String modelId;
        private String modelName;
        private String status;
        private String statusLabel;
        private String statusMessage;
        private Integer totalRecords;
        private Integer processedRecords;
        private Integer failedRecords;
        private Integer progress;
        private String progressLabel;
        private String inputFileName;
        private String outputFileName;
        private String downloadUrl;
        private BatchSummary summary;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long processingTimeMs;
        private String processingTimeLabel;
        private String etaLabel;
        private String errorMessage;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSummary {
        private Integer totalPredictions;
        private Integer successfulPredictions;
        private Integer failedPredictions;
        private Map<String, Integer> classCounts;
        private Map<String, Double> classPercentages;
        private Double avgConfidence;
        private String avgConfidenceLabel;
        private Double minConfidence;
        private Double maxConfidence;
        private Integer highRiskCount;
        private Integer mediumRiskCount;
        private Integer lowRiskCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchValidation {
        private Boolean valid;
        private String fileName;
        private Integer totalRows;
        private List<String> requiredColumns;
        private List<String> foundColumns;
        private List<String> missingColumns;
        private Integer rowsWithMissingValues;
        private List<Map<String, Object>> previewRows;
        private List<String> warnings;
        private List<String> errors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchListItem {
        private String jobId;
        private String jobName;
        private String modelName;
        private String status;
        private String statusLabel;
        private Integer totalRecords;
        private Integer progress;
        private String progressLabel;
        private LocalDateTime createdAt;
        private String createdAtLabel;
    }

    // ========== PREDICTION HISTORY ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryFilter {
        private String projectId;
        private String modelId;
        private String type;
        private String result;
        private String dateRange;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String search;
        private Integer page;
        private Integer pageSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String predictionId;
        private String modelId;
        private String modelName;
        private String predictionType;
        private String predictionTypeLabel;
        private String source;
        private String predictedClass;
        private String predictedLabel;
        private Double probability;
        private String probabilityLabel;
        private Double confidence;
        private String confidenceLabel;
        private String riskLevel;
        private String riskColor;
        private Map<String, Object> inputFeatures;
        private Map<String, Double> probabilities;
        private String batchId;
        private Integer batchSize;
        private String batchResultLabel;
        private LocalDateTime timestamp;
        private String timestampLabel;
        private Long processingTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryResponse {
        private List<HistoryItem> predictions;
        private Integer total;
        private Integer page;
        private Integer pageSize;
        private Integer totalPages;
        private HistoryStats stats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryStats {
        private Long totalPredictions;
        private String totalLabel;
        private Map<String, Long> resultCounts;
        private Map<String, Double> resultPercentages;
        private Long singleCount;
        private Long batchCount;
        private Long apiCount;
        private Long todayCount;
        private Long thisWeekCount;
        private Long thisMonthCount;
        private Double avgConfidence;
        private String avgConfidenceLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionDetail {
        private String predictionId;
        private String modelId;
        private String modelName;
        private String predictionType;
        private String predictionTypeLabel;
        private LocalDateTime timestamp;
        private String timestampLabel;
        private Map<String, Object> inputFeatures;
        private List<FeatureDisplay> inputFeaturesDisplay;
        private String predictedClass;
        private String predictedLabel;
        private Double confidence;
        private String confidenceLabel;
        private Map<String, Double> probabilities;
        private String riskLevel;
        private String riskColor;
        private List<FeatureContribution> topContributions;
        private Long processingTimeMs;
        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureDisplay {
        private String name;
        private String label;
        private Object value;
        private String valueLabel;
    }

    // ========== API INTEGRATION ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiIntegrationInfo {
        private String modelId;
        private String modelName;
        private String endpoint;
        private String method;
        private String apiKeyId;
        private String apiKeyPrefix;
        private String apiKeyFull;
        private LocalDateTime apiKeyCreatedAt;
        private ApiUsageStats usageStats;
        private RateLimitInfo rateLimit;
        private Map<String, String> codeExamples;
        private Map<String, Object> sampleRequest;
        private Map<String, Object> sampleResponse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiUsageStats {
        private Long todayRequests;
        private String todayLabel;
        private Long monthRequests;
        private String monthLabel;
        private Double avgLatencyMs;
        private String avgLatencyLabel;
        private Double successRate;
        private String successRateLabel;
        private List<DailyUsage> dailyUsage;
        private List<HourlyUsage> hourlyUsage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsage {
        private String date;
        private Long requests;
        private Double avgLatency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyUsage {
        private Integer hour;
        private Long requests;
        private Double avgLatency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitInfo {
        private Integer limitPerHour;
        private Integer usedThisHour;
        private Integer remainingThisHour;
        private Double usagePercentage;
        private String usageLabel;
        private String resetTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiKeyRequest {
        private String modelId;
        private String projectId;
        private String name;
        private Integer rateLimitPerHour;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiKeyResponse {
        private String id;
        private String name;
        private String keyPrefix;
        private String keyFull;
        private String modelId;
        private String projectId;
        private Boolean isActive;
        private Integer rateLimitPerHour;
        private Long totalRequests;
        private LocalDateTime lastUsedAt;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
    }

    // ========== MODEL STATS ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelPredictionStats {
        private String modelId;
        private String modelName;
        private String algorithm;
        private Double accuracy;
        private String accuracyLabel;
        private LocalDateTime trainedAt;
        private String trainedAtLabel;
        private Long totalPredictions;
        private Map<String, Long> resultCounts;
    }

    // ========== REALTIME / PUBLIC API ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealtimeRequest {
        private Map<String, Object> features;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealtimeResponse {
        private String prediction;
        private Double confidence;
        private Map<String, Double> probabilities;
        private Long latencyMs;
    }

    // ========== EXPORT ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportRequest {
        private String projectId;
        private String modelId;
        private String type;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String format;
    }
}
