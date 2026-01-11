package com.mlengine.controller;

import com.mlengine.model.dto.PredictionDTO;
import com.mlengine.service.ApiKeyService;
import com.mlengine.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Prediction operations.
 * Supports ALL features in the Predictions UI screens.
 */
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@Tag(name = "Predictions", description = "Single, batch, and API prediction endpoints")
@CrossOrigin
public class PredictionController {

    private final PredictionService predictionService;
    private final ApiKeyService apiKeyService;

    // ========== SINGLE PREDICTION ==========

    @PostMapping("/single")
    @Operation(summary = "Make single prediction",
               description = "Make a prediction for a single input")
    public ResponseEntity<PredictionDTO.SingleResponse> predictSingle(
            @Valid @RequestBody PredictionDTO.SingleRequest request) {
        return ResponseEntity.ok(predictionService.predictSingle(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prediction details",
               description = "Returns full details of a specific prediction for the modal view")
    public ResponseEntity<PredictionDTO.PredictionDetail> getPrediction(@PathVariable String id) {
        return ResponseEntity.ok(predictionService.getPredictionDetail(id));
    }

    // ========== MODEL STATS (for header bar) ==========

    @GetMapping("/models/{modelId}/stats")
    @Operation(summary = "Get model prediction stats",
               description = "Returns stats for header: total predictions, approved/rejected counts")
    public ResponseEntity<PredictionDTO.ModelPredictionStats> getModelStats(
            @PathVariable String modelId) {
        return ResponseEntity.ok(predictionService.getModelPredictionStats(modelId));
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
            @RequestParam(required = false) String projectId,
            @Parameter(description = "Include confidence scores in output")
            @RequestParam(defaultValue = "true") boolean includeConfidence,
            @Parameter(description = "Include class probabilities in output")
            @RequestParam(defaultValue = "true") boolean includeProbabilities) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(predictionService.startBatchPrediction(modelId, jobName, file, projectId,
                        includeConfidence, includeProbabilities));
    }

    @PostMapping(value = "/batch/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate batch file",
               description = "Validate CSV file before starting batch prediction")
    public ResponseEntity<PredictionDTO.BatchValidation> validateBatchFile(
            @RequestParam String modelId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(predictionService.validateBatchFile(modelId, file));
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

    @GetMapping("/batch/template/{modelId}")
    @Operation(summary = "Download template CSV",
               description = "Download a template CSV file with required columns")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable String modelId) {
        byte[] template = predictionService.generateTemplateCSV(modelId);
        ByteArrayResource resource = new ByteArrayResource(template);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"prediction_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    // ========== PREDICTION HISTORY ==========

    @GetMapping("/history")
    @Operation(summary = "Get prediction history with filters",
               description = "Returns paginated and filtered prediction history with stats")
    public ResponseEntity<PredictionDTO.HistoryResponse> getHistory(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String modelId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        PredictionDTO.HistoryFilter filter = PredictionDTO.HistoryFilter.builder()
                .projectId(projectId)
                .modelId(modelId)
                .type(type)
                .result(result)
                .dateRange(dateRange)
                .startDate(startDate)
                .endDate(endDate)
                .search(search)
                .page(page)
                .pageSize(pageSize)
                .build();
        
        return ResponseEntity.ok(predictionService.getHistory(filter));
    }

    @GetMapping("/history/export")
    @Operation(summary = "Export prediction history",
               description = "Export filtered prediction history as CSV")
    public ResponseEntity<Resource> exportHistory(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String modelId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        
        PredictionDTO.ExportRequest request = PredictionDTO.ExportRequest.builder()
                .projectId(projectId)
                .modelId(modelId)
                .type(type)
                .startDate(startDate)
                .endDate(endDate)
                .format("csv")
                .build();
        
        byte[] data = predictionService.exportHistory(request);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"prediction_history.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    // ========== API INTEGRATION ==========

    @GetMapping("/api-integration/{modelId}")
    @Operation(summary = "Get API integration info",
               description = "Returns API endpoint, key, usage stats, and code examples")
    public ResponseEntity<PredictionDTO.ApiIntegrationInfo> getApiIntegrationInfo(
            @PathVariable String modelId,
            @RequestParam(required = false) String projectId,
            HttpServletRequest request) {
        
        String baseUrl = getBaseUrl(request);
        return ResponseEntity.ok(apiKeyService.getApiIntegrationInfo(modelId, projectId, baseUrl));
    }

    @PostMapping("/api-keys")
    @Operation(summary = "Create API key",
               description = "Create a new API key for a model")
    public ResponseEntity<PredictionDTO.ApiKeyResponse> createApiKey(
            @RequestBody PredictionDTO.ApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.createApiKey(request));
    }

    @PostMapping("/api-keys/{apiKeyId}/regenerate")
    @Operation(summary = "Regenerate API key",
               description = "Regenerate an existing API key")
    public ResponseEntity<PredictionDTO.ApiKeyResponse> regenerateApiKey(
            @PathVariable String apiKeyId) {
        return ResponseEntity.ok(apiKeyService.regenerateApiKey(apiKeyId));
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    @Operation(summary = "Revoke API key",
               description = "Revoke an API key")
    public ResponseEntity<Void> revokeApiKey(@PathVariable String apiKeyId) {
        apiKeyService.revokeApiKey(apiKeyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api-usage/{modelId}")
    @Operation(summary = "Get API usage stats",
               description = "Returns API usage statistics for a model")
    public ResponseEntity<PredictionDTO.ApiUsageStats> getApiUsageStats(
            @PathVariable String modelId,
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(apiKeyService.getUsageStats(modelId, projectId));
    }

    @GetMapping("/rate-limit/{modelId}")
    @Operation(summary = "Get rate limit info",
               description = "Returns current rate limit status for a model")
    public ResponseEntity<PredictionDTO.RateLimitInfo> getRateLimitInfo(
            @PathVariable String modelId) {
        return ResponseEntity.ok(apiKeyService.getRateLimitInfo(modelId));
    }

    // ========== PUBLIC API ENDPOINT (for external applications) ==========

    @PostMapping("/v1/models/{modelId}/predict")
    @Operation(summary = "Public prediction API",
               description = "Public API endpoint for making predictions from external applications")
    public ResponseEntity<?> publicPredict(
            @PathVariable String modelId,
            @RequestBody PredictionDTO.RealtimeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract API key from header
        String apiKey = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        }

        // Validate API key if provided
        if (apiKey != null && !apiKeyService.validateAndCheckRateLimit(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or rate-limited API key"));
        }

        try {
            PredictionDTO.RealtimeResponse response = predictionService.predictRealtime(
                    modelId, request.getFeatures(), apiKey, null);
            
            // Record usage if API key provided
            if (apiKey != null) {
                apiKeyService.recordUsage(apiKey, response.getLatencyMs(), true);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (apiKey != null) {
                apiKeyService.recordUsage(apiKey, 0, false);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== HELPER METHODS ==========

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        return url.toString();
    }
}
