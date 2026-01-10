package com.mlengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.model.dto.TrainingConfigurationDTO;
import com.mlengine.model.entity.Project;
import com.mlengine.model.entity.TrainingConfiguration;
import com.mlengine.model.entity.TrainingConfiguration.ConfigScope;
import com.mlengine.model.enums.ProblemType;
import com.mlengine.repository.ProjectRepository;
import com.mlengine.repository.TrainingConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingConfigurationService {

    private final TrainingConfigurationRepository configRepository;
    private final ProjectRepository projectRepository;
    private final AlgorithmService algorithmService;
    private final ObjectMapper objectMapper;

    /**
     * Save a new training configuration at the specified scope level.
     */
    @Transactional
    public TrainingConfigurationDTO.Response saveConfiguration(TrainingConfigurationDTO.CreateRequest request) {
        log.info("Saving training configuration: {} at scope: {}", request.getName(), request.getScope());

        // Determine scope
        ConfigScope scope = parseScope(request.getScope());
        String scopeId = getScopeId(request, scope);

        // Check for duplicate name within scope
        if (request.getName() != null) {
            Optional<TrainingConfiguration> existing = configRepository.findByNameAndScope(
                    request.getName(), scope, scopeId);
            if (existing.isPresent()) {
                throw new RuntimeException("Configuration with name '" + request.getName() + "' already exists in this scope");
            }
        }

        // Get project if specified
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId()).orElse(null);
        }

        // Get algorithm display name
        String algorithmDisplayName = request.getAlgorithmDisplayName();
        if (algorithmDisplayName == null && request.getAlgorithm() != null) {
            algorithmDisplayName = algorithmService.getAlgorithmDisplayName(request.getAlgorithm());
        }

        // Build entity
        TrainingConfiguration config = TrainingConfiguration.builder()
                .name(request.getName())
                .description(request.getDescription())
                .scope(scope)
                .datasourceId(request.getDatasourceId())
                .datasourceName(request.getDatasourceName())
                .datasetId(request.getDatasetId())
                .datasetName(request.getDatasetName())
                .targetVariable(request.getTargetVariable())
                .problemType(request.getProblemType() != null ? 
                        ProblemType.valueOf(request.getProblemType()) : null)
                .algorithm(request.getAlgorithm())
                .algorithmDisplayName(algorithmDisplayName)
                .trainTestSplit(request.getTrainTestSplit() != null ? request.getTrainTestSplit() : 0.8)
                .crossValidationFolds(request.getCrossValidationFolds() != null ? request.getCrossValidationFolds() : 5)
                .hyperparametersJson(serializeHyperparameters(request.getHyperparameters()))
                .gpuAcceleration(request.getGpuAcceleration() != null ? request.getGpuAcceleration() : false)
                .autoHyperparameterTuning(request.getAutoHyperparameterTuning() != null ? request.getAutoHyperparameterTuning() : false)
                .earlyStopping(request.getEarlyStopping() != null ? request.getEarlyStopping() : true)
                .earlyStoppingPatience(request.getEarlyStoppingPatience() != null ? request.getEarlyStoppingPatience() : 10)
                .batchSize(request.getBatchSize() != null ? request.getBatchSize() : 32)
                .evaluationMetric(request.getEvaluationMetric() != null ? request.getEvaluationMetric() : "accuracy")
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .project(project)
                .usageCount(0)
                .build();

        TrainingConfiguration saved = configRepository.save(config);
        log.info("Saved training configuration: {} with ID: {} at scope: {}", saved.getName(), saved.getId(), scope);

        return toResponse(saved);
    }

    /**
     * Get configuration by ID.
     */
    public TrainingConfigurationDTO.Response getConfiguration(String id) {
        TrainingConfiguration config = configRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + id));
        return toResponse(config);
    }

    /**
     * List configurations based on scope filters.
     * 
     * @param scope Filter by specific scope (PROJECT, DATASET, DATASOURCE, GLOBAL)
     * @param projectId Project ID for PROJECT scope or to get all available configs for a project
     * @param datasetId Dataset ID for DATASET scope
     * @param datasourceId DataSource ID for DATASOURCE scope
     * @param search Search query
     * @param includeParentScopes If true, also include configs from parent scopes (e.g., project + global for dataset)
     */
    public List<TrainingConfigurationDTO.ListItem> listConfigurations(
            String scope, 
            String projectId, 
            String datasetId, 
            String datasourceId,
            String search,
            boolean includeParentScopes) {
        
        List<TrainingConfiguration> configs;

        // If search is provided, search across all accessible configs
        if (search != null && !search.isEmpty()) {
            if (projectId != null) {
                configs = configRepository.searchInProject(projectId, search);
            } else {
                configs = configRepository.search(search);
            }
        }
        // If includeParentScopes, get all configs available at the given level
        else if (includeParentScopes) {
            if (datasetId != null) {
                configs = configRepository.findAvailableForDataset(projectId, datasetId, datasourceId);
            } else if (projectId != null) {
                configs = configRepository.findAvailableForProject(projectId);
            } else {
                configs = configRepository.findAllActive();
            }
        }
        // Filter by specific scope
        else if (scope != null) {
            ConfigScope configScope = parseScope(scope);
            switch (configScope) {
                case DATASOURCE:
                    configs = datasourceId != null ? 
                            configRepository.findByDatasourceId(datasourceId) : new ArrayList<>();
                    break;
                case DATASET:
                    configs = datasetId != null ? 
                            configRepository.findByDatasetId(datasetId) : new ArrayList<>();
                    break;
                case PROJECT:
                    configs = projectId != null ? 
                            configRepository.findByProjectId(projectId) : new ArrayList<>();
                    break;
                case GLOBAL:
                    configs = configRepository.findGlobalConfigs();
                    break;
                default:
                    configs = configRepository.findAllActive();
            }
        }
        // Default: return all active configs
        else {
            configs = configRepository.findAllActive();
        }

        return configs.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    /**
     * Update configuration.
     */
    @Transactional
    public TrainingConfigurationDTO.Response updateConfiguration(String id, TrainingConfigurationDTO.CreateRequest request) {
        TrainingConfiguration config = configRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + id));

        // Update fields
        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getScope() != null) config.setScope(parseScope(request.getScope()));
        if (request.getDatasourceId() != null) config.setDatasourceId(request.getDatasourceId());
        if (request.getDatasourceName() != null) config.setDatasourceName(request.getDatasourceName());
        if (request.getDatasetId() != null) config.setDatasetId(request.getDatasetId());
        if (request.getDatasetName() != null) config.setDatasetName(request.getDatasetName());
        if (request.getTargetVariable() != null) config.setTargetVariable(request.getTargetVariable());
        if (request.getProblemType() != null) config.setProblemType(ProblemType.valueOf(request.getProblemType()));
        if (request.getAlgorithm() != null) {
            config.setAlgorithm(request.getAlgorithm());
            config.setAlgorithmDisplayName(algorithmService.getAlgorithmDisplayName(request.getAlgorithm()));
        }
        if (request.getTrainTestSplit() != null) config.setTrainTestSplit(request.getTrainTestSplit());
        if (request.getCrossValidationFolds() != null) config.setCrossValidationFolds(request.getCrossValidationFolds());
        if (request.getHyperparameters() != null) config.setHyperparametersJson(serializeHyperparameters(request.getHyperparameters()));
        if (request.getGpuAcceleration() != null) config.setGpuAcceleration(request.getGpuAcceleration());
        if (request.getAutoHyperparameterTuning() != null) config.setAutoHyperparameterTuning(request.getAutoHyperparameterTuning());
        if (request.getEarlyStopping() != null) config.setEarlyStopping(request.getEarlyStopping());
        if (request.getEarlyStoppingPatience() != null) config.setEarlyStoppingPatience(request.getEarlyStoppingPatience());
        if (request.getBatchSize() != null) config.setBatchSize(request.getBatchSize());
        if (request.getEvaluationMetric() != null) config.setEvaluationMetric(request.getEvaluationMetric());
        if (request.getTags() != null) config.setTags(String.join(",", request.getTags()));

        TrainingConfiguration saved = configRepository.save(config);
        return toResponse(saved);
    }

    /**
     * Delete configuration (soft delete).
     */
    @Transactional
    public void deleteConfiguration(String id) {
        TrainingConfiguration config = configRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + id));
        
        config.setDeletedAt(LocalDateTime.now());
        configRepository.save(config);
        log.info("Soft deleted configuration: {}", id);
    }

    /**
     * Clone configuration.
     */
    @Transactional
    public TrainingConfigurationDTO.Response cloneConfiguration(String id, String newName, String newScope) {
        TrainingConfiguration original = configRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + id));

        ConfigScope scope = newScope != null ? parseScope(newScope) : original.getScope();

        TrainingConfiguration clone = TrainingConfiguration.builder()
                .name(newName != null ? newName : original.getName() + " (Copy)")
                .description(original.getDescription())
                .scope(scope)
                .datasourceId(original.getDatasourceId())
                .datasourceName(original.getDatasourceName())
                .datasetId(original.getDatasetId())
                .datasetName(original.getDatasetName())
                .targetVariable(original.getTargetVariable())
                .problemType(original.getProblemType())
                .algorithm(original.getAlgorithm())
                .algorithmDisplayName(original.getAlgorithmDisplayName())
                .trainTestSplit(original.getTrainTestSplit())
                .crossValidationFolds(original.getCrossValidationFolds())
                .hyperparametersJson(original.getHyperparametersJson())
                .gpuAcceleration(original.getGpuAcceleration())
                .autoHyperparameterTuning(original.getAutoHyperparameterTuning())
                .earlyStopping(original.getEarlyStopping())
                .earlyStoppingPatience(original.getEarlyStoppingPatience())
                .batchSize(original.getBatchSize())
                .evaluationMetric(original.getEvaluationMetric())
                .tags(original.getTags())
                .project(original.getProject())
                .usageCount(0)
                .build();

        TrainingConfiguration saved = configRepository.save(clone);
        log.info("Cloned configuration {} to {}", id, saved.getId());
        return toResponse(saved);
    }

    /**
     * Record usage of a configuration.
     */
    @Transactional
    public void recordUsage(String id) {
        TrainingConfiguration config = configRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + id));
        
        config.setUsageCount(config.getUsageCount() + 1);
        config.setLastUsedAt(LocalDateTime.now());
        configRepository.save(config);
    }

    /**
     * Get popular configurations.
     */
    public List<TrainingConfigurationDTO.ListItem> getPopularConfigurations(
            String projectId, String datasetId, int limit) {
        List<TrainingConfiguration> configs;
        
        if (datasetId != null && projectId != null) {
            configs = configRepository.findMostPopularForDataset(projectId, datasetId);
        } else if (projectId != null) {
            configs = configRepository.findMostPopularForProject(projectId);
        } else {
            configs = configRepository.findMostPopularByScope(ConfigScope.GLOBAL);
        }
        
        return configs.stream()
                .limit(limit)
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    /**
     * Get recently used configurations.
     */
    public List<TrainingConfigurationDTO.ListItem> getRecentConfigurations(String projectId, int limit) {
        List<TrainingConfiguration> configs;
        if (projectId != null) {
            configs = configRepository.findRecentlyUsedForProject(projectId);
        } else {
            configs = configRepository.findRecentlyUsed();
        }
        
        return configs.stream()
                .limit(limit)
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    /**
     * Export configuration as JSON.
     */
    public Map<String, Object> exportConfiguration(String id) {
        TrainingConfiguration config = configRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Configuration not found: " + id));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("name", config.getName());
        export.put("description", config.getDescription());
        export.put("scope", config.getScope() != null ? config.getScope().name() : null);
        export.put("algorithm", config.getAlgorithm());
        export.put("algorithmDisplayName", config.getAlgorithmDisplayName());
        export.put("problemType", config.getProblemType() != null ? config.getProblemType().name() : null);
        export.put("targetVariable", config.getTargetVariable());
        export.put("trainTestSplit", config.getTrainTestSplit());
        export.put("crossValidationFolds", config.getCrossValidationFolds());
        export.put("hyperparameters", deserializeHyperparameters(config.getHyperparametersJson()));
        export.put("gpuAcceleration", config.getGpuAcceleration());
        export.put("autoHyperparameterTuning", config.getAutoHyperparameterTuning());
        export.put("earlyStopping", config.getEarlyStopping());
        export.put("earlyStoppingPatience", config.getEarlyStoppingPatience());
        export.put("batchSize", config.getBatchSize());
        export.put("evaluationMetric", config.getEvaluationMetric());
        export.put("exportedAt", LocalDateTime.now().toString());

        return export;
    }

    // ============ Helper Methods ============

    private ConfigScope parseScope(String scope) {
        if (scope == null || scope.isEmpty()) {
            return ConfigScope.PROJECT;  // Default
        }
        try {
            return ConfigScope.valueOf(scope.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ConfigScope.PROJECT;
        }
    }

    private String getScopeId(TrainingConfigurationDTO.CreateRequest request, ConfigScope scope) {
        switch (scope) {
            case DATASOURCE:
                return request.getDatasourceId();
            case DATASET:
                return request.getDatasetId();
            case PROJECT:
                return request.getProjectId();
            case GLOBAL:
            default:
                return null;
        }
    }

    private String getScopeLabel(ConfigScope scope) {
        if (scope == null) return "Project Level";
        switch (scope) {
            case GLOBAL: return "Global";
            case PROJECT: return "Project Level";
            case DATASET: return "Dataset Level";
            case DATASOURCE: return "DataSource Level";
            default: return "Project Level";
        }
    }

    private String getScopeEntityName(TrainingConfiguration config) {
        if (config.getScope() == null) return null;
        switch (config.getScope()) {
            case DATASOURCE: return config.getDatasourceName();
            case DATASET: return config.getDatasetName();
            case PROJECT: return config.getProject() != null ? config.getProject().getName() : null;
            case GLOBAL: return "All Projects";
            default: return null;
        }
    }

    private String serializeHyperparameters(Map<String, Object> hyperparameters) {
        if (hyperparameters == null || hyperparameters.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(hyperparameters);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize hyperparameters", e);
            return null;
        }
    }

    private Map<String, Object> deserializeHyperparameters(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize hyperparameters", e);
            return new HashMap<>();
        }
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(tags.split(","));
    }

    private TrainingConfigurationDTO.Response toResponse(TrainingConfiguration config) {
        return TrainingConfigurationDTO.Response.builder()
                .id(config.getId())
                .name(config.getName())
                .description(config.getDescription())
                .scope(config.getScope() != null ? config.getScope().name() : null)
                .scopeLabel(getScopeLabel(config.getScope()))
                .datasourceId(config.getDatasourceId())
                .datasourceName(config.getDatasourceName())
                .datasetId(config.getDatasetId())
                .datasetName(config.getDatasetName())
                .projectId(config.getProjectId())
                .targetVariable(config.getTargetVariable())
                .problemType(config.getProblemType() != null ? config.getProblemType().name() : null)
                .algorithm(config.getAlgorithm())
                .algorithmDisplayName(config.getAlgorithmDisplayName())
                .trainTestSplit(config.getTrainTestSplit())
                .crossValidationFolds(config.getCrossValidationFolds())
                .hyperparameters(deserializeHyperparameters(config.getHyperparametersJson()))
                .gpuAcceleration(config.getGpuAcceleration())
                .autoHyperparameterTuning(config.getAutoHyperparameterTuning())
                .earlyStopping(config.getEarlyStopping())
                .earlyStoppingPatience(config.getEarlyStoppingPatience())
                .batchSize(config.getBatchSize())
                .evaluationMetric(config.getEvaluationMetric())
                .tags(parseTags(config.getTags()))
                .usageCount(config.getUsageCount())
                .lastUsedAt(config.getLastUsedAt())
                .createdBy(config.getCreatedBy())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private TrainingConfigurationDTO.ListItem toListItem(TrainingConfiguration config) {
        return TrainingConfigurationDTO.ListItem.builder()
                .id(config.getId())
                .name(config.getName())
                .description(config.getDescription())
                .scope(config.getScope() != null ? config.getScope().name() : null)
                .scopeLabel(getScopeLabel(config.getScope()))
                .scopeEntityName(getScopeEntityName(config))
                .algorithm(config.getAlgorithm())
                .algorithmDisplayName(config.getAlgorithmDisplayName())
                .problemType(config.getProblemType() != null ? config.getProblemType().name() : null)
                .datasetName(config.getDatasetName())
                .targetVariable(config.getTargetVariable())
                .usageCount(config.getUsageCount())
                .lastUsedAt(config.getLastUsedAt())
                .createdAt(config.getCreatedAt())
                .build();
    }
}
