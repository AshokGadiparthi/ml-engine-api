package com.mlengine.repository;

import com.mlengine.model.entity.DataSource;
import com.mlengine.model.enums.DataSourceStatus;
import com.mlengine.model.enums.DataSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DataSource entity.
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, String> {

    List<DataSource> findByProjectIdOrderByUpdatedAtDesc(String projectId);

    List<DataSource> findByProjectIdAndStatusOrderByUpdatedAtDesc(String projectId, DataSourceStatus status);

    List<DataSource> findBySourceTypeOrderByUpdatedAtDesc(DataSourceType sourceType);

    @Query("SELECT ds FROM DataSource ds WHERE ds.projectId = :projectId AND ds.status = 'CONNECTED' ORDER BY ds.updatedAt DESC")
    List<DataSource> findConnectedByProjectId(@Param("projectId") String projectId);

    @Query("SELECT COUNT(ds) FROM DataSource ds WHERE ds.projectId = :projectId AND ds.status = 'CONNECTED'")
    Integer countConnectedByProjectId(@Param("projectId") String projectId);
    
    @Query("SELECT COUNT(ds) FROM DataSource ds WHERE ds.projectId = :projectId")
    Integer countByProjectId(@Param("projectId") String projectId);

    boolean existsByNameIgnoreCaseAndProjectId(String name, String projectId);
}
