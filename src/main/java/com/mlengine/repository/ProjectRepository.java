package com.mlengine.repository;

import com.mlengine.model.entity.Project;
import com.mlengine.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Project entity.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    List<Project> findByStatusOrderByUpdatedAtDesc(ProjectStatus status);

    List<Project> findByStatusNotOrderByUpdatedAtDesc(ProjectStatus status);

    Optional<Project> findByNameIgnoreCase(String name);

    @Query("SELECT p FROM Project p WHERE p.status = 'ACTIVE' ORDER BY p.updatedAt DESC")
    List<Project> findActiveProjects();

    @Query("SELECT COUNT(p) FROM Project p WHERE p.status = 'ACTIVE'")
    Long countActiveProjects();

    boolean existsByNameIgnoreCase(String name);
}
