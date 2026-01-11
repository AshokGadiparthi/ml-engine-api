package com.mlengine.model.entity;

import com.mlengine.model.enums.JobStatus;
import com.mlengine.model.enums.ProblemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * TrainingJob entity - represents a model training job.
 * Maps to React UI's Training Jobs section.
 */
@Entity
@Table(name = "training_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Job identification
    @Column(name = "job_name", nullable = false)
    private String jobName;  // e.g., "Churn Prediction XGBoost"

    @Column(name = "experiment_name")
    private String experimentName;  // e.g., "Customer Churn Prediction v1"

    // Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JobStatus status = JobStatus.QUEUED;

    @Column(name = "status_message")
    private String statusMessage;

    // Progress tracking
    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;  // 0-100

    @Column(name = "current_epoch")
    @Builder.Default
    private Integer currentEpoch = 0;

    @Column(name = "total_epochs")
    @Builder.Default
    private Integer totalEpochs = 100;

    @Column(name = "current_accuracy")
    private Double currentAccuracy;

    @Column(name = "best_accuracy")
    private Double bestAccuracy;

    @Column(name = "current_loss")
    private Double currentLoss;

    // Configuration - Basic
    @Column(name = "dataset_id")
    private String datasetId;

    @Column(name = "dataset_name")
    private String datasetName;

    @Column(name = "algorithm", nullable = false)
    private String algorithm;  // e.g., "xgboost", "random_forest"

    @Column(name = "algorithm_display_name")
    private String algorithmDisplayName;  // e.g., "XGBoost (Gradient Boosting)"

    @Column(name = "target_variable")
    private String targetVariable;  // e.g., "churn"

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_type")
    private ProblemType problemType;

    @Column(name = "train_test_split")
    @Builder.Default
    private Double trainTestSplit = 0.8;  // 80% train, 20% test

    @Column(name = "cross_validation_folds")
    @Builder.Default
    private Integer crossValidationFolds = 5;

    // Configuration - Advanced (stored as JSON)
    @Column(name = "hyperparameters_json", columnDefinition = "TEXT")
    private String hyperparametersJson;

    // Configuration - Optimization
    @Column(name = "gpu_acceleration")
    @Builder.Default
    private Boolean gpuAcceleration = false;

    @Column(name = "auto_hyperparameter_tuning")
    @Builder.Default
    private Boolean autoHyperparameterTuning = false;

    @Column(name = "early_stopping")
    @Builder.Default
    private Boolean earlyStopping = true;

    @Column(name = "early_stopping_patience")
    @Builder.Default
    private Integer earlyStoppingPatience = 10;

    @Column(name = "batch_size")
    @Builder.Default
    private Integer batchSize = 32;

    @Column(name = "evaluation_metric")
    @Builder.Default
    private String evaluationMetric = "accuracy";

    // Timing
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "eta_seconds")
    private Long etaSeconds;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    // Results
    @Column(name = "model_id")
    private String modelId;  // Reference to trained model

    @Column(name = "model_path")
    private String modelPath;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;  // Final metrics as JSON

    // Resources
    @Column(name = "compute_resources")
    private String computeResources;  // e.g., "4x GPU"

    @Column(name = "cost_estimate")
    private Double costEstimate;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "project_id", insertable = false, updatable = false)
    private String projectId;
    
    // Store project ID directly to avoid lazy loading issues in async threads
    @Column(name = "project_id_value")
    private String projectIdValue;

    // Error handling
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
