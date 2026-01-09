package com.mlengine.model.dto;

import com.mlengine.model.enums.JobStatus;
import com.mlengine.model.enums.ProblemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTOs for Training Job operations.
 * Matches React UI Model Training screens.
 */
public class TrainingJobDTO {

    /**
     * Request to start a new training job.
     * Matches React UI's Training Configuration form.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        // Basic Configuration
        private String experimentName;  // e.g., "Customer Churn Prediction v1"

        @NotBlank(message = "Dataset ID is required")
        private String datasetId;

        @NotBlank(message = "Algorithm is required")
        private String algorithm;  // e.g., "xgboost"

        @NotBlank(message = "Target variable is required")
        private String targetVariable;  // e.g., "churn"

        @NotNull(message = "Problem type is required")
        private ProblemType problemType;

        @Builder.Default
        private Double trainTestSplit = 0.8;  // 80/20

        @Builder.Default
        private Integer crossValidationFolds = 5;

        private String projectId;

        // Advanced Configuration (hyperparameters)
        private Map<String, Object> hyperparameters;

        // Optimization Configuration
        @Builder.Default
        private Boolean gpuAcceleration = false;

        @Builder.Default
        private Boolean autoHyperparameterTuning = false;

        @Builder.Default
        private Boolean earlyStopping = true;

        @Builder.Default
        private Integer earlyStoppingPatience = 10;

        @Builder.Default
        private Integer batchSize = 32;

        @Builder.Default
        private String evaluationMetric = "accuracy";
    }

    /**
     * Training job response - matches React UI Training Jobs list.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String jobName;
        private String experimentName;

        // Status
        private JobStatus status;
        private String statusLabel;  // "Training", "Completed", etc.
        private String statusMessage;

        // Progress
        private Integer progress;  // 0-100
        private String progressLabel;  // "67/100"
        private Integer currentEpoch;
        private Integer totalEpochs;
        private Double currentAccuracy;
        private String currentAccuracyLabel;  // "94.2%"
        private Double bestAccuracy;
        private Double currentLoss;

        // Configuration
        private String datasetId;
        private String datasetName;
        private String algorithm;
        private String algorithmDisplayName;
        private String targetVariable;
        private ProblemType problemType;
        private Double trainTestSplit;
        private Integer crossValidationFolds;
        private Map<String, Object> hyperparameters;

        // Optimization
        private Boolean gpuAcceleration;
        private Boolean autoHyperparameterTuning;
        private Boolean earlyStopping;
        private Integer earlyStoppingPatience;
        private Integer batchSize;
        private String evaluationMetric;

        // Timing
        private LocalDateTime startedAt;
        private String startedAtLabel;  // "2026-01-08 10:23"
        private LocalDateTime completedAt;
        private Long etaSeconds;
        private String etaLabel;  // "15 min"
        private Long durationSeconds;
        private String durationLabel;  // "23 minutes"

        // Results
        private String modelId;
        private Map<String, Object> metrics;

        // Resources
        private String computeResources;  // "4x GPU"
        private Double costEstimate;
        private String costLabel;  // "$0.42"

        // Error
        private String errorMessage;

        // Project
        private String projectId;

        // Timestamps
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Training job list item - lighter response for lists.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String id;
        private String jobName;
        private String algorithm;
        private String algorithmDisplayName;
        private String datasetName;
        private JobStatus status;
        private String statusLabel;
        private Integer progress;
        private String progressLabel;
        private Double currentAccuracy;
        private String currentAccuracyLabel;
        private LocalDateTime startedAt;
        private String startedAtLabel;
        private String etaLabel;
    }

    /**
     * Training progress update - for real-time updates.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressUpdate {
        private String jobId;
        private JobStatus status;
        private Integer progress;
        private Integer currentEpoch;
        private Integer totalEpochs;
        private Double currentAccuracy;
        private Double currentLoss;
        private Long etaSeconds;
        private String etaLabel;
        private String message;
    }

    /**
     * Training metrics - intermediate and final metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private String jobId;
        private Double accuracy;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private Double aucRoc;
        private Double loss;
        private Integer epoch;
        private LocalDateTime timestamp;
    }
}
