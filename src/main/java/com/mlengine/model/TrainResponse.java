package com.mlengine.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response from model training.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainResponse {
    
    private String jobId;
    private String status;  // PENDING, RUNNING, COMPLETED, FAILED
    private String modelId;
    private String modelPath;
    private String algorithm;
    private String problemType;
    
    // Metrics
    private Double accuracy;
    private Double precision;
    private Double recall;
    private Double f1Score;
    private Double rmse;
    private Double mae;
    private Double r2;
    
    private Map<String, Object> metrics;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    
    private String message;
    private String error;
}
