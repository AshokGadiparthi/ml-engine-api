package com.mlengine.model.entity;

import com.mlengine.model.enums.ProblemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * TrainingConfiguration entity - stores saved training configurations.
 * Allows users to save and reuse training settings at different scope levels:
 * - PROJECT: Available for all datasets in a project
 * - DATASET: Specific to a dataset
 * - DATASOURCE: Specific to a data source
 * - GLOBAL: Available everywhere (no project/dataset/datasource restriction)
 */
@Entity
@Table(name = "training_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingConfiguration {

    public enum ConfigScope {
        GLOBAL,      // Available everywhere
        PROJECT,     // Available for all datasets in a project
        DATASET,     // Specific to a dataset
        DATASOURCE   // Specific to a data source
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Basic Info
    @Column(name = "name", nullable = false)
    private String name;  // e.g., "My XGBoost Config"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Scope - determines at what level this config is saved
    @Enumerated(EnumType.STRING)
    @Column(name = "scope")
    @Builder.Default
    private ConfigScope scope = ConfigScope.PROJECT;

    // DataSource (for DATASOURCE scope)
    @Column(name = "datasource_id")
    private String datasourceId;

    @Column(name = "datasource_name")
    private String datasourceName;

    // Dataset & Target (for DATASET scope)
    @Column(name = "dataset_id")
    private String datasetId;

    @Column(name = "dataset_name")
    private String datasetName;

    @Column(name = "target_variable")
    private String targetVariable;

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_type")
    private ProblemType problemType;

    // Algorithm
    @Column(name = "algorithm", nullable = false)
    private String algorithm;

    @Column(name = "algorithm_display_name")
    private String algorithmDisplayName;

    // Basic Settings
    @Column(name = "train_test_split")
    @Builder.Default
    private Double trainTestSplit = 0.8;

    @Column(name = "cross_validation_folds")
    @Builder.Default
    private Integer crossValidationFolds = 5;

    // Hyperparameters (stored as JSON)
    @Column(name = "hyperparameters_json", columnDefinition = "TEXT")
    private String hyperparametersJson;

    // Optimization Settings
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

    // Usage tracking
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // Tags for filtering
    @Column(name = "tags")
    private String tags;  // comma-separated

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "project_id", insertable = false, updatable = false)
    private String projectId;

    @Column(name = "created_by")
    private String createdBy;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
