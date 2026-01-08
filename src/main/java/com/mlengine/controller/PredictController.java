package com.mlengine.controller;

import com.mlengine.model.PredictRequest;
import com.mlengine.model.PredictResponse;
import com.mlengine.service.MLService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller for predictions.
 */
@Slf4j
@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
@Tag(name = "Predictions", description = "Model prediction endpoints")
public class PredictController {
    
    private final MLService mlService;
    
    /**
     * Make a single prediction.
     */
    @PostMapping
    @Operation(summary = "Make a prediction",
               description = "Use a trained model to make predictions")
    public ResponseEntity<PredictResponse> predict(@Valid @RequestBody PredictRequest request) {
        
        log.info("Prediction request for model: {}", request.getModelId());
        
        if (request.getFeatures() == null && request.getBatch() == null) {
            return ResponseEntity.badRequest().body(
                PredictResponse.builder()
                    .status("FAILED")
                    .error("Either 'features' or 'batch' must be provided")
                    .build()
            );
        }
        
        PredictResponse response = mlService.predict(request);
        
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Make batch predictions.
     */
    @PostMapping("/batch")
    @Operation(summary = "Make batch predictions",
               description = "Make predictions for multiple samples")
    public ResponseEntity<PredictResponse> predictBatch(@Valid @RequestBody PredictRequest request) {
        
        if (request.getBatch() == null || request.getBatch().isEmpty()) {
            return ResponseEntity.badRequest().body(
                PredictResponse.builder()
                    .status("FAILED")
                    .error("'batch' field is required for batch predictions")
                    .build()
            );
        }
        
        log.info("Batch prediction request for model: {}, samples: {}", 
                 request.getModelId(), request.getBatch().size());
        
        PredictResponse response = mlService.predict(request);
        
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}
