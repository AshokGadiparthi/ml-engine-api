package com.mlengine.service;

import com.mlengine.client.FastAPIEDAClient;
import com.mlengine.model.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * EDA Enhanced Service
 * Orchestrates EDA analysis across 3-layer architecture
 */
@Slf4j
@Service
public class EDAEnhancedService {
    
    @Autowired
    private FastAPIEDAClient fastAPIClient;
    
    /**
     * Run complete EDA analysis
     */
    public Map<String, Object> runCompleteAnalysis(byte[] fileData, String filename) {
        log.info("Running complete EDA analysis for file: {}", filename);
        
        try {
            // Call FastAPI service
            Map<String, Object> results = fastAPIClient.analyzeDataset(fileData, filename);
            
            log.info("Complete EDA analysis completed for: {}", filename);
            return results;
        } catch (Exception e) {
            log.error("Error running complete analysis: {}", e.getMessage());
            throw new RuntimeException("Failed to run complete analysis", e);
        }
    }
    
    /**
     * Get histogram analysis data
     */
    public Map<String, HistogramResponseDTO> getHistogramData(byte[] fileData) {
        log.info("Getting histogram analysis data");
        
        try {
            Map<String, Object> results = fastAPIClient.getHistogramAnalysis(fileData);
            // Convert to HistogramResponseDTO format
            return convertToHistogramDTOs(results);
        } catch (Exception e) {
            log.error("Error getting histogram data: {}", e.getMessage());
            throw new RuntimeException("Failed to get histogram data", e);
        }
    }
    
    /**
     * Get categorical distribution data
     */
    public Map<String, CategoricalResponseDTO> getCategoricalData(byte[] fileData) {
        log.info("Getting categorical analysis data");
        
        try {
            Map<String, Object> results = fastAPIClient.getCategoricalAnalysis(fileData);
            // Convert to CategoricalResponseDTO format
            return convertToCategoricalDTOs(results);
        } catch (Exception e) {
            log.error("Error getting categorical data: {}", e.getMessage());
            throw new RuntimeException("Failed to get categorical data", e);
        }
    }
    
    /**
     * Get missing pattern data
     */
    public MissingPatternResponseDTO getMissingPatternData(byte[] fileData) {
        log.info("Getting missing pattern analysis data");
        
        try {
            Map<String, Object> results = fastAPIClient.getMissingPatternAnalysis(fileData);
            // Convert to MissingPatternResponseDTO
            return convertToMissingPatternDTO(results);
        } catch (Exception e) {
            log.error("Error getting missing pattern data: {}", e.getMessage());
            throw new RuntimeException("Failed to get missing pattern data", e);
        }
    }
    
    /**
     * Get outlier detection data
     */
    public Map<String, OutlierResponseDTO> getOutlierData(byte[] fileData, String method) {
        log.info("Getting outlier detection data with method: {}", method);
        
        try {
            Map<String, Object> results = fastAPIClient.getOutlierDetection(fileData, method);
            // Convert to OutlierResponseDTO format
            return convertToOutlierDTOs(results);
        } catch (Exception e) {
            log.error("Error getting outlier data: {}", e.getMessage());
            throw new RuntimeException("Failed to get outlier data", e);
        }
    }
    
    /**
     * Get correlation analysis data
     */
    public CorrelationResponseDTO getCorrelationData(byte[] fileData) {
        log.info("Getting correlation analysis data");
        
        try {
            Map<String, Object> results = fastAPIClient.getCorrelationAnalysis(fileData);
            // Convert to CorrelationResponseDTO
            return convertToCorrelationDTO(results);
        } catch (Exception e) {
            log.error("Error getting correlation data: {}", e.getMessage());
            throw new RuntimeException("Failed to get correlation data", e);
        }
    }
    
    /**
     * Get data quality assessment
     */
    public Map<String, Object> getQualityAssessment(byte[] fileData) {
        log.info("Getting data quality assessment");
        
        try {
            Map<String, Object> results = fastAPIClient.getQualityAssessment(fileData);
            return results;
        } catch (Exception e) {
            log.error("Error getting quality assessment: {}", e.getMessage());
            throw new RuntimeException("Failed to get quality assessment", e);
        }
    }
    
    /**
     * Retrieve cached analysis results
     */
    public Map<String, Object> getAnalysisResults(String edaId) {
        log.info("Retrieving cached analysis results for: {}", edaId);
        
        try {
            return fastAPIClient.getAnalysisResults(edaId);
        } catch (Exception e) {
            log.error("Error retrieving analysis results: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve analysis results", e);
        }
    }
    
    /**
     * Check if FastAPI service is available
     */
    public boolean isEDAServiceAvailable() {
        return fastAPIClient.isServiceAvailable();
    }
    
    // ==================== CONVERSION METHODS ====================
    
    private Map<String, HistogramResponseDTO> convertToHistogramDTOs(Map<String, Object> results) {
        Map<String, HistogramResponseDTO> dtos = new HashMap<>();
        // Conversion logic here
        return dtos;
    }
    
    private Map<String, CategoricalResponseDTO> convertToCategoricalDTOs(Map<String, Object> results) {
        Map<String, CategoricalResponseDTO> dtos = new HashMap<>();
        // Conversion logic here
        return dtos;
    }
    
    private MissingPatternResponseDTO convertToMissingPatternDTO(Map<String, Object> results) {
        // Conversion logic here
        return MissingPatternResponseDTO.builder().build();
    }
    
    private Map<String, OutlierResponseDTO> convertToOutlierDTOs(Map<String, Object> results) {
        Map<String, OutlierResponseDTO> dtos = new HashMap<>();
        // Conversion logic here
        return dtos;
    }
    
    private CorrelationResponseDTO convertToCorrelationDTO(Map<String, Object> results) {
        // Conversion logic here
        return CorrelationResponseDTO.builder().build();
    }
}
