package com.mlengine.model.dto;

import com.mlengine.model.enums.ProblemType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class TrainingConfigurationDTO {

    /**
     * Request DTO for creating/updating training configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String name;
        private String description;
        
        // Scope - PROJECT, DATASET, DATASOURCE, or GLOBAL
        private String scope;
        
        // For DATASOURCE scope
        private String datasourceId;
        private String datasourceName;
        
        // For DATASET scope
        private String datasetId;
        private String datasetName;
        
        // For PROJECT scope
        private String projectId;
        
        private String targetVariable;
        private String problemType;
        private String algorithm;
        private String algorithmDisplayName;
        private Double trainTestSplit;
        private Integer crossValidationFolds;
        private Map<String, Object> hyperparameters;
        private Boolean gpuAcceleration;
        private Boolean autoHyperparameterTuning;
        private Boolean earlyStopping;
        private Integer earlyStoppingPatience;
        private Integer batchSize;
        private String evaluationMetric;
        private List<String> tags;
    }

    /**
     * Response DTO for training configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String description;
        
        // Scope info
        private String scope;
        private String scopeLabel;  // Human readable: "Project Level", "Dataset Level", etc.
        
        // Scope identifiers
        private String datasourceId;
        private String datasourceName;
        private String datasetId;
        private String datasetName;
        private String projectId;
        
        private String targetVariable;
        private String problemType;
        private String algorithm;
        private String algorithmDisplayName;
        private Double trainTestSplit;
        private Integer crossValidationFolds;
        private Map<String, Object> hyperparameters;
        private Boolean gpuAcceleration;
        private Boolean autoHyperparameterTuning;
        private Boolean earlyStopping;
        private Integer earlyStoppingPatience;
        private Integer batchSize;
        private String evaluationMetric;
        private List<String> tags;
        private Integer usageCount;
        private LocalDateTime lastUsedAt;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * List item DTO for training configuration (lighter weight).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String id;
        private String name;
        private String description;
        
        // Scope info
        private String scope;
        private String scopeLabel;
        private String scopeEntityName;  // Name of the project/dataset/datasource
        
        private String algorithm;
        private String algorithmDisplayName;
        private String problemType;
        private String datasetName;
        private String targetVariable;
        private Integer usageCount;
        private LocalDateTime lastUsedAt;
        private LocalDateTime createdAt;
    }
}
