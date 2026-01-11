package com.mlengine.controller;

import com.mlengine.model.dto.ModelDTO;
import com.mlengine.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Model operations.
 * Matches React UI Model Evaluation screens (screenshots 11-16).
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Tag(name = "Models", description = "Model management and evaluation endpoints")
@CrossOrigin
public class ModelController {

    private final ModelService modelService;

    // ========== MODEL CRUD ==========

    @GetMapping
    @Operation(summary = "Get all models",
               description = "Returns list of all trained models, optionally filtered by project")
    public ResponseEntity<List<ModelDTO.ListItem>> getAllModels(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(modelService.getAllModels(projectId));
    }
    
    @GetMapping("/for-predictions")
    @Operation(summary = "Get models ready for predictions",
               description = "Returns models that have a trained model path and can be used for predictions")
    public ResponseEntity<List<ModelDTO.ListItem>> getModelsForPredictions(
            @Parameter(description = "Filter by project ID (optional)")
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(modelService.getModelsForPredictions(projectId));
    }
    
    @GetMapping("/deployed")
    @Operation(summary = "Get deployed models",
               description = "Returns only deployed models (isDeployed=true) for predictions page")
    public ResponseEntity<List<ModelDTO.ListItem>> getDeployedModels(
            @Parameter(description = "Filter by project ID (optional)")
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(modelService.getDeployedModels(projectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get model by ID",
               description = "Returns full model details including all metrics")
    public ResponseEntity<ModelDTO.Response> getModel(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getModel(id));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent models",
               description = "Returns most recently trained models for dashboard")
    public ResponseEntity<List<ModelDTO.ListItem>> getRecentModels(
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(modelService.getRecentModels(projectId, limit));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update model",
               description = "Update model name and description")
    public ResponseEntity<ModelDTO.Response> updateModel(
            @PathVariable String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(modelService.updateModel(id, name, description));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete model")
    public ResponseEntity<Void> deleteModel(@PathVariable String id) {
        modelService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }

    // ========== METRICS & EVALUATION ==========

    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get model metrics",
               description = "Returns all metrics (accuracy, precision, recall, F1, AUC-ROC) for metrics cards")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getMetrics(id));
    }

    @GetMapping("/{id}/confusion-matrix")
    @Operation(summary = "Get confusion matrix",
               description = "Returns confusion matrix data for visualization")
    public ResponseEntity<ModelDTO.ConfusionMatrix> getConfusionMatrix(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getConfusionMatrix(id));
    }

    @GetMapping("/{id}/roc-curve")
    @Operation(summary = "Get ROC curve",
               description = "Returns ROC curve data points for chart")
    public ResponseEntity<ModelDTO.RocCurve> getRocCurve(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getRocCurve(id));
    }

    @GetMapping("/{id}/pr-curve")
    @Operation(summary = "Get PR curve",
               description = "Returns Precision-Recall curve data points")
    public ResponseEntity<ModelDTO.PrCurve> getPrCurve(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getPrCurve(id));
    }

    @GetMapping("/{id}/feature-importance")
    @Operation(summary = "Get feature importance",
               description = "Returns feature importance rankings for bar chart")
    public ResponseEntity<List<ModelDTO.FeatureImportance>> getFeatureImportance(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getFeatureImportance(id));
    }

    @GetMapping("/{id}/learning-curve")
    @Operation(summary = "Get learning curve",
               description = "Returns learning curve data (train/val accuracy & loss over epochs)")
    public ResponseEntity<ModelDTO.LearningCurve> getLearningCurve(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getLearningCurve(id));
    }

    @GetMapping("/{id}/health")
    @Operation(summary = "Get model health",
               description = "Returns model health indicators (overfitting risk, class balance, etc.)")
    public ResponseEntity<ModelDTO.ModelHealth> getModelHealth(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getModelHealth(id));
    }

    @GetMapping("/{id}/training-details")
    @Operation(summary = "Get training details",
               description = "Returns training details for Classification Report section")
    public ResponseEntity<ModelDTO.TrainingDetails> getTrainingDetails(@PathVariable String id) {
        return ResponseEntity.ok(modelService.getTrainingDetails(id));
    }

    // ========== MODEL COMPARISON ==========

    @GetMapping("/compare")
    @Operation(summary = "Compare models",
               description = "Compare multiple models side by side")
    public ResponseEntity<List<ModelDTO.ComparisonItem>> compareModels(
            @Parameter(description = "Comma-separated model IDs")
            @RequestParam List<String> ids) {
        return ResponseEntity.ok(modelService.compareModels(ids));
    }

    // ========== DEPLOYMENT ==========

    @PostMapping("/{id}/deploy")
    @Operation(summary = "Deploy model",
               description = "Deploy model for predictions")
    public ResponseEntity<ModelDTO.Response> deployModel(@PathVariable String id) {
        return ResponseEntity.ok(modelService.deployModel(id));
    }

    @PostMapping("/{id}/undeploy")
    @Operation(summary = "Undeploy model",
               description = "Remove model from deployment")
    public ResponseEntity<ModelDTO.Response> undeployModel(@PathVariable String id) {
        return ResponseEntity.ok(modelService.undeployModel(id));
    }
}
