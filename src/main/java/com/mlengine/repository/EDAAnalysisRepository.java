package com.mlengine.repository;

import com.mlengine.model.entity.EDAAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for EDA Analysis entities
 * Provides database access for exploratory data analysis results
 */
@Repository
public interface EDAAnalysisRepository extends JpaRepository<EDAAnalysis, String> {
    
    /**
     * Find EDA analysis by EDA ID
     */
    Optional<EDAAnalysis> findByEdaId(String edaId);
    
    /**
     * Find all EDA analyses for a dataset
     */
    List<EDAAnalysis> findByDatasetId(String datasetId);
    
    /**
     * Find all EDA analyses for a project
     */
    List<EDAAnalysis> findByProjectId(String projectId);
    
    /**
     * Find EDA analyses with pagination
     */
    Page<EDAAnalysis> findByProjectId(String projectId, Pageable pageable);
    
    /**
     * Find EDA analyses for multiple datasets
     */
    List<EDAAnalysis> findByDatasetIdIn(List<String> datasetIds);
    
    /**
     * Find latest EDA analysis for a dataset
     */
    @Query("SELECT e FROM EDAAnalysis e WHERE e.datasetId = :datasetId ORDER BY e.createdAt DESC LIMIT 1")
    Optional<EDAAnalysis> findLatestByDatasetId(@Param("datasetId") String datasetId);
    
    /**
     * Find EDA analyses by quality score range
     */
    @Query("SELECT e FROM EDAAnalysis e WHERE e.overallQualityScore >= :minScore AND e.overallQualityScore <= :maxScore")
    List<EDAAnalysis> findByQualityScoreRange(
            @Param("minScore") Double minScore,
            @Param("maxScore") Double maxScore
    );
    
    /**
     * Find EDA analyses with critical issues
     */
    @Query("SELECT e FROM EDAAnalysis e WHERE e.criticalInsights > 0 ORDER BY e.criticalInsights DESC")
    List<EDAAnalysis> findWithCriticalIssues();
    
    /**
     * Find EDA analyses created in date range
     */
    @Query("SELECT e FROM EDAAnalysis e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    List<EDAAnalysis> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Count EDA analyses per project
     */
    long countByProjectId(String projectId);
    
    /**
     * Check if EDA analysis exists for dataset
     */
    boolean existsByDatasetId(String datasetId);
    
    /**
     * Delete EDA analyses for a dataset
     */
    void deleteByDatasetId(String datasetId);
    
    /**
     * Delete old EDA analyses (before given date)
     */
    @Query("DELETE FROM EDAAnalysis e WHERE e.createdAt < :date")
    void deleteOlderThan(@Param("date") LocalDateTime date);
    
    /**
     * Find analyses by assessment level
     */
    List<EDAAnalysis> findByQualityAssessment(String assessment);
    
    /**
     * Get average quality score for a project
     */
    @Query("SELECT AVG(e.overallQualityScore) FROM EDAAnalysis e WHERE e.projectId = :projectId")
    Double getAverageQualityScore(@Param("projectId") String projectId);
    
    /**
     * Get top datasets by quality score in a project
     */
    @Query("SELECT e FROM EDAAnalysis e WHERE e.projectId = :projectId ORDER BY e.overallQualityScore DESC")
    Page<EDAAnalysis> getTopDatasetsByQuality(
            @Param("projectId") String projectId,
            Pageable pageable
    );
    
    /**
     * Get EDA analyses needing attention (low quality)
     */
    @Query("SELECT e FROM EDAAnalysis e WHERE e.projectId = :projectId AND e.overallQualityScore < :threshold ORDER BY e.overallQualityScore ASC")
    Page<EDAAnalysis> getDatasetsPendingAttention(
            @Param("projectId") String projectId,
            @Param("threshold") Double threshold,
            Pageable pageable
    );
}
