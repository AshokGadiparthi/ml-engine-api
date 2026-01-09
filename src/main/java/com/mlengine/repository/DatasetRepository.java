package com.mlengine.repository;

import com.mlengine.model.entity.Dataset;
import com.mlengine.model.enums.DatasetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Dataset entity.
 */
@Repository
public interface DatasetRepository extends JpaRepository<Dataset, String> {

    List<Dataset> findByProjectIdOrderByUpdatedAtDesc(String projectId);

    List<Dataset> findByProjectIdAndStatusOrderByUpdatedAtDesc(String projectId, DatasetStatus status);

    List<Dataset> findByStatusOrderByUpdatedAtDesc(DatasetStatus status);

    @Query("SELECT d FROM Dataset d WHERE d.projectId = :projectId AND d.status != 'DELETED' ORDER BY d.updatedAt DESC")
    List<Dataset> findActiveByProjectId(@Param("projectId") String projectId);

    @Query("SELECT COUNT(d) FROM Dataset d WHERE d.projectId = :projectId AND d.status = 'ACTIVE'")
    Integer countActiveByProjectId(@Param("projectId") String projectId);

    @Query("SELECT COALESCE(SUM(d.fileSizeBytes), 0) FROM Dataset d WHERE d.projectId = :projectId AND d.status = 'ACTIVE'")
    Long sumFileSizeByProjectId(@Param("projectId") String projectId);

    @Query("SELECT AVG(d.qualityScore) FROM Dataset d WHERE d.projectId = :projectId AND d.status = 'ACTIVE'")
    Double avgQualityScoreByProjectId(@Param("projectId") String projectId);

    boolean existsByNameIgnoreCaseAndProjectId(String name, String projectId);

    List<Dataset> findByDataSourceId(String dataSourceId);
}
