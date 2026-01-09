package com.mlengine.model.dto;

import com.mlengine.model.enums.JobStatus;
import com.mlengine.model.enums.ProblemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for AutoML operations.
 */
public class AutoMLDTO {

    /**
     * Request to start a new AutoML job.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartRequest {
        private String projectId;

        @NotBlank(message = "Dataset ID is required")
        private String datasetId;

        private String name;

        private String description;

        @NotBlank(message = "Target column is required")
        private String targetColumn;

        @NotNull(message = "Problem type is required")
        private ProblemType problemType;

        @Builder.Default
        private Integer maxTrainingTimeMinutes = 60;

        @Builder.Default
        private String accuracyVsSpeed = "medium"; // low, medium, high

        @Builder.Default
        private String interpretability = "medium"; // low, medium, high

        private AutoMLConfig config;
    }

    /**
     * AutoML configuration options.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutoMLConfig {
        @Builder.Default
        private Boolean enableFeatureEngineering = true;

        private String scalingMethod; // standard, minmax, robust

        private Integer polynomialDegree;

        private Integer selectFeatures;

        @Builder.Default
        private Integer cvFolds = 5;

        @Builder.Default
        private Boolean enableExplainability = false;

        @Builder.Default
        private Boolean enableHyperparameterTuning = false;

        private String tuningMethod; // grid, random
    }

    /**
     * AutoML job response (basic info).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobResponse {
        private String jobId;
        private String projectId;
        private String datasetId;
        private String datasetName;
        private String name;
        private String description;
        private String targetColumn;
        private ProblemType problemType;
        private JobStatus status;
        private String statusLabel;
        private Integer maxTrainingTimeMinutes;
        private String accuracyVsSpeed;
        private String interpretability;
        private AutoMLConfig config;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String message;
    }

    /**
     * AutoML job status/progress response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressResponse {
        private String jobId;
        private String name;
        private JobStatus status;
        private String statusLabel;
        private Integer progress;
        private String currentPhase;
        private String currentAlgorithm;
        private List<PhaseInfo> phases;
        private Integer algorithmsCompleted;
        private Integer algorithmsTotal;
        private Double currentBestScore;
        private String currentBestAlgorithm;
        private Long elapsedTimeSeconds;
        private Long estimatedRemainingSeconds;
        private List<LogEntry> logs;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorMessage;
    }

    /**
     * Phase information for progress tracking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseInfo {
        private String name;
        private String label;
        private String status; // PENDING, RUNNING, COMPLETED, FAILED
        private Integer progress;
        private String message;
    }

    /**
     * Log entry for progress tracking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private LocalDateTime timestamp;
        private String level; // INFO, WARN, ERROR
        private String message;
    }

    /**
     * AutoML results response (after completion).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultsResponse {
        private String jobId;
        private String name;
        private JobStatus status;
        private ProblemType problemType;
        private String targetColumn;
        private DatasetInfo datasetInfo;
        private FeatureEngineeringInfo featureEngineering;
        private List<LeaderboardEntry> leaderboard;
        private BestModelInfo bestModel;
        private List<FeatureImportanceEntry> featureImportance;
        private String comparisonCsvPath;
        private Long totalTrainingTimeSeconds;
        private LocalDateTime completedAt;
        
        // Deployment info
        private Boolean isDeployed;
        private String deploymentId;
        private String deployedModelId;
        private String deploymentEndpoint;
        private LocalDateTime deployedAt;
        private Integer deploymentVersion;
        private String deploymentVersionLabel;
        private Boolean isActiveDeployment;
    }

    /**
     * Dataset info for results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetInfo {
        private String datasetId;
        private String datasetName;
        private Long totalRows;
        private Integer totalFeatures;
        private Long trainSize;
        private Long testSize;
    }

    /**
     * Feature engineering info for results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureEngineeringInfo {
        private Boolean enabled;
        private String scalingMethod;
        private Integer originalFeatures;
        private Integer engineeredFeatures;
        private List<String> featuresUsed;
    }

    /**
     * Leaderboard entry for algorithm comparison.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private Integer rank;
        private String modelId;
        private String algorithm;
        private Double accuracy;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private Double auc;
        private Double r2; // for regression
        private Double mae; // for regression
        private Double rmse; // for regression
        private Double cvScore;
        private Double cvStd;
        private Long trainingTimeSeconds;
    }

    /**
     * Best model info.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestModelInfo {
        private String modelId;
        private String algorithm;
        private Double score;
        private String metric;
        private String modelPath;
        private String featureEngineerPath;
        private String featureNamesPath;
    }

    /**
     * Feature importance entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureImportanceEntry {
        private String feature;
        private Double importance;
        private Integer rank;
    }

    /**
     * AutoML job list item (for listing jobs).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String jobId;
        private String name;
        private String projectId;
        private String datasetId;
        private String datasetName;
        private ProblemType problemType;
        private JobStatus status;
        private String statusLabel;
        private String bestAlgorithm;
        private Double bestScore;
        private Integer algorithmsCount;
        private Long elapsedTimeSeconds;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        
        // Deployment info
        private Boolean isDeployed;
        private String deploymentId;
        private String deployedModelId;
        private String deploymentEndpoint;
        private LocalDateTime deployedAt;
        private Integer deploymentVersion;
        private String deploymentVersionLabel;
        private Boolean isActiveDeployment; // Is this the currently active deployment?
    }

    /**
     * Stop job response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopResponse {
        private String jobId;
        private JobStatus status;
        private String message;
        private Integer algorithmsCompleted;
        private Double bestScoreAchieved;
        private LocalDateTime stoppedAt;
    }

    /**
     * Deploy request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeployRequest {
        private String modelId;
        private String deploymentName;
        private String description;
    }

    /**
     * Deploy response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeployResponse {
        private String deploymentId;
        private String modelId;
        private String name;
        private String status;
        private String endpoint;
        private LocalDateTime deployedAt;
        
        // New versioning fields
        private Integer version;
        private String versionLabel;
        private String algorithm;
        private Double score;
        private String scoreFormatted;
        private String message;
    }

    /**
     * Paginated list response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse {
        private List<ListItem> content;
        private Long totalElements;
        private Integer totalPages;
        private Integer page;
        private Integer size;
    }
}
