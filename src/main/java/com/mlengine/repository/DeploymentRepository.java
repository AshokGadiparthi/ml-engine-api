package com.mlengine.repository;

import com.mlengine.model.entity.Deployment;
import com.mlengine.model.enums.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Deployment operations.
 */
@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, String> {

    // Find active deployment for a project (should be only one)
    Optional<Deployment> findByProjectIdAndStatus(String projectId, DeploymentStatus status);

    // Find all deployments for a project
    List<Deployment> findByProjectIdOrderByVersionDesc(String projectId);

    Page<Deployment> findByProjectIdOrderByVersionDesc(String projectId, Pageable pageable);

    // Find by status
    List<Deployment> findByStatus(DeploymentStatus status);

    // Find by model
    List<Deployment> findByModelId(String modelId);

    Optional<Deployment> findByModelIdAndStatus(String modelId, DeploymentStatus status);

    // Find by AutoML job
    List<Deployment> findByAutoMLJobId(String autoMLJobId);

    Optional<Deployment> findByAutoMLJobIdAndStatus(String autoMLJobId, DeploymentStatus status);

    // Get latest version number for a project
    @Query("SELECT COALESCE(MAX(d.version), 0) FROM Deployment d WHERE d.project.id = :projectId")
    Integer findMaxVersionByProjectId(String projectId);

    // Get active deployment
    @Query("SELECT d FROM Deployment d WHERE d.project.id = :projectId AND d.status = 'ACTIVE'")
    Optional<Deployment> findActiveByProjectId(String projectId);

    // Count deployments
    Long countByProjectId(String projectId);

    Long countByProjectIdAndStatus(String projectId, DeploymentStatus status);

    // Get deployment history
    @Query("SELECT d FROM Deployment d WHERE d.project.id = :projectId ORDER BY d.version DESC")
    List<Deployment> findDeploymentHistory(String projectId);

    // Find recent deployments across all projects
    List<Deployment> findTop10ByOrderByDeployedAtDesc();

    // Check if model is deployed
    @Query("SELECT COUNT(d) > 0 FROM Deployment d WHERE d.model.id = :modelId AND d.status = 'ACTIVE'")
    boolean isModelActivelyDeployed(String modelId);
}
