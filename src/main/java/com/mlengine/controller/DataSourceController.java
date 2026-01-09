package com.mlengine.controller;

import com.mlengine.model.dto.DataSourceDTO;
import com.mlengine.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for DataSource operations.
 * Matches React UI "Connect New Data Source" modal.
 */
@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
@Tag(name = "Data Sources", description = "Data source connection endpoints")
@CrossOrigin
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PostMapping
    @Operation(summary = "Create new data source connection",
               description = "Connect to PostgreSQL, MySQL, BigQuery, S3, or GCS")
    public ResponseEntity<DataSourceDTO.Response> createDataSource(
            @Valid @RequestBody DataSourceDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dataSourceService.createDataSource(request));
    }

    @GetMapping
    @Operation(summary = "Get all data sources",
               description = "Get data sources, optionally filtered by project")
    public ResponseEntity<List<DataSourceDTO.ListItem>> getDataSources(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(dataSourceService.getDataSourcesByProject(projectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get data source by ID")
    public ResponseEntity<DataSourceDTO.Response> getDataSource(@PathVariable String id) {
        return ResponseEntity.ok(dataSourceService.getDataSource(id));
    }

    @PostMapping("/test")
    @Operation(summary = "Test connection",
               description = "Test connection without saving")
    public ResponseEntity<DataSourceDTO.TestConnectionResponse> testConnection(
            @Valid @RequestBody DataSourceDTO.TestConnectionRequest request) {
        return ResponseEntity.ok(dataSourceService.testConnection(request));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test existing connection",
               description = "Test connection for an existing data source")
    public ResponseEntity<DataSourceDTO.TestConnectionResponse> testExistingConnection(
            @PathVariable String id) {
        return ResponseEntity.ok(dataSourceService.testConnection(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update data source")
    public ResponseEntity<DataSourceDTO.Response> updateDataSource(
            @PathVariable String id,
            @Valid @RequestBody DataSourceDTO.UpdateRequest request) {
        return ResponseEntity.ok(dataSourceService.updateDataSource(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete data source")
    public ResponseEntity<Void> deleteDataSource(@PathVariable String id) {
        dataSourceService.deleteDataSource(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/browse")
    @Operation(summary = "Browse data source tables",
               description = "Get list of tables/collections from the data source")
    public ResponseEntity<DataSourceDTO.BrowseResponse> browseDataSource(
            @PathVariable String id) {
        return ResponseEntity.ok(dataSourceService.browseDataSource(id));
    }

    @GetMapping("/{id}/tables/{tableName}/preview")
    @Operation(summary = "Preview table data",
               description = "Get sample data from a specific table in the data source")
    public ResponseEntity<DataSourceDTO.TablePreviewResponse> previewTable(
            @PathVariable String id,
            @PathVariable String tableName,
            @Parameter(description = "Number of rows to preview")
            @RequestParam(defaultValue = "100") int rows) {
        return ResponseEntity.ok(dataSourceService.previewTable(id, tableName, rows));
    }
}
