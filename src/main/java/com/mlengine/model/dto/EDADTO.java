package com.mlengine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for EDA (Exploratory Data Analysis) endpoints
 * Provides data quality assessment, feature analysis, and insights
 */
public class EDADTO {

    /**
     * Request to analyze a dataset
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisRequest implements Serializable {
        
        @Schema(description = "Dataset ID to analyze", example = "ds_12345")
        private String datasetId;
        
        @Schema(description = "Target column for analysis", example = "target")
        private String targetColumn;
        
        @Schema(description = "Number of rows to sample (optional)", example = "5000")
        private Integer sampleRows;
        
        @Schema(description = "Project ID (optional)", example = "proj_123")
        private String projectId;
    }

    /**
     * EDA analysis response with all results
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResponse implements Serializable {
        
        @Schema(description = "Unique EDA analysis ID")
        private String edaId;
        
        @Schema(description = "Dataset ID analyzed")
        private String datasetId;
        
        @Schema(description = "Analysis status")
        private String status;
        
        @Schema(description = "Quality metrics")
        private QualityMetrics quality;
        
        @Schema(description = "Feature analysis")
        private FeaturesAnalysis features;
        
        @Schema(description = "Generated insights")
        private List<Insight> insights;
        
        @Schema(description = "Timestamp of analysis")
        private LocalDateTime timestamp;
    }

    /**
     * Quality assessment metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityMetrics implements Serializable {
        
        @Schema(description = "Overall quality score (0-100)")
        private double overallScore;
        
        @Schema(description = "Quality assessment: Excellent/Good/Fair/Poor")
        private String assessment;
        
        @Schema(description = "Data completeness percentage")
        private double completeness;
        
        @Schema(description = "Column uniqueness percentage")
        private double uniqueness;
        
        @Schema(description = "Data consistency percentage")
        private double consistency;
        
        @Schema(description = "Data validity percentage")
        private double validity;
        
        @Schema(description = "Number of rows analyzed")
        private long rowCount;
        
        @Schema(description = "Number of columns")
        private int columnCount;
        
        @Schema(description = "Number of missing values")
        private long missingValues;
        
        @Schema(description = "Number of duplicate rows")
        private long duplicateRows;
    }

    /**
     * Features analysis with statistical information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturesAnalysis implements Serializable {
        
        @Schema(description = "Total number of features")
        private int totalFeatures;
        
        @Schema(description = "Numeric features")
        private int numericFeatures;
        
        @Schema(description = "Categorical features")
        private int categoricalFeatures;
        
        @Schema(description = "DateTime features")
        private int dateTimeFeatures;
        
        @Schema(description = "Feature statistics")
        private List<FeatureStats> statistics;
        
        @Schema(description = "Feature correlations")
        private List<Correlation> correlations;
    }

    /**
     * Statistical information for a feature
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureStats implements Serializable {
        
        @Schema(description = "Feature name")
        private String name;
        
        @Schema(description = "Data type: numeric, categorical, datetime, string")
        private String dataType;
        
        @Schema(description = "Missing value count")
        private long missingCount;
        
        @Schema(description = "Missing value percentage")
        private double missingPercentage;
        
        @Schema(description = "Unique value count")
        private long uniqueCount;
        
        @Schema(description = "For numeric: mean value")
        private Double mean;
        
        @Schema(description = "For numeric: standard deviation")
        private Double stdDev;
        
        @Schema(description = "For numeric: minimum value")
        private Double min;
        
        @Schema(description = "For numeric: median value")
        private Double median;
        
        @Schema(description = "For numeric: maximum value")
        private Double max;
        
        @Schema(description = "For categorical: mode (most frequent value)")
        private String mode;
        
        @Schema(description = "For categorical: frequency of mode")
        private Long modeFrequency;
    }

    /**
     * Feature correlation information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Correlation implements Serializable {
        
        @Schema(description = "First feature")
        private String feature1;
        
        @Schema(description = "Second feature")
        private String feature2;
        
        @Schema(description = "Correlation coefficient (-1 to 1)")
        private double correlationValue;
        
        @Schema(description = "Correlation strength: Strong/Moderate/Weak")
        private String strength;
    }

    /**
     * Data quality insight
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight implements Serializable {
        
        @Schema(description = "Insight ID")
        private String id;
        
        @Schema(description = "Insight title")
        private String title;
        
        @Schema(description = "Detailed description")
        private String description;
        
        @Schema(description = "Insight type: data_quality, missing_data, outliers, imbalance, etc")
        private String type;
        
        @Schema(description = "Severity: CRITICAL, HIGH, MEDIUM, LOW, INFO")
        private String severity;
        
        @Schema(description = "Affected features")
        private List<String> affectedFeatures;
        
        @Schema(description = "Recommendation for improvement")
        private String recommendation;
    }

    /**
     * Summary response with key metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse implements Serializable {
        
        @Schema(description = "EDA ID")
        private String edaId;
        
        @Schema(description = "Dataset ID")
        private String datasetId;
        
        @Schema(description = "Overall quality score")
        private double qualityScore;
        
        @Schema(description = "Quality assessment")
        private String assessment;
        
        @Schema(description = "Row count")
        private long rowCount;
        
        @Schema(description = "Column count")
        private int columnCount;
        
        @Schema(description = "Missing values percentage")
        private double missingPercentage;
        
        @Schema(description = "Duplicate rows count")
        private long duplicateRowsCount;
        
        @Schema(description = "Number of critical issues")
        private int criticalIssues;
        
        @Schema(description = "Number of high issues")
        private int highIssues;
        
        @Schema(description = "Top concern title")
        private String topConcern;
        
        @Schema(description = "Recommended action")
        private String recommendation;
        
        @Schema(description = "Analysis timestamp")
        private LocalDateTime timestamp;
    }

    /**
     * Quality metrics response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityResponse implements Serializable {
        
        @Schema(description = "EDA ID")
        private String edaId;
        
        @Schema(description = "Quality metrics")
        private QualityMetrics metrics;
        
        @Schema(description = "Quality assessment details")
        private QualityAssessment assessment;
        
        @Schema(description = "Recommendations")
        private List<String> recommendations;
    }

    /**
     * Detailed quality assessment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityAssessment implements Serializable {
        
        @Schema(description = "Completeness assessment")
        private String completenessStatus;
        
        @Schema(description = "Consistency assessment")
        private String consistencyStatus;
        
        @Schema(description = "Validity assessment")
        private String validityStatus;
        
        @Schema(description = "Uniqueness assessment")
        private String uniquenessStatus;
        
        @Schema(description = "Timeliness assessment")
        private String timelinessStatus;
        
        @Schema(description = "Overall assessment summary")
        private String overallAssessment;
    }

    /**
     * Feature analysis response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturesResponse implements Serializable {
        
        @Schema(description = "EDA ID")
        private String edaId;
        
        @Schema(description = "Features analysis")
        private FeaturesAnalysis analysis;
    }

    /**
     * Feature importance response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureImportanceResponse implements Serializable {
        
        @Schema(description = "EDA ID")
        private String edaId;
        
        @Schema(description = "Feature rankings")
        private List<FeatureRanking> rankings;
    }

    /**
     * Feature ranking information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureRanking implements Serializable {
        
        @Schema(description = "Feature rank")
        private int rank;
        
        @Schema(description = "Feature name")
        private String feature;
        
        @Schema(description = "Importance/correlation value")
        private double importance;
        
        @Schema(description = "Data type")
        private String dataType;
        
        @Schema(description = "Missing percentage")
        private double missingPercentage;
    }

    /**
     * Insights response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightsResponse implements Serializable {
        
        @Schema(description = "EDA ID")
        private String edaId;
        
        @Schema(description = "Total insights count")
        private int totalCount;
        
        @Schema(description = "Critical insights count")
        private int criticalCount;
        
        @Schema(description = "High severity insights count")
        private int highCount;
        
        @Schema(description = "Medium severity insights count")
        private int mediumCount;
        
        @Schema(description = "Low severity insights count")
        private int lowCount;
        
        @Schema(description = "Insights list")
        private List<Insight> insights;
    }

    /**
     * Health check response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthResponse implements Serializable {
        
        @Schema(description = "EDA service status")
        private String status;
        
        @Schema(description = "Service availability")
        private boolean available;
        
        @Schema(description = "Message")
        private String message;
        
        @Schema(description = "Timestamp")
        private LocalDateTime timestamp;
    }

    /**
     * List item response for getting EDA analyses
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem implements Serializable {
        
        @Schema(description = "EDA ID")
        private String edaId;
        
        @Schema(description = "Dataset ID")
        private String datasetId;
        
        @Schema(description = "Dataset name")
        private String datasetName;
        
        @Schema(description = "Quality score")
        private double qualityScore;
        
        @Schema(description = "Analysis timestamp")
        private LocalDateTime timestamp;
        
        @Schema(description = "Number of insights found")
        private int insightsCount;
    }
}
