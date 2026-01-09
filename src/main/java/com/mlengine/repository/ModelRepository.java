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

    List<Model> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Model> findByIsDeployedTrueOrderByDeployedAtDesc();

    List<Model> findByProjectIdAndIsDeployedTrueOrderByDeployedAtDesc(String projectId);

    Optional<Model> findByTrainingJobId(String trainingJobId);

    @Query("SELECT m FROM Model m WHERE m.projectId = :projectId ORDER BY m.accuracy DESC")
    List<Model> findByProjectOrderByAccuracy(@Param("projectId") String projectId);

    @Query("SELECT m FROM Model m WHERE m.projectId = :projectId AND m.isBest = true")
    Optional<Model> findBestByProject(@Param("projectId") String projectId);

    @Query("SELECT COUNT(m) FROM Model m WHERE m.projectId = :projectId")
    Integer countByProject(@Param("projectId") String projectId);

    @Query("SELECT COUNT(m) FROM Model m WHERE m.projectId = :projectId AND m.isDeployed = true")
    Integer countDeployedByProject(@Param("projectId") String projectId);

    @Query("SELECT AVG(m.accuracy) FROM Model m WHERE m.projectId = :projectId")
    Double avgAccuracyByProject(@Param("projectId") String projectId);

    List<Model> findTop5ByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Model> findAllByOrderByCreatedAtDesc();
}
