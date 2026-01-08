package com.mlengine.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Model information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    
    private String modelId;
    private String name;
    private String algorithm;
    private String problemType;
    private String status;  // READY, TRAINING, ARCHIVED
    private String version;
    
    // Training info
    private LocalDateTime trainedAt;
    private Long trainingSamples;
    private List<String> featureNames;
    
    // Metrics
    private Map<String, Double> metrics;
    
    // File info
    private String modelPath;
    private Long fileSizeBytes;
    
    // Metadata
    private Map<String, Object> metadata;
}
