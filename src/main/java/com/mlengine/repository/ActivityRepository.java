package com.mlengine.repository;

import com.mlengine.model.entity.Activity;
import com.mlengine.model.entity.Activity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Activity entity.
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, String> {

    // Find by project with pagination
    Page<Activity> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);

    // Find all with pagination
    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Find recent by project (no pagination)
    List<Activity> findTop10ByProjectIdOrderByCreatedAtDesc(String projectId);

    // Find recent all (no pagination)
    List<Activity> findTop10ByOrderByCreatedAtDesc();

    // Find by activity type
    List<Activity> findByProjectIdAndActivityTypeOrderByCreatedAtDesc(
            String projectId, ActivityType activityType);

    // Find by entity
    List<Activity> findByEntityIdOrderByCreatedAtDesc(String entityId);

    // Find by user
    List<Activity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    // Count by project
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.projectId = :projectId")
    Long countByProject(@Param("projectId") String projectId);

    // Count by project since date
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.projectId = :projectId AND a.createdAt > :since")
    Long countByProjectSince(@Param("projectId") String projectId, @Param("since") LocalDateTime since);

    // Count by type and project
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.projectId = :projectId AND a.activityType = :type")
    Long countByProjectAndType(@Param("projectId") String projectId, @Param("type") ActivityType type);

    // Count since date
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.createdAt > :since")
    Long countSince(@Param("since") LocalDateTime since);

    // Count by type since date
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.activityType = :type AND a.createdAt > :since")
    Long countByTypeSince(@Param("type") ActivityType type, @Param("since") LocalDateTime since);
}
