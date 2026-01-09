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
 */
@Repository
public interface PredictionRepository extends JpaRepository<Prediction, String> {

    List<Prediction> findByModelIdOrderByCreatedAtDesc(String modelId);

    List<Prediction> findByBatchIdOrderByBatchIndex(String batchId);

    Page<Prediction> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);

    Page<Prediction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.projectId = :projectId")
    Long countByProject(@Param("projectId") String projectId);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.projectId = :projectId AND p.createdAt > :since")
    Long countByProjectSince(@Param("projectId") String projectId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.createdAt > :since")
    Long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(p.confidence) FROM Prediction p WHERE p.projectId = :projectId")
    Double avgConfidenceByProject(@Param("projectId") String projectId);

    List<Prediction> findTop10ByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Prediction> findTop10ByOrderByCreatedAtDesc();

    Page<Prediction> findByModel_Project_Id(String projectId, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime since);
}
