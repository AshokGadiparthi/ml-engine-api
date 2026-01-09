package com.mlengine.repository;

import com.mlengine.model.entity.AutoMLJob;
import com.mlengine.model.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AutoML jobs.
 */
@Repository
public interface AutoMLJobRepository extends JpaRepository<AutoMLJob, String> {

    List<AutoMLJob> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Page<AutoMLJob> findByProjectId(String projectId, Pageable pageable);

    List<AutoMLJob> findByStatus(JobStatus status);

    List<AutoMLJob> findByProjectIdAndStatus(String projectId, JobStatus status);

    Page<AutoMLJob> findByProjectIdAndStatus(String projectId, JobStatus status, Pageable pageable);

    @Query("SELECT j FROM AutoMLJob j WHERE j.status IN ('QUEUED', 'STARTING', 'TRAINING', 'VALIDATING')")
    List<AutoMLJob> findRunningJobs();

    @Query("SELECT j FROM AutoMLJob j WHERE j.project.id = :projectId ORDER BY j.createdAt DESC")
    Page<AutoMLJob> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);

    @Query("SELECT COUNT(j) FROM AutoMLJob j WHERE j.project.id = :projectId")
    Long countByProjectId(String projectId);

    @Query("SELECT COUNT(j) FROM AutoMLJob j WHERE j.project.id = :projectId AND j.status = :status")
    Long countByProjectIdAndStatus(String projectId, JobStatus status);

    List<AutoMLJob> findTop5ByProjectIdOrderByCreatedAtDesc(String projectId);

    List<AutoMLJob> findByDatasetId(String datasetId);
}
