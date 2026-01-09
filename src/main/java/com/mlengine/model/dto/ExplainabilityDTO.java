package com.mlengine.model.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTOs for Model Explainability (SHAP, LIME, PDP, What-If).
 * Matches React UI Model Interpretability screens.
 */
public class ExplainabilityDTO {

    // ========== SHAP ==========

    /**
     * SHAP global feature importance - matches React UI SHAP Global Importance.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShapGlobal {
        private String modelId;
        private List<ShapFeature> features;
        private String explanation;
    }

    /**
     * SHAP feature with positive/negative indicators.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShapFeature {
        private String feature;
        private Double importance;
        private String importanceLabel;  // "0.28"
        private Double positiveImpact;   // Impact when feature is high
        private Double negativeImpact;   // Impact when feature is low
        private String direction;        // "positive", "negative", "mixed"
        private Integer rank;
    }

    /**
     * SHAP summary plot data - for beeswarm/violin plot.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShapSummary {
        private String modelId;
        private List<String> features;
        private List<List<Double>> shapValues;  // [feature][sample]
        private List<List<Double>> featureValues;  // Actual feature values
        private Integer sampleCount;
    }

    /**
     * Request for SHAP local explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShapLocalRequest {
        private Map<String, Object> features;  // Input features
        private Integer topK;  // Top K features to show (default: 10)
    }

    /**
     * SHAP local explanation - for individual prediction.
     * Matches React UI Individual Prediction Explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShapLocal {
        private String modelId;
        private Double baseValue;          // Expected value (baseline)
        private Double predictedValue;     // Final prediction
        private String predictedClass;     // For classification
        private Double probability;        // Prediction probability
        private List<ShapContribution> contributions;
        private String explanation;        // Natural language explanation
    }

    /**
     * Individual feature contribution in SHAP local.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShapContribution {
        private String feature;
        private Object featureValue;       // Actual value
        private Double shapValue;          // SHAP contribution
        private String shapLabel;          // "+0.15" or "-0.08"
        private String direction;          // "positive" or "negative"
        private String impact;             // "High value increases prediction"
    }

    // ========== LIME ==========

    /**
     * Request for LIME explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimeRequest {
        private Map<String, Object> features;
        private Integer numFeatures;       // Number of features to show (default: 10)
        private Integer numSamples;        // Number of samples for LIME (default: 5000)
    }

    /**
     * LIME explanation response.
     * Matches React UI LIME Explanation section.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimeExplanation {
        private String modelId;
        private Double predictedProbability;
        private String predictedClass;
        private List<LimeFeature> features;
        private Double intercept;
        private Double localPrediction;
        private Double r2Score;            // Local model fit
        private String explanation;
    }

    /**
     * LIME feature contribution.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimeFeature {
        private String feature;
        private String condition;          // "age > 35" or "income <= 50000"
        private Double weight;
        private String weightLabel;
        private String direction;          // "positive" or "negative"
    }

    // ========== PDP & ICE ==========

    /**
     * Partial Dependence Plot data.
     * Matches React UI PDP visualization.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartialDependence {
        private String modelId;
        private String feature;
        private String featureType;        // "numeric" or "categorical"
        private List<Object> featureValues; // X-axis values
        private List<Double> pdpValues;     // Y-axis (average prediction)
        private Double featureMin;
        private Double featureMax;
        private Double featureMean;
        private String interpretation;      // "Higher values of X increase prediction"
    }

    /**
     * ICE (Individual Conditional Expectation) plot data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IcePlot {
        private String modelId;
        private String feature;
        private List<Object> featureValues;     // X-axis
        private List<List<Double>> iceLines;    // Each line is one sample's predictions
        private List<Double> pdpLine;           // Average (PDP line)
        private Integer sampleCount;
    }

    // ========== WHAT-IF ANALYSIS ==========

    /**
     * What-If analysis request.
     * Matches React UI What-If Analysis form.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatIfRequest {
        private Map<String, Object> originalFeatures;
        private Map<String, Object> modifiedFeatures;
    }

    /**
     * What-If analysis response.
     * Matches React UI What-If Analysis results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatIfResponse {
        private String modelId;
        
        // Original prediction
        private Double originalProbability;
        private String originalClass;
        private String originalRiskLevel;      // "High Risk", "Medium Risk", "Low Risk"
        
        // New prediction
        private Double newProbability;
        private String newClass;
        private String newRiskLevel;
        
        // Change analysis
        private Double probabilityChange;
        private String probabilityChangeLabel;  // "+15.3%" or "-8.2%"
        private String changeDirection;         // "increased" or "decreased"
        
        // Feature changes
        private List<FeatureChange> featureChanges;
        
        // AI Recommendations
        private List<Recommendation> recommendations;
        
        // Explanation
        private String explanation;
    }

    /**
     * Feature change in What-If analysis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureChange {
        private String feature;
        private Object originalValue;
        private Object newValue;
        private Double impact;              // How much this change affected prediction
        private String impactLabel;         // "+5.2%" or "-3.1%"
    }

    /**
     * AI recommendation for What-If analysis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String title;
        private String description;
        private String actionType;          // "increase", "decrease", "maintain"
        private String targetFeature;
        private Object suggestedValue;
        private Double expectedImpact;
        private String priority;            // "high", "medium", "low"
    }

    // ========== COUNTERFACTUAL ==========

    /**
     * Counterfactual explanation - what to change to flip prediction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counterfactual {
        private String modelId;
        private String originalClass;
        private String targetClass;
        private Double originalProbability;
        private Double counterfactualProbability;
        private List<FeatureChange> requiredChanges;
        private String explanation;
    }
}
