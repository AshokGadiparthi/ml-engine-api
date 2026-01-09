package com.mlengine.model.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Prediction operations.
 * Matches React UI Predictions screens.
 */
public class PredictionDTO {

    // ========== SINGLE PREDICTION ==========

    /**
     * Single prediction request.
     * Matches React UI Single Prediction form.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleRequest {
        @NotBlank(message = "Model ID is required")
        private String modelId;

        @NotNull(message = "Features are required")
        private Map<String, Object> features;

        private Boolean includeExplanation;  // Include SHAP values
        private String projectId;
    }

    /**
     * Single prediction response.
     * Matches React UI Prediction Result card.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleResponse {
        private String predictionId;
        private String modelId;
        private String modelName;

        // Main prediction
        private String predictedClass;      // "Positive", "Negative", "Churn", "No Churn"
        private String predictedLabel;      // Display label
        private Double probability;
        private String probabilityLabel;    // "78.5%"
        private Double confidence;
        private String confidenceLabel;     // "High Confidence"

        // Risk level (for classification)
        private String riskLevel;           // "High Risk", "Medium Risk", "Low Risk"
        private String riskColor;           // "red", "yellow", "green"

        // Regression result
        private Double predictedValue;      // For regression
        private String predictedValueLabel;

        // Input features echo
        private Map<String, Object> inputFeatures;

        // Optional explanation
        private List<FeatureContribution> topContributions;
        private String explanation;

        // Metadata
        private Long processingTimeMs;
        private LocalDateTime timestamp;
    }

    /**
     * Feature contribution for explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureContribution {
        private String feature;
        private Object value;
        private Double contribution;
        private String contributionLabel;  // "+0.15" or "-0.08"
        private String direction;          // "positive" or "negative"
    }

    // ========== BATCH PREDICTION ==========

    /**
     * Batch prediction request.
     * File is uploaded separately via multipart.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRequest {
        @NotBlank(message = "Model ID is required")
        private String modelId;

        private String jobName;
        private Boolean includeExplanations;
        private String projectId;
    }

    /**
     * Batch prediction job response.
     * Matches React UI Batch Prediction status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchResponse {
        private String jobId;
        private String jobName;
        private String modelId;
        private String modelName;

        // Status
        private String status;          // "QUEUED", "PROCESSING", "COMPLETED", "FAILED"
        private String statusLabel;
        private String statusMessage;

        // Progress
        private Integer totalRecords;
        private Integer processedRecords;
        private Integer failedRecords;
        private Integer progress;       // 0-100
        private String progressLabel;   // "1,234 / 5,000"

        // Files
        private String inputFileName;
        private String outputFileName;
        private String downloadUrl;

        // Results summary (when completed)
        private BatchSummary summary;

        // Timing
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long processingTimeMs;
        private String processingTimeLabel;  // "2 minutes 34 seconds"
        private String etaLabel;             // "~5 minutes remaining"

        // Error
        private String errorMessage;

        // Timestamps
        private LocalDateTime createdAt;
    }

    /**
     * Batch prediction summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSummary {
        private Integer totalPredictions;
        private Integer successfulPredictions;
        private Integer failedPredictions;

        // Classification breakdown
        private Integer positiveCount;
        private Integer negativeCount;
        private Double positivePercentage;
        private Double negativePercentage;

        // Confidence
        private Double avgConfidence;
        private String avgConfidenceLabel;
        private Double minConfidence;
        private Double maxConfidence;

        // Risk distribution
        private Integer highRiskCount;
        private Integer mediumRiskCount;
        private Integer lowRiskCount;
    }

    /**
     * Batch job list item.
     */
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
        private String createdAtLabel;  // "2h ago"
    }

    // ========== PREDICTION HISTORY ==========

    /**
     * Prediction history item.
     * Matches React UI Prediction History table.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String predictionId;
        private String modelId;
        private String modelName;
        private String predictionType;  // "single", "batch"
        private String predictedClass;
        private Double probability;
        private String probabilityLabel;
        private String riskLevel;
        private String riskColor;
        private LocalDateTime timestamp;
        private String timestampLabel;  // "2 hours ago"

        // For batch
        private String batchId;
        private Integer batchSize;
    }

    /**
     * Prediction history response with pagination.
     */
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

        // Quick stats
        private Long totalPredictions;
        private Long todayPredictions;
        private Double avgConfidence;
    }

    // ========== REALTIME PREDICTION ==========

    /**
     * Realtime prediction for API endpoint.
     */
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
        private Double probability;
        private Double confidence;
        private Map<String, Double> classProbabilities;
        private Long latencyMs;
    }
}
