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
 * FastAPI Evaluation Client
 * Communicates with Python FastAPI service (Layer 3) for model evaluation
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

    public boolean isServiceAvailable() {
        try {
            String url = fastApiBaseUrl + evaluationEndpoint + "/threshold/health";
            Map response = restTemplate.getForObject(url, Map.class);
            log.info("FastAPI Evaluation service is available");
            return response != null;
        } catch (Exception e) {
            log.warn("FastAPI Evaluation service is not available: {}", e.getMessage());
            return false;
        }
    }

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

    public Map<String, Object> completeEvaluation(
            String modelId,
            double[] xTest,
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

            // Convert 1D x_test array to 2D for FastAPI compatibility
            if (xTest != null) {
                double[][] xTestReshaped = new double[yTest.length][1];
                for (int i = 0; i < yTest.length && i < xTest.length; i++) {
                    xTestReshaped[i][0] = xTest[i];
                }
                requestBody.put("x_test", xTestReshaped);
                log.info("Reshaped x_test from 1D to 2D ({}x1)", yTest.length);
            }

            // Required fields
            requestBody.put("y_test", yTest);
            requestBody.put("y_pred_proba", yPredProba);

            // Only include if provided
            if (yTrain != null) {
                requestBody.put("y_train", yTrain);
            }
            if (yPredTrain != null) {
                requestBody.put("y_pred_train", yPredTrain);
            }

            // Required fields
            requestBody.put("threshold", threshold);
            requestBody.put("cost_fp", costFp);
            requestBody.put("cost_fn", costFn);
            requestBody.put("revenue_tp", revenueTp);

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
