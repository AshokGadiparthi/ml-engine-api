package com.mlengine.controller;

import com.mlengine.model.dto.DeploymentDTO;
import com.mlengine.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Deployment operations.
 * Manages model deployments with versioning and history.
 */
@Slf4j
@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
@Tag(name = "Deployments", description = "Model deployment management with versioning")
public class DeploymentController {

    private final DeploymentService deploymentService;

    /**
     * Deploy from AutoML job (primary method).
     */
    @PostMapping("/from-automl/{autoMLJobId}")
    @Operation(summary = "Deploy model from AutoML job")
    public ResponseEntity<DeploymentDTO.Response> deployFromAutoML(
            @PathVariable String autoMLJobId,
            @RequestBody(required = false) DeploymentDTO.DeployFromAutoMLRequest request) {
        
        if (request == null) {
            request = new DeploymentDTO.DeployFromAutoMLRequest();
        }
        request.setAutoMLJobId(autoMLJobId);
        
        DeploymentDTO.Response response = deploymentService.deployFromAutoML(autoMLJobId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deploy from Training job (same as AutoML but for manual training).
     */
    @PostMapping("/from-training/{trainingJobId}")
    @Operation(summary = "Deploy model from Training job")
    public ResponseEntity<DeploymentDTO.Response> deployFromTraining(
            @PathVariable String trainingJobId,
            @RequestBody(required = false) DeploymentDTO.DeployFromTrainingRequest request) {
        
        if (request == null) {
            request = new DeploymentDTO.DeployFromTrainingRequest();
        }
        request.setTrainingJobId(trainingJobId);
        
        DeploymentDTO.Response response = deploymentService.deployFromTraining(trainingJobId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deploy a model directly.
     */
    @PostMapping
    @Operation(summary = "Deploy a model")
    public ResponseEntity<DeploymentDTO.Response> deployModel(
            @RequestBody DeploymentDTO.CreateRequest request) {
        DeploymentDTO.Response response = deploymentService.deployModel(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get deployment by ID.
     */
    @GetMapping("/{deploymentId}")
    @Operation(summary = "Get deployment details")
    public ResponseEntity<DeploymentDTO.Response> getDeployment(
            @PathVariable String deploymentId) {
        DeploymentDTO.Response response = deploymentService.getDeployment(deploymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get active deployment for a project.
     */
    @GetMapping("/active")
    @Operation(summary = "Get active deployment for a project")
    public ResponseEntity<DeploymentDTO.Response> getActiveDeployment(
            @RequestParam String projectId) {
        DeploymentDTO.Response response = deploymentService.getActiveDeployment(projectId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get active deployment summary (for dashboard/header).
     */
    @GetMapping("/active/summary")
    @Operation(summary = "Get active deployment summary")
    public ResponseEntity<DeploymentDTO.ActiveSummary> getActiveSummary(
            @RequestParam String projectId) {
        DeploymentDTO.ActiveSummary summary = deploymentService.getActiveSummary(projectId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get deployment history for a project.
     */
    @GetMapping("/history")
    @Operation(summary = "Get deployment history for a project")
    public ResponseEntity<DeploymentDTO.HistoryResponse> getDeploymentHistory(
            @RequestParam String projectId) {
        DeploymentDTO.HistoryResponse history = deploymentService.getDeploymentHistory(projectId);
        return ResponseEntity.ok(history);
    }

    /**
     * List deployments with pagination.
     */
    @GetMapping
    @Operation(summary = "List deployments")
    public ResponseEntity<DeploymentDTO.PagedResponse> listDeployments(
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        DeploymentDTO.PagedResponse response = deploymentService.listDeployments(projectId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Rollback to a previous deployment version.
     */
    @PostMapping("/{deploymentId}/rollback")
    @Operation(summary = "Rollback to a previous deployment version")
    public ResponseEntity<DeploymentDTO.Response> rollback(
            @PathVariable String deploymentId,
            @RequestBody(required = false) DeploymentDTO.RollbackRequest request) {
        DeploymentDTO.Response response = deploymentService.rollback(deploymentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate a specific deployment (same as rollback).
     */
    @PostMapping("/{deploymentId}/activate")
    @Operation(summary = "Activate a specific deployment version")
    public ResponseEntity<DeploymentDTO.Response> activate(
            @PathVariable String deploymentId,
            @RequestBody(required = false) DeploymentDTO.RollbackRequest request) {
        DeploymentDTO.Response response = deploymentService.rollback(deploymentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate a deployment.
     */
    @PostMapping("/{deploymentId}/deactivate")
    @Operation(summary = "Deactivate a deployment")
    public ResponseEntity<DeploymentDTO.Response> deactivate(
            @PathVariable String deploymentId,
            @RequestBody(required = false) DeploymentDTO.DeactivateRequest request) {
        DeploymentDTO.Response response = deploymentService.deactivate(deploymentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Compare two deployments.
     */
    @GetMapping("/compare")
    @Operation(summary = "Compare two deployments")
    public ResponseEntity<DeploymentDTO.CompareResponse> compare(
            @RequestParam String deploymentId1,
            @RequestParam String deploymentId2) {
        DeploymentDTO.CompareResponse response = deploymentService.compare(deploymentId1, deploymentId2);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a deployment (only if not active).
     */
    @DeleteMapping("/{deploymentId}")
    @Operation(summary = "Delete a deployment")
    public ResponseEntity<Void> deleteDeployment(@PathVariable String deploymentId) {
        deploymentService.deleteDeployment(deploymentId);
        return ResponseEntity.noContent().build();
    }
}
