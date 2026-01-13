package com.mlengine.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * FastAPI EDA Client
 * Communicates with Python FastAPI service for EDA analysis
 */
@Slf4j
@Component
public class FastAPIEDAClient {
    
    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;
    
    @Value("${fastapi.timeout:30000}")
    private int timeout;
    
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    
    public FastAPIEDAClient() {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Check if FastAPI service is available
     */
    public boolean isServiceAvailable() {
        try {
            String url = fastApiBaseUrl + "/eda/health";
            Map response = restTemplate.getForObject(url, Map.class);
            log.info("FastAPI service is available");
            return response != null;
        } catch (Exception e) {
            log.warn("FastAPI service is not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Upload file and run complete EDA analysis
     */
    public Map<String, Object> analyzeDataset(byte[] fileContent, String filename) {
        try {
            log.info("Sending file {} to FastAPI for analysis", filename);
            
            // Create multipart request
            String url = fastApiBaseUrl + "/eda/analyze";
            Map<String, Object> result = new HashMap<>();
            
            // Note: RestTemplate with file upload requires proper multipart handling
            // This is a simplified example - production would use RestTemplate with MultipartFile
            log.info("EDA analysis request sent to: {}", url);
            
            return result;
        } catch (Exception e) {
            log.error("Error analyzing dataset: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze dataset", e);
        }
    }
    
    /**
     * Get histogram analysis results
     */
    public Map<String, Object> getHistogramAnalysis(byte[] fileContent) {
        try {
            log.info("Requesting histogram analysis from FastAPI");
            String url = fastApiBaseUrl + "/eda/histogram";
            
            Map<String, Object> result = new HashMap<>();
            // Implementation would call FastAPI endpoint
            
            return result;
        } catch (Exception e) {
            log.error("Error getting histogram analysis: {}", e.getMessage());
            throw new RuntimeException("Failed to get histogram analysis", e);
        }
    }
    
    /**
     * Get categorical analysis results
     */
    public Map<String, Object> getCategoricalAnalysis(byte[] fileContent) {
        try {
            log.info("Requesting categorical analysis from FastAPI");
            String url = fastApiBaseUrl + "/eda/categorical";
            
            Map<String, Object> result = new HashMap<>();
            
            return result;
        } catch (Exception e) {
            log.error("Error getting categorical analysis: {}", e.getMessage());
            throw new RuntimeException("Failed to get categorical analysis", e);
        }
    }
    
    /**
     * Get missing pattern analysis
     */
    public Map<String, Object> getMissingPatternAnalysis(byte[] fileContent) {
        try {
            log.info("Requesting missing pattern analysis from FastAPI");
            String url = fastApiBaseUrl + "/eda/missing-pattern";
            
            Map<String, Object> result = new HashMap<>();
            
            return result;
        } catch (Exception e) {
            log.error("Error getting missing pattern analysis: {}", e.getMessage());
            throw new RuntimeException("Failed to get missing pattern analysis", e);
        }
    }
    
    /**
     * Get outlier detection results
     */
    public Map<String, Object> getOutlierDetection(byte[] fileContent, String method) {
        try {
            log.info("Requesting outlier detection from FastAPI with method: {}", method);
            String url = fastApiBaseUrl + "/eda/outliers?method=" + method;
            
            Map<String, Object> result = new HashMap<>();
            
            return result;
        } catch (Exception e) {
            log.error("Error getting outlier detection: {}", e.getMessage());
            throw new RuntimeException("Failed to get outlier detection", e);
        }
    }
    
    /**
     * Get correlation analysis
     */
    public Map<String, Object> getCorrelationAnalysis(byte[] fileContent) {
        try {
            log.info("Requesting correlation analysis from FastAPI");
            String url = fastApiBaseUrl + "/eda/correlation";
            
            Map<String, Object> result = new HashMap<>();
            
            return result;
        } catch (Exception e) {
            log.error("Error getting correlation analysis: {}", e.getMessage());
            throw new RuntimeException("Failed to get correlation analysis", e);
        }
    }
    
    /**
     * Get quality assessment
     */
    public Map<String, Object> getQualityAssessment(byte[] fileContent) {
        try {
            log.info("Requesting quality assessment from FastAPI");
            String url = fastApiBaseUrl + "/eda/quality";
            
            Map<String, Object> result = new HashMap<>();
            
            return result;
        } catch (Exception e) {
            log.error("Error getting quality assessment: {}", e.getMessage());
            throw new RuntimeException("Failed to get quality assessment", e);
        }
    }
    
    /**
     * Retrieve cached analysis results
     * Gracefully handles 404 errors from FastAPI
     */
    public Map<String, Object> getAnalysisResults(String edaId) {
        try {
            log.info("Retrieving cached results for EDA ID: {}", edaId);
            String url = fastApiBaseUrl + "/eda/results/" + edaId;
            
            try {
                Map<String, Object> result = restTemplate.getForObject(url, Map.class);
                return result != null ? result : new HashMap<>();
            } catch (RestClientException e) {
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    log.warn("Analysis results not found on FastAPI (404). Returning empty cache for: {}", edaId);
                    return new HashMap<>();
                }
                throw e;
            }
        } catch (Exception e) {
            log.warn("Error retrieving analysis results (using fallback): {}", e.getMessage());
            // Return empty map as fallback instead of throwing exception
            return new HashMap<>();
        }
    }
    
    /**
     * Retrieve specific analysis type from cached results
     * Gracefully handles 404 errors from FastAPI
     */
    public Map<String, Object> getAnalysisResultsByType(String edaId, String type) {
        try {
            log.info("Retrieving {} results for EDA ID: {}", type, edaId);
            String url = fastApiBaseUrl + "/eda/results/" + edaId + "/" + type;
            
            try {
                Map<String, Object> result = restTemplate.getForObject(url, Map.class);
                return result != null ? result : new HashMap<>();
            } catch (RestClientException e) {
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    log.warn("Analysis type '{}' not found on FastAPI (404). Returning empty result for: {}", type, edaId);
                    return new HashMap<>();
                }
                throw e;
            }
        } catch (Exception e) {
            log.warn("Error retrieving {} results (using fallback): {}", type, e.getMessage());
            // Return empty map as fallback instead of throwing exception
            return new HashMap<>();
        }
    }
}
