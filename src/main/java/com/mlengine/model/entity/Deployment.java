package com.mlengine.model.entity;

import com.mlengine.model.enums.DeploymentStatus;
import com.mlengine.model.enums.ProblemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Deployment entity - tracks model deployments with versioning.
 * Supports one active deployment per project with full history.
 */
@Entity
@Table(name = "deployments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deployment {

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private Model model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "automl_job_id")
    private AutoMLJob autoMLJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_job_id")
    private TrainingJob trainingJob;

    // Versioning - auto-incremented per project
    @Column(nullable = false)
    private Integer version;

    @Column(name = "version_label")
    private String versionLabel; // "v1", "v2", etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeploymentStatus status = DeploymentStatus.PENDING;

    // Model info (denormalized for quick access)
    private String algorithm;

    private Double score;

    private String metric; // "Accuracy" or "R²"

    @Enumerated(EnumType.STRING)
    private ProblemType problemType;

    private String targetColumn;

    private String datasetName;

    // Endpoint info
    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column(name = "endpoint_path")
    private String endpointPath; // /api/predictions/realtime/{modelId}

    // Timestamps
    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    // Stats
    @Column(name = "predictions_count")
    @Builder.Default
    private Long predictionsCount = 0L;

    @Column(name = "last_prediction_at")
    private LocalDateTime lastPredictionAt;

    // Metadata
    @Column(name = "deployed_by")
    private String deployedBy;

    @Column(name = "deactivated_by")
    private String deactivatedBy;

    @Column(name = "deactivation_reason")
    private String deactivationReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
