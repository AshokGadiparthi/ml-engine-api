package com.mlengine.controller;

import com.mlengine.model.ModelInfo;
import com.mlengine.service.MLService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for model management.
 */
@Slf4j
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Tag(name = "Models", description = "Model management endpoints")
public class ModelController {
    
    private final MLService mlService;
    
    /**
     * List all models.
     */
    @GetMapping
    @Operation(summary = "List all models",
               description = "Get a list of all trained models")
    public ResponseEntity<List<ModelInfo>> listModels() {
        List<ModelInfo> models = mlService.listModels();
        return ResponseEntity.ok(models);
    }
    
    /**
     * Get model by ID.
     */
    @GetMapping("/{modelId}")
    @Operation(summary = "Get model details",
               description = "Get detailed information about a specific model")
    public ResponseEntity<ModelInfo> getModel(@PathVariable String modelId) {
        ModelInfo model = mlService.getModel(modelId);
        
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(model);
    }
    
    /**
     * Delete a model.
     */
    @DeleteMapping("/{modelId}")
    @Operation(summary = "Delete a model",
               description = "Delete a trained model")
    public ResponseEntity<Void> deleteModel(@PathVariable String modelId) {
        boolean deleted = mlService.deleteModel(modelId);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
}
