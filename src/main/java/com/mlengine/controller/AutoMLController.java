package com.mlengine.controller;

import com.mlengine.model.dto.AutoMLDTO;
import com.mlengine.service.AutoMLService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for AutoML operations.
 * Matches the AutoML Engine screen in the UI.
 */
@RestController
@RequestMapping("/api/automl")
@RequiredArgsConstructor
@Tag(name = "AutoML", description = "Automatic Machine Learning endpoints")
@CrossOrigin
public class AutoMLController {

    private final AutoMLService autoMLService;

    @PostMapping("/jobs")
    @Operation(summary = "Start AutoML job",
               description = "Start automatic model selection and training")
    public ResponseEntity<AutoMLDTO.JobResponse> startAutoML(
            @Valid @RequestBody AutoMLDTO.StartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(autoMLService.startAutoML(request));
    }

    @GetMapping("/jobs")
    @Operation(summary = "List AutoML jobs",
               description = "Get list of AutoML jobs with optional filtering")
    public ResponseEntity<AutoMLDTO.PagedResponse> listJobs(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) String projectId,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(autoMLService.listJobs(projectId, status, page, size));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get AutoML job status",
               description = "Get current status and progress of an AutoML job")
    public ResponseEntity<AutoMLDTO.ProgressResponse> getJobStatus(
            @PathVariable String jobId) {
        return ResponseEntity.ok(autoMLService.getJobProgress(jobId));
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get AutoML results",
               description = "Get full results including leaderboard after job completion")
    public ResponseEntity<AutoMLDTO.ResultsResponse> getJobResults(
            @PathVariable String jobId) {
        return ResponseEntity.ok(autoMLService.getJobResults(jobId));
    }

    @PostMapping("/jobs/{jobId}/stop")
    @Operation(summary = "Stop AutoML job",
               description = "Stop a running AutoML job")
    public ResponseEntity<AutoMLDTO.StopResponse> stopJob(
            @PathVariable String jobId) {
        return ResponseEntity.ok(autoMLService.stopJob(jobId));
    }

    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Delete AutoML job",
               description = "Delete an AutoML job and its results")
    public ResponseEntity<Void> deleteJob(@PathVariable String jobId) {
        autoMLService.deleteJob(jobId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs/{jobId}/deploy")
    @Operation(summary = "Deploy best model",
               description = "Deploy the best model from an AutoML job")
    public ResponseEntity<AutoMLDTO.DeployResponse> deployBestModel(
            @PathVariable String jobId,
            @RequestBody AutoMLDTO.DeployRequest request) {
        return ResponseEntity.ok(autoMLService.deployBestModel(jobId, request));
    }

    @GetMapping("/jobs/{jobId}/leaderboard")
    @Operation(summary = "Get algorithm leaderboard",
               description = "Get the algorithm comparison leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable String jobId) {
        return ResponseEntity.ok(autoMLService.getLeaderboard(jobId));
    }

    @GetMapping("/jobs/{jobId}/feature-importance")
    @Operation(summary = "Get feature importance",
               description = "Get feature importance from the best model")
    public ResponseEntity<?> getFeatureImportance(@PathVariable String jobId) {
        return ResponseEntity.ok(autoMLService.getFeatureImportance(jobId));
    }

    @GetMapping("/jobs/{jobId}/logs")
    @Operation(summary = "Get job logs",
               description = "Get execution logs for an AutoML job")
    public ResponseEntity<?> getJobLogs(
            @PathVariable String jobId,
            @Parameter(description = "Number of recent log entries")
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(autoMLService.getJobLogs(jobId, limit));
    }

    @GetMapping("/algorithms")
    @Operation(summary = "List available algorithms",
               description = "Get list of algorithms available for AutoML")
    public ResponseEntity<?> listAlgorithms(
            @Parameter(description = "Problem type filter")
            @RequestParam(required = false) String problemType) {
        return ResponseEntity.ok(autoMLService.listAlgorithms(problemType));
    }
}
