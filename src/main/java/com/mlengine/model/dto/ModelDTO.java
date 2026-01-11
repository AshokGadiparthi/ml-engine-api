package com.mlengine.model.dto;

import com.mlengine.model.enums.ProblemType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Model operations.
 * Matches React UI Model Evaluation and Recent Models sections.
 */
public class ModelDTO {

    /**
     * Model response - full details for Model Evaluation page.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String description;
        private String version;

        // Algorithm
        private String algorithm;
        private String algorithmDisplayName;
        private ProblemType problemType;

        // Status
        private Boolean isDeployed;
        private Boolean isProductionReady;
        private Boolean isBest;
        private String statusLabel;  // "Deployed", "Production Ready", "Best"

        // Training info
        private String trainingJobId;
        private String datasetId;
        private String datasetName;
        private String targetVariable;
        private Integer featureCount;
        private Long trainingSamples;

        // Performance Metrics (for dashboard cards)
        private Double accuracy;
        private String accuracyLabel;  // "95.8%"
        private String accuracyTrend;  // "+2.3% vs baseline"

        private Double precisionScore;
        private String precisionLabel;
        private String precisionInfo;  // "False positives: 2.1%"

        private Double recall;
        private String recallLabel;
        private String recallInfo;  // "False negatives: 1.8%"

        private Double f1Score;
        private String f1Label;
        private String f1Info;  // "Harmonic mean"

        private Double aucRoc;
        private String aucRocLabel;
        private String aucRocInfo;  // "Excellent performance"

        // Regression metrics
        private Double mse;
        private Double rmse;
        private Double mae;
        private Double r2Score;

        // Detailed metrics
        private Map<String, Object> metrics;
        private ConfusionMatrix confusionMatrix;
        private List<FeatureImportance> featureImportance;
        private Map<String, Object> hyperparameters;

        // Model Health
        private ModelHealth health;

        // Training Details
        private TrainingDetails trainingDetails;

        // Files
        private String modelPath;
        private Long modelSizeBytes;
        private String modelSizeLabel;

        // Deployment
        private LocalDateTime deployedAt;
        private String endpointUrl;
        private Long predictionsCount;

        // Project
        private String projectId;

        // Timestamps
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Model list item - for Recent Models and model selectors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String id;
        private String name;
        private String version;
        private String algorithm;
        private String algorithmDisplayName;
        private Double accuracy;
        private String accuracyLabel;
        private Boolean isDeployed;
        private Boolean isBest;
        private String statusLabel;
        private String source;         // "AUTOML" or "TRAINING"
        private String projectId;
        private String modelPath;      // FastAPI model ID for predictions
        private LocalDateTime deployedAt;
        private LocalDateTime createdAt;
        private String createdAtLabel;  // "2h ago"
    }

    /**
     * Confusion Matrix data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfusionMatrix {
        private Long truePositives;
        private Long trueNegatives;
        private Long falsePositives;
        private Long falseNegatives;
        private List<List<Long>> matrix;  // For multi-class
        private List<String> labels;
    }

    /**
     * Feature importance entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureImportance {
        private String feature;
        private Double importance;
        private String importanceLabel;  // "0.28"
        private Integer rank;
    }

    /**
     * Model health indicators.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelHealth {
        private String overfittingRisk;  // "Low", "Medium", "High"
        private String overfittingRiskColor;  // "green", "yellow", "red"

        private String classBalance;  // "Good", "Moderate", "Poor"
        private String classBalanceColor;

        private String featureCorrelation;  // "Low", "Moderate", "High"
        private String featureCorrelationColor;

        private String dataQuality;  // "Excellent", "Good", "Moderate", "Poor"
        private String dataQualityColor;
    }

    /**
     * Training details for Classification Report section.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingDetails {
        private String trainingTime;  // "23 minutes"
        private Long trainingTimeSeconds;
        private String datasetSize;  // "1.2M records"
        private Long datasetRecords;
        private String featuresUsed;  // "45 features"
        private Integer featureCount;
        private String crossValidation;  // "5-fold"
        private Integer crossValidationFolds;
    }

    /**
     * Classification report metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationReport {
        private Double weightedAvgPrecision;
        private Double weightedAvgRecall;
        private Double weightedAvgF1;
        private Long support;
        private List<ClassMetrics> perClassMetrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassMetrics {
        private String className;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private Long support;
    }

    /**
     * Model comparison - for comparing multiple models.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonItem {
        private String id;
        private String name;
        private String version;
        private Double accuracy;
        private Double precision;
        private Double recall;
        private Double f1Score;
        private Double aucRoc;
        private String status;  // "Best", "Baseline"
    }

    /**
     * ROC Curve data points.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RocCurve {
        private List<Double> fpr;  // False positive rates
        private List<Double> tpr;  // True positive rates
        private List<Double> thresholds;
        private Double aucScore;
    }

    /**
     * PR Curve data points.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrCurve {
        private List<Double> precision;
        private List<Double> recall;
        private List<Double> thresholds;
        private Double avgPrecision;
    }

    /**
     * Learning curve data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningCurve {
        private List<Integer> epochs;
        private List<Double> trainAccuracy;
        private List<Double> valAccuracy;
        private List<Double> trainLoss;
        private List<Double> valLoss;
        private Integer convergenceEpoch;
        private String convergenceInfo;  // "Model converged at epoch 50 with minimal overfitting"
    }
}
