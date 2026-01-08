package com.mlengine.controller;

import com.mlengine.model.TrainRequest;
import com.mlengine.model.TrainResponse;
import com.mlengine.service.MLService;
import com.mlengine.config.MLEngineConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * REST Controller for model training.
 */
@Slf4j
@RestController
@RequestMapping("/api/train")
@RequiredArgsConstructor
@Tag(name = "Training", description = "Model training endpoints")
public class TrainController {
    
    private final MLService mlService;
    private final MLEngineConfig config;
    
    /**
     * Train a new model.
     */
    @PostMapping
    @Operation(summary = "Train a new model", 
               description = "Upload training data and train a new ML model")
    public ResponseEntity<TrainResponse> train(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetColumn") String targetColumn,
            @RequestParam(defaultValue = "xgboost") String algorithm,
            @RequestParam(defaultValue = "classification") String problemType,
            @RequestParam(defaultValue = "0.2") double testSize,
            @RequestParam(defaultValue = "false") boolean useAutoML,
            @RequestParam(defaultValue = "false") boolean tuneHyperparameters,
            @RequestParam(required = false) String modelName) {
        
        try {
            log.info("Training request received:");
            log.info("  File: {}", file.getOriginalFilename());
            log.info("  Target: {}", targetColumn);
            log.info("  Algorithm: {}", algorithm);
            log.info("  Problem Type: {}", problemType);
            
            // Save uploaded file
            String dataPath = saveUploadedFile(file);
            
            // Build request
            TrainRequest request = TrainRequest.builder()
                    .targetColumn(targetColumn)
                    .algorithm(algorithm)
                    .problemType(problemType)
                    .testSize(testSize)
                    .useAutoML(useAutoML)
                    .tuneHyperparameters(tuneHyperparameters)
                    .modelName(modelName)
                    .build();
            
            // Train model
            TrainResponse response = mlService.trainModel(dataPath, request);
            
            // Clean up temp file
            Files.deleteIfExists(Path.of(dataPath));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Training error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                TrainResponse.builder()
                    .status("FAILED")
                    .error(e.getMessage())
                    .build()
            );
        }
    }
    
    /**
     * Train model asynchronously (returns job ID immediately).
     */
    @PostMapping("/async")
    @Operation(summary = "Train model asynchronously",
               description = "Start training and return job ID for status tracking")
    public ResponseEntity<TrainResponse> trainAsync(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute TrainRequest request) {
        
        try {
            String dataPath = saveUploadedFile(file);
            
            // Start async training
            mlService.trainModelAsync(dataPath, request);
            
            return ResponseEntity.accepted().body(
                TrainResponse.builder()
                    .status("PENDING")
                    .message("Training job started")
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Async training error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                TrainResponse.builder()
                    .status("FAILED")
                    .error(e.getMessage())
                    .build()
            );
        }
    }
    
    /**
     * Get training job status.
     */
    @GetMapping("/status/{jobId}")
    @Operation(summary = "Get training job status",
               description = "Check the status of an async training job")
    public ResponseEntity<TrainResponse> getStatus(@PathVariable String jobId) {
        TrainResponse status = mlService.getTrainingStatus(jobId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get available algorithms.
     */
    @GetMapping("/algorithms")
    @Operation(summary = "List available algorithms")
    public ResponseEntity<?> getAlgorithms() {
        return ResponseEntity.ok(java.util.Map.of(
            "classification", java.util.List.of(
                "logistic", "random_forest", "xgboost", "svm", "gradient_boosting"
            ),
            "regression", java.util.List.of(
                "linear", "ridge", "lasso", "random_forest", "xgboost", 
                "gradient_boosting", "svr"
            )
        ));
    }
    
    // ========== Helper Methods ==========
    
    private String saveUploadedFile(MultipartFile file) throws Exception {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path tempPath = Path.of(config.getTempDir(), filename);
        file.transferTo(tempPath.toFile());
        return tempPath.toString();
    }
}
