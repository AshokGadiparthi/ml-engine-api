package com.mlengine.repository;

import com.mlengine.model.entity.TrainingJob;
import com.mlengine.model.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TrainingJob entity.
 */
@Repository
public interface TrainingJobRepository extends JpaRepository<TrainingJob, String> {

    List<TrainingJob> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<TrainingJob> findByStatusOrderByCreatedAtDesc(JobStatus status);

    List<TrainingJob> findByStatusInOrderByCreatedAtDesc(List<JobStatus> statuses);

    @Query("SELECT t FROM TrainingJob t WHERE t.projectId = :projectId ORDER BY t.createdAt DESC")
    List<TrainingJob> findByProject(@Param("projectId") String projectId);

    @Query("SELECT t FROM TrainingJob t WHERE t.status IN ('QUEUED', 'STARTING', 'TRAINING', 'VALIDATING') ORDER BY t.createdAt ASC")
    List<TrainingJob> findActiveJobs();

    @Query("SELECT COUNT(t) FROM TrainingJob t WHERE t.projectId = :projectId AND t.status = 'COMPLETED'")
    Integer countCompletedByProject(@Param("projectId") String projectId);

    @Query("SELECT t FROM TrainingJob t WHERE t.projectId = :projectId AND t.status IN ('QUEUED', 'TRAINING') ORDER BY t.createdAt DESC")
    List<TrainingJob> findRunningByProject(@Param("projectId") String projectId);

    List<TrainingJob> findTop10ByProjectIdOrderByCreatedAtDesc(String projectId);

    List<TrainingJob> findAllByOrderByCreatedAtDesc();
}
