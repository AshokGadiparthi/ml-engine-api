package com.mlengine.controller;

import com.mlengine.service.EDAEnhancedService;
import com.mlengine.model.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

/**
 * Enhanced EDA Controller - Advanced Analysis Features
 * Path: /api/eda/analysis/* (SEPARATE from original /api/eda/*)
 * 
 * This controller provides advanced EDA analysis endpoints.
 * All paths use /api/eda/analysis/ prefix to avoid conflicts with EDAController.
 * 
 * Endpoints:
 * - POST /api/eda/analysis/upload - Upload CSV and run analysis
 * - GET /api/eda/analysis/histogram/{edaId}/{featureName}
 * - GET /api/eda/analysis/categorical/{edaId}/{featureName}
 * - GET /api/eda/analysis/missing-pattern/{edaId}
 * - GET /api/eda/analysis/outliers/{edaId}
 * - GET /api/eda/analysis/correlation/{edaId}
 * - GET /api/eda/analysis/quality/{edaId}
 */
@Slf4j
@RestController
@RequestMapping("/api/eda/analysis")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "EDA Analysis", description = "Advanced EDA analysis features (histogram, categorical, outliers, correlation, quality assessment)")
public class EDAEnhancedController {
    
    @Autowired
    private EDAEnhancedService edaService;
    
    @Operation(summary = "Histogram Analysis", description = "Get histogram distribution for numeric feature")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Histogram data retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/histogram/{edaId}/{featureName}")
    public ResponseEntity<?> getHistogramData(
            @Parameter(description = "EDA Analysis ID", required = true)
            @PathVariable String edaId,
            @Parameter(description = "Feature name", required = true)
            @PathVariable String featureName) {
        log.info("Getting histogram data for feature: {}", featureName);
        
        try {
            Map<String, Object> results = edaService.getAnalysisResults(edaId);
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "featureName", featureName,
                            "results", results,
                            "status", "success",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (Exception e) {
            log.error("Error getting histogram data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get histogram data", "message", e.getMessage()));
        }
    }
    
    @Operation(summary = "Categorical Analysis", description = "Get categorical distribution for feature")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categorical data retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/categorical/{edaId}/{featureName}")
    public ResponseEntity<?> getCategoricalData(
            @Parameter(description = "EDA Analysis ID", required = true)
            @PathVariable String edaId,
            @Parameter(description = "Feature name", required = true)
            @PathVariable String featureName,
            @Parameter(description = "Top N values to return", example = "10")
            @RequestParam(defaultValue = "10") int topN) {
        log.info("Getting categorical data for feature: {} (top {})", featureName, topN);
        
        try {
            Map<String, Object> results = edaService.getAnalysisResults(edaId);
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "featureName", featureName,
                            "topN", topN,
                            "results", results,
                            "status", "success",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (Exception e) {
            log.error("Error getting categorical data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get categorical data", "message", e.getMessage()));
        }
    }
    
    @Operation(summary = "Missing Pattern Analysis", description = "Analyze missing data patterns in dataset")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Missing pattern data retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/missing-pattern/{edaId}")
    public ResponseEntity<?> getMissingPatternData(
            @Parameter(description = "EDA Analysis ID", required = true)
            @PathVariable String edaId) {
        log.info("Getting missing pattern data for EDA ID: {}", edaId);
        
        try {
            Map<String, Object> results = edaService.getAnalysisResults(edaId);
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "edaId", edaId,
                            "results", results,
                            "status", "success",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (Exception e) {
            log.error("Error getting missing pattern data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get missing pattern data", "message", e.getMessage()));
        }
    }
    
    @Operation(summary = "Outlier Detection", description = "Detect outliers in numeric features using IQR method")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Outlier data retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/outliers/{edaId}")
    public ResponseEntity<?> getOutliersData(
            @Parameter(description = "EDA Analysis ID", required = true)
            @PathVariable String edaId,
            @Parameter(description = "Outlier detection method", example = "IQR")
            @RequestParam(defaultValue = "IQR") String method) {
        log.info("Getting outliers data for EDA ID: {} with method: {}", edaId, method);
        
        try {
            Map<String, Object> results = edaService.getAnalysisResults(edaId);
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "edaId", edaId,
                            "method", method,
                            "results", results,
                            "status", "success",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (Exception e) {
            log.error("Error getting outliers data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get outliers data", "message", e.getMessage()));
        }
    }
    
    @Operation(summary = "Correlation Analysis", description = "Get correlation matrix and strong correlations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Correlation data retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/correlation/{edaId}")
    public ResponseEntity<?> getCorrelationData(
            @Parameter(description = "EDA Analysis ID", required = true)
            @PathVariable String edaId,
            @Parameter(description = "Strong correlation threshold", example = "0.7")
            @RequestParam(defaultValue = "0.7") double threshold) {
        log.info("Getting correlation data for EDA ID: {} with threshold: {}", edaId, threshold);
        
        try {
            Map<String, Object> results = edaService.getAnalysisResults(edaId);
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "edaId", edaId,
                            "threshold", threshold,
                            "results", results,
                            "status", "success",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (Exception e) {
            log.error("Error getting correlation data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get correlation data", "message", e.getMessage()));
        }
    }
    
    @Operation(summary = "Quality Assessment", description = "Get data quality score, assessment level, and recommendations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quality assessment retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/quality/{edaId}")
    public ResponseEntity<?> getQualityAssessment(
            @Parameter(description = "EDA Analysis ID", required = true)
            @PathVariable String edaId) {
        log.info("Getting quality assessment for EDA ID: {}", edaId);
        
        try {
            Map<String, Object> quality = edaService.getQualityAssessment(new byte[0]);
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "edaId", edaId,
                            "quality_score", quality.getOrDefault("quality_score", 0),
                            "assessment", quality.getOrDefault("assessment", "Unknown"),
                            "metrics", quality.getOrDefault("metrics", Map.of()),
                            "recommendations", quality.getOrDefault("recommendations", List.of()),
                            "status", "success",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (Exception e) {
            log.error("Error getting quality assessment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get quality assessment", "message", e.getMessage()));
        }
    }
    
    @Operation(summary = "Upload and Analyze", description = "Upload CSV file and run complete EDA analysis")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File analyzed successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request or invalid file"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/upload")
    public ResponseEntity<?> analyzeFile(
            @Parameter(description = "CSV file to analyze", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("Analyzing file: {}", file.getOriginalFilename());
        
        try {
            byte[] fileData = file.getBytes();
            Map<String, Object> results = edaService.runCompleteAnalysis(fileData, file.getOriginalFilename());
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "filename", file.getOriginalFilename(),
                            "size", file.getSize(),
                            "results", results,
                            "status", "success",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (IOException e) {
            log.error("Error reading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Failed to read file",
                            "message", e.getMessage(),
                            "status", "error",
                            "timestamp", System.currentTimeMillis()
                    ));
        } catch (Exception e) {
            log.error("Error analyzing file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to analyze file",
                            "message", e.getMessage(),
                            "status", "error",
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }
}
