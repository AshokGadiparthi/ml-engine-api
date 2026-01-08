package com.mlengine.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Request for making predictions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictRequest {
    
    @NotBlank(message = "Model ID is required")
    private String modelId;
    
    // Single prediction - feature values
    private Map<String, Object> features;
    
    // Batch prediction - list of feature values
    private List<Map<String, Object>> batch;
    
    // Include probability scores (classification only)
    @Builder.Default
    private boolean includeProbabilities = false;
}
