package com.mlengine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Model Evaluation API (Layer 3 Integration)
 * Handles requests and responses for model evaluation, business impact, and production readiness
 */
public class EvaluationDTO {

    // ==================== THRESHOLD EVALUATION ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to evaluate model at specific threshold")
    public static class ThresholdEvaluationRequest implements Serializable {

        @NotNull(message = "y_true cannot be null")
        @Schema(description = "True labels array", example = "[0, 1, 1, 0, 1]")
        private double[] yTrue;

        @NotNull(message = "y_pred_proba cannot be null")
        @Schema(description = "Predicted probabilities", example = "[0.1, 0.9, 0.8, 0.2, 0.7]")
        private double[] yPredProba;

        @NotNull(message = "threshold cannot be null")
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        @Schema(description = "Classification threshold", example = "0.5")
        private Double threshold;

        @Schema(description = "Target names", example = "[\"negative\", \"positive\"]")
        private String[] targetNames;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Response from threshold evaluation")
    public static class ThresholdEvaluationResponse implements Serializable {

        @Schema(description = "Model identifier")
        private String modelId;

        @Schema(description = "Evaluation threshold used")
        private Double threshold;

        @Schema(description = "Confusion matrix")
        private ConfusionMatrix confusionMatrix;

        @Schema(description = "Classification metrics")
        private Metrics metrics;

        @Schema(description = "Rate metrics (TPR, FPR, etc.)")
        private Rates rates;
    }

    // ==================== BUSINESS IMPACT ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request for business impact calculation")
    public static class BusinessImpactRequest implements Serializable {

        @NotNull(message = "evaluation_result cannot be null")
        @Schema(description = "Evaluation result from /threshold endpoint")
        private Map<String, Object> evaluationResult;

        @NotNull(message = "cost_false_positive cannot be null")
        @Schema(description = "Cost of false positive prediction", example = "500")
        private Double costFalsePositive;

        @NotNull(message = "cost_false_negative cannot be null")
        @Schema(description = "Cost of false negative prediction", example = "2000")
        private Double costFalseNegative;

        @NotNull(message = "revenue_true_positive cannot be null")
        @Schema(description = "Revenue from true positive prediction", example = "1000")
        private Double revenueTruePositive;

        @NotNull(message = "volume cannot be null")
        @DecimalMin(value = "0.0", inclusive = false)
        @Schema(description = "Prediction volume", example = "10000")
        private Double volume;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Business impact analysis response")
    public static class BusinessImpactResponse implements Serializable {

        @Schema(description = "Model identifier")
        private String modelId;

        @Schema(description = "Cost metrics")
        private CostMetrics costs;

        @Schema(description = "Revenue metrics")
        private RevenueMetrics revenue;

        @Schema(description = "Financial summary")
        private FinancialSummary financial;
    }

    // ==================== OPTIMAL THRESHOLD ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to find optimal threshold")
    public static class OptimalThresholdRequest implements Serializable {

        @NotNull(message = "y_true cannot be null")
        @Schema(description = "True labels")
        private double[] yTrue;

        @NotNull(message = "y_pred_proba cannot be null")
        @Schema(description = "Predicted probabilities")
        private double[] yPredProba;

        @NotNull(message = "cost_false_positive cannot be null")
        @Schema(description = "Cost of false positive", example = "500")
        private Double costFalsePositive;

        @NotNull(message = "cost_false_negative cannot be null")
        @Schema(description = "Cost of false negative", example = "2000")
        private Double costFalseNegative;

        @NotNull(message = "revenue_true_positive cannot be null")
        @Schema(description = "Revenue from true positive", example = "1000")
        private Double revenueTruePositive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Optimal threshold response")
    public static class OptimalThresholdResponse implements Serializable {

        @Schema(description = "Model identifier")
        private String modelId;

        @Schema(description = "Optimal threshold value")
        private Double optimalThreshold;

        @Schema(description = "Expected profit at optimal threshold")
        private Double expectedProfit;

        @Schema(description = "Metrics at optimal threshold")
        private Metrics metricsAtThreshold;
    }

    // ==================== PRODUCTION READINESS ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request for production readiness assessment")
    public static class ProductionReadinessRequest implements Serializable {

        @NotNull(message = "eval_result cannot be null")
        @Schema(description = "Evaluation result")
        private Map<String, Object> evalResult;

        @Schema(description = "Learning curve data")
        private Map<String, Object> learningCurve;

        @Schema(description = "Business impact data")
        private Map<String, Object> businessImpact;

