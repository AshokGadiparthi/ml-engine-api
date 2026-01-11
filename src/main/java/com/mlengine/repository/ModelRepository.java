package com.mlengine.repository;

import com.mlengine.model.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Model entity.
 */
@Repository
public interface ModelRepository extends JpaRepository<Model, String> {

    List<Model> findByProject_IdOrderByCreatedAtDesc(String projectId);

    List<Model> findByIsDeployedTrueOrderByDeployedAtDesc();

    List<Model> findByProject_IdAndIsDeployedTrueOrderByDeployedAtDesc(String projectId);

    Optional<Model> findByTrainingJobId(String trainingJobId);

    @Query("SELECT m FROM Model m WHERE m.project.id = :projectId ORDER BY m.accuracy DESC")
    List<Model> findByProjectOrderByAccuracy(@Param("projectId") String projectId);

    @Query("SELECT m FROM Model m WHERE m.project.id = :projectId AND m.isBest = true")
    Optional<Model> findBestByProject(@Param("projectId") String projectId);

    @Query("SELECT COUNT(m) FROM Model m WHERE m.project.id = :projectId")
    Integer countByProject(@Param("projectId") String projectId);

    @Query("SELECT COUNT(m) FROM Model m WHERE m.project.id = :projectId AND m.isDeployed = true")
    Integer countDeployedByProject(@Param("projectId") String projectId);

    @Query("SELECT AVG(m.accuracy) FROM Model m WHERE m.project.id = :projectId AND m.accuracy IS NOT NULL")
    Double avgAccuracyByProject(@Param("projectId") String projectId);

    List<Model> findTop5ByProject_IdOrderByCreatedAtDesc(String projectId);

    List<Model> findAllByOrderByCreatedAtDesc();
    
    // Get all models with modelPath set (ready for predictions)
    @Query("SELECT m FROM Model m WHERE m.modelPath IS NOT NULL ORDER BY m.createdAt DESC")
    List<Model> findAllReadyForPredictions();
    
    // Get all models for a project OR all models if project is null (for predictions page)
    @Query("SELECT m FROM Model m WHERE m.modelPath IS NOT NULL AND (m.project.id = :projectId OR :projectId IS NULL) ORDER BY m.createdAt DESC")
    List<Model> findReadyForPredictionsByProject(@Param("projectId") String projectId);
    
    // Get models by source type
    List<Model> findBySourceOrderByCreatedAtDesc(String source);
    
    // Count by source
    @Query("SELECT COUNT(m) FROM Model m WHERE m.source = :source")
    Long countBySource(@Param("source") String source);
}
