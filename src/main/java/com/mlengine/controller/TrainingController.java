package com.mlengine.controller;

import com.mlengine.model.dto.TrainingJobDTO;
import com.mlengine.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Training Job operations.
 * Matches React UI Model Training screens.
 */
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Tag(name = "Training", description = "Model training job management")
@CrossOrigin
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/jobs")
    @Operation(summary = "Start new training job",
               description = "Creates and starts a new model training job")
    public ResponseEntity<TrainingJobDTO.Response> startTraining(
            @Valid @RequestBody TrainingJobDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingService.startTraining(request));
    }

    @GetMapping("/jobs")
    @Operation(summary = "Get all training jobs",
               description = "Returns list of all training jobs, optionally filtered by project")
    public ResponseEntity<List<TrainingJobDTO.ListItem>> getAllJobs(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(trainingService.getAllJobs(projectId));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get training job by ID",
               description = "Returns full details of a training job")
    public ResponseEntity<TrainingJobDTO.Response> getJob(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.getJob(id));
    }

    @GetMapping("/jobs/{id}/progress")
    @Operation(summary = "Get training progress",
               description = "Returns current progress of a training job")
    public ResponseEntity<TrainingJobDTO.ProgressUpdate> getProgress(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.getProgress(id));
    }

    @PostMapping("/jobs/{id}/stop")
    @Operation(summary = "Stop training job",
               description = "Stops a running training job")
    public ResponseEntity<TrainingJobDTO.Response> stopJob(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.stopJob(id));
    }

    @PostMapping("/jobs/{id}/pause")
    @Operation(summary = "Pause training job",
               description = "Pauses a running training job")
    public ResponseEntity<TrainingJobDTO.Response> pauseJob(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.pauseJob(id));
    }

    @PostMapping("/jobs/{id}/resume")
    @Operation(summary = "Resume training job",
               description = "Resumes a paused training job")
    public ResponseEntity<TrainingJobDTO.Response> resumeJob(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.resumeJob(id));
    }

    @DeleteMapping("/jobs/{id}")
    @Operation(summary = "Delete training job",
               description = "Deletes a training job and stops it if running")
    public ResponseEntity<Void> deleteJob(@PathVariable String id) {
        trainingService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    // ========== DEPLOYMENT ENDPOINTS (Same as AutoML) ==========

    @PostMapping("/jobs/{id}/deploy")
    @Operation(summary = "Deploy model from training job",
               description = "Deploys the trained model, making it available for predictions. Works exactly like AutoML deploy.")
    public ResponseEntity<TrainingJobDTO.DeployResponse> deployModel(
            @PathVariable String id,
            @RequestBody(required = false) TrainingJobDTO.DeployRequest request) {
        return ResponseEntity.ok(trainingService.deployModel(id, request));
    }

    @GetMapping("/jobs/{id}/results")
    @Operation(summary = "Get training results",
               description = "Returns results from a completed training job including metrics and model info")
    public ResponseEntity<TrainingJobDTO.ResultsResponse> getResults(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.getResults(id));
    }

    @GetMapping("/jobs/{id}/deployment-status")
    @Operation(summary = "Get deployment status",
               description = "Returns current deployment status of the model from this training job")
    public ResponseEntity<TrainingJobDTO.DeploymentStatus> getDeploymentStatus(@PathVariable String id) {
        return ResponseEntity.ok(trainingService.getDeploymentStatus(id));
    }
}
