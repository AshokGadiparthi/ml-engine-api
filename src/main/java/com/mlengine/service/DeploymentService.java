package com.mlengine.service;

import com.mlengine.model.dto.DeploymentDTO;
import com.mlengine.model.entity.AutoMLJob;
import com.mlengine.model.entity.Deployment;
import com.mlengine.model.entity.Model;
import com.mlengine.model.entity.Project;
import com.mlengine.model.entity.TrainingJob;
import com.mlengine.model.enums.DeploymentStatus;
import com.mlengine.model.enums.JobStatus;
import com.mlengine.model.enums.ProblemType;
import com.mlengine.repository.AutoMLJobRepository;
import com.mlengine.repository.DeploymentRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.ProjectRepository;
import com.mlengine.repository.TrainingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for Deployment operations.
 * Manages model deployments with versioning, one active per project.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ModelRepository modelRepository;
    private final AutoMLJobRepository autoMLJobRepository;
    private final TrainingJobRepository trainingJobRepository;
    private final ProjectRepository projectRepository;

    /**
     * Deploy a model from AutoML job.
     * This is the primary deployment method.
     */
    @Transactional
    public DeploymentDTO.Response deployFromAutoML(String autoMLJobId, DeploymentDTO.DeployFromAutoMLRequest request) {
        log.info("Deploying model from AutoML job: {}", autoMLJobId);

        // Get AutoML job
        AutoMLJob job = autoMLJobRepository.findById(autoMLJobId)
                .orElseThrow(() -> new IllegalArgumentException("AutoML job not found: " + autoMLJobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Can only deploy from completed AutoML jobs. Current status: " + job.getStatus());
        }

        // Get or create model
        Model model = job.getBestModel();
        if (model == null) {
            model = createModelFromAutoMLJob(job);
            job.setBestModel(model);
            autoMLJobRepository.save(job);
        }

        // Get project
        Project project = job.getProject();
        if (project == null) {
            throw new IllegalStateException("AutoML job has no associated project");
        }

        // Deactivate current active deployment
        deactivateCurrentActive(project.getId(), "Replaced by new deployment");

        // Get next version
        Integer nextVersion = deploymentRepository.findMaxVersionByProjectId(project.getId()) + 1;

        // Create deployment name
        String deploymentName = request.getName();
        if (deploymentName == null || deploymentName.isBlank()) {
            deploymentName = String.format("%s v%d - %s", 
                    job.getBestAlgorithm(), nextVersion, job.getTargetColumn());
        }

        // Create deployment
        Deployment deployment = Deployment.builder()
                .name(deploymentName)
                .description(request.getDescription())
                .project(project)
                .model(model)
                .autoMLJob(job)
                .version(nextVersion)
                .versionLabel("v" + nextVersion)
                .status(DeploymentStatus.ACTIVE)
                .algorithm(job.getBestAlgorithm())
                .score(job.getBestScore())
                .metric(job.getBestMetric())
                .problemType(job.getProblemType())
                .targetColumn(job.getTargetColumn())
                .datasetName(job.getDataset().getName())
                .endpointPath("/api/predictions/realtime/" + model.getId())
                .endpointUrl("/api/predictions/realtime/" + model.getId())
                .deployedAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .deployedBy(request.getDeployedBy())
                .predictionsCount(0L)
                .build();

        deployment = deploymentRepository.save(deployment);

        // Update model deployment status
        model.setIsDeployed(true);
        model.setDeployedAt(LocalDateTime.now());
        modelRepository.save(model);

        log.info("Created deployment {} (v{}) for project {}", 
                deployment.getId(), deployment.getVersion(), project.getId());

        return toResponse(deployment, "Model deployed successfully as v" + nextVersion);
    }

    /**
     * Deploy a model from Training job.
     * Same as AutoML but for manual training jobs.
     */
    @Transactional
    public DeploymentDTO.Response deployFromTraining(String trainingJobId, DeploymentDTO.DeployFromTrainingRequest request) {
        log.info("Deploying model from Training job: {}", trainingJobId);

        // Get Training job
        TrainingJob job = trainingJobRepository.findById(trainingJobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + trainingJobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Can only deploy from completed Training jobs. Current status: " + job.getStatus());
        }

        // Get model from job
        Model model = null;
        if (job.getModelId() != null) {
            model = modelRepository.findById(job.getModelId()).orElse(null);
        }
        
        if (model == null) {
            throw new IllegalStateException("Training job has no associated model. Model may not have been created.");
        }

        // Get project - try multiple sources
        Project project = model.getProject();
        if (project == null) {
            String projectId = job.getProjectIdValue();
            if (projectId == null) {
                projectId = job.getProjectId();
            }
            if (projectId != null) {
                project = projectRepository.findById(projectId).orElse(null);
            }
        }
        
        if (project == null) {
            throw new IllegalStateException("Training job has no associated project");
        }

        // Deactivate current active deployment
        deactivateCurrentActive(project.getId(), "Replaced by new deployment");

        // Get next version
        Integer nextVersion = deploymentRepository.findMaxVersionByProjectId(project.getId()) + 1;

        // Create deployment name
        String deploymentName = request.getName();
        if (deploymentName == null || deploymentName.isBlank()) {
            deploymentName = String.format("%s v%d - %s", 
                    job.getAlgorithm(), nextVersion, job.getTargetVariable());
        }

        // Get score
        Double score = job.getBestAccuracy();
        if (score == null) {
            score = job.getCurrentAccuracy();
        }
        if (score == null) {
            score = model.getAccuracy();
        }

        // Create deployment
        Deployment deployment = Deployment.builder()
                .name(deploymentName)
                .description(request.getDescription())
                .project(project)
                .model(model)
                .trainingJob(job)
                .version(nextVersion)
                .versionLabel("v" + nextVersion)
                .status(DeploymentStatus.ACTIVE)
                .algorithm(job.getAlgorithm())
                .score(score)
                .metric("accuracy")
                .problemType(job.getProblemType())
                .targetColumn(job.getTargetVariable())
                .datasetName(job.getDatasetName())
                .endpointPath("/api/predictions/realtime/" + model.getModelPath())
                .endpointUrl("/api/predictions/realtime/" + model.getModelPath())
                .deployedAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .deployedBy(request.getDeployedBy())
                .predictionsCount(0L)
                .build();

        deployment = deploymentRepository.save(deployment);

        // Update model deployment status
        model.setIsDeployed(true);
        model.setDeployedAt(LocalDateTime.now());
        modelRepository.save(model);

        log.info("Created deployment {} (v{}) from Training job {} for project {}", 
                deployment.getId(), deployment.getVersion(), trainingJobId, project.getId());

        return toResponse(deployment, "Model deployed successfully as v" + nextVersion);
    }

    /**
     * Deploy a model directly (not from AutoML).
     */
    @Transactional
    public DeploymentDTO.Response deployModel(DeploymentDTO.CreateRequest request) {
        log.info("Deploying model: {}", request.getModelId());

        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + request.getModelId()));

        Project project = model.getProject();
        if (project == null && request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));
        }

        if (project == null) {
            throw new IllegalStateException("Model has no associated project");
        }

        // Deactivate current active deployment
        deactivateCurrentActive(project.getId(), "Replaced by new deployment");

        // Get next version
        Integer nextVersion = deploymentRepository.findMaxVersionByProjectId(project.getId()) + 1;

        String deploymentName = request.getName();
        if (deploymentName == null || deploymentName.isBlank()) {
            deploymentName = String.format("%s v%d", model.getAlgorithm(), nextVersion);
        }

        Deployment deployment = Deployment.builder()
                .name(deploymentName)
                .description(request.getDescription())
                .project(project)
                .model(model)
                .version(nextVersion)
                .versionLabel("v" + nextVersion)
                .status(DeploymentStatus.ACTIVE)
                .algorithm(model.getAlgorithm())
                .score(model.getAccuracy() != null ? model.getAccuracy() : model.getR2Score())
                .metric(model.getAccuracy() != null ? "Accuracy" : "R²")
                .problemType(model.getProblemType())
                .targetColumn(model.getTargetVariable())
                .datasetName(model.getDatasetName())
                .endpointPath("/api/predictions/realtime/" + model.getId())
                .endpointUrl("/api/predictions/realtime/" + model.getId())
                .deployedAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .deployedBy(request.getDeployedBy())
                .predictionsCount(0L)
                .build();

        deployment = deploymentRepository.save(deployment);

        model.setIsDeployed(true);
        model.setDeployedAt(LocalDateTime.now());
        modelRepository.save(model);

        return toResponse(deployment, "Model deployed successfully as v" + nextVersion);
    }

    /**
     * Rollback to a previous deployment version.
     */
    @Transactional
    public DeploymentDTO.Response rollback(String deploymentId, DeploymentDTO.RollbackRequest request) {
        log.info("Rolling back to deployment: {}", deploymentId);

        Deployment targetDeployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));

        if (targetDeployment.getStatus() == DeploymentStatus.ACTIVE) {
            throw new IllegalStateException("Deployment is already active");
        }

        String projectId = targetDeployment.getProject().getId();

        // Deactivate current active
        deactivateCurrentActive(projectId, "Rolled back to v" + targetDeployment.getVersion());

        // Activate target deployment
        targetDeployment.setStatus(DeploymentStatus.ACTIVE);
        targetDeployment.setActivatedAt(LocalDateTime.now());
        targetDeployment.setDeactivatedAt(null);
        targetDeployment.setDeactivatedBy(null);
        targetDeployment.setDeactivationReason(null);
        deploymentRepository.save(targetDeployment);

        // Update model status
        Model model = targetDeployment.getModel();
        if (model != null) {
            model.setIsDeployed(true);
            model.setDeployedAt(LocalDateTime.now());
            modelRepository.save(model);
        }

        log.info("Rolled back to deployment v{}", targetDeployment.getVersion());

        return toResponse(targetDeployment, "Rolled back to v" + targetDeployment.getVersion());
    }

    /**
     * Deactivate a deployment.
     */
    @Transactional
    public DeploymentDTO.Response deactivate(String deploymentId, DeploymentDTO.DeactivateRequest request) {
        log.info("Deactivating deployment: {}", deploymentId);

        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));

        if (deployment.getStatus() != DeploymentStatus.ACTIVE) {
            throw new IllegalStateException("Deployment is not active. Current status: " + deployment.getStatus());
        }

        deployment.setStatus(DeploymentStatus.INACTIVE);
        deployment.setDeactivatedAt(LocalDateTime.now());
        deployment.setDeactivatedBy(request != null ? request.getDeactivatedBy() : null);
        deployment.setDeactivationReason(request != null ? request.getReason() : "Manually deactivated");
        deploymentRepository.save(deployment);

        // Update model status
        Model model = deployment.getModel();
        if (model != null) {
            model.setIsDeployed(false);
            modelRepository.save(model);
        }

        return toResponse(deployment, "Deployment deactivated");
    }

    /**
     * Get deployment by ID.
     */
    public DeploymentDTO.Response getDeployment(String deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));
        return toResponse(deployment, null);
    }

    /**
     * Get active deployment for a project.
     */
    public DeploymentDTO.Response getActiveDeployment(String projectId) {
        Optional<Deployment> active = deploymentRepository.findActiveByProjectId(projectId);
        return active.map(d -> toResponse(d, null)).orElse(null);
    }

    /**
     * Get active deployment summary.
     */
    public DeploymentDTO.ActiveSummary getActiveSummary(String projectId) {
        Optional<Deployment> active = deploymentRepository.findActiveByProjectId(projectId);
        
        if (active.isEmpty()) {
            return DeploymentDTO.ActiveSummary.builder()
                    .hasActiveDeployment(false)
                    .build();
        }

        Deployment d = active.get();
        return DeploymentDTO.ActiveSummary.builder()
                .id(d.getId())
                .name(d.getName())
                .version(d.getVersion())
                .versionLabel(d.getVersionLabel())
                .algorithm(d.getAlgorithm())
                .score(d.getScore())
                .scoreFormatted(formatScore(d.getScore(), d.getProblemType()))
                .endpointPath(d.getEndpointPath())
                .deployedAt(d.getDeployedAt())
                .predictionsCount(d.getPredictionsCount())
                .hasActiveDeployment(true)
                .build();
    }

    /**
     * Get deployment history for a project.
     */
    public DeploymentDTO.HistoryResponse getDeploymentHistory(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<Deployment> deployments = deploymentRepository.findDeploymentHistory(projectId);
        List<DeploymentDTO.ListItem> history = deployments.stream()
                .map(this::toListItem)
                .toList();

        DeploymentDTO.ActiveSummary activeSummary = getActiveSummary(projectId);

        return DeploymentDTO.HistoryResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalDeployments(deployments.size())
                .activeDeployment(activeSummary)
                .history(history)
                .build();
    }

    /**
     * List deployments with pagination.
     */
    public DeploymentDTO.PagedResponse listDeployments(String projectId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "version"));

        Page<Deployment> deploymentPage;
        if (projectId != null) {
            deploymentPage = deploymentRepository.findByProjectIdOrderByVersionDesc(projectId, pageRequest);
        } else {
            deploymentPage = deploymentRepository.findAll(pageRequest);
        }

        List<DeploymentDTO.ListItem> items = deploymentPage.getContent().stream()
                .map(this::toListItem)
                .toList();

        return DeploymentDTO.PagedResponse.builder()
                .content(items)
                .totalElements(deploymentPage.getTotalElements())
                .totalPages(deploymentPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * Compare two deployments.
     */
    public DeploymentDTO.CompareResponse compare(String deploymentId1, String deploymentId2) {
        Deployment d1 = deploymentRepository.findById(deploymentId1)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId1));
        Deployment d2 = deploymentRepository.findById(deploymentId2)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId2));

        double scoreDiff = (d1.getScore() != null && d2.getScore() != null) 
                ? (d1.getScore() - d2.getScore()) * 100 
                : 0;

        String recommendation;
        if (Math.abs(scoreDiff) < 0.1) {
            recommendation = "Both versions have similar performance";
        } else if (scoreDiff > 0) {
            recommendation = String.format("v%d has %.1f%% better %s", 
                    d1.getVersion(), Math.abs(scoreDiff), d1.getMetric().toLowerCase());
        } else {
            recommendation = String.format("v%d has %.1f%% better %s", 
                    d2.getVersion(), Math.abs(scoreDiff), d2.getMetric().toLowerCase());
        }

        return DeploymentDTO.CompareResponse.builder()
                .deployment1(toListItem(d1))
                .deployment2(toListItem(d2))
                .scoreDifference(scoreDiff)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Delete a deployment (only if not active).
     */
    @Transactional
    public void deleteDeployment(String deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found: " + deploymentId));

        if (deployment.getStatus() == DeploymentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot delete active deployment. Deactivate first.");
        }

        deploymentRepository.delete(deployment);
        log.info("Deleted deployment: {}", deploymentId);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Deactivate current active deployment for a project.
     */
    private void deactivateCurrentActive(String projectId, String reason) {
        Optional<Deployment> currentActive = deploymentRepository.findActiveByProjectId(projectId);
        
        if (currentActive.isPresent()) {
            Deployment active = currentActive.get();
            active.setStatus(DeploymentStatus.INACTIVE);
            active.setDeactivatedAt(LocalDateTime.now());
            active.setDeactivationReason(reason);
            deploymentRepository.save(active);

            // Update model status
            Model model = active.getModel();
            if (model != null) {
                model.setIsDeployed(false);
                modelRepository.save(model);
            }

            log.info("Deactivated previous active deployment v{}", active.getVersion());
        }
    }

    /**
     * Create Model entity from AutoML job if not exists.
     */
    private Model createModelFromAutoMLJob(AutoMLJob job) {
        // Fetch project properly to avoid potential lazy loading issues
        Project project = null;
        if (job.getProjectIdValue() != null) {
            project = projectRepository.findById(job.getProjectIdValue()).orElse(null);
        }
        
        Model model = Model.builder()
                .name(job.getBestAlgorithm() + " - " + job.getName())
                .algorithm(job.getBestAlgorithm())
                .algorithmDisplayName(job.getBestAlgorithm())
                .problemType(job.getProblemType())
                .project(project)
                .datasetId(job.getDatasetIdValue())    // Use stored value
                .datasetName(job.getDatasetName())     // Use stored value
                .targetVariable(job.getTargetColumn())
                .modelPath(job.getModelPath())  // CRITICAL: FastAPI model ID for predictions!
                .source("AUTOML")               // Model created via AutoML Engine
                .sourceJobId(job.getId())       // Link to AutoML job
                .isDeployed(false)
                .build();

        if (job.getProblemType() == ProblemType.REGRESSION) {
            model.setR2Score(job.getBestScore());
        } else {
            model.setAccuracy(job.getBestScore());
        }

        return modelRepository.save(model);
    }

    /**
     * Format score based on problem type.
     */
    private String formatScore(Double score, ProblemType problemType) {
        if (score == null) return "N/A";
        
        if (problemType == ProblemType.REGRESSION) {
            return String.format("R² = %.4f", score);
        }
        return String.format("%.1f%%", score * 100);
    }

    /**
     * Convert to Response DTO.
     */
    private DeploymentDTO.Response toResponse(Deployment d, String message) {
        return DeploymentDTO.Response.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .projectId(d.getProject() != null ? d.getProject().getId() : null)
                .projectName(d.getProject() != null ? d.getProject().getName() : null)
                .modelId(d.getModel() != null ? d.getModel().getId() : null)
                .autoMLJobId(d.getAutoMLJob() != null ? d.getAutoMLJob().getId() : null)
                .trainingJobId(d.getTrainingJob() != null ? d.getTrainingJob().getId() : null)
                .version(d.getVersion())
                .versionLabel(d.getVersionLabel())
                .status(d.getStatus())
                .statusLabel(d.getStatus().name().toLowerCase())
                .isActive(d.getStatus() == DeploymentStatus.ACTIVE)
                .algorithm(d.getAlgorithm())
                .score(d.getScore())
                .metric(d.getMetric())
                .scoreFormatted(formatScore(d.getScore(), d.getProblemType()))
                .problemType(d.getProblemType())
                .targetColumn(d.getTargetColumn())
                .datasetName(d.getDatasetName())
                .endpointUrl(d.getEndpointUrl())
                .endpointPath(d.getEndpointPath())
                .deployedAt(d.getDeployedAt())
                .activatedAt(d.getActivatedAt())
                .deactivatedAt(d.getDeactivatedAt())
                .createdAt(d.getCreatedAt())
                .predictionsCount(d.getPredictionsCount())
                .lastPredictionAt(d.getLastPredictionAt())
                .deployedBy(d.getDeployedBy())
                .deactivatedBy(d.getDeactivatedBy())
                .deactivationReason(d.getDeactivationReason())
                .message(message)
                .build();
    }

    /**
     * Convert to ListItem DTO.
     */
    private DeploymentDTO.ListItem toListItem(Deployment d) {
        // Determine source
        String source = "UNKNOWN";
        if (d.getAutoMLJob() != null) {
            source = "AUTOML";
        } else if (d.getTrainingJob() != null) {
            source = "TRAINING";
        }
        
        return DeploymentDTO.ListItem.builder()
                .id(d.getId())
                .name(d.getName())
                .projectId(d.getProject() != null ? d.getProject().getId() : null)
                .version(d.getVersion())
                .versionLabel(d.getVersionLabel())
                .status(d.getStatus())
                .statusLabel(d.getStatus().name().toLowerCase())
                .isActive(d.getStatus() == DeploymentStatus.ACTIVE)
                .algorithm(d.getAlgorithm())
                .score(d.getScore())
                .metric(d.getMetric())
                .scoreFormatted(formatScore(d.getScore(), d.getProblemType()))
                .problemType(d.getProblemType())
                .autoMLJobId(d.getAutoMLJob() != null ? d.getAutoMLJob().getId() : null)
                .autoMLJobName(d.getAutoMLJob() != null ? d.getAutoMLJob().getName() : null)
                .trainingJobId(d.getTrainingJob() != null ? d.getTrainingJob().getId() : null)
                .trainingJobName(d.getTrainingJob() != null ? d.getTrainingJob().getJobName() : null)
                .source(source)
                .endpointPath(d.getEndpointPath())
                .deployedAt(d.getDeployedAt())
                .deactivatedAt(d.getDeactivatedAt())
                .predictionsCount(d.getPredictionsCount())
                .deployedBy(d.getDeployedBy())
                .build();
    }
}
