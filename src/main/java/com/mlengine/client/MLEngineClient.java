package com.mlengine.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for communicating with Python ML Engine FastAPI service.
 */
@Slf4j
@Service
public class MLEngineClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ml-engine.api.base-url:http://localhost:8000/api}")
    private String baseUrl;

    @Value("${ml-engine.api.timeout:30000}")
    private int timeout;

    public MLEngineClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ============ AUTOML ============

    /**
     * Start AutoML job.
     */
    public Map<String, Object> startAutoML(
            String datasetPath,
            String targetColumn,
            String problemType,
            int cvFolds,
            boolean useFeatureEngineering,
            String scalingMethod
    ) {
        String url = baseUrl + "/automl/start";
        
        Map<String, Object> request = new HashMap<>();
        // Generate a dataset_id from the path (FastAPI requires this)
        String datasetId = datasetPath != null ? 
            datasetPath.substring(datasetPath.lastIndexOf('/') + 1).replace(".csv", "") : 
            "dataset_" + System.currentTimeMillis();
        request.put("dataset_id", datasetId);
        request.put("dataset_path", datasetPath);
        request.put("target_column", targetColumn);
        request.put("problem_type", problemType.toLowerCase());
        request.put("cv_folds", cvFolds);
        request.put("use_feature_engineering", useFeatureEngineering);
        request.put("scaling_method", scalingMethod);
        
        log.info("Starting AutoML job: {}", request);
        
        return postRequest(url, request);
    }

    /**
     * Get AutoML job progress.
     */
    public Map<String, Object> getAutoMLProgress(String jobId) {
        String url = baseUrl + "/automl/jobs/" + jobId + "/progress";
        return getRequest(url);
    }

    /**
     * Get AutoML job results.
     */
    public Map<String, Object> getAutoMLResults(String jobId) {
        String url = baseUrl + "/automl/jobs/" + jobId + "/results";
        return getRequest(url);
    }

    /**
     * Stop AutoML job.
     */
    public Map<String, Object> stopAutoML(String jobId) {
        String url = baseUrl + "/automl/jobs/" + jobId + "/stop";
        return postRequest(url, new HashMap<>());
    }

    // ============ TRAINING ============

    /**
     * Start training job.
     */
    public Map<String, Object> startTraining(
            String datasetPath,
            String targetColumn,
            String algorithm,
            String problemType,
            Map<String, Object> config
    ) {
        String url = baseUrl + "/training/start";
        
        Map<String, Object> request = new HashMap<>();
        request.put("dataset_path", datasetPath);
        request.put("target_column", targetColumn);
        request.put("algorithm", algorithm.toLowerCase());
        request.put("problem_type", problemType.toLowerCase());
        if (config != null) {
            request.put("config", config);
        }
        
        log.info("Starting training job: {}", request);
        
        return postRequest(url, request);
    }

    /**
     * Get training job progress.
     */
    public Map<String, Object> getTrainingProgress(String jobId) {
        String url = baseUrl + "/training/jobs/" + jobId + "/progress";
        return getRequest(url);
    }

    /**
     * Get training job results.
     */
    public Map<String, Object> getTrainingResults(String jobId) {
        String url = baseUrl + "/training/jobs/" + jobId + "/results";
        return getRequest(url);
    }

    // ============ PREDICTIONS ============

    /**
     * Make single prediction.
     */
    public Map<String, Object> predict(String modelId, Map<String, Object> features) {
        String url = baseUrl + "/predictions/realtime/" + modelId;
        return postRequest(url, features);
    }

    /**
     * Make batch predictions.
     */
    public Map<String, Object> predictBatch(String modelId, Object data) {
        String url = baseUrl + "/predictions/batch/" + modelId;
        
        Map<String, Object> request = new HashMap<>();
        request.put("data", data);
        
        return postRequest(url, request);
    }

    // ============ MODELS ============

    /**
     * List all models.
     */
    public Map<String, Object> listModels() {
        String url = baseUrl + "/models";
        return getRequest(url);
    }

    /**
     * Get model details.
     */
    public Map<String, Object> getModel(String modelId) {
        String url = baseUrl + "/models/" + modelId;
        return getRequest(url);
    }

    /**
     * Deploy model.
     */
    public Map<String, Object> deployModel(String modelId) {
        String url = baseUrl + "/models/" + modelId + "/deploy";
        return postRequest(url, new HashMap<>());
    }

    // ============ HEALTH ============

    /**
     * Check ML Engine health.
     */
    public Map<String, Object> healthCheck() {
        try {
            String url = baseUrl.replace("/api", "") + "/health";
            return getRequest(url);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * Check if ML Engine is available.
     */
    public boolean isAvailable() {
        try {
            Map<String, Object> health = healthCheck();
            return "ok".equals(health.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    // ============ HELPER METHODS ============

    private Map<String, Object> getRequest(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );
            
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("GET request failed: {} - {}", url, e.getMessage());
            throw new RuntimeException("ML Engine request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> postRequest(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("POST request failed: {} - {}", url, e.getMessage());
            throw new RuntimeException("ML Engine request failed: " + e.getMessage(), e);
        }
    }
}
