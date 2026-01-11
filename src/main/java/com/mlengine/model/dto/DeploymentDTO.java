package com.mlengine.model.dto;

import com.mlengine.model.enums.DeploymentStatus;
import com.mlengine.model.enums.ProblemType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Deployment operations.
 */
public class DeploymentDTO {

    /**
     * Request to create a new deployment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String projectId;

        @NotBlank(message = "Model ID or AutoML Job ID is required")
        private String modelId;

        private String autoMLJobId; // Alternative: deploy from AutoML job

        private String name; // Optional, auto-generated if not provided

        private String description;

        private String deployedBy; // User identifier
    }

    /**
     * Request to deploy from AutoML job.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeployFromAutoMLRequest {
        @NotBlank(message = "AutoML Job ID is required")
        private String autoMLJobId;

        private String name;

        private String description;

        private String deployedBy;
    }

    /**
     * Request to deploy from Training job.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeployFromTrainingRequest {
        @NotBlank(message = "Training Job ID is required")
        private String trainingJobId;

        private String name;

        private String description;

        private String deployedBy;
    }

    /**
     * Request to rollback/activate a specific version.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackRequest {
        private String reason;
        private String activatedBy;
    }

    /**
     * Request to deactivate a deployment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeactivateRequest {
        private String reason;
        private String deactivatedBy;
    }

    /**
     * Deployment response (full details).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String description;
        private String projectId;
        private String projectName;
        private String modelId;
        private String autoMLJobId;
        private String trainingJobId;  // For models from manual training

        // Versioning
        private Integer version;
        private String versionLabel;

        // Status
        private DeploymentStatus status;
        private String statusLabel;
        private Boolean isActive;

        // Model info
        private String algorithm;
        private Double score;
        private String metric;
        private String scoreFormatted; // "93.1%" or "R² = 0.89"
        private ProblemType problemType;
        private String targetColumn;
        private String datasetName;

        // Endpoint
        private String endpointUrl;
        private String endpointPath;

        // Timestamps
        private LocalDateTime deployedAt;
        private LocalDateTime activatedAt;
        private LocalDateTime deactivatedAt;
        private LocalDateTime createdAt;

        // Stats
        private Long predictionsCount;
        private LocalDateTime lastPredictionAt;

        // Metadata
        private String deployedBy;
        private String deactivatedBy;
        private String deactivationReason;

        // Message
        private String message;
    }

    /**
     * Deployment list item (for history grid).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String id;
        private String name;
        private String projectId;

        // Versioning
        private Integer version;
        private String versionLabel;

        // Status
        private DeploymentStatus status;
        private String statusLabel;
        private Boolean isActive;

        // Model info
        private String algorithm;
        private Double score;
        private String metric;
        private String scoreFormatted;
        private ProblemType problemType;

        // Source
        private String autoMLJobId;
        private String autoMLJobName;
        private String trainingJobId;   // For models from manual training
        private String trainingJobName;
        private String source;          // "AUTOML" or "TRAINING"

        // Endpoint
        private String endpointPath;

        // Timestamps
        private LocalDateTime deployedAt;
        private LocalDateTime deactivatedAt;

        // Stats
        private Long predictionsCount;

        // User
        private String deployedBy;
    }

    /**
     * Active deployment summary (for dashboard/header).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveSummary {
        private String id;
        private String name;
        private Integer version;
        private String versionLabel;
        private String algorithm;
        private Double score;
        private String scoreFormatted;
        private String endpointPath;
        private LocalDateTime deployedAt;
        private Long predictionsCount;
        private Boolean hasActiveDeployment;
    }

    /**
     * Deployment history response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryResponse {
        private String projectId;
        private String projectName;
        private Integer totalDeployments;
        private ActiveSummary activeDeployment;
        private List<ListItem> history;
    }

    /**
     * Paginated deployment list.
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

    /**
     * Comparison between two deployments.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompareResponse {
        private ListItem deployment1;
        private ListItem deployment2;
        private Double scoreDifference;
        private String recommendation; // "v3 has 2.1% better accuracy"
    }
}
