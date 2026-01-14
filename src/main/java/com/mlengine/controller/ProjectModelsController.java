package com.mlengine.controller;

import com.mlengine.model.dto.ModelDTO;
import com.mlengine.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for accessing models by project.
 * Handles: GET /api/projects/{projectId}/models
 */
@Slf4j
@RestController
@RequestMapping("/api/projects/{projectId}/models")
@RequiredArgsConstructor
@Tag(name = "Project Models", description = "Get models for a specific project")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectModelsController {

    private final ModelService modelService;

    @GetMapping
    @Operation(summary = "Get all models for a project",
            description = "Returns list of all models in a specific project")
    public ResponseEntity<List<ModelDTO.ListItem>> getProjectModels(
            @Parameter(description = "The project ID")
            @PathVariable String projectId) {

        log.info("GET /api/projects/{}/models", projectId);

        try {
            List<ModelDTO.ListItem> models = modelService.getAllModels(projectId);
            log.info("Found {} models for project {}", models.size(), projectId);
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Error fetching models for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{modelId}")
    @Operation(summary = "Get specific model from project",
            description = "Returns a specific model from a project")
    public ResponseEntity<ModelDTO.Response> getProjectModel(
            @Parameter(description = "The project ID")
            @PathVariable String projectId,
            @Parameter(description = "The model ID")
            @PathVariable String modelId) {

        log.info("GET /api/projects/{}/models/{}", projectId, modelId);

        try {
            ModelDTO.Response model = modelService.getModel(modelId);

            if (!model.getProjectId().equals(projectId)) {
                log.warn("Model {} does not belong to project {}", modelId, projectId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(model);
        } catch (Exception e) {
            log.error("Error fetching model {} for project {}: {}",
                    modelId, projectId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}