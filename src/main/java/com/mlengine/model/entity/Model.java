package com.mlengine.model.entity;

import com.mlengine.model.enums.ProblemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Model entity - represents a trained ML model.
 * Maps to React UI's Recent Models and Model Evaluation sections.
 * 
 * Models can be created from two sources:
 * - AUTOML: Automatic algorithm selection and hyperparameter tuning
 * - TRAINING: Manual algorithm selection via Model Training
 */
@Entity
@Table(name = "models")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Model identification
    @Column(nullable = false)
    private String name;  // e.g., "XGBoost Model v2.1"

    @Column(length = 1000)
    private String description;

    @Column(name = "version")
    @Builder.Default
    private String version = "1.0";

    // ========== MODEL SOURCE/CATEGORY ==========
    /**
     * Source of the model - how it was created.
     * AUTOML = Created via AutoML Engine (automatic algorithm selection)
     * TRAINING = Created via Model Training (manual algorithm selection)
     * IMPORTED = Imported from external source (future)
     */
    @Column(name = "source")
    @Builder.Default
    private String source = "TRAINING";  // AUTOML, TRAINING, IMPORTED

    @Column(name = "source_job_id")
    private String sourceJobId;  // ID of the AutoML job or Training job that created this model

    // Algorithm info
    @Column(name = "algorithm", nullable = false)
    private String algorithm;  // e.g., "xgboost"

    @Column(name = "algorithm_display_name")
    private String algorithmDisplayName;  // e.g., "XGBoost (Gradient Boosting)"

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_type")
    private ProblemType problemType;

    // Status
    @Column(name = "is_deployed")
    @Builder.Default
    private Boolean isDeployed = false;

    @Column(name = "is_production_ready")
    @Builder.Default
    private Boolean isProductionReady = false;

    @Column(name = "is_best")
    @Builder.Default
    private Boolean isBest = false;

    // Training info
    @Column(name = "training_job_id")
    private String trainingJobId;

    @Column(name = "dataset_id")
    private String datasetId;

    @Column(name = "dataset_name")
    private String datasetName;

    @Column(name = "target_variable")
    private String targetVariable;

    @Column(name = "feature_count")
    private Integer featureCount;

    @Column(name = "training_samples")
    private Long trainingSamples;

    // Performance metrics
    @Column(name = "accuracy")
    private Double accuracy;

    @Column(name = "precision_score")
    private Double precisionScore;

    @Column(name = "recall")
    private Double recall;

    @Column(name = "f1_score")
    private Double f1Score;

    @Column(name = "auc_roc")
    private Double aucRoc;

    @Column(name = "mse")
    private Double mse;  // For regression

    @Column(name = "rmse")
    private Double rmse;  // For regression

    @Column(name = "mae")
    private Double mae;  // For regression

    @Column(name = "r2_score")
    private Double r2Score;  // For regression

    // Detailed metrics (JSON)
    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    @Column(name = "confusion_matrix_json", columnDefinition = "TEXT")
    private String confusionMatrixJson;

    @Column(name = "feature_importance_json", columnDefinition = "TEXT")
    private String featureImportanceJson;

    @Column(name = "hyperparameters_json", columnDefinition = "TEXT")
    private String hyperparametersJson;

    // Model files - CRITICAL: This is the FastAPI model ID for predictions!
    @Column(name = "model_path")
    private String modelPath;

    @Column(name = "model_size_bytes")
    private Long modelSizeBytes;

    // Training details
    @Column(name = "training_time_seconds")
    private Long trainingTimeSeconds;

    @Column(name = "cross_validation_folds")
    private Integer crossValidationFolds;

    // Health indicators
    @Column(name = "overfitting_risk")
    private String overfittingRisk;  // "Low", "Medium", "High"

    @Column(name = "class_balance")
    private String classBalance;  // "Good", "Moderate", "Poor"

    @Column(name = "feature_correlation")
    private String featureCorrelation;  // "Low", "Moderate", "High"

    @Column(name = "data_quality")
    private String dataQuality;  // "Excellent", "Good", "Moderate", "Poor"

    // Deployment info
    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column(name = "predictions_count")
    @Builder.Default
    private Long predictionsCount = 0L;

    // Project relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "project_id", insertable = false, updatable = false)
    private String projectId;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ========== HELPER METHODS ==========
    
    public boolean isAutoML() {
        return "AUTOML".equals(source);
    }
    
    public boolean isManualTraining() {
        return "TRAINING".equals(source);
    }
    
    public boolean isReadyForPredictions() {
        return modelPath != null && !modelPath.isBlank();
    }
}
