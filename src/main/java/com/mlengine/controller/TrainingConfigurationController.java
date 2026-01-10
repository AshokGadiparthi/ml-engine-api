package com.mlengine.controller;

import com.mlengine.model.dto.TrainingConfigurationDTO;
import com.mlengine.service.TrainingConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Training Configuration management.
 * Allows users to save, load, and manage training configurations at different scope levels:
 * - GLOBAL: Available everywhere
 * - PROJECT: Available for all datasets in a project
 * - DATASET: Specific to a dataset
 * - DATASOURCE: Specific to a data source
 */
@RestController
@RequestMapping("/api/training/configurations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TrainingConfigurationController {

    private final TrainingConfigurationService configService;

    /**
     * Save a new training configuration.
     * 
     * POST /api/training/configurations
     * 
     * Request body should include 'scope' field:
     * - "GLOBAL" - Available everywhere
     * - "PROJECT" - Available for all datasets in projectId
     * - "DATASET" - Specific to datasetId
     * - "DATASOURCE" - Specific to datasourceId
     */
    @PostMapping
    public ResponseEntity<TrainingConfigurationDTO.Response> saveConfiguration(
            @RequestBody TrainingConfigurationDTO.CreateRequest request) {
        log.info("Saving training configuration: {} at scope: {}", request.getName(), request.getScope());
        TrainingConfigurationDTO.Response response = configService.saveConfiguration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get configuration by ID.
     * GET /api/training/configurations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TrainingConfigurationDTO.Response> getConfiguration(@PathVariable String id) {
        TrainingConfigurationDTO.Response response = configService.getConfiguration(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List configurations with optional scope filters.
     * 
     * GET /api/training/configurations?scope=PROJECT&projectId=xxx&datasetId=xxx&datasourceId=xxx&search=xxx&includeParentScopes=true
     * 
     * @param scope Filter by specific scope (PROJECT, DATASET, DATASOURCE, GLOBAL)
     * @param projectId Project ID (required for PROJECT scope, optional for others)
     * @param datasetId Dataset ID (required for DATASET scope)
     * @param datasourceId DataSource ID (required for DATASOURCE scope)
     * @param search Search query for name/description
     * @param includeParentScopes If true, also include configs from parent scopes
     */
    @GetMapping
    public ResponseEntity<List<TrainingConfigurationDTO.ListItem>> listConfigurations(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String datasetId,
            @RequestParam(required = false) String datasourceId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean includeParentScopes) {
        
        List<TrainingConfigurationDTO.ListItem> configs = configService.listConfigurations(
                scope, projectId, datasetId, datasourceId, search, includeParentScopes);
        return ResponseEntity.ok(configs);
    }

    /**
     * Update configuration.
     * PUT /api/training/configurations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TrainingConfigurationDTO.Response> updateConfiguration(
            @PathVariable String id,
            @RequestBody TrainingConfigurationDTO.CreateRequest request) {
        log.info("Updating training configuration: {}", id);
        TrainingConfigurationDTO.Response response = configService.updateConfiguration(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete configuration (soft delete).
     * DELETE /api/training/configurations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable String id) {
        log.info("Deleting training configuration: {}", id);
        configService.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clone configuration to same or different scope.
     * POST /api/training/configurations/{id}/clone?newName=xxx&newScope=PROJECT
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<TrainingConfigurationDTO.Response> cloneConfiguration(
            @PathVariable String id,
            @RequestParam(required = false) String newName,
            @RequestParam(required = false) String newScope) {
        log.info("Cloning training configuration: {} to {} at scope {}", id, newName, newScope);
        TrainingConfigurationDTO.Response response = configService.cloneConfiguration(id, newName, newScope);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Record usage of a configuration (call when using a saved config to start training).
     * POST /api/training/configurations/{id}/usage
     */
    @PostMapping("/{id}/usage")
    public ResponseEntity<Void> recordUsage(@PathVariable String id) {
        configService.recordUsage(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get popular configurations.
     * GET /api/training/configurations/popular?projectId=xxx&datasetId=xxx&limit=10
     */
    @GetMapping("/popular")
    public ResponseEntity<List<TrainingConfigurationDTO.ListItem>> getPopularConfigurations(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String datasetId,
            @RequestParam(defaultValue = "10") int limit) {
        List<TrainingConfigurationDTO.ListItem> configs = configService.getPopularConfigurations(
                projectId, datasetId, limit);
        return ResponseEntity.ok(configs);
    }

    /**
     * Get recently used configurations.
     * GET /api/training/configurations/recent?projectId=xxx&limit=10
     */
    @GetMapping("/recent")
    public ResponseEntity<List<TrainingConfigurationDTO.ListItem>> getRecentConfigurations(
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "10") int limit) {
        List<TrainingConfigurationDTO.ListItem> configs = configService.getRecentConfigurations(projectId, limit);
        return ResponseEntity.ok(configs);
    }

    /**
     * Export configuration as JSON.
     * GET /api/training/configurations/{id}/export
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<Map<String, Object>> exportConfiguration(@PathVariable String id) {
        Map<String, Object> export = configService.exportConfiguration(id);
        return ResponseEntity.ok(export);
    }

    /**
     * Import configuration from JSON.
     * POST /api/training/configurations/import?scope=PROJECT&projectId=xxx
     */
    @PostMapping("/import")
    public ResponseEntity<TrainingConfigurationDTO.Response> importConfiguration(
            @RequestBody TrainingConfigurationDTO.CreateRequest request,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String datasetId,
            @RequestParam(required = false) String datasourceId) {
        
        // Override scope fields from query params if provided
        if (scope != null) request.setScope(scope);
        if (projectId != null) request.setProjectId(projectId);
        if (datasetId != null) request.setDatasetId(datasetId);
        if (datasourceId != null) request.setDatasourceId(datasourceId);
        
        TrainingConfigurationDTO.Response response = configService.saveConfiguration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
