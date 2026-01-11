package com.mlengine.controller;

import com.mlengine.model.dto.ProjectDTO;
import com.mlengine.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Project operations.
 * Matches React UI Dashboard requirements.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management endpoints")
@CrossOrigin
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ProjectDTO.Response> createProject(
            @Valid @RequestBody ProjectDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request));
    }

    @GetMapping
    @Operation(summary = "Get all projects")
    public ResponseEntity<List<ProjectDTO.ListItem>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectDTO.Response> getProject(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project")
    public ResponseEntity<ProjectDTO.Response> updateProject(
            @PathVariable String id,
            @Valid @RequestBody ProjectDTO.UpdateRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get dashboard statistics for project",
               description = "Returns all stats needed for the Dashboard UI")
    public ResponseEntity<ProjectDTO.DashboardStats> getDashboardStats(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getDashboardStats(id));
    }
    
    @PostMapping("/{id}/refresh-stats")
    @Operation(summary = "Refresh project statistics",
               description = "Recalculates all project stats (models, datasets, datasources). Call after training/upload completes.")
    public ResponseEntity<ProjectDTO.Response> refreshStats(@PathVariable String id) {
        return ResponseEntity.ok(projectService.refreshProjectStats(id));
    }
}
