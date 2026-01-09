package com.mlengine.repository;

import com.mlengine.model.entity.BatchPredictionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for BatchPredictionJob entity.
 */
@Repository
public interface BatchPredictionJobRepository extends JpaRepository<BatchPredictionJob, String> {

    List<BatchPredictionJob> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<BatchPredictionJob> findByStatusOrderByCreatedAtDesc(String status);

    List<BatchPredictionJob> findAllByOrderByCreatedAtDesc();

    @Query("SELECT b FROM BatchPredictionJob b WHERE b.status IN ('QUEUED', 'PROCESSING') ORDER BY b.createdAt ASC")
    List<BatchPredictionJob> findActiveJobs();

    @Query("SELECT COUNT(b) FROM BatchPredictionJob b WHERE b.projectId = :projectId")
    Integer countByProject(@Param("projectId") String projectId);

    List<BatchPredictionJob> findTop10ByProjectIdOrderByCreatedAtDesc(String projectId);

    List<BatchPredictionJob> findTop10ByOrderByCreatedAtDesc();
}
