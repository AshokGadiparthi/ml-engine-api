package com.mlengine.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JPA Entity for EDA Analysis results
 * Stores exploratory data analysis results in database
 */
@Entity
@Table(name = "eda_analyses", indexes = {
    @Index(name = "idx_dataset_id", columnList = "dataset_id"),
    @Index(name = "idx_quality_score", columnList = "quality_score"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EDAAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String edaId;
    
    @Column(nullable = false)
    private String datasetId;
    
    @Column(nullable = false)
    private String datasetName;
    
    @Column(nullable = false)
    private String projectId;
    
    // Quality Metrics
    @Column(nullable = false)
    private Double overallQualityScore;
    
    @Column(length = 50)
    private String qualityAssessment;
    
    @Column
    private Double completeness;
    
    @Column
    private Double uniqueness;
    
    @Column
    private Double consistency;
    
    @Column
    private Double validity;
    
    // Dataset Information
    @Column(nullable = false)
    private Long rowCount;
    
    @Column(nullable = false)
    private Integer columnCount;
    
    @Column
    private Long missingValues;
    
    @Column
    private Long duplicateRows;
    
    @Column
    private Double missingPercentage;
    
    // Feature Analysis
    @Column
    private Integer numericFeatures;
    
    @Column
    private Integer categoricalFeatures;
    
    @Column
    private Integer dateTimeFeatures;
    
    // Insights Summary
    @Column
    private Integer totalInsights;
    
    @Column
    private Integer criticalInsights;
    
    @Column
    private Integer highInsights;
    
    @Column
    private Integer mediumInsights;
    
    @Column
    private Integer lowInsights;
    
    @Column(length = 500)
    private String topConcern;
    
    @Column(length = 1000)
    private String recommendation;
    
    // JSON Storage for detailed data
    @Column(columnDefinition = "TEXT")
    private String qualityMetricsJson;
    
    @Column(columnDefinition = "TEXT")
    private String featuresAnalysisJson;
    
    @Column(columnDefinition = "TEXT")
    private String insightsJson;
    
    @Column(columnDefinition = "TEXT")
    private String correlationsJson;
    
    // Metadata
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private String status;
    
    @Column
    private String analysisType;
    
    @Column
    private Integer sampleRows;
    
    @Column
    private Long analysisTimeMs;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
