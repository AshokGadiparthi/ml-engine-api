package com.mlengine.model.entity;

import com.mlengine.model.enums.JobStatus;
import com.mlengine.model.enums.ProblemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * AutoML Job entity - tracks automatic model selection runs.
 */
@Entity
@Table(name = "automl_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoMLJob {

    @Id
    @UuidGenerator
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    
    // Store project ID directly to avoid lazy loading issues in async threads
    @Column(name = "project_id_value")
    private String projectIdValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;
    
    // Store dataset info directly to avoid lazy loading issues in async threads
    @Column(name = "dataset_id_value")
    private String datasetIdValue;
    
    @Column(name = "dataset_name")
    private String datasetName;

    @Column(nullable = false)
    private String targetColumn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemType problemType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.QUEUED;

    // Configuration
    @Builder.Default
    private Integer maxTrainingTimeMinutes = 60;

    @Builder.Default
    private String accuracyVsSpeed = "medium"; // low, medium, high

    @Builder.Default
    private String interpretability = "medium"; // low, medium, high

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

    // Progress tracking
    @Builder.Default
    private Integer progress = 0;

    private String currentPhase; // DATA_VALIDATION, FEATURE_ENGINEERING, ALGORITHM_SELECTION, MODEL_TRAINING, EVALUATION

    private String currentAlgorithm;

    @Builder.Default
    private Integer algorithmsCompleted = 0;

    @Builder.Default
    private Integer algorithmsTotal = 5;

    private Double currentBestScore;

    private String currentBestAlgorithm;

    // Results
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "best_model_id")
    private Model bestModel;

    private String bestAlgorithm;

    private Double bestScore;

    private String bestMetric;

    @Column(columnDefinition = "TEXT")
    private String leaderboardJson; // JSON array of algorithm results

    @Column(columnDefinition = "TEXT")
    private String featureImportanceJson; // JSON array of feature importance

    @Column(columnDefinition = "TEXT")
    private String logsJson; // JSON array of log entries

    // Paths
    private String comparisonCsvPath;

    private String modelPath;

    private String featureEngineerPath;

    // Timing
    private Long elapsedTimeSeconds;

    private Long estimatedRemainingSeconds;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
