package com.mlengine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Activity entity - tracks all user activities for activity feed.
 */
@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activity_project", columnList = "project_id"),
    @Index(name = "idx_activity_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Activity type
    @Column(name = "activity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    // Title and description
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    // User who performed the action
    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_email")
    private String userEmail;

    // Related entity info
    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "entity_type")
    private String entityType;  // "MODEL", "DATASET", "TRAINING_JOB", "PREDICTION"

    @Column(name = "entity_name")
    private String entityName;

    // Additional metadata (JSON)
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    // Project association
    @Column(name = "project_id")
    private String projectId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Activity types enum.
     */
    public enum ActivityType {
        // Model activities
        MODEL_CREATED,
        MODEL_DEPLOYED,
        MODEL_UNDEPLOYED,
        MODEL_DELETED,
        
        // Dataset activities
        DATASET_UPLOADED,
        DATASET_DELETED,
        
        // Training activities
        TRAINING_STARTED,
        TRAINING_COMPLETED,
        TRAINING_FAILED,
        TRAINING_STOPPED,
        
        // Prediction activities
        PREDICTION_SINGLE,
        PREDICTION_BATCH_STARTED,
        PREDICTION_BATCH_COMPLETED,
        
        // Project activities
        PROJECT_CREATED,
        PROJECT_UPDATED,
        
        // Data source activities
        DATASOURCE_CONNECTED,
        DATASOURCE_DISCONNECTED
    }
}
