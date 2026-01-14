package com.mlengine.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * FastAPI Evaluation Client - FIXED VERSION
 * Communicates with Python FastAPI service (Layer 3) for model evaluation
 *
 * FIXES:
 * 1. Fixed health endpoint path (removed /threshold)
 * 2. Updated completeEvaluation to properly handle 2D X_test arrays
 */
@Slf4j
@Component
public class FastAPIEvaluationClient {

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    @Value("${fastapi.timeout:30000}")
    private int timeout;

    @Value("${fastapi.evaluation-endpoint:/api/evaluation}")
    private String evaluationEndpoint;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    public FastAPIEvaluationClient() {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * Check if FastAPI evaluation service is available
     * FIXED: Corrected health endpoint path
     */
    public boolean isServiceAvailable() {
        try {
            // FIXED: Changed from /threshold/health to /health
            String url = fastApiBaseUrl + evaluationEndpoint + "/health";
            Map response = restTemplate.getForObject(url, Map.class);
            log.info("FastAPI Evaluation service is available");
            return response != null;
        } catch (Exception e) {
            log.warn("FastAPI Evaluation service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Evaluate model at specific threshold
     */
    public Map<String, Object> evaluateWithThreshold(
            String modelId,
            double[] yTrue,
            double[] yPredProba,
            double threshold,
            String... targetNames
    ) {
        try {
            log.info("Evaluating model {} at threshold {}", modelId, threshold);

            String url = fastApiBaseUrl + evaluationEndpoint + "/threshold/" + modelId;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("y_true", yTrue);
            requestBody.put("y_pred_proba", yPredProba);
            requestBody.put("threshold", threshold);
            requestBody.put("target_names", targetNames != null ? Arrays.asList(targetNames) : Arrays.asList("negative", "positive"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            log.info("Threshold evaluation completed for model {}", modelId);
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error evaluating model at threshold: {}", e.getMessage());
            throw new RuntimeException("Failed to evaluate model at threshold", e);
        }
    }

    /**
     * Calculate business impact of predictions
     */
    public Map<String, Object> calculateBusinessImpact(
            String modelId,
            Map<String, Object> evaluationResult,
            double costFalsePositive,
            double costFalseNegative,
            double reveneTruePositive,
            double volume
    ) {
        try {
            log.info("Calculating business impact for model {}", modelId);

            String url = fastApiBaseUrl + evaluationEndpoint + "/business-impact/" + modelId;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("evaluation_result", evaluationResult);
            requestBody.put("cost_false_positive", costFalsePositive);
            requestBody.put("cost_false_negative", costFalseNegative);
            requestBody.put("revenue_true_positive", reveneTruePositive);
            requestBody.put("volume", volume);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            log.info("Business impact calculation completed for model {}", modelId);
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error calculating business impact: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate business impact", e);
        }
    }

    /**
     * Find optimal threshold for profit maximization
     */
    public Map<String, Object> getOptimalThreshold(
            String modelId,
            double[] yTrue,
            double[] yPredProba,
            double costFalsePositive,
            double costFalseNegative,
            double revenueTruePositive
    ) {
        try {
            log.info("Finding optimal threshold for model {}", modelId);

            String url = fastApiBaseUrl + evaluationEndpoint + "/optimal-threshold/" + modelId;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("y_true", yTrue);
            requestBody.put("y_pred_proba", yPredProba);
            requestBody.put("cost_false_positive", costFalsePositive);
            requestBody.put("cost_false_negative", costFalseNegative);
            requestBody.put("revenue_true_positive", revenueTruePositive);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            log.info("Optimal threshold found for model {}", modelId);
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error finding optimal threshold: {}", e.getMessage());
            throw new RuntimeException("Failed to find optimal threshold", e);
        }
    }

    /**
     * Assess production readiness with 18-point checklist
     */
    public Map<String, Object> assessProductionReadiness(
            String modelId,
            Map<String, Object> evalResult,
            Map<String, Object> learningCurve,
            Map<String, Object> businessImpact,
            Map<String, Object> featureImportance
    ) {
        try {
            log.info("Assessing production readiness for model {}", modelId);

            String url = fastApiBaseUrl + evaluationEndpoint + "/production-readiness/" + modelId;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("eval_result", evalResult);
            requestBody.put("learning_curve", learningCurve);
            requestBody.put("business_impact", businessImpact);
            requestBody.put("feature_importance", featureImportance);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            log.info("Production readiness assessment completed for model {}", modelId);
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error assessing production readiness: {}", e.getMessage());
            throw new RuntimeException("Failed to assess production readiness", e);
        }
    }

    /**
     * Complete model evaluation in one call
     *
     * FIXED:
     * - Changed xTest parameter from double[] to double[][]
     * - Proper handling of 2D feature matrix
     * - X_test field name alignment with FastAPI backend
     */
    public Map<String, Object> completeEvaluation(
            String modelId,
            double[][] xTest,           // FIXED: Changed from double[] to double[][]
            double[] yTest,
            double[] yPredProba,
            double[] yTrain,
            double[] yPredTrain,
            double threshold,
            double costFp,
            double costFn,
            double revenueTp
    ) {
        try {
            log.info("Running complete evaluation for model {}", modelId);

            String url = fastApiBaseUrl + evaluationEndpoint + "/complete/" + modelId;

            Map<String, Object> requestBody = new HashMap<>();

            // FIXED: X_test is now a 2D array, send directly without reshaping
            if (xTest != null && xTest.length > 0) {
                requestBody.put("X_test", xTest);  // Send 2D array directly
                log.info("X_test shape: {}x{}", xTest.length, xTest[0].length);
            } else {
                log.warn("X_test is null or empty for model {}", modelId);
            }

            // Required fields
            requestBody.put("y_test", yTest);
            requestBody.put("y_pred_proba", yPredProba);

            // Optional fields - only include if provided
            if (yTrain != null && yTrain.length > 0) {
                requestBody.put("y_train", yTrain);
                log.debug("Including y_train with {} samples", yTrain.length);
            }
            if (yPredTrain != null && yPredTrain.length > 0) {
                requestBody.put("y_pred_train", yPredTrain);
                log.debug("Including y_pred_train with {} samples", yPredTrain.length);
            }

            // Required fields
            requestBody.put("threshold", threshold);
            requestBody.put("cost_fp", costFp);
            requestBody.put("cost_fn", costFn);
            requestBody.put("revenue_tp", revenueTp);

            log.debug("Complete evaluation request body: {}", requestBody.keySet());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            log.info("Complete evaluation finished for model {}", modelId);
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error in complete evaluation: {}", e.getMessage());
            throw new RuntimeException("Failed to complete evaluation", e);
        }
    }

    /**
     * Get health status of evaluation service
     */
    public Map<String, Object> getHealthStatus() {
        try {
            String url = fastApiBaseUrl + evaluationEndpoint + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting health status: {}", e.getMessage());
            return Map.of("status", "unavailable", "error", e.getMessage());
        }
    }
}