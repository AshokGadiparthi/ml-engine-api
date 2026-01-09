package com.mlengine.controller;

import com.mlengine.model.dto.ExplainabilityDTO;
import com.mlengine.service.ExplainabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Model Explainability.
 * Matches React UI Model Interpretability screens (screenshots 17-22).
 */
@RestController
@RequestMapping("/api/models/{modelId}")
@RequiredArgsConstructor
@Tag(name = "Explainability", description = "Model interpretability endpoints (SHAP, LIME, PDP, What-If)")
@CrossOrigin
public class ExplainabilityController {

    private final ExplainabilityService explainabilityService;

    // ========== SHAP ENDPOINTS ==========

    @GetMapping("/shap/global")
    @Operation(summary = "Get SHAP global feature importance",
               description = "Returns global feature importance with positive/negative indicators")
    public ResponseEntity<ExplainabilityDTO.ShapGlobal> getShapGlobal(
            @PathVariable String modelId) {
        return ResponseEntity.ok(explainabilityService.getShapGlobal(modelId));
    }

    @GetMapping("/shap/summary")
    @Operation(summary = "Get SHAP summary plot data",
               description = "Returns data for SHAP beeswarm/summary plot visualization")
    public ResponseEntity<ExplainabilityDTO.ShapSummary> getShapSummary(
            @PathVariable String modelId,
            @Parameter(description = "Number of samples to include")
            @RequestParam(defaultValue = "100") int samples) {
        return ResponseEntity.ok(explainabilityService.getShapSummary(modelId, samples));
    }

    @PostMapping("/shap/local")
    @Operation(summary = "Get SHAP local explanation",
               description = "Returns SHAP explanation for an individual prediction")
    public ResponseEntity<ExplainabilityDTO.ShapLocal> getShapLocal(
            @PathVariable String modelId,
            @RequestBody ExplainabilityDTO.ShapLocalRequest request) {
        return ResponseEntity.ok(explainabilityService.getShapLocal(modelId, request));
    }

    // ========== LIME ENDPOINTS ==========

    @PostMapping("/lime")
    @Operation(summary = "Get LIME explanation",
               description = "Returns LIME (Local Interpretable Model-agnostic Explanations) for a prediction")
    public ResponseEntity<ExplainabilityDTO.LimeExplanation> getLimeExplanation(
            @PathVariable String modelId,
            @RequestBody ExplainabilityDTO.LimeRequest request) {
        return ResponseEntity.ok(explainabilityService.getLimeExplanation(modelId, request));
    }

    // ========== PDP & ICE ENDPOINTS ==========

    @GetMapping("/pdp/{feature}")
    @Operation(summary = "Get Partial Dependence Plot",
               description = "Returns PDP data showing marginal effect of a feature on predictions")
    public ResponseEntity<ExplainabilityDTO.PartialDependence> getPartialDependence(
            @PathVariable String modelId,
            @PathVariable String feature) {
        return ResponseEntity.ok(explainabilityService.getPartialDependence(modelId, feature));
    }

    @GetMapping("/ice/{feature}")
    @Operation(summary = "Get ICE Plot",
               description = "Returns Individual Conditional Expectation plot data")
    public ResponseEntity<ExplainabilityDTO.IcePlot> getIcePlot(
            @PathVariable String modelId,
            @PathVariable String feature,
            @Parameter(description = "Number of samples to include")
            @RequestParam(defaultValue = "50") int samples) {
        return ResponseEntity.ok(explainabilityService.getIcePlot(modelId, feature, samples));
    }

    // ========== WHAT-IF ANALYSIS ==========

    @PostMapping("/whatif")
    @Operation(summary = "Perform What-If analysis",
               description = "Analyze how changes to features affect predictions with AI recommendations")
    public ResponseEntity<ExplainabilityDTO.WhatIfResponse> analyzeWhatIf(
            @PathVariable String modelId,
            @RequestBody ExplainabilityDTO.WhatIfRequest request) {
        return ResponseEntity.ok(explainabilityService.analyzeWhatIf(modelId, request));
    }

    // ========== COUNTERFACTUAL ==========

    @PostMapping("/counterfactual")
    @Operation(summary = "Get counterfactual explanation",
               description = "Returns what changes would flip the prediction to target class")
    public ResponseEntity<ExplainabilityDTO.Counterfactual> getCounterfactual(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> features,
            @Parameter(description = "Target class for counterfactual")
            @RequestParam(defaultValue = "Negative") String targetClass) {
        return ResponseEntity.ok(explainabilityService.getCounterfactual(modelId, features, targetClass));
    }
}
