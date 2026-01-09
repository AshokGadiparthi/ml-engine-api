package com.mlengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.model.dto.ActivityDTO;
import com.mlengine.model.entity.Activity;
import com.mlengine.model.entity.Activity.ActivityType;
import com.mlengine.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Activity operations.
 * Tracks and retrieves user activities for activity feed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    // ========== RECORD ACTIVITIES ==========

    /**
     * Record a new activity.
     */
    @Transactional
    public Activity recordActivity(
            ActivityType type,
            String title,
            String description,
            String userName,
            String userEmail,
            String entityId,
            String entityType,
            String entityName,
            String projectId,
            Map<String, Object> metadata) {

        Activity activity = Activity.builder()
                .activityType(type)
                .title(title)
                .description(description)
                .userName(userName != null ? userName : "System")
                .userEmail(userEmail)
                .entityId(entityId)
                .entityType(entityType)
                .entityName(entityName)
                .projectId(projectId)
                .metadataJson(toJson(metadata))
                .build();

        activity = activityRepository.save(activity);
        log.info("Recorded activity: {} - {} by {}", type, title, userName);

        return activity;
    }

    // Convenience methods for common activities

    public void recordModelDeployed(String modelId, String modelName, String userName, String projectId) {
        recordActivity(
                ActivityType.MODEL_DEPLOYED,
                "Model deployed to production",
                modelName,
                userName, null,
                modelId, "MODEL", modelName,
                projectId, null);
    }

    public void recordModelCreated(String modelId, String modelName, String accuracy, String userName, String projectId) {
        recordActivity(
                ActivityType.MODEL_CREATED,
                "New model created",
                modelName + " - " + accuracy + " accuracy",
                userName, null,
                modelId, "MODEL", modelName,
                projectId, Map.of("accuracy", accuracy));
    }

    public void recordDatasetUploaded(String datasetId, String datasetName, String fileSize, String userName, String projectId) {
        recordActivity(
                ActivityType.DATASET_UPLOADED,
                "New dataset uploaded",
                datasetName + " (" + fileSize + ")",
                userName, null,
                datasetId, "DATASET", datasetName,
                projectId, Map.of("fileSize", fileSize));
    }

    public void recordTrainingStarted(String jobId, String jobName, String algorithm, String userName, String projectId) {
        recordActivity(
                ActivityType.TRAINING_STARTED,
                "Model training started",
                jobName + " using " + algorithm,
                userName, null,
                jobId, "TRAINING_JOB", jobName,
                projectId, Map.of("algorithm", algorithm));
    }

    public void recordTrainingCompleted(String jobId, String jobName, String accuracy, String userName, String projectId) {
        recordActivity(
                ActivityType.TRAINING_COMPLETED,
                "Model training completed",
                jobName + " - " + accuracy + " accuracy",
                userName, null,
                jobId, "TRAINING_JOB", jobName,
                projectId, Map.of("accuracy", accuracy));
    }

    public void recordTrainingFailed(String jobId, String jobName, String error, String userName, String projectId) {
        recordActivity(
                ActivityType.TRAINING_FAILED,
                "Model training failed",
                jobName + " - " + error,
                userName, null,
                jobId, "TRAINING_JOB", jobName,
                projectId, Map.of("error", error));
    }

    public void recordBatchPredictionCompleted(String jobId, String jobName, int count, String userName, String projectId) {
        recordActivity(
                ActivityType.PREDICTION_BATCH_COMPLETED,
                "Predictions batch processed",
                String.format("%,d predictions completed", count),
                userName, null,
                jobId, "BATCH_PREDICTION", jobName,
                projectId, Map.of("count", count));
    }

    public void recordProjectCreated(String projectId, String projectName, String userName) {
        recordActivity(
                ActivityType.PROJECT_CREATED,
                "Project created",
                projectName,
                userName, null,
                projectId, "PROJECT", projectName,
                projectId, null);
    }

    // ========== RETRIEVE ACTIVITIES ==========

    /**
     * Get recent activities for a project.
     */
    public List<ActivityDTO.Response> getRecentActivities(String projectId, int limit) {
        List<Activity> activities;

        if (projectId != null) {
            activities = activityRepository.findTop10ByProjectIdOrderByCreatedAtDesc(projectId);
        } else {
            activities = activityRepository.findTop10ByOrderByCreatedAtDesc();
        }

        return activities.stream()
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get activities with pagination.
     */
    public ActivityDTO.ListResponse getActivities(String projectId, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);

        Page<Activity> activities = projectId != null
                ? activityRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageRequest)
                : activityRepository.findAllByOrderByCreatedAtDesc(pageRequest);

        List<ActivityDTO.Response> items = activities.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ActivityDTO.ListResponse.builder()
                .activities(items)
                .total((int) activities.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .totalPages(activities.getTotalPages())
                .build();
    }

    /**
     * Get activities by type.
     */
    public List<ActivityDTO.Response> getActivitiesByType(String projectId, ActivityType type) {
        List<Activity> activities = activityRepository
                .findByProjectIdAndActivityTypeOrderByCreatedAtDesc(projectId, type);

        return activities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get activity stats.
     */
    public ActivityDTO.Stats getActivityStats(String projectId) {
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime weekAgo = today.minusDays(7);

        Long total = projectId != null
                ? activityRepository.countByProject(projectId)
                : activityRepository.count();

        Long todayCount = projectId != null
                ? activityRepository.countByProjectSince(projectId, today)
                : activityRepository.countSince(today);

        Long weekCount = projectId != null
                ? activityRepository.countByProjectSince(projectId, weekAgo)
                : activityRepository.countSince(weekAgo);

        return ActivityDTO.Stats.builder()
                .totalActivities(total)
                .todayActivities(todayCount)
                .thisWeekActivities(weekCount)
                .modelDeployments(activityRepository.countByTypeSince(ActivityType.MODEL_DEPLOYED, weekAgo))
                .datasetsUploaded(activityRepository.countByTypeSince(ActivityType.DATASET_UPLOADED, weekAgo))
                .trainingCompleted(activityRepository.countByTypeSince(ActivityType.TRAINING_COMPLETED, weekAgo))
                .predictionsProcessed(activityRepository.countByTypeSince(ActivityType.PREDICTION_BATCH_COMPLETED, weekAgo))
                .build();
    }

    // ========== HELPERS ==========

    private ActivityDTO.Response toResponse(Activity activity) {
        return ActivityDTO.Response.builder()
                .id(activity.getId())
                .activityType(activity.getActivityType())
                .activityTypeLabel(getActivityTypeLabel(activity.getActivityType()))
                .icon(getActivityIcon(activity.getActivityType()))
                .iconColor(getActivityIconColor(activity.getActivityType()))
                .title(activity.getTitle())
                .description(activity.getDescription())
                .userName(activity.getUserName())
                .userEmail(activity.getUserEmail())
                .userInitials(getInitials(activity.getUserName()))
                .entityId(activity.getEntityId())
                .entityType(activity.getEntityType())
                .entityName(activity.getEntityName())
                .projectId(activity.getProjectId())
                .createdAt(activity.getCreatedAt())
                .createdAtLabel(formatTimeAgo(activity.getCreatedAt()))
                .build();
    }

    private String getActivityTypeLabel(ActivityType type) {
        return switch (type) {
            case MODEL_DEPLOYED -> "Model Deployed";
            case MODEL_CREATED -> "Model Created";
            case MODEL_UNDEPLOYED -> "Model Undeployed";
            case MODEL_DELETED -> "Model Deleted";
            case DATASET_UPLOADED -> "Dataset Uploaded";
            case DATASET_DELETED -> "Dataset Deleted";
            case TRAINING_STARTED -> "Training Started";
            case TRAINING_COMPLETED -> "Training Completed";
            case TRAINING_FAILED -> "Training Failed";
            case TRAINING_STOPPED -> "Training Stopped";
            case PREDICTION_SINGLE -> "Prediction Made";
            case PREDICTION_BATCH_STARTED -> "Batch Started";
            case PREDICTION_BATCH_COMPLETED -> "Batch Completed";
            case PROJECT_CREATED -> "Project Created";
            case PROJECT_UPDATED -> "Project Updated";
            case DATASOURCE_CONNECTED -> "Data Source Connected";
            case DATASOURCE_DISCONNECTED -> "Data Source Disconnected";
        };
    }

    private String getActivityIcon(ActivityType type) {
        return switch (type) {
            case MODEL_DEPLOYED, MODEL_UNDEPLOYED -> "rocket";
            case MODEL_CREATED -> "cube";
            case MODEL_DELETED -> "trash";
            case DATASET_UPLOADED -> "upload";
            case DATASET_DELETED -> "trash";
            case TRAINING_STARTED, TRAINING_COMPLETED -> "cog";
            case TRAINING_FAILED -> "exclamation-circle";
            case TRAINING_STOPPED -> "stop";
            case PREDICTION_SINGLE, PREDICTION_BATCH_STARTED, PREDICTION_BATCH_COMPLETED -> "chart-line";
            case PROJECT_CREATED, PROJECT_UPDATED -> "folder";
            case DATASOURCE_CONNECTED -> "plug";
            case DATASOURCE_DISCONNECTED -> "plug";
        };
    }

    private String getActivityIconColor(ActivityType type) {
        return switch (type) {
            case MODEL_DEPLOYED -> "green";
            case MODEL_CREATED -> "blue";
            case MODEL_UNDEPLOYED, MODEL_DELETED, DATASET_DELETED -> "gray";
            case DATASET_UPLOADED -> "purple";
            case TRAINING_STARTED -> "blue";
            case TRAINING_COMPLETED -> "green";
            case TRAINING_FAILED -> "red";
            case TRAINING_STOPPED -> "yellow";
            case PREDICTION_SINGLE, PREDICTION_BATCH_STARTED, PREDICTION_BATCH_COMPLETED -> "cyan";
            case PROJECT_CREATED, PROJECT_UPDATED -> "indigo";
            case DATASOURCE_CONNECTED -> "green";
            case DATASOURCE_DISCONNECTED -> "gray";
        };
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 7) return days + "d ago";
        long weeks = days / 7;
        return weeks + "w ago";
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
