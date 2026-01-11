package com.mlengine.service;

import com.mlengine.model.dto.ProjectDTO;
import com.mlengine.model.entity.Project;
import com.mlengine.model.enums.ProjectStatus;
import com.mlengine.repository.DatasetRepository;
import com.mlengine.repository.DataSourceRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.PredictionRepository;
import com.mlengine.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Project operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DatasetRepository datasetRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ModelRepository modelRepository;
    private final PredictionRepository predictionRepository;
    private final ActivityService activityService;

    /**
     * Create a new project.
     */
    @Transactional
    public ProjectDTO.Response createProject(ProjectDTO.CreateRequest request) {
        log.info("Creating project: {}", request.getName());

        // Check for duplicate name
        if (projectRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Project with name '" + request.getName() + "' already exists");
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .color(request.getColor() != null ? request.getColor() : "#3B82F6")
                .ownerEmail(request.getOwnerEmail())
                .status(ProjectStatus.ACTIVE)
                .teamMembers(1)
                .build();

        project = projectRepository.save(project);
        log.info("Created project with ID: {}", project.getId());
        
        // Record project creation activity
        try {
            activityService.recordProjectCreated(
                    project.getId(),
                    project.getName(),
                    "System"
            );
        } catch (Exception e) {
            log.warn("Failed to record activity: {}", e.getMessage());
        }

        return toResponse(project);
    }

    /**
     * Get all active projects with updated statistics.
     */
    public List<ProjectDTO.ListItem> getAllProjects() {
        List<Project> projects = projectRepository.findByStatusNotOrderByUpdatedAtDesc(ProjectStatus.DELETED);
        
        // Update stats for each project before returning
        for (Project project : projects) {
            updateProjectStatsQuick(project);
        }
        
        return projects
                .stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }
    
    /**
     * Quick stats update for list view (counts only).
     */
    private void updateProjectStatsQuick(Project project) {
        String projectId = project.getId();
        
        Integer datasetsCount = datasetRepository.countActiveByProjectId(projectId);
        Integer modelsCount = modelRepository.countByProject(projectId);
        Integer dataSourcesCount = dataSourceRepository.countByProjectId(projectId);  // Count ALL datasources
        
        project.setDatasetsCount(datasetsCount != null ? datasetsCount : 0);
        project.setModelsCount(modelsCount != null ? modelsCount : 0);
        project.setDataSourcesCount(dataSourcesCount != null ? dataSourcesCount : 0);
        
        projectRepository.save(project);
    }

    /**
     * Get project by ID.
     */
    public ProjectDTO.Response getProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        // Update statistics
        updateProjectStats(project);

        return toResponse(project);
    }

    /**
     * Update project.
     */
    @Transactional
    public ProjectDTO.Response updateProject(String id, ProjectDTO.UpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            project.setIconUrl(request.getIconUrl());
        }
        if (request.getColor() != null) {
            project.setColor(request.getColor());
        }
        if (request.getTeamMembers() != null) {
            project.setTeamMembers(request.getTeamMembers());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        project = projectRepository.save(project);
        return toResponse(project);
    }

    /**
     * Delete project (soft delete).
     */
    @Transactional
    public void deleteProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        project.setStatus(ProjectStatus.DELETED);
        projectRepository.save(project);
        log.info("Deleted project: {}", id);
    }
    
    /**
     * Refresh project statistics on demand.
     * Called by frontend after operations complete to get updated counts.
     */
    @Transactional
    public ProjectDTO.Response refreshProjectStats(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // Force recalculate all stats
        updateProjectStats(project);
        
        log.info("Refreshed stats for project {}: models={}, datasets={}, datasources={}", 
                projectId, project.getModelsCount(), project.getDatasetsCount(), project.getDataSourcesCount());

        return toResponse(project);
    }

    /**
     * Get dashboard statistics for a project.
     */
    public ProjectDTO.DashboardStats getDashboardStats(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        updateProjectStats(project);

        return ProjectDTO.DashboardStats.builder()
                .modelsCount(project.getModelsCount())
                .deployedModelsCount(project.getDeployedModelsCount())
                .datasetsCount(project.getDatasetsCount())
                .dataSourcesCount(project.getDataSourcesCount())
                .totalDataSize(formatFileSize(project.getTotalDataSizeBytes()))
                .totalDataSizeBytes(project.getTotalDataSizeBytes())
                .avgAccuracy(project.getAvgAccuracy())
                .accuracyTrend(project.getAccuracyTrend())
                .accuracyTrendLabel(formatTrend(project.getAccuracyTrend()))
                .predictionsCount(project.getPredictionsCount())
                .predictionsThisMonth(project.getPredictionsThisMonth())
                .predictionsLabel("This month")
                .dataQualityScore(datasetRepository.avgQualityScoreByProjectId(projectId))
                .build();
    }

    // ========== Private Helper Methods ==========

    private void updateProjectStats(Project project) {
        String projectId = project.getId();
        
        // Update dataset count and size
        Integer datasetsCount = datasetRepository.countActiveByProjectId(projectId);
        Long totalSize = datasetRepository.sumFileSizeByProjectId(projectId);
        project.setDatasetsCount(datasetsCount != null ? datasetsCount : 0);
        project.setTotalDataSizeBytes(totalSize != null ? totalSize : 0L);
        
        // Update model counts
        Integer modelsCount = modelRepository.countByProject(projectId);
        Integer deployedModelsCount = modelRepository.countDeployedByProject(projectId);
        project.setModelsCount(modelsCount != null ? modelsCount : 0);
        project.setDeployedModelsCount(deployedModelsCount != null ? deployedModelsCount : 0);
        
        // Update datasource count
        Integer dataSourcesCount = dataSourceRepository.countByProjectId(projectId);  // Count ALL datasources
        project.setDataSourcesCount(dataSourcesCount != null ? dataSourcesCount : 0);
        
        // Update average accuracy
        Double avgAccuracy = modelRepository.avgAccuracyByProject(projectId);
        project.setAvgAccuracy(avgAccuracy);
        
        // Calculate accuracy trend (compare to last week - simplified)
        // For now, just set based on whether we have models
        if (avgAccuracy != null && avgAccuracy > 0) {
            project.setAccuracyTrend(0.0); // neutral trend
        }
        
        // Update prediction counts
        Long predictionsCount = predictionRepository.countByProject(projectId);
        project.setPredictionsCount(predictionsCount != null ? predictionsCount : 0L);
        
        // Predictions this month
        LocalDateTime monthStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
        Long predictionsThisMonth = predictionRepository.countByProjectSince(projectId, monthStart);
        project.setPredictionsThisMonth(predictionsThisMonth != null ? predictionsThisMonth : 0L);

        // Save updated stats
        projectRepository.save(project);
    }

    private ProjectDTO.Response toResponse(Project project) {
        return ProjectDTO.Response.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .iconUrl(project.getIconUrl())
                .color(project.getColor())
                .status(project.getStatus())
                .ownerEmail(project.getOwnerEmail())
                .teamMembers(project.getTeamMembers())
                .modelsCount(project.getModelsCount())
                .deployedModelsCount(project.getDeployedModelsCount())
                .datasetsCount(project.getDatasetsCount())
                .dataSourcesCount(project.getDataSourcesCount())
                .totalDataSize(formatFileSize(project.getTotalDataSizeBytes()))
                .totalDataSizeBytes(project.getTotalDataSizeBytes())
                .avgAccuracy(project.getAvgAccuracy())
                .accuracyTrend(project.getAccuracyTrend())
                .predictionsCount(project.getPredictionsCount())
                .predictionsThisMonth(project.getPredictionsThisMonth())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectDTO.ListItem toListItem(Project project) {
        return ProjectDTO.ListItem.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .iconUrl(project.getIconUrl())
                .color(project.getColor())
                .status(project.getStatus())
                .modelsCount(project.getModelsCount())
                .datasetsCount(project.getDatasetsCount())
                .dataSourcesCount(project.getDataSourcesCount())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    private String formatTrend(Double trend) {
        if (trend == null) return null;
        String sign = trend >= 0 ? "+" : "";
        return sign + String.format("%.1f%% from last week", trend);
    }
}
