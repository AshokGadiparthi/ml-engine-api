package com.mlengine.controller;

import com.mlengine.model.dto.PredictionDTO;
import com.mlengine.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Prediction operations.
 * Matches React UI Predictions screens (screenshots 23-27).
 */
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@Tag(name = "Predictions", description = "Single and batch prediction endpoints")
@CrossOrigin
public class PredictionController {

    private final PredictionService predictionService;

    // ========== SINGLE PREDICTION ==========

    @PostMapping("/single")
    @Operation(summary = "Make single prediction",
               description = "Make a prediction for a single input with optional explanation")
    public ResponseEntity<PredictionDTO.SingleResponse> predictSingle(
            @Valid @RequestBody PredictionDTO.SingleRequest request) {
        return ResponseEntity.ok(predictionService.predictSingle(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prediction by ID",
               description = "Returns details of a specific prediction")
    public ResponseEntity<PredictionDTO.SingleResponse> getPrediction(@PathVariable String id) {
        return ResponseEntity.ok(predictionService.getPrediction(id));
    }

    // ========== BATCH PREDICTION ==========

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Start batch prediction",
               description = "Upload CSV file and start batch prediction job")
    public ResponseEntity<PredictionDTO.BatchResponse> startBatchPrediction(
            @Parameter(description = "Model ID for predictions")
            @RequestParam String modelId,
            @Parameter(description = "Optional job name")
            @RequestParam(required = false) String jobName,
            @Parameter(description = "CSV file with records to predict")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Project ID")
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(predictionService.startBatchPrediction(modelId, jobName, file, projectId));
    }

    @GetMapping("/batch")
    @Operation(summary = "Get all batch jobs",
               description = "Returns list of batch prediction jobs")
    public ResponseEntity<List<PredictionDTO.BatchListItem>> getAllBatchJobs(
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(predictionService.getAllBatchJobs(projectId));
    }

    @GetMapping("/batch/{jobId}")
    @Operation(summary = "Get batch job status",
               description = "Returns status and progress of a batch prediction job")
    public ResponseEntity<PredictionDTO.BatchResponse> getBatchJob(@PathVariable String jobId) {
        return ResponseEntity.ok(predictionService.getBatchJob(jobId));
    }

    @GetMapping("/batch/{jobId}/download")
    @Operation(summary = "Download batch results",
               description = "Download the CSV file with prediction results")
    public ResponseEntity<Resource> downloadBatchResults(@PathVariable String jobId) {
        String filePath = predictionService.getBatchOutputPath(jobId);
        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"predictions_" + jobId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    // ========== PREDICTION HISTORY ==========

    @GetMapping("/history")
    @Operation(summary = "Get prediction history",
               description = "Returns paginated prediction history")
    public ResponseEntity<PredictionDTO.HistoryResponse> getHistory(
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(predictionService.getHistory(projectId, page, pageSize));
    }

    // ========== REALTIME PREDICTION (FOR DEPLOYED MODELS) ==========

    @PostMapping("/realtime/{modelId}")
    @Operation(summary = "Realtime prediction",
               description = "Low-latency prediction endpoint for deployed models")
    public ResponseEntity<PredictionDTO.RealtimeResponse> predictRealtime(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> features) {
        return ResponseEntity.ok(predictionService.predictRealtime(modelId, features));
    }
}
