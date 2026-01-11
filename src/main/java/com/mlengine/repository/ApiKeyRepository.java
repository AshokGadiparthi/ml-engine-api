package com.mlengine.repository;

import com.mlengine.model.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ApiKey entity.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    Optional<ApiKey> findByKeyValue(String keyValue);

    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    List<ApiKey> findByProjectIdAndIsActiveTrue(String projectId);

    List<ApiKey> findByModelIdAndIsActiveTrue(String modelId);

    List<ApiKey> findByUserIdAndIsActiveTrue(String userId);

    @Query("SELECT a FROM ApiKey a WHERE a.projectId = :projectId ORDER BY a.createdAt DESC")
    List<ApiKey> findByProjectId(@Param("projectId") String projectId);

    @Query("SELECT a FROM ApiKey a WHERE a.modelId = :modelId ORDER BY a.createdAt DESC")
    List<ApiKey> findByModelId(@Param("modelId") String modelId);

    boolean existsByKeyValue(String keyValue);
}