        @Schema(description = "Feature importance data")
        private Map<String, Object> featureImportance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Production readiness assessment response")
    public static class ProductionReadinessResponse implements Serializable {

        @Schema(description = "Model identifier")
        private String modelId;

        @Schema(description = "Overall readiness status (READY, WARNING, NOT_READY)")
        private String overallStatus;

        @Schema(description = "Summary of assessment")
        private ReadinessSummary summary;

        @Schema(description = "List of readiness criteria")
        private List<ReadinessCriterion> criteria;
    }

    // ==================== COMPLETE EVALUATION ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request for complete evaluation (all metrics)")
    public static class CompleteEvaluationRequest implements Serializable {

        @Schema(description = "Test feature matrix")
        private double[] xTest;

        @NotNull(message = "y_test cannot be null")
        @Schema(description = "Test labels")
        private double[] yTest;

        @NotNull(message = "y_pred_proba cannot be null")
        @Schema(description = "Test predictions")
        private double[] yPredProba;

        @Schema(description = "Train labels")
        private double[] yTrain;

        @Schema(description = "Train predictions")
        private double[] yPredTrain;

        @NotNull(message = "threshold cannot be null")
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        @Schema(description = "Threshold for evaluation", example = "0.5")
        private Double threshold;

        @NotNull(message = "cost_fp cannot be null")
        @Schema(description = "Cost of false positive", example = "500")
        private Double costFp;

        @NotNull(message = "cost_fn cannot be null")
        @Schema(description = "Cost of false negative", example = "2000")
        private Double costFn;

        @NotNull(message = "revenue_tp cannot be null")
        @Schema(description = "Revenue from true positive", example = "1000")
        private Double revenueTp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Complete evaluation response with all metrics")
    public static class CompleteEvaluationResponse implements Serializable {

        @Schema(description = "Model identifier")
        private String modelId;

        @Schema(description = "Threshold evaluation results")
        private ThresholdEvaluationResponse thresholdEvaluation;

        @Schema(description = "Business impact results")
        private BusinessImpactResponse businessImpact;

        @Schema(description = "Production readiness assessment")
        private ProductionReadinessResponse productionReadiness;

        @Schema(description = "Overall quality score")
        private Double overallScore;
    }

    // ==================== SHARED MODELS ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfusionMatrix implements Serializable {
        @Schema(description = "True negatives")
        private Long tn;

        @Schema(description = "False positives")
        private Long fp;

        @Schema(description = "False negatives")
        private Long fn;

        @Schema(description = "True positives")
        private Long tp;

        @Schema(description = "Total predictions")
        private Long total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metrics implements Serializable {
        @Schema(description = "Accuracy score")
        private Double accuracy;

        @Schema(description = "Precision score")
        private Double precision;

        @Schema(description = "Recall/Sensitivity score")
        private Double recall;

        @Schema(description = "F1 score")
        private Double f1Score;

        @Schema(description = "AUC-ROC score")
        private Double aucRoc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Rates implements Serializable {
        @Schema(description = "False positive rate")
        private Double falsePositiveRate;

        @Schema(description = "False negative rate")
        private Double falseNegativeRate;

        @Schema(description = "True positive rate")
        private Double truePositiveRate;

        @Schema(description = "True negative rate")
        private Double trueNegativeRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostMetrics implements Serializable {
        @Schema(description = "Total cost from false positives")
        private Double falsePositiveCost;

        @Schema(description = "Total cost from false negatives")
        private Double falseNegativeCost;

        @Schema(description = "Total cost")
        private Double totalCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueMetrics implements Serializable {
        @Schema(description = "Revenue from true positives")
        private Double truePositiveRevenue;

        @Schema(description = "Revenue potential (if all positive)")
        private Double revenueIfOptimal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialSummary implements Serializable {
        @Schema(description = "Net profit from model")
        private Double profit;

        @Schema(description = "Improvement vs baseline")
        private Double improvementVsBaseline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadinessSummary implements Serializable {
        @Schema(description = "Number of passed criteria")
        private Integer passed;

        @Schema(description = "Total number of criteria")
        private Integer totalCriteria;

        @Schema(description = "Pass percentage")
        private Double passPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadinessCriterion implements Serializable {
        @Schema(description = "Criterion name")
        private String name;

        @Schema(description = "Is criterion passed")
        private Boolean passed;

        @Schema(description = "Criterion value")
        private Object value;

        @Schema(description = "Required threshold")
        private Object threshold;

        @Schema(description = "Criterion description")
        private String description;
    }
}
