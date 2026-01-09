package com.mlengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.config.MLEngineConfig;
import com.mlengine.model.dto.PredictionDTO;
import com.mlengine.model.entity.BatchPredictionJob;
import com.mlengine.model.entity.Model;
import com.mlengine.model.entity.Prediction;
import com.mlengine.repository.BatchPredictionJobRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Prediction operations.
 * Handles single and batch predictions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final BatchPredictionJobRepository batchJobRepository;
    private final ModelRepository modelRepository;
    private final MLEngineConfig config;
    private final ObjectMapper objectMapper;

    // ========== SINGLE PREDICTION ==========

    /**
     * Make a single prediction.
     * Matches React UI Single Prediction form.
     */
    @Transactional
    public PredictionDTO.SingleResponse predictSingle(PredictionDTO.SingleRequest request) {
        long startTime = System.currentTimeMillis();

        // Get model
        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + request.getModelId()));

        // Simulate prediction (in real implementation, call Python ML engine)
        Map<String, Object> features = request.getFeatures();
        PredictionResult result = simulatePrediction(features, model);

        // Save prediction
        Prediction prediction = Prediction.builder()
                .modelId(model.getId())
                .modelName(model.getName())
                .predictionType("single")
                .inputJson(toJson(features))
                .outputJson(toJson(result))
                .predictedClass(result.predictedClass)
                .probability(result.probability)
                .confidence(result.confidence)
                .riskLevel(result.riskLevel)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .projectId(request.getProjectId())
                .build();

        prediction = predictionRepository.save(prediction);

        // Update model prediction count
        model.setPredictionsCount(model.getPredictionsCount() + 1);
        modelRepository.save(model);

        // Build response
        List<PredictionDTO.FeatureContribution> contributions = null;
        String explanation = null;

        if (Boolean.TRUE.equals(request.getIncludeExplanation())) {
            contributions = generateTopContributions(features);
            explanation = generateExplanation(result, contributions);
        }

        return PredictionDTO.SingleResponse.builder()
                .predictionId(prediction.getId())
                .modelId(model.getId())
                .modelName(model.getName())
                .predictedClass(result.predictedClass)
                .predictedLabel(result.predictedClass)
                .probability(result.probability)
                .probabilityLabel(String.format("%.1f%%", result.probability * 100))
                .confidence(result.confidence)
                .confidenceLabel(getConfidenceLabel(result.confidence))
                .riskLevel(result.riskLevel)
                .riskColor(getRiskColor(result.riskLevel))
                .inputFeatures(features)
                .topContributions(contributions)
                .explanation(explanation)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Realtime prediction for deployed model endpoint.
     */
    public PredictionDTO.RealtimeResponse predictRealtime(String modelId, Map<String, Object> features) {
        long startTime = System.currentTimeMillis();

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        if (!Boolean.TRUE.equals(model.getIsDeployed())) {
            throw new IllegalStateException("Model is not deployed: " + modelId);
        }

        PredictionResult result = simulatePrediction(features, model);

        Map<String, Double> classProbabilities = new HashMap<>();
        classProbabilities.put("Positive", result.probability);
        classProbabilities.put("Negative", 1.0 - result.probability);

        return PredictionDTO.RealtimeResponse.builder()
                .prediction(result.predictedClass)
                .probability(result.probability)
                .confidence(result.confidence)
                .classProbabilities(classProbabilities)
                .latencyMs(System.currentTimeMillis() - startTime)
                .build();
    }

    // ========== BATCH PREDICTION ==========

    /**
     * Start a batch prediction job.
     */
    @Transactional
    public PredictionDTO.BatchResponse startBatchPrediction(
            String modelId, String jobName, MultipartFile file, String projectId) {
        
        // Validate model
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        // Save input file
        String inputFileName = file.getOriginalFilename();
        String inputFilePath = saveUploadedFile(file);

        // Count records
        int totalRecords = countCsvRecords(inputFilePath);

        // Create batch job
        BatchPredictionJob job = BatchPredictionJob.builder()
                .jobName(jobName != null ? jobName : "Batch Prediction - " + inputFileName)
                .modelId(modelId)
                .modelName(model.getName())
                .status("QUEUED")
                .totalRecords(totalRecords)
                .processedRecords(0)
                .progress(0)
                .inputFilePath(inputFilePath)
                .inputFileName(inputFileName)
                .projectId(projectId)
                .build();

        job = batchJobRepository.save(job);
        log.info("Created batch prediction job: {} with {} records", job.getId(), totalRecords);

        // Start processing asynchronously
        processBatchAsync(job.getId());

        return toBatchResponse(job);
    }

    /**
     * Get batch job status.
     */
    public PredictionDTO.BatchResponse getBatchJob(String jobId) {
        BatchPredictionJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + jobId));
        return toBatchResponse(job);
    }

    /**
     * Get all batch jobs.
     */
    public List<PredictionDTO.BatchListItem> getAllBatchJobs(String projectId) {
        List<BatchPredictionJob> jobs = projectId != null
                ? batchJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                : batchJobRepository.findAllByOrderByCreatedAtDesc();

        return jobs.stream()
                .map(this::toBatchListItem)
                .collect(Collectors.toList());
    }

    /**
     * Get output file path for download.
     */
    public String getBatchOutputPath(String jobId) {
        BatchPredictionJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + jobId));

        if (!"COMPLETED".equals(job.getStatus())) {
            throw new IllegalStateException("Batch job is not completed");
        }

        return job.getOutputFilePath();
    }

    // ========== PREDICTION HISTORY ==========

    /**
     * Get prediction history with pagination.
     */
    public PredictionDTO.HistoryResponse getHistory(String projectId, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);

        Page<Prediction> predictions = projectId != null
                ? predictionRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageRequest)
                : predictionRepository.findAllByOrderByCreatedAtDesc(pageRequest);

        List<PredictionDTO.HistoryItem> items = predictions.getContent().stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());

        // Get stats
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        Long totalPredictions = projectId != null 
                ? predictionRepository.countByProject(projectId)
                : predictionRepository.count();
        Long todayPredictions = projectId != null
                ? predictionRepository.countByProjectSince(projectId, today)
                : predictionRepository.countSince(today);
        Double avgConfidence = projectId != null
                ? predictionRepository.avgConfidenceByProject(projectId)
                : 0.85;

        return PredictionDTO.HistoryResponse.builder()
                .predictions(items)
                .total((int) predictions.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .totalPages(predictions.getTotalPages())
                .totalPredictions(totalPredictions)
                .todayPredictions(todayPredictions)
                .avgConfidence(avgConfidence != null ? avgConfidence : 0.0)
                .build();
    }

    /**
     * Get prediction by ID.
     */
    public PredictionDTO.SingleResponse getPrediction(String predictionId) {
        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new IllegalArgumentException("Prediction not found: " + predictionId));

        Map<String, Object> features = fromJson(prediction.getInputJson());

        return PredictionDTO.SingleResponse.builder()
                .predictionId(prediction.getId())
                .modelId(prediction.getModelId())
                .modelName(prediction.getModelName())
                .predictedClass(prediction.getPredictedClass())
                .predictedLabel(prediction.getPredictedClass())
                .probability(prediction.getProbability())
                .probabilityLabel(String.format("%.1f%%", prediction.getProbability() * 100))
                .confidence(prediction.getConfidence())
                .confidenceLabel(getConfidenceLabel(prediction.getConfidence()))
                .riskLevel(prediction.getRiskLevel())
                .riskColor(getRiskColor(prediction.getRiskLevel()))
                .inputFeatures(features)
                .processingTimeMs(prediction.getProcessingTimeMs())
                .timestamp(prediction.getCreatedAt())
                .build();
    }

    // ========== ASYNC BATCH PROCESSING ==========

    @Async
    protected void processBatchAsync(String jobId) {
        try {
            Thread.sleep(100);  // Ensure transaction is committed
            processBatch(jobId);
        } catch (Exception e) {
            log.error("Batch processing failed", e);
            updateBatchStatus(jobId, "FAILED", e.getMessage());
        }
    }

    private void processBatch(String jobId) {
        BatchPredictionJob job = batchJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            log.info("Starting batch prediction: {}", jobId);

            job.setStatus("PROCESSING");
            job.setStartedAt(LocalDateTime.now());
            batchJobRepository.save(job);

            // Read input file
            List<Map<String, Object>> records = readCsvRecords(job.getInputFilePath());
            int total = records.size();
            int processed = 0;
            int positiveCount = 0;
            int negativeCount = 0;
            double totalConfidence = 0;

            // Prepare output file
            String outputFileName = "predictions_" + jobId + ".csv";
            String outputFilePath = config.getExportsDir() + "/" + outputFileName;
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
                // Write header
                writer.println("index,predicted_class,probability,confidence,risk_level");

                // Process each record
                for (int i = 0; i < records.size(); i++) {
                    Map<String, Object> features = records.get(i);
                    PredictionResult result = simulatePrediction(features, null);

                    // Write result
                    writer.printf("%d,%s,%.4f,%.4f,%s%n",
                            i + 1,
                            result.predictedClass,
                            result.probability,
                            result.confidence,
                            result.riskLevel);

                    // Track stats
                    if ("Positive".equals(result.predictedClass)) {
                        positiveCount++;
                    } else {
                        negativeCount++;
                    }
                    totalConfidence += result.confidence;

                    // Save individual prediction
                    Prediction prediction = Prediction.builder()
                            .modelId(job.getModelId())
                            .modelName(job.getModelName())
                            .predictionType("batch")
                            .inputJson(toJson(features))
                            .predictedClass(result.predictedClass)
                            .probability(result.probability)
                            .confidence(result.confidence)
                            .riskLevel(result.riskLevel)
                            .batchId(jobId)
                            .batchIndex(i)
                            .projectId(job.getProjectId())
                            .build();
                    predictionRepository.save(prediction);

                    processed++;

                    // Update progress every 10%
                    if (processed % Math.max(1, total / 10) == 0) {
                        job.setProcessedRecords(processed);
                        job.setProgress((int) ((processed * 100.0) / total));
                        batchJobRepository.save(job);
                    }
                }
            }

            // Update job as completed
            job.setStatus("COMPLETED");
            job.setProcessedRecords(total);
            job.setProgress(100);
            job.setOutputFilePath(outputFilePath);
            job.setOutputFileName(outputFileName);
            job.setPositiveCount(positiveCount);
            job.setNegativeCount(negativeCount);
            job.setAvgConfidence(totalConfidence / total);
            job.setCompletedAt(LocalDateTime.now());
            job.setProcessingTimeMs(ChronoUnit.MILLIS.between(job.getStartedAt(), job.getCompletedAt()));
            job.setStatusMessage("Successfully processed " + total + " predictions");
            batchJobRepository.save(job);

            log.info("Batch prediction completed: {} - {} records", jobId, total);

        } catch (Exception e) {
            log.error("Batch processing failed for job: {}", jobId, e);
            updateBatchStatus(jobId, "FAILED", e.getMessage());
        }
    }

    private void updateBatchStatus(String jobId, String status, String message) {
        try {
            BatchPredictionJob job = batchJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.setStatus(status);
                job.setStatusMessage(message);
                job.setErrorMessage(message);
                batchJobRepository.save(job);
            }
        } catch (Exception e) {
            log.error("Failed to update batch status", e);
        }
    }

    // ========== HELPER METHODS ==========

    private static class PredictionResult {
        String predictedClass;
        double probability;
        double confidence;
        String riskLevel;
    }

    private PredictionResult simulatePrediction(Map<String, Object> features, Model model) {
        // Simulate realistic prediction based on features
        Random random = new Random();
        double baseProb = 0.5;

        // Adjust based on features
        if (features.containsKey("credit_score")) {
            Object val = features.get("credit_score");
            double score = val instanceof Number ? ((Number) val).doubleValue() : 650;
            baseProb -= (score - 650) / 500.0 * 0.3;  // Higher score = lower churn
        }
        if (features.containsKey("account_age")) {
            Object val = features.get("account_age");
            double age = val instanceof Number ? ((Number) val).doubleValue() : 12;
            baseProb -= (age - 12) / 60.0 * 0.2;  // Longer tenure = lower churn
        }
        if (features.containsKey("num_transactions")) {
            Object val = features.get("num_transactions");
            double trans = val instanceof Number ? ((Number) val).doubleValue() : 5;
            baseProb -= (trans - 5) / 20.0 * 0.15;  // More activity = lower churn
        }

        // Add some randomness
        double probability = Math.max(0.05, Math.min(0.95, baseProb + random.nextGaussian() * 0.1));
        double confidence = 0.7 + random.nextDouble() * 0.25;

        PredictionResult result = new PredictionResult();
        result.probability = probability;
        result.predictedClass = probability > 0.5 ? "Positive" : "Negative";
        result.confidence = confidence;
        result.riskLevel = getRiskLevel(probability);

        return result;
    }

    private String getRiskLevel(double probability) {
        if (probability >= 0.7) return "High Risk";
        if (probability >= 0.4) return "Medium Risk";
        return "Low Risk";
    }

    private String getRiskColor(String riskLevel) {
        if ("High Risk".equals(riskLevel)) return "red";
        if ("Medium Risk".equals(riskLevel)) return "yellow";
        return "green";
    }

    private String getConfidenceLabel(Double confidence) {
        if (confidence == null) return "Unknown";
        if (confidence >= 0.9) return "Very High Confidence";
        if (confidence >= 0.75) return "High Confidence";
        if (confidence >= 0.6) return "Moderate Confidence";
        return "Low Confidence";
    }

    private List<PredictionDTO.FeatureContribution> generateTopContributions(Map<String, Object> features) {
        List<PredictionDTO.FeatureContribution> contributions = new ArrayList<>();
        double[] impacts = {0.18, -0.12, 0.09, -0.07, 0.05};
        int i = 0;

        for (String feature : features.keySet()) {
            if (i >= impacts.length) break;
            double impact = impacts[i];

            contributions.add(PredictionDTO.FeatureContribution.builder()
                    .feature(feature)
                    .value(features.get(feature))
                    .contribution(impact)
                    .contributionLabel(String.format("%+.2f", impact))
                    .direction(impact > 0 ? "positive" : "negative")
                    .build());
            i++;
        }

        return contributions;
    }

    private String generateExplanation(PredictionResult result, List<PredictionDTO.FeatureContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return "No explanation available.";
        }

        PredictionDTO.FeatureContribution top = contributions.get(0);
        return String.format(
                "The model predicts %s with %.1f%% probability (%s). " +
                "The most influential factor is %s (value: %s), which %s the prediction by %.0f%%.",
                result.predictedClass,
                result.probability * 100,
                result.riskLevel,
                top.getFeature(),
                top.getValue(),
                top.getContribution() > 0 ? "increases" : "decreases",
                Math.abs(top.getContribution()) * 100
        );
    }

    private String saveUploadedFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(config.getTempDir(), fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return path.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded file", e);
        }
    }

    private int countCsvRecords(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int count = 0;
            reader.readLine();  // Skip header
            while (reader.readLine() != null) count++;
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    private List<Map<String, Object>> readCsvRecords(String filePath) {
        List<Map<String, Object>> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return records;

            String[] headers = headerLine.split(",");
            String line;

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, Object> record = new HashMap<>();

                for (int i = 0; i < headers.length && i < values.length; i++) {
                    String value = values[i].trim();
                    // Try to parse as number
                    try {
                        record.put(headers[i].trim(), Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        record.put(headers[i].trim(), value);
                    }
                }
                records.add(record);
            }
        } catch (IOException e) {
            log.error("Failed to read CSV file", e);
        }
        return records;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // ========== DTO CONVERTERS ==========

    private PredictionDTO.BatchResponse toBatchResponse(BatchPredictionJob job) {
        PredictionDTO.BatchSummary summary = null;

        if ("COMPLETED".equals(job.getStatus())) {
            summary = PredictionDTO.BatchSummary.builder()
                    .totalPredictions(job.getTotalRecords())
                    .successfulPredictions(job.getProcessedRecords())
                    .failedPredictions(job.getFailedRecords())
                    .positiveCount(job.getPositiveCount())
                    .negativeCount(job.getNegativeCount())
                    .positivePercentage(job.getPositiveCount() != null && job.getTotalRecords() > 0
                            ? (job.getPositiveCount() * 100.0 / job.getTotalRecords()) : 0)
                    .negativePercentage(job.getNegativeCount() != null && job.getTotalRecords() > 0
                            ? (job.getNegativeCount() * 100.0 / job.getTotalRecords()) : 0)
                    .avgConfidence(job.getAvgConfidence())
                    .avgConfidenceLabel(String.format("%.1f%%", job.getAvgConfidence() != null ? job.getAvgConfidence() * 100 : 0))
                    .build();
        }

        return PredictionDTO.BatchResponse.builder()
                .jobId(job.getId())
                .jobName(job.getJobName())
                .modelId(job.getModelId())
                .modelName(job.getModelName())
                .status(job.getStatus())
                .statusLabel(job.getStatus())
                .statusMessage(job.getStatusMessage())
                .totalRecords(job.getTotalRecords())
                .processedRecords(job.getProcessedRecords())
                .failedRecords(job.getFailedRecords())
                .progress(job.getProgress())
                .progressLabel(String.format("%,d / %,d", job.getProcessedRecords(), job.getTotalRecords()))
                .inputFileName(job.getInputFileName())
                .outputFileName(job.getOutputFileName())
                .downloadUrl(job.getOutputFilePath() != null ? "/api/predictions/batch/" + job.getId() + "/download" : null)
                .summary(summary)
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .processingTimeMs(job.getProcessingTimeMs())
                .processingTimeLabel(formatDuration(job.getProcessingTimeMs()))
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private PredictionDTO.BatchListItem toBatchListItem(BatchPredictionJob job) {
        return PredictionDTO.BatchListItem.builder()
                .jobId(job.getId())
                .jobName(job.getJobName())
                .modelName(job.getModelName())
                .status(job.getStatus())
                .statusLabel(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .progress(job.getProgress())
                .progressLabel(String.format("%d%%", job.getProgress()))
                .createdAt(job.getCreatedAt())
                .createdAtLabel(formatTimeAgo(job.getCreatedAt()))
                .build();
    }

    private PredictionDTO.HistoryItem toHistoryItem(Prediction prediction) {
        return PredictionDTO.HistoryItem.builder()
                .predictionId(prediction.getId())
                .modelId(prediction.getModelId())
                .modelName(prediction.getModelName())
                .predictionType(prediction.getPredictionType())
                .predictedClass(prediction.getPredictedClass())
                .probability(prediction.getProbability())
                .probabilityLabel(String.format("%.1f%%", prediction.getProbability() * 100))
                .riskLevel(prediction.getRiskLevel())
                .riskColor(getRiskColor(prediction.getRiskLevel()))
                .timestamp(prediction.getCreatedAt())
                .timestampLabel(formatTimeAgo(prediction.getCreatedAt()))
                .batchId(prediction.getBatchId())
                .build();
    }

    private String formatDuration(Long millis) {
        if (millis == null) return null;
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + " seconds";
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + " min " + seconds + " sec";
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}
