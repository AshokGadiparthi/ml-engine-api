package com.mlengine.repository;

import com.mlengine.model.entity.TrainingConfiguration;
import com.mlengine.model.entity.TrainingConfiguration.ConfigScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingConfigurationRepository extends JpaRepository<TrainingConfiguration, String> {

    // ============ SCOPE-BASED QUERIES ============

    // Find by DataSource scope
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.scope = 'DATASOURCE' AND tc.datasourceId = :datasourceId AND tc.deletedAt IS NULL ORDER BY tc.createdAt DESC")
    List<TrainingConfiguration> findByDatasourceId(@Param("datasourceId") String datasourceId);

    // Find by Dataset scope
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.scope = 'DATASET' AND tc.datasetId = :datasetId AND tc.deletedAt IS NULL ORDER BY tc.createdAt DESC")
    List<TrainingConfiguration> findByDatasetId(@Param("datasetId") String datasetId);

    // Find by Project scope
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.scope = 'PROJECT' AND tc.projectId = :projectId AND tc.deletedAt IS NULL ORDER BY tc.createdAt DESC")
    List<TrainingConfiguration> findByProjectId(@Param("projectId") String projectId);

    // Find GLOBAL configs
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.scope = 'GLOBAL' AND tc.deletedAt IS NULL ORDER BY tc.createdAt DESC")
    List<TrainingConfiguration> findGlobalConfigs();

    // Find all configs available for a dataset (includes dataset-specific, project-level, and global)
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND " +
           "(tc.scope = 'GLOBAL' OR " +
           "(tc.scope = 'PROJECT' AND tc.projectId = :projectId) OR " +
           "(tc.scope = 'DATASET' AND tc.datasetId = :datasetId) OR " +
           "(tc.scope = 'DATASOURCE' AND tc.datasourceId = :datasourceId)) " +
           "ORDER BY tc.scope, tc.createdAt DESC")
    List<TrainingConfiguration> findAvailableForDataset(
            @Param("projectId") String projectId,
            @Param("datasetId") String datasetId,
            @Param("datasourceId") String datasourceId);

    // Find all configs available for a project (includes project-level and global)
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND " +
           "(tc.scope = 'GLOBAL' OR (tc.scope = 'PROJECT' AND tc.projectId = :projectId)) " +
           "ORDER BY tc.scope, tc.createdAt DESC")
    List<TrainingConfiguration> findAvailableForProject(@Param("projectId") String projectId);

    // ============ GENERAL QUERIES ============

    // Find all active (excluding soft-deleted)
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL ORDER BY tc.createdAt DESC")
    List<TrainingConfiguration> findAllActive();

    // Find by ID (excluding soft-deleted)
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.id = :id AND tc.deletedAt IS NULL")
    Optional<TrainingConfiguration> findByIdActive(@Param("id") String id);

    // Find by name within scope (for duplicate check)
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.name = :name AND tc.scope = :scope AND " +
           "((tc.scope = 'PROJECT' AND tc.projectId = :scopeId) OR " +
           "(tc.scope = 'DATASET' AND tc.datasetId = :scopeId) OR " +
           "(tc.scope = 'DATASOURCE' AND tc.datasourceId = :scopeId) OR " +
           "(tc.scope = 'GLOBAL')) AND tc.deletedAt IS NULL")
    Optional<TrainingConfiguration> findByNameAndScope(
            @Param("name") String name, 
            @Param("scope") ConfigScope scope,
            @Param("scopeId") String scopeId);

    // ============ POPULARITY QUERIES ============

    // Find most popular by scope
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.scope = :scope AND tc.deletedAt IS NULL ORDER BY tc.usageCount DESC")
    List<TrainingConfiguration> findMostPopularByScope(@Param("scope") ConfigScope scope);

    // Find most popular for project
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND " +
           "(tc.scope = 'GLOBAL' OR (tc.scope = 'PROJECT' AND tc.projectId = :projectId)) " +
           "ORDER BY tc.usageCount DESC")
    List<TrainingConfiguration> findMostPopularForProject(@Param("projectId") String projectId);

    // Find most popular for dataset
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND " +
           "(tc.scope = 'GLOBAL' OR " +
           "(tc.scope = 'PROJECT' AND tc.projectId = :projectId) OR " +
           "(tc.scope = 'DATASET' AND tc.datasetId = :datasetId)) " +
           "ORDER BY tc.usageCount DESC")
    List<TrainingConfiguration> findMostPopularForDataset(
            @Param("projectId") String projectId,
            @Param("datasetId") String datasetId);

    // ============ RECENT QUERIES ============

    // Find recently used
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND tc.lastUsedAt IS NOT NULL ORDER BY tc.lastUsedAt DESC")
    List<TrainingConfiguration> findRecentlyUsed();

    // Find recently used for project
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND tc.lastUsedAt IS NOT NULL AND " +
           "(tc.scope = 'GLOBAL' OR (tc.scope = 'PROJECT' AND tc.projectId = :projectId)) " +
           "ORDER BY tc.lastUsedAt DESC")
    List<TrainingConfiguration> findRecentlyUsedForProject(@Param("projectId") String projectId);

    // ============ SEARCH QUERIES ============

    // Search within scope
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND " +
           "(LOWER(tc.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(tc.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY tc.usageCount DESC")
    List<TrainingConfiguration> search(@Param("query") String query);

    // Search within project scope
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.deletedAt IS NULL AND " +
           "(tc.scope = 'GLOBAL' OR (tc.scope = 'PROJECT' AND tc.projectId = :projectId)) AND " +
           "(LOWER(tc.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(tc.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY tc.usageCount DESC")
    List<TrainingConfiguration> searchInProject(@Param("projectId") String projectId, @Param("query") String query);

    // Find by algorithm
    @Query("SELECT tc FROM TrainingConfiguration tc WHERE tc.algorithm = :algorithm AND tc.deletedAt IS NULL ORDER BY tc.usageCount DESC")
    List<TrainingConfiguration> findByAlgorithm(@Param("algorithm") String algorithm);

    // Count by scope
    @Query("SELECT COUNT(tc) FROM TrainingConfiguration tc WHERE tc.scope = :scope AND tc.deletedAt IS NULL")
    long countByScope(@Param("scope") ConfigScope scope);
}
