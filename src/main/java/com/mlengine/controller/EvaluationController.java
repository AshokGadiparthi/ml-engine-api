package com.mlengine.controller;

import com.mlengine.model.dto.EvaluationDTO;
import com.mlengine.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Model Evaluation (Layer 1 - Java API)
 * Provides endpoints for threshold analysis, business impact, and production readiness
 */
@Slf4j
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
@Tag(name = "Model Evaluation", description = "Model evaluation endpoints")
@CrossOrigin
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/threshold/{model_id}")
    @Operation(summary = "Evaluate model at threshold", description = "Evaluate model at specific classification threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evaluation completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Evaluation failed")
    })
    public ResponseEntity<EvaluationDTO.ThresholdEvaluationResponse> evaluateWithThreshold(
            @Parameter(description = "Model identifier")
            @PathVariable("model_id") String modelId,

            @Valid @RequestBody EvaluationDTO.ThresholdEvaluationRequest request
    ) {
        log.info("Evaluating model {} at threshold {}", modelId, request.getThreshold());

        try {
            EvaluationDTO.ThresholdEvaluationResponse response =
                    evaluationService.evaluateWithThreshold(modelId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error evaluating model: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/business-impact/{model_id}")
    @Operation(summary = "Calculate business impact", description = "Calculate financial impact of model predictions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Impact calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Calculation failed")
    })
    public ResponseEntity<EvaluationDTO.BusinessImpactResponse> calculateBusinessImpact(
            @Parameter(description = "Model identifier")
            @PathVariable("model_id") String modelId,

            @Valid @RequestBody EvaluationDTO.BusinessImpactRequest request
    ) {
        log.info("Calculating business impact for model {}", modelId);

        try {
            EvaluationDTO.BusinessImpactResponse response =
                    evaluationService.calculateBusinessImpact(modelId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calculating business impact: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/optimal-threshold/{model_id}")
    @Operation(summary = "Find optimal threshold", description = "Find optimal threshold for profit maximization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Optimal threshold found"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Calculation failed")
    })
    public ResponseEntity<EvaluationDTO.OptimalThresholdResponse> findOptimalThreshold(
            @Parameter(description = "Model identifier")
            @PathVariable("model_id") String modelId,

            @Valid @RequestBody EvaluationDTO.OptimalThresholdRequest request
    ) {
        log.info("Finding optimal threshold for model {}", modelId);

        try {
            EvaluationDTO.OptimalThresholdResponse response =
                    evaluationService.findOptimalThreshold(modelId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error finding optimal threshold: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/production-readiness/{model_id}")
    @Operation(summary = "Assess production readiness", description = "Assess model readiness for production deployment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Assessment failed")
    })
    public ResponseEntity<EvaluationDTO.ProductionReadinessResponse> assessProductionReadiness(
            @Parameter(description = "Model identifier")
            @PathVariable("model_id") String modelId,

            @Valid @RequestBody EvaluationDTO.ProductionReadinessRequest request
    ) {
        log.info("Assessing production readiness for model {}", modelId);

        try {
            EvaluationDTO.ProductionReadinessResponse response =
                    evaluationService.assessProductionReadiness(modelId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error assessing production readiness: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/complete/{model_id}")
    @Operation(summary = "Complete evaluation", description = "Perform complete evaluation with all metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Complete evaluation finished"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Evaluation failed")
    })
    public ResponseEntity<EvaluationDTO.CompleteEvaluationResponse> completeEvaluation(
            @Parameter(description = "Model identifier")
            @PathVariable("model_id") String modelId,

            @Valid @RequestBody EvaluationDTO.CompleteEvaluationRequest request
    ) {
        log.info("Running complete evaluation for model {}", modelId);

        try {
            EvaluationDTO.CompleteEvaluationResponse response =
                    evaluationService.completeEvaluation(modelId, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in complete evaluation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if evaluation service is operational")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Health check requested");

        try {
            boolean available = evaluationService.isServiceAvailable();

            Map<String, Object> response = new HashMap<>();
            response.put("status", available ? "healthy" : "degraded");
            response.put("service", "evaluation");
            response.put("fastapi_backend", available ? "connected" : "disconnected");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "unhealthy");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Service status", description = "Get detailed status of evaluation service")
    @ApiResponse(responseCode = "200", description = "Status retrieved")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info("Status check requested");

        try {
            Map<String, Object> status = evaluationService.getHealthStatus();
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Status check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/info")
    @Operation(summary = "API information", description = "Get information about available evaluation endpoints")
    @ApiResponse(responseCode = "200", description = "Information retrieved")
    public ResponseEntity<Map<String, Object>> getInfo() {
        log.info("Info requested");

        Map<String, Object> info = new HashMap<>();
        info.put("service", "Model Evaluation API (Layer 1 - Java)");
        info.put("description", "Model evaluation and production readiness assessment");
        info.put("version", "1.0.0");
        info.put("backend", "FastAPI Layer 3");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("threshold", "POST /api/evaluation/threshold/{model_id}");
        endpoints.put("business_impact", "POST /api/evaluation/business-impact/{model_id}");
        endpoints.put("optimal_threshold", "POST /api/evaluation/optimal-threshold/{model_id}");
        endpoints.put("production_readiness", "POST /api/evaluation/production-readiness/{model_id}");
        endpoints.put("complete", "POST /api/evaluation/complete/{model_id}");
        endpoints.put("health", "GET /api/evaluation/health");
        endpoints.put("status", "GET /api/evaluation/status");

        info.put("endpoints", endpoints);

        return ResponseEntity.ok(info);
    }
}
