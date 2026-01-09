package com.mlengine.model.dto;

import com.mlengine.model.entity.Activity.ActivityType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Activity operations.
 */
public class ActivityDTO {

    /**
     * Activity response item.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private ActivityType activityType;
        private String activityTypeLabel;   // Human-readable type
        private String icon;                 // Icon name for UI
        private String iconColor;            // Icon color
        
        private String title;
        private String description;
        
        private String userName;
        private String userEmail;
        private String userInitials;         // "JS" for "Jane Smith"
        
        private String entityId;
        private String entityType;
        private String entityName;
        
        private String projectId;
        
        private LocalDateTime createdAt;
        private String createdAtLabel;       // "2h ago"
    }

    /**
     * Activity list response with pagination.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> activities;
        private Integer total;
        private Integer page;
        private Integer pageSize;
        private Integer totalPages;
    }

    /**
     * Activity stats for dashboard.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private Long totalActivities;
        private Long todayActivities;
        private Long thisWeekActivities;
        private Long modelDeployments;
        private Long datasetsUploaded;
        private Long trainingCompleted;
        private Long predictionsProcessed;
    }
}
