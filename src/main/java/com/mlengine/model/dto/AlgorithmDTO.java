package com.mlengine.model.dto;

import com.mlengine.model.enums.ProblemType;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTOs for Algorithm information.
 * Matches React UI Algorithm Selection and hyperparameter forms.
 */
public class AlgorithmDTO {

    /**
     * Algorithm information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        private String id;  // "xgboost"
        private String name;  // "XGBoost"
        private String displayName;  // "XGBoost (Gradient Boosting)"
        private String description;
        private List<ProblemType> supportedProblemTypes;
        private Boolean supportsGpu;
        private String category;  // "ensemble", "linear", "neural_network"
        private List<String> pros;
        private List<String> cons;
        private String recommendedFor;
    }

    /**
     * Algorithm hyperparameters schema.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameters {
        private String algorithmId;
        private String algorithmName;
        private List<Parameter> parameters;
        private RecommendedSettings recommended;
    }

    /**
     * Single parameter definition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String name;  // "max_depth"
        private String displayName;  // "Max Depth"
        private String description;
        private String type;  // "integer", "float", "string", "boolean", "select"
        private Object defaultValue;
        private Object minValue;
        private Object maxValue;
        private Double step;  // For sliders
        private List<String> options;  // For select type
        private String category;  // "basic", "advanced", "optimization"
        private Boolean required;
    }

    /**
     * Recommended settings for an algorithm.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedSettings {
        private String problemType;  // "classification", "regression"
        private Map<String, Object> values;
        private List<String> tips;
    }

    /**
     * All algorithms list response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Info> classification;
        private List<Info> regression;
        private List<Info> timeSeries;
        private List<Info> clustering;
    }
}
