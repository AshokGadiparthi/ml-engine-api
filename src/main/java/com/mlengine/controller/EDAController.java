package com.mlengine.controller;

import com.mlengine.model.dto.EDADTO;
import com.mlengine.service.EDAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for EDA (Exploratory Data Analysis) operations
 * Provides endpoints for data quality assessment, feature analysis, and insights
 */
@RestController
@RequestMapping("/api/eda")
@RequiredArgsConstructor
@Tag(name = "EDA", description = "Exploratory Data Analysis endpoints for data quality assessment and insights")
@CrossOrigin
public class EDAController {
    
    private final EDAService edaService;
    
    /**
     * Analyze a dataset and generate EDA insights
     * POST /api/eda/analyze
     */
    @PostMapping("/analyze")
    @Operation(
            summary = "Analyze dataset",
            description = "Perform comprehensive exploratory data analysis on a dataset. " +
                    "Generates quality metrics, feature analysis, and actionable insights."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis completed successfully",
                    content = @Content(schema = @Schema(implementation = EDADTO.AnalysisResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Dataset not found"),
            @ApiResponse(responseCode = "500", description = "Analysis failed")
    })
    public ResponseEntity<EDADTO.AnalysisResponse> analyzeDataset(
            @Valid @RequestBody EDADTO.AnalysisRequest request
    ) {
        EDADTO.AnalysisResponse response = edaService.analyzeDataset(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get summary of EDA analysis
     * GET /api/eda/summary/{edaId}
     */
    @GetMapping("/summary/{edaId}")
    @Operation(
            summary = "Get EDA summary",
            description = "Retrieve executive summary of an EDA analysis including key metrics and recommendations"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EDADTO.SummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "EDA analysis not found")
    })
    public ResponseEntity<EDADTO.SummaryResponse> getSummary(
            @Parameter(description = "EDA analysis ID", example = "eda_12345")
            @PathVariable String edaId
    ) {
        EDADTO.SummaryResponse response = edaService.getSummary(edaId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get quality metrics
     * GET /api/eda/quality/{edaId}
     */
    @GetMapping("/quality/{edaId}")
    @Operation(
            summary = "Get quality metrics",
            description = "Retrieve detailed quality metrics including completeness, consistency, " +
                    "validity, and uniqueness scores"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quality metrics retrieved",
                    content = @Content(schema = @Schema(implementation = EDADTO.QualityResponse.class))),
            @ApiResponse(responseCode = "404", description = "EDA analysis not found")
    })
    public ResponseEntity<EDADTO.QualityResponse> getQualityMetrics(
            @Parameter(description = "EDA analysis ID")
            @PathVariable String edaId
    ) {
        EDADTO.QualityResponse response = edaService.getQualityMetrics(edaId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get insights
     * GET /api/eda/insights/{edaId}
     */
    @GetMapping("/insights/{edaId}")
    @Operation(
            summary = "Get data insights",
            description = "Retrieve all insights generated from the analysis, " +
                    "categorized by severity (Critical, High, Medium, Low, Info)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Insights retrieved",
                    content = @Content(schema = @Schema(implementation = EDADTO.InsightsResponse.class))),
            @ApiResponse(responseCode = "404", description = "EDA analysis not found")
    })
    public ResponseEntity<EDADTO.InsightsResponse> getInsights(
            @Parameter(description = "EDA analysis ID")
            @PathVariable String edaId
    ) {
        EDADTO.InsightsResponse response = edaService.getInsights(edaId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get feature analysis
     * GET /api/eda/features/{edaId}
     */
    @GetMapping("/features/{edaId}")
    @Operation(
            summary = "Get feature analysis",
            description = "Retrieve feature-level statistics including distributions, correlations, " +
                    "and data type information"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feature analysis retrieved",
                    content = @Content(schema = @Schema(implementation = EDADTO.FeaturesResponse.class))),
            @ApiResponse(responseCode = "404", description = "EDA analysis not found")
    })
    public ResponseEntity<EDADTO.FeaturesResponse> getFeatures(
            @Parameter(description = "EDA analysis ID")
            @PathVariable String edaId
    ) {
        try {
            EDAAnalysis analysis = edaRepository.findByEdaId(edaId)
                    .orElseThrow(() -> new RuntimeException("EDA analysis not found: " + edaId));
            
            // Deserialize features analysis from JSON
            EDADTO.FeaturesAnalysis featuresAnalysis = objectMapper.readValue(
                    analysis.getFeaturesAnalysisJson(),
                    EDADTO.FeaturesAnalysis.class
            );
            
            EDADTO.FeaturesResponse response = EDADTO.FeaturesResponse.builder()
                    .edaId(edaId)
                    .numericFeatures(featuresAnalysis.getNumericFeatures())
                    .categoricalFeatures(featuresAnalysis.getCategoricalFeatures())
                    .dateTimeFeatures(featuresAnalysis.getDateTimeFeatures())
                    .totalFeatures(featuresAnalysis.getTotalFeatures())
                    .statistics(featuresAnalysis.getStatistics())
                    .correlations(featuresAnalysis.getCorrelations())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving feature analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving features: " + e.getMessage());
        }
    }
    
    /**
     * Get feature importance ranking
     * GET /api/eda/importance/{edaId}
     */
    @GetMapping("/importance/{edaId}")
    @Operation(
            summary = "Get feature importance ranking",
            description = "Retrieve ranked features by importance. " +
                    "Importance is determined by data quality and correlation strength"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feature importance retrieved",
                    content = @Content(schema = @Schema(implementation = EDADTO.FeatureImportanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "EDA analysis not found")
    })
    public ResponseEntity<EDADTO.FeatureImportanceResponse> getFeatureImportance(
            @Parameter(description = "EDA analysis ID")
            @PathVariable String edaId,
            
            @Parameter(description = "Maximum number of features to return", example = "10")
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        EDADTO.FeatureImportanceResponse response = edaService.getFeatureImportance(edaId, limit);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get latest analysis for dataset
     * GET /api/eda/dataset/{datasetId}/latest
     */
    @GetMapping("/dataset/{datasetId}/latest")
    @Operation(
            summary = "Get latest analysis for dataset",
            description = "Retrieve the most recent EDA analysis for a specific dataset"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest analysis retrieved",
                    content = @Content(schema = @Schema(implementation = EDADTO.SummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "No analysis found for dataset")
    })
    public ResponseEntity<EDADTO.SummaryResponse> getLatestAnalysis(
            @Parameter(description = "Dataset ID")
            @PathVariable String datasetId
    ) {
        EDADTO.SummaryResponse response = edaService.getLatestAnalysis(datasetId);
        
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * List all analyses for a project
     * GET /api/eda/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "List project analyses",
            description = "Retrieve all EDA analyses for a project with pagination support"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analyses list retrieved"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<Page<EDADTO.ListItem>> listAnalyses(
            @Parameter(description = "Project ID")
            @PathVariable String projectId,
            
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(value = "size", defaultValue = "10") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            
            @Parameter(description = "Sort direction")
            @RequestParam(value = "direction", defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        Page<EDADTO.ListItem> response = edaService.listAnalyses(projectId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check for EDA service
     * GET /api/eda/health
     */
    @GetMapping("/health")
    @Operation(
            summary = "EDA service health check",
            description = "Check if the EDA service is operational"
    )
    @ApiResponse(responseCode = "200", description = "Service is operational",
            content = @Content(schema = @Schema(implementation = EDADTO.HealthResponse.class)))
    public ResponseEntity<EDADTO.HealthResponse> health() {
        EDADTO.HealthResponse response = edaService.getHealthStatus();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get comparison of analyses
     * GET /api/eda/compare
     */
    @GetMapping("/compare")
    @Operation(
            summary = "Compare EDA analyses",
            description = "Compare quality metrics between two datasets"
    )
    public ResponseEntity<?> compareAnalyses(
            @Parameter(description = "First EDA ID")
            @RequestParam String edaId1,
            
            @Parameter(description = "Second EDA ID")
            @RequestParam String edaId2
    ) {
        EDADTO.SummaryResponse analysis1 = edaService.getSummary(edaId1);
        EDADTO.SummaryResponse analysis2 = edaService.getSummary(edaId2);
        
        return ResponseEntity.ok(java.util.Map.of(
                "analysis1", analysis1,
                "analysis2", analysis2,
                "scoreDifference", analysis1.getQualityScore() - analysis2.getQualityScore()
        ));
    }
    
    /**
     * Get recommendations
     * GET /api/eda/{edaId}/recommendations
     */
    @GetMapping("/{edaId}/recommendations")
    @Operation(
            summary = "Get improvement recommendations",
            description = "Get actionable recommendations based on EDA analysis"
    )
    public ResponseEntity<?> getRecommendations(
            @Parameter(description = "EDA analysis ID")
            @PathVariable String edaId
    ) {
        EDADTO.InsightsResponse insights = edaService.getInsights(edaId);
        
        java.util.List<String> recommendations = insights.getInsights().stream()
                .map(EDADTO.Insight::getRecommendation)
                .distinct()
                .toList();
        
        return ResponseEntity.ok(java.util.Map.of(
                "edaId", edaId,
                "recommendationCount", recommendations.size(),
                "recommendations", recommendations
        ));
    }
}
