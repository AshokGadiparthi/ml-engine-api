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
 *
 * Provides endpoints for:
 * - Threshold-based evaluation
 * - Business impact analysis
 * - Optimal threshold finding
 * - Production readiness assessment
 * - Complete evaluation
 *
 * Delegates to Layer 3 (FastAPI) for actual computation.
 */
@Slf4j
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
@Tag(name = "Model Evaluation", description = "Model evaluation endpoints for threshold analysis, business impact, and production readiness")
@CrossOrigin
public class EvaluationController {

    private final EvaluationService evaluationService;

    // ==================== THRESHOLD EVALUATION ====================

    /**
     * Evaluate model at specific threshold
     * POST /api/evaluation/threshold/{model_id}
     *
     * Calculates confusion matrix, metrics, and rates at a specific threshold.
     */
    @PostMapping("/threshold/{model_id}")
    @Operation(
            summary = "Evaluate model at threshold",
            description = "Evaluate model predictions at a specific classification threshold. " +
                    "Generates confusion matrix, accuracy metrics, and classification rates."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evaluation completed successfully",
                    content = @Content(schema = @Schema(implementation = EvaluationDTO.ThresholdEvaluationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Evaluation failed")
    })
    public ResponseEntity<EvaluationDTO.ThresholdEvaluationResponse> evaluateWithThreshold(
            @Parameter(description = "Model identifier", example = "model_123")
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

    // ==================== BUSINESS IMPACT ====================

    /**
     * Calculate business impact of model
     * POST /api/evaluation/business-impact/{model_id}
     *
     * Analyzes financial impact including costs of errors and revenue from correct predictions.
     */
    @PostMapping("/business-impact/{model_id}")
    @Operation(
            summary = "Calculate business impact",
            description = "Calculate financial impact of model predictions including " +
                    "costs from false positives/negatives and revenue from true positives. " +
                    "Requires output from threshold evaluation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Impact calculated successfully",
                    content = @Content(schema = @Schema(implementation = EvaluationDTO.BusinessImpactResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request - missing or invalid evaluation_result"),
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

    // ==================== OPTIMAL THRESHOLD ====================

    /**
     * Find optimal classification threshold
     * POST /api/evaluation/optimal-threshold/{model_id}
     *
     * Calculates the threshold that maximizes profit based on costs and revenue.
     */
    @PostMapping("/optimal-threshold/{model_id}")
    @Operation(
            summary = "Find optimal threshold",
            description = "Find the optimal classification threshold that maximizes profit " +
                    "based on costs of errors and revenue from correct predictions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Optimal threshold found",
                    content = @Content(schema = @Schema(implementation = EvaluationDTO.OptimalThresholdResponse.class))),
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

    // ==================== PRODUCTION READINESS ====================

    /**
     * Assess production readiness
     * POST /api/evaluation/production-readiness/{model_id}
     *
     * Comprehensive assessment of model readiness for production deployment.
     */
    @PostMapping("/production-readiness/{model_id}")
    @Operation(
            summary = "Assess production readiness",
            description = "Comprehensive assessment of model readiness for production deployment. " +
                    "Evaluates performance stability, business viability, and other criteria."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment completed successfully",
                    content = @Content(schema = @Schema(implementation = EvaluationDTO.ProductionReadinessResponse.class))),
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

    // ==================== COMPLETE EVALUATION ====================

    /**
     * Run complete evaluation
     * POST /api/evaluation/complete/{model_id}
     *
     * All-in-one endpoint that performs comprehensive evaluation.
     */
    @PostMapping("/complete/{model_id}")
    @Operation(
            summary = "Complete evaluation",
            description = "Perform complete evaluation with all metrics including threshold analysis, " +
                    "business impact, and production readiness assessment."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Complete evaluation finished",
                    content = @Content(schema = @Schema(implementation = EvaluationDTO.CompleteEvaluationResponse.class))),
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

    // ==================== HEALTH & STATUS ====================

    /**
     * Health check for evaluation service
     * GET /api/evaluation/health
     */
    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Check if the evaluation service and underlying FastAPI backend are operational"
    )
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

    /**
     * Get service status
     * GET /api/evaluation/status
     */
    @GetMapping("/status")
    @Operation(
            summary = "Service status",
            description = "Get detailed status of evaluation service"
    )
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

    // ==================== INFO ====================

    /**
     * Get evaluation API documentation
     * GET /api/evaluation/info
     */
    @GetMapping("/info")
    @Operation(
            summary = "API information",
            description = "Get information about available evaluation endpoints"
    )
    @ApiResponse(responseCode = "200", description = "Information retrieved")
    public ResponseEntity<Map<String, Object>> getInfo() {
        log.info("Info requested");

        Map<String, Object> info = new HashMap<>();
        info.put("service", "Model Evaluation API (Layer 1 - Java)");
        info.put("description", "Provides model evaluation, threshold analysis, business impact calculation, and production readiness assessment");
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
