package com.mlengine.controller;

import com.mlengine.model.dto.DatasetDTO;
import com.mlengine.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for Dataset operations.
 * Matches React UI Data Management requirements.
 */
@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
@Tag(name = "Datasets", description = "Dataset management endpoints")
@CrossOrigin
public class DatasetController {

    private final DatasetService datasetService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and create dataset from file",
               description = "Upload CSV, Excel, or Parquet file to create a new dataset")
    public ResponseEntity<DatasetDTO.Response> createFromFile(
            @Parameter(description = "Data file (CSV, Excel, Parquet)")
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "Dataset name")
            @RequestParam("name") String name,

            @Parameter(description = "Dataset description")
            @RequestParam(value = "description", required = false) String description,

            @Parameter(description = "Project ID")
            @RequestParam(value = "projectId", required = false) String projectId
    ) throws Exception {
        DatasetDTO.CreateFromFileRequest request = DatasetDTO.CreateFromFileRequest.builder()
                .name(name)
                .description(description)
                .projectId(projectId)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(datasetService.createFromFile(file, request));
    }

    @GetMapping
    @Operation(summary = "Get all datasets",
               description = "Get datasets, optionally filtered by project")
    public ResponseEntity<List<DatasetDTO.ListItem>> getDatasets(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(datasetService.getDatasetsByProject(projectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dataset by ID")
    public ResponseEntity<DatasetDTO.Response> getDataset(@PathVariable String id) {
        return ResponseEntity.ok(datasetService.getDataset(id));
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Get data preview",
               description = "Returns first N rows of the dataset")
    public ResponseEntity<DatasetDTO.PreviewResponse> getPreview(
            @PathVariable String id,
            @Parameter(description = "Number of rows to preview")
            @RequestParam(defaultValue = "100") int rows) throws Exception {
        return ResponseEntity.ok(datasetService.getPreview(id, rows));
    }

    @GetMapping("/{id}/columns")
    @Operation(summary = "Get column information",
               description = "Returns metadata for all columns in the dataset")
    public ResponseEntity<List<DatasetDTO.ColumnInfo>> getColumns(@PathVariable String id) {
        return ResponseEntity.ok(datasetService.getColumns(id));
    }

    @GetMapping("/{id}/quality")
    @Operation(summary = "Get data quality report",
               description = "Returns quality analysis for the dataset")
    public ResponseEntity<DatasetDTO.QualityReport> getQualityReport(@PathVariable String id) {
        return ResponseEntity.ok(datasetService.getQualityReport(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dataset")
    public ResponseEntity<DatasetDTO.Response> updateDataset(
            @PathVariable String id,
            @Valid @RequestBody DatasetDTO.UpdateRequest request) {
        return ResponseEntity.ok(datasetService.updateDataset(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete dataset")
    public ResponseEntity<Void> deleteDataset(@PathVariable String id) {
        datasetService.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }
}
