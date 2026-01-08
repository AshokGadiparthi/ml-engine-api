package com.mlengine.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response from predictions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictResponse {
    
    private String modelId;
    private String status;  // SUCCESS, FAILED
    
    // Single prediction result
    private Object prediction;
    private Map<String, Double> probabilities;
    
    // Batch prediction results
    private List<PredictionResult> predictions;
    
    private Long durationMs;
    private String error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionResult {
        private int index;
        private Object prediction;
        private Map<String, Double> probabilities;
    }
}
