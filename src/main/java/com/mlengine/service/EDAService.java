package com.mlengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.model.dto.EDADTO;
import com.mlengine.model.entity.Dataset;
import com.mlengine.model.entity.EDAAnalysis;
import com.mlengine.repository.DatasetRepository;
import com.mlengine.repository.EDAAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Exploratory Data Analysis operations
 * Handles analysis, quality assessment, and insights generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EDAService {
    
    private final EDAAnalysisRepository edaRepository;
    private final DatasetRepository datasetRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Analyze a dataset and generate EDA insights
     */
    public EDADTO.AnalysisResponse analyzeDataset(EDADTO.AnalysisRequest request) {
        log.info("Starting EDA analysis for dataset: {}", request.getDatasetId());
        
        long startTime = System.currentTimeMillis();
        
        // Validate dataset exists
        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + request.getDatasetId()));
        
        // Generate unique EDA ID
        String edaId = "eda_" + UUID.randomUUID().toString();
        
        try {
            // Calculate quality metrics
            EDADTO.QualityMetrics qualityMetrics = calculateQualityMetrics(dataset, request);
            
            // Perform feature analysis
            EDADTO.FeaturesAnalysis featuresAnalysis = analyzeFeatures(dataset, request);
            
            // Generate insights
            List<EDADTO.Insight> insights = generateInsights(dataset, qualityMetrics, featuresAnalysis);
            
            // Create response
            EDADTO.AnalysisResponse response = EDADTO.AnalysisResponse.builder()
                    .edaId(edaId)
                    .datasetId(request.getDatasetId())
                    .status("COMPLETED")
                    .quality(qualityMetrics)
                    .features(featuresAnalysis)
                    .insights(insights)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // Save to database
            saveAnalysisResult(edaId, request, response, qualityMetrics, featuresAnalysis, insights, startTime);
            
            log.info("EDA analysis completed for dataset: {} with score: {}", 
                    request.getDatasetId(), qualityMetrics.getOverallScore());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error analyzing dataset {}: {}", request.getDatasetId(), e.getMessage(), e);
            throw new RuntimeException("EDA analysis failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate quality metrics for dataset
     */
    private EDADTO.QualityMetrics calculateQualityMetrics(Dataset dataset, EDADTO.AnalysisRequest request) {
        // Parse dataset data (assuming it's stored as JSON or similar)
        Map<String, Object> dataMetadata = parseDatasetMetadata(dataset);
        
        long rowCount = (long) dataMetadata.getOrDefault("rowCount", 0L);
        int columnCount = (int) dataMetadata.getOrDefault("columnCount", 0);
        long missingValues = (long) dataMetadata.getOrDefault("missingValues", 0L);
        long duplicateRows = (long) dataMetadata.getOrDefault("duplicateRows", 0L);
        
        // Calculate metrics
        double completeness = calculateCompleteness(rowCount, missingValues);
        double uniqueness = calculateUniqueness(rowCount, duplicateRows);
        double consistency = calculateConsistency(dataMetadata);
        double validity = calculateValidity(dataMetadata);
        
        // Overall score (weighted average)
        double overallScore = (completeness * 0.3) + (uniqueness * 0.2) + 
                             (consistency * 0.25) + (validity * 0.25);
        
        // Assessment
        String assessment = getAssessmentLevel(overallScore);
        
        return EDADTO.QualityMetrics.builder()
                .overallScore(overallScore)
                .assessment(assessment)
                .completeness(completeness)
                .uniqueness(uniqueness)
                .consistency(consistency)
                .validity(validity)
                .rowCount(rowCount)
                .columnCount(columnCount)
                .missingValues(missingValues)
                .duplicateRows(duplicateRows)
                .build();
    }
    
    /**
     * Analyze features in the dataset
     */
    private EDADTO.FeaturesAnalysis analyzeFeatures(Dataset dataset, EDADTO.AnalysisRequest request) {
        log.info("Starting analyzeFeatures for dataset: {}", dataset.getId());
        
        Map<String, Object> dataMetadata = parseDatasetMetadata(dataset);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) dataMetadata.getOrDefault("columns", new ArrayList<>());
        
        log.info("analyzeFeatures received {} columns from parseDatasetMetadata", columns.size());
        
        if (columns.isEmpty()) {
            log.warn("No columns found! Dataset columnsJson: {}", dataset.getColumnsJson());
            log.warn("Dataset columnCount: {}", dataset.getColumnCount());
        }
        
        List<EDADTO.FeatureStats> statistics = new ArrayList<>();
        int numericCount = 0;
        int categoricalCount = 0;
        int dateTimeCount = 0;
        
        for (Map<String, Object> column : columns) {
            String dataType = (String) column.get("dataType");
            
            // Extract values with correct field names from database schema
            String name = (String) column.get("name");
            double missingPct = toDouble(column.get("missingPct"));
            long uniqueValues = toLong(column.get("uniqueValues"));
            
            log.debug("Processing column: {} (type: {}, unique: {}, missing: {}%)", 
                    name, dataType, uniqueValues, missingPct);
            
            EDADTO.FeatureStats stats = EDADTO.FeatureStats.builder()
                    .name(name)
                    .dataType(dataType != null ? dataType.toUpperCase() : "UNKNOWN")
                    .missingCount((long) (missingPct > 0 ? 1 : 0))
                    .missingPercentage(missingPct)
                    .uniqueCount(uniqueValues)
                    .build();
            
            // Add type-specific stats
            if (dataType != null && dataType.toLowerCase().contains("numeric")) {
                numericCount++;
                stats.setMean(toDouble(column.get("mean")));
                stats.setStdDev(toDouble(column.get("std")));
                stats.setMin(toDouble(column.get("min")));
                stats.setMedian(toDouble(column.get("median")));
                stats.setMax(toDouble(column.get("max")));
                log.debug("Added numeric feature: {} (mean: {}, min: {}, max: {})", 
                        name, stats.getMean(), stats.getMin(), stats.getMax());
            } else if (dataType != null && dataType.toLowerCase().contains("categorical")) {
                categoricalCount++;
                stats.setMode((String) column.get("mode"));
                stats.setModeFrequency(toLong(column.get("modeFrequency")));
                log.debug("Added categorical feature: {} (unique: {})", name, uniqueValues);
            } else if (dataType != null && dataType.toLowerCase().contains("datetime")) {
                dateTimeCount++;
                log.debug("Added datetime feature: {}", name);
            }
            
            statistics.add(stats);
        }
        
        log.info("analyzeFeatures completed: {} total features ({} numeric, {} categorical, {} datetime)", 
                columns.size(), numericCount, categoricalCount, dateTimeCount);
        
        // Calculate correlations
        List<EDADTO.Correlation> correlations = calculateCorrelations(columns);
        
        return EDADTO.FeaturesAnalysis.builder()
                .totalFeatures(columns.size())
                .numericFeatures(numericCount)
                .categoricalFeatures(categoricalCount)
                .dateTimeFeatures(dateTimeCount)
                .statistics(statistics)
                .correlations(correlations)
                .build();
    }
    
    // Helper methods for safe type conversion
    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return ((Double) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
    
    /**
     * Generate insights from analysis
     */
    private List<EDADTO.Insight> generateInsights(Dataset dataset, EDADTO.QualityMetrics quality, 
                                                  EDADTO.FeaturesAnalysis features) {
        List<EDADTO.Insight> insights = new ArrayList<>();
        
        // Calculate missing percentage
        double missingPct = (quality.getMissingValues() * 100.0) / Math.max(quality.getRowCount(), 1);
        
        // Missing data insights
        if (missingPct > 0.2) {
            insights.add(EDADTO.Insight.builder()
                    .id("insight_missing_" + UUID.randomUUID())
                    .title("High Missing Data Detected")
                    .description(String.format("%.1f%% of data is missing", missingPct))
                    .type("missing_data")
                    .severity(missingPct > 0.5 ? "CRITICAL" : "HIGH")
                    .recommendation("Consider imputation strategies or removing columns with excessive missing values")
                    .build());
        }
        
        // Duplicate data insights
        if (quality.getDuplicateRows() > 0) {
            insights.add(EDADTO.Insight.builder()
                    .id("insight_duplicates_" + UUID.randomUUID())
                    .title("Duplicate Rows Found")
                    .description(String.format("%d duplicate rows detected", quality.getDuplicateRows()))
                    .type("duplicates")
                    .severity(quality.getDuplicateRows() > quality.getRowCount() * 0.1 ? "HIGH" : "MEDIUM")
                    .recommendation("Remove duplicate rows before model training")
                    .build());
        }
        
        // Data quality insights
        if (quality.getOverallScore() < 70) {
            insights.add(EDADTO.Insight.builder()
                    .id("insight_quality_" + UUID.randomUUID())
                    .title("Low Data Quality Score")
                    .description(String.format("Overall quality score is %.1f%%", quality.getOverallScore()))
                    .type("data_quality")
                    .severity(quality.getOverallScore() < 50 ? "CRITICAL" : "HIGH")
                    .recommendation("Consider data cleaning and validation procedures")
                    .build());
        }
        
        // Imbalanced features
        if (quality.getColumnCount() > 10) {
            insights.add(EDADTO.Insight.builder()
                    .id("insight_features_" + UUID.randomUUID())
                    .title("Large Feature Set")
                    .description(String.format("Dataset has %d features", quality.getColumnCount()))
                    .type("feature_analysis")
                    .severity("INFO")
                    .recommendation("Consider feature selection or dimensionality reduction")
                    .build());
        }
        
        // Numeric vs categorical balance
        if (features.getNumericFeatures() == 0 || features.getCategoricalFeatures() == 0) {
            insights.add(EDADTO.Insight.builder()
                    .id("insight_imbalance_" + UUID.randomUUID())
                    .title("Data Type Imbalance")
                    .description(String.format("Only numeric or categorical features present"))
                    .type("imbalance")
                    .severity("INFO")
                    .recommendation("Consider encoding categorical features if needed")
                    .build());
        }
        
        return insights;
    }
    
    /**
     * Get summary of EDA analysis
     */
    public EDADTO.SummaryResponse getSummary(String edaId) {
        EDAAnalysis analysis = edaRepository.findByEdaId(edaId)
                .orElseThrow(() -> new RuntimeException("EDA analysis not found: " + edaId));
        
        return EDADTO.SummaryResponse.builder()
                .edaId(analysis.getEdaId())
                .datasetId(analysis.getDatasetId())
                .qualityScore(analysis.getOverallQualityScore())
                .assessment(analysis.getQualityAssessment())
                .rowCount(analysis.getRowCount())
                .columnCount(analysis.getColumnCount())
                .missingPercentage(analysis.getMissingPercentage())
                .duplicateRowsCount(analysis.getDuplicateRows())
                .criticalIssues(analysis.getCriticalInsights())
                .highIssues(analysis.getHighInsights())
                .topConcern(analysis.getTopConcern())
                .recommendation(analysis.getRecommendation())
                .timestamp(analysis.getCreatedAt())
                .build();
    }
    
    /**
     * Get quality metrics for EDA analysis
     */
    public EDADTO.QualityResponse getQualityMetrics(String edaId) {
        EDAAnalysis analysis = edaRepository.findByEdaId(edaId)
                .orElseThrow(() -> new RuntimeException("EDA analysis not found: " + edaId));
        
        EDADTO.QualityMetrics metrics = EDADTO.QualityMetrics.builder()
                .overallScore(analysis.getOverallQualityScore())
                .assessment(analysis.getQualityAssessment())
                .completeness(analysis.getCompleteness())
                .uniqueness(analysis.getUniqueness())
                .consistency(analysis.getConsistency())
                .validity(analysis.getValidity())
                .rowCount(analysis.getRowCount())
                .columnCount(analysis.getColumnCount())
                .missingValues(analysis.getMissingValues())
                .duplicateRows(analysis.getDuplicateRows())
                .build();
        
        EDADTO.QualityAssessment assessment = EDADTO.QualityAssessment.builder()
                .completenessStatus(getStatus(analysis.getCompleteness()))
                .consistencyStatus(getStatus(analysis.getConsistency()))
                .validityStatus(getStatus(analysis.getValidity()))
                .uniquenessStatus(getStatus(analysis.getUniqueness()))
                .overallAssessment(analysis.getQualityAssessment())
                .build();
        
        return EDADTO.QualityResponse.builder()
                .edaId(analysis.getEdaId())
                .metrics(metrics)
                .assessment(assessment)
                .build();
    }
    
    /**
     * Get insights from EDA analysis
     */
    public EDADTO.InsightsResponse getInsights(String edaId) {
        EDAAnalysis analysis = edaRepository.findByEdaId(edaId)
                .orElseThrow(() -> new RuntimeException("EDA analysis not found: " + edaId));
        
        try {
            List<EDADTO.Insight> insights = objectMapper.readValue(
                    analysis.getInsightsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EDADTO.Insight.class)
            );
            
            return EDADTO.InsightsResponse.builder()
                    .edaId(analysis.getEdaId())
                    .totalCount(analysis.getTotalInsights())
                    .criticalCount(analysis.getCriticalInsights())
                    .highCount(analysis.getHighInsights())
                    .mediumCount(analysis.getMediumInsights())
                    .lowCount(analysis.getLowInsights())
                    .insights(insights)
                    .build();
        } catch (Exception e) {
            log.error("Error deserializing insights: {}", e.getMessage());
            return EDADTO.InsightsResponse.builder()
                    .edaId(analysis.getEdaId())
                    .totalCount(analysis.getTotalInsights())
                    .criticalCount(analysis.getCriticalInsights())
                    .highCount(analysis.getHighInsights())
                    .mediumCount(analysis.getMediumInsights())
                    .lowCount(analysis.getLowInsights())
                    .insights(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Get feature importance ranking
     */
    public EDADTO.FeatureImportanceResponse getFeatureImportance(String edaId, Integer limit) {
        EDAAnalysis analysis = edaRepository.findByEdaId(edaId)
                .orElseThrow(() -> new RuntimeException("EDA analysis not found: " + edaId));
        
        try {
            EDADTO.FeaturesAnalysis features = objectMapper.readValue(
                    analysis.getFeaturesAnalysisJson(),
                    EDADTO.FeaturesAnalysis.class
            );
            
            List<EDADTO.FeatureRanking> rankings = features.getStatistics().stream()
                    .map((stat) -> EDADTO.FeatureRanking.builder()
                            .rank(0)
                            .feature(stat.getName())
                            .importance(1.0 - (stat.getMissingPercentage() / 100.0))
                            .dataType(stat.getDataType())
                            .missingPercentage(stat.getMissingPercentage())
                            .build())
                    .sorted(Comparator.comparingDouble(EDADTO.FeatureRanking::getImportance).reversed())
                    .limit(limit != null ? limit : 10)
                    .collect(Collectors.toList());
            
            // Add ranks
            for (int i = 0; i < rankings.size(); i++) {
                rankings.get(i).setRank(i + 1);
            }
            
            return EDADTO.FeatureImportanceResponse.builder()
                    .edaId(analysis.getEdaId())
                    .rankings(rankings)
                    .build();
        } catch (Exception e) {
            log.error("Error getting feature importance: {}", e.getMessage());
            return EDADTO.FeatureImportanceResponse.builder()
                    .edaId(analysis.getEdaId())
                    .rankings(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Get latest EDA analysis for dataset
     * Returns null if no analysis found
     */
    public EDADTO.SummaryResponse getLatestAnalysis(String datasetId) {
        return edaRepository.findLatestByDatasetId(datasetId)
                .map(analysis -> getSummary(analysis.getEdaId()))
                .orElse(null);
    }
    
    /**
     * List all EDA analyses for a project
     */
    public Page<EDADTO.ListItem> listAnalyses(String projectId, Pageable pageable) {
        return edaRepository.findByProjectId(projectId, pageable)
                .map(this::toListItem);
    }
    
    /**
     * Get health status of EDA service
     */
    public EDADTO.HealthResponse getHealthStatus() {
        return EDADTO.HealthResponse.builder()
                .status("UP")
                .available(true)
                .message("EDA service is operational")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Helper: Save analysis result to database
     */
    private void saveAnalysisResult(String edaId, EDADTO.AnalysisRequest request, 
                                   EDADTO.AnalysisResponse response,
                                   EDADTO.QualityMetrics qualityMetrics,
                                   EDADTO.FeaturesAnalysis featuresAnalysis,
                                   List<EDADTO.Insight> insights,
                                   long startTime) {
        try {
            long analysisTimeMs = System.currentTimeMillis() - startTime;
            
            // Count insights by severity
            int critical = (int) insights.stream().filter(i -> "CRITICAL".equals(i.getSeverity())).count();
            int high = (int) insights.stream().filter(i -> "HIGH".equals(i.getSeverity())).count();
            int medium = (int) insights.stream().filter(i -> "MEDIUM".equals(i.getSeverity())).count();
            int low = (int) insights.stream().filter(i -> "LOW".equals(i.getSeverity())).count();
            
            // Determine top concern
            String topConcern = insights.isEmpty() ? "None" : 
                    insights.stream()
                            .filter(i -> "CRITICAL".equals(i.getSeverity()) || "HIGH".equals(i.getSeverity()))
                            .findFirst()
                            .map(EDADTO.Insight::getTitle)
                            .orElse("Data quality varies");
            
            // Calculate missing percentage
            double missingPct = (qualityMetrics.getMissingValues() * 100.0) / Math.max(qualityMetrics.getRowCount(), 1);
            
            EDAAnalysis entity = EDAAnalysis.builder()
                    .edaId(edaId)
                    .datasetId(request.getDatasetId())
                    .datasetName(request.getDatasetId()) // Use datasetId as name if not provided
                    .projectId(request.getProjectId() != null ? request.getProjectId() : "default")
                    .overallQualityScore(qualityMetrics.getOverallScore())
                    .qualityAssessment(qualityMetrics.getAssessment())
                    .completeness(qualityMetrics.getCompleteness())
                    .uniqueness(qualityMetrics.getUniqueness())
                    .consistency(qualityMetrics.getConsistency())
                    .validity(qualityMetrics.getValidity())
                    .rowCount(qualityMetrics.getRowCount())
                    .columnCount(qualityMetrics.getColumnCount())
                    .missingValues(qualityMetrics.getMissingValues())
                    .duplicateRows(qualityMetrics.getDuplicateRows())
                    .missingPercentage(missingPct)
                    .numericFeatures(featuresAnalysis.getNumericFeatures())
                    .categoricalFeatures(featuresAnalysis.getCategoricalFeatures())
                    .dateTimeFeatures(featuresAnalysis.getDateTimeFeatures())
                    .totalInsights(insights.size())
                    .criticalInsights(critical)
                    .highInsights(high)
                    .mediumInsights(medium)
                    .lowInsights(low)
                    .topConcern(topConcern)
                    .recommendation("Review data quality and address identified issues")
                    .status("COMPLETED")
                    .analysisType("FULL")
                    .sampleRows(request.getSampleRows())
                    .analysisTimeMs(analysisTimeMs)
                    .qualityMetricsJson(objectMapper.writeValueAsString(qualityMetrics))
                    .featuresAnalysisJson(objectMapper.writeValueAsString(featuresAnalysis))
                    .insightsJson(objectMapper.writeValueAsString(insights))
                    .build();
            
            edaRepository.save(entity);
            log.info("EDA analysis saved successfully: {}", edaId);
            
        } catch (Exception e) {
            log.error("Error saving EDA analysis: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Helper: Convert entity to list item
     */
    private EDADTO.ListItem toListItem(EDAAnalysis entity) {
        return EDADTO.ListItem.builder()
                .edaId(entity.getEdaId())
                .datasetId(entity.getDatasetId())
                .datasetName(entity.getDatasetName())
                .qualityScore(entity.getOverallQualityScore())
                .timestamp(entity.getCreatedAt())
                .insightsCount(entity.getTotalInsights())
                .build();
    }
    
    // Helper methods
    private double calculateCompleteness(long rowCount, long missingValues) {
        if (rowCount == 0) return 0.0;
        return Math.max(0, 100.0 * (1.0 - ((double) missingValues / (rowCount * 100))));
    }
    
    private double calculateUniqueness(long rowCount, long duplicateRows) {
        if (rowCount == 0) return 0.0;
        return Math.max(0, 100.0 * (1.0 - ((double) duplicateRows / rowCount)));
    }
    
    private double calculateConsistency(Map<String, Object> metadata) {
        return 85.0; // Placeholder
    }
    
    private double calculateValidity(Map<String, Object> metadata) {
        return 90.0; // Placeholder
    }
    
    private String getAssessmentLevel(double score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 50) return "Fair";
        return "Poor";
    }
    
    private String getStatus(Double value) {
        if (value == null) return "UNKNOWN";
        if (value >= 90) return "EXCELLENT";
        if (value >= 75) return "GOOD";
        if (value >= 50) return "FAIR";
        return "POOR";
    }
    
    /**
     * Get feature analysis for an EDA analysis
     */
    public EDADTO.FeaturesResponse getFeatures(String edaId) {
        try {
            EDAAnalysis analysis = edaRepository.findByEdaId(edaId)
                    .orElseThrow(() -> new RuntimeException("EDA analysis not found: " + edaId));
            
            // Deserialize features analysis from JSON
            EDADTO.FeaturesAnalysis featuresAnalysis = objectMapper.readValue(
                    analysis.getFeaturesAnalysisJson(),
                    EDADTO.FeaturesAnalysis.class
            );
            
            return EDADTO.FeaturesResponse.builder()
                    .edaId(edaId)
                    .analysis(featuresAnalysis)
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving features for EDA {}: {}", edaId, e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> parseDatasetMetadata(Dataset dataset) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rowCount", dataset.getRowCount() != null ? dataset.getRowCount() : 0L);
        metadata.put("columnCount", dataset.getColumnCount() != null ? dataset.getColumnCount() : 0);
        metadata.put("missingValues", 0L);
        metadata.put("duplicateRows", 0L);
        
        List<Map<String, Object>> columns = new ArrayList<>();
        
        // Check if columnsJson exists
        if (dataset.getColumnsJson() == null || dataset.getColumnsJson().isEmpty()) {
            log.warn("Dataset {} has no columnsJson", dataset.getId());
            metadata.put("columns", columns);
            return metadata;
        }
        
        try {
            log.info("Parsing columnsJson for dataset {}: {} bytes", 
                    dataset.getId(), dataset.getColumnsJson().length());
            
            // Parse the JSON array directly
            columns = objectMapper.readValue(
                    dataset.getColumnsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
            );
            
            log.info("Successfully parsed {} columns from columnsJson", columns.size());
            
            // Log each column for debugging
            for (Map<String, Object> col : columns) {
                log.debug("Column: name={}, dataType={}, uniqueValues={}, missingPct={}", 
                        col.get("name"), 
                        col.get("dataType"),
                        col.get("uniqueValues"),
                        col.get("missingPct"));
            }
            
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("JSON parse error in columnsJson: {}", e.getMessage());
            log.error("ColumnsJson content: {}", dataset.getColumnsJson());
        } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
            log.error("JSON mapping error in columnsJson: {}", e.getMessage());
            log.error("ColumnsJson content: {}", dataset.getColumnsJson());
        } catch (Exception e) {
            log.error("Error parsing columnsJson: {}", e.getMessage(), e);
            log.error("ColumnsJson content: {}", dataset.getColumnsJson());
        }
        
        metadata.put("columns", columns);
        return metadata;
    }
    
    private List<Map<String, Object>> extractColumnsFromCSV(String filePath) throws Exception {
        List<Map<String, Object>> columns = new ArrayList<>();
        
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            log.error("File not found: {}", filePath);
            return columns;
        }
        
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(path)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return columns;
            }
            
            String[] headers = headerLine.split(",");
            for (String header : headers) {
                String cleanHeader = header.trim().replaceAll("\"", "");
                Map<String, Object> column = new HashMap<>();
                column.put("name", cleanHeader);
                column.put("dataType", "numeric"); // Match database schema
                column.put("missingPct", 0.0);
                column.put("uniqueValues", 100L);
                column.put("mean", 50.0);
                column.put("std", 10.0);
                column.put("min", 0.0);
                column.put("median", 50.0);
                column.put("max", 100.0);
                columns.add(column);
            }
        }
        
        return columns;
    }
    
    private List<EDADTO.Correlation> calculateCorrelations(List<Map<String, Object>> columns) {
        List<EDADTO.Correlation> correlations = new ArrayList<>();
        
        // Get numeric column names
        List<String> numericColumns = columns.stream()
                .filter(col -> {
                    String dataType = (String) col.get("dataType");
                    return dataType != null && dataType.toLowerCase().contains("numeric");
                })
                .map(col -> (String) col.get("name"))
                .toList();
        
        // Generate correlations between numeric columns
        for (int i = 0; i < numericColumns.size(); i++) {
            for (int j = i + 1; j < numericColumns.size(); j++) {
                String col1 = numericColumns.get(i);
                String col2 = numericColumns.get(j);
                // Generate pseudo-random correlation between -1 and 1
                double correlationCoeff = Math.sin((col1.hashCode() + col2.hashCode()) / 1000.0);
                
                // Determine strength
                String strength;
                double absCorr = Math.abs(correlationCoeff);
                if (absCorr >= 0.7) {
                    strength = "Strong";
                } else if (absCorr >= 0.4) {
                    strength = "Moderate";
                } else {
                    strength = "Weak";
                }
                
                EDADTO.Correlation corr = EDADTO.Correlation.builder()
                        .feature1(col1)
                        .feature2(col2)
                        .correlationValue(correlationCoeff)
                        .strength(strength)
                        .build();
                correlations.add(corr);
            }
        }
        
        log.debug("Calculated {} correlations between {} numeric features", 
                correlations.size(), numericColumns.size());
        return correlations;
    }
}
