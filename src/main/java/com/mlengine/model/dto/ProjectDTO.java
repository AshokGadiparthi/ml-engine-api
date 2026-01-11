package com.mlengine.model.dto;

import com.mlengine.model.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTOs for Project operations.
 */
public class ProjectDTO {

    /**
     * Request to create a new project.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Project name is required")
        @Size(max = 100, message = "Name must be less than 100 characters")
        private String name;

        @Size(max = 1000, message = "Description must be less than 1000 characters")
        private String description;

        private String iconUrl;
        private String color;
        private String ownerEmail;
    }

    /**
     * Request to update a project.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 100, message = "Name must be less than 100 characters")
        private String name;

        @Size(max = 1000, message = "Description must be less than 1000 characters")
        private String description;

        private String iconUrl;
        private String color;
        private Integer teamMembers;
        private ProjectStatus status;
    }

    /**
     * Project response - matches React UI Dashboard.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String description;
        private String iconUrl;
        private String color;
        private ProjectStatus status;
        private String ownerEmail;
        private Integer teamMembers;

        // Statistics for Dashboard
        private Integer modelsCount;
        private Integer deployedModelsCount;
        private Integer datasetsCount;
        private Integer dataSourcesCount;
        private String totalDataSize;  // Formatted: "2.4 GB"
        private Long totalDataSizeBytes;
        private Double avgAccuracy;
        private Double accuracyTrend;  // e.g., +2.3
        private Long predictionsCount;
        private Long predictionsThisMonth;

        // Timestamps
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Project list item (lighter response for lists).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String id;
        private String name;
        private String description;
        private String iconUrl;
        private String color;
        private ProjectStatus status;
        private Integer modelsCount;
        private Integer datasetsCount;
        private Integer dataSourcesCount;
        private LocalDateTime updatedAt;
    }

    /**
     * Dashboard statistics response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        // Models
        private Integer modelsCount;
        private Integer deployedModelsCount;

        // Datasets
        private Integer datasetsCount;
        private String totalDataSize;
        private Long totalDataSizeBytes;
        
        // Data Sources
        private Integer dataSourcesCount;

        // Accuracy
        private Double avgAccuracy;
        private Double accuracyTrend;
        private String accuracyTrendLabel;  // "+2.3% from last week"

        // Predictions
        private Long predictionsCount;
        private Long predictionsThisMonth;
        private String predictionsLabel;  // "This month"

        // Data quality
        private Double dataQualityScore;
    }
}
