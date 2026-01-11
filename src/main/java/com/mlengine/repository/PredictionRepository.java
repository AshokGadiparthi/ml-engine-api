package com.mlengine.repository;

import com.mlengine.model.entity.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Prediction entity.
 * Supports filtering, stats, and history queries.
 */
@Repository
public interface PredictionRepository extends JpaRepository<Prediction, String> {

    // ========== BASIC QUERIES ==========

    List<Prediction> findByModelIdOrderByCreatedAtDesc(String modelId);

    List<Prediction> findByBatchIdOrderByBatchIndex(String batchId);

    Page<Prediction> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);

    Page<Prediction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ========== FILTERING QUERIES ==========

    // By project and type
    Page<Prediction> findByProjectIdAndPredictionType(String projectId, String predictionType, Pageable pageable);

    // By project and model
    Page<Prediction> findByProjectIdAndModelId(String projectId, String modelId, Pageable pageable);

    // By project, type, and model
    Page<Prediction> findByProjectIdAndPredictionTypeAndModelId(
            String projectId, String predictionType, String modelId, Pageable pageable);

    // By project and date range
    @Query("SELECT p FROM Prediction p WHERE p.projectId = :projectId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    Page<Prediction> findByProjectIdAndDateRange(
            @Param("projectId") String projectId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // By project and result (predicted class)
    Page<Prediction> findByProjectIdAndPredictedClass(String projectId, String predictedClass, Pageable pageable);

    // Complex filter: project, type, model, result, date range
    @Query("SELECT p FROM Prediction p WHERE " +
           "(:projectId IS NULL OR p.projectId = :projectId) AND " +
           "(:type IS NULL OR p.predictionType = :type) AND " +
           "(:modelId IS NULL OR p.modelId = :modelId) AND " +
           "(:result IS NULL OR p.predictedClass = :result) AND " +
           "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.createdAt <= :endDate) " +
           "ORDER BY p.createdAt DESC")
    Page<Prediction> findWithFilters(
            @Param("projectId") String projectId,
            @Param("type") String type,
            @Param("modelId") String modelId,
            @Param("result") String result,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Search by model name
    @Query("SELECT p FROM Prediction p WHERE p.projectId = :projectId AND LOWER(p.modelName) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY p.createdAt DESC")
    Page<Prediction> searchByModelName(
            @Param("projectId") String projectId,
            @Param("search") String search,
            Pageable pageable);

    // ========== COUNT QUERIES ==========

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.projectId = :projectId")
    Long countByProject(@Param("projectId") String projectId);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.projectId = :projectId AND p.createdAt > :since")
    Long countByProjectSince(@Param("projectId") String projectId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.createdAt > :since")
    Long countSince(@Param("since") LocalDateTime since);

    long countByCreatedAtAfter(LocalDateTime since);

    // Count by type
    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.projectId = :projectId AND p.predictionType = :type")
    Long countByProjectAndType(@Param("projectId") String projectId, @Param("type") String type);

    // Count by result
    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.projectId = :projectId AND p.predictedClass = :result")
    Long countByProjectAndResult(@Param("projectId") String projectId, @Param("result") String result);

    // Count by model
    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.modelId = :modelId")
    Long countByModelId(@Param("modelId") String modelId);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.modelId = :modelId AND p.createdAt > :since")
    Long countByModelIdSince(@Param("modelId") String modelId, @Param("since") LocalDateTime since);

    // Count with filters
    @Query("SELECT COUNT(p) FROM Prediction p WHERE " +
           "(:projectId IS NULL OR p.projectId = :projectId) AND " +
           "(:type IS NULL OR p.predictionType = :type) AND " +
           "(:modelId IS NULL OR p.modelId = :modelId) AND " +
           "(:result IS NULL OR p.predictedClass = :result) AND " +
           "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.createdAt <= :endDate)")
    Long countWithFilters(
            @Param("projectId") String projectId,
            @Param("type") String type,
            @Param("modelId") String modelId,
            @Param("result") String result,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ========== STATISTICS QUERIES ==========

    @Query("SELECT AVG(p.confidence) FROM Prediction p WHERE p.projectId = :projectId")
    Double avgConfidenceByProject(@Param("projectId") String projectId);

    @Query("SELECT AVG(p.processingTimeMs) FROM Prediction p WHERE p.projectId = :projectId AND p.processingTimeMs IS NOT NULL")
    Double avgProcessingTimeByProject(@Param("projectId") String projectId);

    @Query("SELECT AVG(p.processingTimeMs) FROM Prediction p WHERE p.modelId = :modelId AND p.processingTimeMs IS NOT NULL")
    Double avgProcessingTimeByModel(@Param("modelId") String modelId);

    // Get distinct predicted classes for a project
    @Query("SELECT DISTINCT p.predictedClass FROM Prediction p WHERE p.projectId = :projectId AND p.predictedClass IS NOT NULL")
    List<String> findDistinctResultsByProject(@Param("projectId") String projectId);

    // ========== RECENT QUERIES ==========

    List<Prediction> findTop10ByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Prediction> findTop10ByOrderByCreatedAtDesc();

    List<Prediction> findTop10ByModelIdOrderByCreatedAtDesc(String modelId);

    Page<Prediction> findByProjectId(String projectId, Pageable pageable);

    // ========== API SPECIFIC ==========

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.source = 'API' AND p.modelId = :modelId AND p.createdAt > :since")
    Long countApiCallsByModelSince(@Param("modelId") String modelId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.source = 'API' AND p.projectId = :projectId AND p.createdAt > :since")
    Long countApiCallsByProjectSince(@Param("projectId") String projectId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(p.processingTimeMs) FROM Prediction p WHERE p.source = 'API' AND p.modelId = :modelId AND p.createdAt > :since AND p.processingTimeMs IS NOT NULL")
    Double avgApiLatencyByModelSince(@Param("modelId") String modelId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(p.processingTimeMs) FROM Prediction p WHERE p.source = 'API' AND p.projectId = :projectId AND p.createdAt > :since AND p.processingTimeMs IS NOT NULL")
    Double avgApiLatencyByProjectSince(@Param("projectId") String projectId, @Param("since") LocalDateTime since);
}
