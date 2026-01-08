package com.mlengine.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request to train a new model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRequest {
    
    @NotBlank(message = "Target column is required")
    private String targetColumn;
    
    @Builder.Default
    private String algorithm = "xgboost";
    
    @Builder.Default
    private String problemType = "classification";
    
    @Builder.Default
    private double testSize = 0.2;
    
    @Builder.Default
    private boolean useFeatureEngineering = false;
    
    @Builder.Default
    private boolean tuneHyperparameters = false;
    
    @Builder.Default
    private boolean useAutoML = false;
    
    private String modelName;
    
    // Additional parameters
    private Map<String, Object> parameters;
}
