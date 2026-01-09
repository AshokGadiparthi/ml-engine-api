package com.mlengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.model.dto.AutoMLDTO;
import com.mlengine.model.dto.DeploymentDTO;
import com.mlengine.model.entity.AutoMLJob;
import com.mlengine.model.entity.Dataset;
import com.mlengine.model.entity.Deployment;
import com.mlengine.model.entity.Model;
import com.mlengine.model.entity.Project;
import com.mlengine.model.enums.DeploymentStatus;
import com.mlengine.model.enums.JobStatus;
import com.mlengine.model.enums.ProblemType;
import com.mlengine.repository.AutoMLJobRepository;
import com.mlengine.repository.DatasetRepository;
import com.mlengine.repository.DeploymentRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for AutoML operations.
 * Manages automatic model selection and training jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoMLService {

    private final AutoMLJobRepository autoMLJobRepository;
    private final DatasetRepository datasetRepository;
    private final ProjectRepository projectRepository;
    private final ModelRepository modelRepository;
    private final DeploymentRepository deploymentRepository;
    private final ObjectMapper objectMapper;

    // Track running jobs for cancellation
    private final Map<String, CompletableFuture<?>> runningJobs = new ConcurrentHashMap<>();
    
    // Thread pool for async execution
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * Start a new AutoML job.
     */
    @Transactional
    public AutoMLDTO.JobResponse startAutoML(AutoMLDTO.StartRequest request) {
        log.info("Starting AutoML job for dataset: {}", request.getDatasetId());

        // Validate dataset exists
        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + request.getDatasetId()));

        // Get project if specified
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));
        }

        // Create job name if not provided
        String jobName = request.getName();
        if (jobName == null || jobName.isBlank()) {
            jobName = "AutoML - " + dataset.getName() + " - " + LocalDateTime.now().toString().substring(0, 16);
        }

        // Create AutoML job entity
        AutoMLJob job = AutoMLJob.builder()
                .name(jobName)
                .description(request.getDescription())
                .project(project)
                .dataset(dataset)
                .targetColumn(request.getTargetColumn())
                .problemType(request.getProblemType())
                .status(JobStatus.QUEUED)
                .maxTrainingTimeMinutes(request.getMaxTrainingTimeMinutes())
                .accuracyVsSpeed(request.getAccuracyVsSpeed())
                .interpretability(request.getInterpretability())
                .progress(0)
                .algorithmsCompleted(0)
                .algorithmsTotal(getAlgorithmCount(request.getProblemType(), request.getAccuracyVsSpeed()))
                .build();

        // Apply config if provided
        if (request.getConfig() != null) {
            AutoMLDTO.AutoMLConfig config = request.getConfig();
            job.setEnableFeatureEngineering(config.getEnableFeatureEngineering());
            job.setScalingMethod(config.getScalingMethod());
            job.setPolynomialDegree(config.getPolynomialDegree());
            job.setSelectFeatures(config.getSelectFeatures());
            job.setCvFolds(config.getCvFolds());
            job.setEnableExplainability(config.getEnableExplainability());
            job.setEnableHyperparameterTuning(config.getEnableHyperparameterTuning());
            job.setTuningMethod(config.getTuningMethod());
        }

        // Initialize phases
        job.setCurrentPhase("QUEUED");
        job.setLogsJson(serializeJson(List.of(
                createLogEntry("INFO", "AutoML job created and queued")
        )));

        job = autoMLJobRepository.save(job);

        // Start async processing AFTER transaction commits to avoid race condition
        final String jobId = job.getId();
        final AutoMLDTO.JobResponse response = toJobResponse(job, "AutoML job started successfully");
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    try {
                        executeAutoML(jobId);
                    } catch (Exception e) {
                        log.error("AutoML execution failed for job: {}", jobId, e);
                    }
                }, executorService);
                runningJobs.put(jobId, future);
            }
        });

        return response;
    }

    /**
     * Execute AutoML job - runs in separate thread.
     * This method handles its own transactions.
     */
    private void executeAutoML(String jobId) {
        try {
            log.info("Executing AutoML job: {}", jobId);
            
            // Fetch fresh copy of job
            AutoMLJob job = autoMLJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found"));

            // Update status to STARTING
            job.setStatus(JobStatus.STARTING);
            job.setStartedAt(LocalDateTime.now());
            job.setCurrentPhase("DATA_VALIDATION");
            job.setProgress(5);
            addLog(job, "INFO", "AutoML job started");
            autoMLJobRepository.save(job);

            // Phase 1: Data Validation
            Thread.sleep(2000); // Simulate work
            
            // Check if stopped
            if (!runningJobs.containsKey(jobId)) {
                markJobStopped(jobId);
                return;
            }
            
            job = autoMLJobRepository.findById(jobId).orElseThrow();
            job.setProgress(15);
            job.setCurrentPhase("FEATURE_ENGINEERING");
            addLog(job, "INFO", "Data validation completed");
            autoMLJobRepository.save(job);

            // Phase 2: Feature Engineering
            if (Boolean.TRUE.equals(job.getEnableFeatureEngineering())) {
                Thread.sleep(3000);
                
                // Check if stopped
                if (!runningJobs.containsKey(jobId)) {
                    markJobStopped(jobId);
                    return;
                }
                
                job = autoMLJobRepository.findById(jobId).orElseThrow();
                addLog(job, "INFO", "Feature engineering: generated additional features");
            }
            
            job = autoMLJobRepository.findById(jobId).orElseThrow();
            job.setProgress(25);
            job.setCurrentPhase("ALGORITHM_SELECTION");
            job.setStatus(JobStatus.TRAINING);
            addLog(job, "INFO", "Starting algorithm comparison");
            autoMLJobRepository.save(job);

            // Phase 3: Algorithm Selection - Test multiple algorithms
            List<AutoMLDTO.LeaderboardEntry> leaderboard = new ArrayList<>();
            List<String> algorithms = getAlgorithmsForProblemType(job.getProblemType());
            int totalAlgorithms = algorithms.size();
            
            job.setAlgorithmsTotal(totalAlgorithms);
            autoMLJobRepository.save(job);

            double bestScore = 0;
            String bestAlgorithm = null;

            for (int i = 0; i < algorithms.size(); i++) {
                // Check if job was stopped
                if (!runningJobs.containsKey(jobId)) {
                    markJobStopped(jobId);
                    return;
                }

                String algorithm = algorithms.get(i);
                
                // Refresh job from database
                job = autoMLJobRepository.findById(jobId).orElseThrow();
                job.setCurrentAlgorithm(algorithm);
                addLog(job, "INFO", "Testing " + algorithm + "...");
                autoMLJobRepository.save(job);

                // Simulate training
                Thread.sleep(2000 + (long) (Math.random() * 3000));

                // Check again after training
                if (!runningJobs.containsKey(jobId)) {
                    markJobStopped(jobId);
                    return;
                }

                // Generate mock results
                double score = generateMockScore(job.getProblemType(), algorithm);
                AutoMLDTO.LeaderboardEntry entry = createLeaderboardEntry(
                        i + 1, algorithm, score, job.getProblemType()
                );
                leaderboard.add(entry);

                // Refresh and update
                job = autoMLJobRepository.findById(jobId).orElseThrow();
                
                if (score > bestScore) {
                    bestScore = score;
                    bestAlgorithm = algorithm;
                    job.setCurrentBestScore(bestScore);
                    job.setCurrentBestAlgorithm(bestAlgorithm);
                    addLog(job, "INFO", algorithm + ": " + formatScore(score, job.getProblemType()) + " - New best!");
                } else {
                    addLog(job, "INFO", algorithm + ": " + formatScore(score, job.getProblemType()));
                }

                job.setAlgorithmsCompleted(i + 1);
                job.setProgress(25 + (int) ((i + 1.0) / totalAlgorithms * 60));
                autoMLJobRepository.save(job);
            }

            // Sort leaderboard by score
            final ProblemType pt = job.getProblemType();
            leaderboard.sort((a, b) -> {
                double scoreA = pt == ProblemType.REGRESSION ? (a.getR2() != null ? a.getR2() : 0) : (a.getAccuracy() != null ? a.getAccuracy() : 0);
                double scoreB = pt == ProblemType.REGRESSION ? (b.getR2() != null ? b.getR2() : 0) : (b.getAccuracy() != null ? b.getAccuracy() : 0);
                return Double.compare(scoreB, scoreA);
            });

            // Update ranks
            for (int i = 0; i < leaderboard.size(); i++) {
                leaderboard.get(i).setRank(i + 1);
            }

            // Phase 4: Model Training (best model)
            job = autoMLJobRepository.findById(jobId).orElseThrow();
            job.setCurrentPhase("MODEL_TRAINING");
            job.setProgress(88);
            addLog(job, "INFO", "Training final model with " + bestAlgorithm);
            autoMLJobRepository.save(job);
            Thread.sleep(2000);

            // Phase 5: Evaluation
            if (!runningJobs.containsKey(jobId)) {
                markJobStopped(jobId);
                return;
            }
            
            job = autoMLJobRepository.findById(jobId).orElseThrow();
            job.setCurrentPhase("EVALUATION");
            job.setProgress(95);
            addLog(job, "INFO", "Running final evaluation on best model");
            autoMLJobRepository.save(job);
            Thread.sleep(2000);

            // Phase 6: Complete
            job = autoMLJobRepository.findById(jobId).orElseThrow();
            job.setStatus(JobStatus.COMPLETED);
            job.setCurrentPhase("COMPLETED");
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            job.setBestAlgorithm(bestAlgorithm);
            job.setBestScore(bestScore);
            job.setBestMetric(job.getProblemType() == ProblemType.REGRESSION ? "R²" : "Accuracy");
            job.setLeaderboardJson(serializeJson(leaderboard));
            job.setFeatureImportanceJson(serializeJson(generateMockFeatureImportance()));

            long elapsed = java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).getSeconds();
            job.setElapsedTimeSeconds(elapsed);

            addLog(job, "INFO", "AutoML completed! Best model: " + bestAlgorithm + " with " + formatScore(bestScore, job.getProblemType()));
            autoMLJobRepository.save(job);

            runningJobs.remove(jobId);
            log.info("AutoML job completed successfully: {}", jobId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AutoML job interrupted: {}", jobId);
            markJobStopped(jobId);
        } catch (Exception e) {
            log.error("AutoML job failed: {}", jobId, e);
            try {
                AutoMLJob job = autoMLJobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    job.setStatus(JobStatus.FAILED);
                    job.setErrorMessage(e.getMessage());
                    addLog(job, "ERROR", "Job failed: " + e.getMessage());
                    autoMLJobRepository.save(job);
                }
            } catch (Exception ex) {
                log.error("Failed to update job status", ex);
            }
            runningJobs.remove(jobId);
        }
    }
    
    /**
     * Mark job as stopped.
     */
    private void markJobStopped(String jobId) {
        try {
            AutoMLJob job = autoMLJobRepository.findById(jobId).orElse(null);
            if (job != null && job.getStatus() != JobStatus.STOPPED) {
                job.setStatus(JobStatus.STOPPED);
                job.setCompletedAt(LocalDateTime.now());
                if (job.getStartedAt() != null) {
                    job.setElapsedTimeSeconds(
                        java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).getSeconds()
                    );
                }
                addLog(job, "WARN", "Job stopped");
                autoMLJobRepository.save(job);
            }
        } catch (Exception e) {
            log.error("Failed to mark job as stopped: {}", jobId, e);
        }
    }

    /**
     * Get job progress/status.
     */
    public AutoMLDTO.ProgressResponse getJobProgress(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        List<AutoMLDTO.PhaseInfo> phases = buildPhaseInfo(job);
        List<AutoMLDTO.LogEntry> logs = deserializeLogs(job.getLogsJson());

        // Calculate elapsed time
        Long elapsed = job.getElapsedTimeSeconds();
        if (elapsed == null && job.getStartedAt() != null) {
            elapsed = java.time.Duration.between(job.getStartedAt(), LocalDateTime.now()).getSeconds();
        }

        // Estimate remaining time
        Long remaining = null;
        if (job.getProgress() != null && job.getProgress() > 0 && elapsed != null) {
            remaining = (long) ((elapsed / (job.getProgress() / 100.0)) * ((100 - job.getProgress()) / 100.0));
        }

        return AutoMLDTO.ProgressResponse.builder()
                .jobId(job.getId())
                .name(job.getName())
                .status(job.getStatus())
                .statusLabel(job.getStatus().name().toLowerCase())
                .progress(job.getProgress())
                .currentPhase(job.getCurrentPhase())
                .currentAlgorithm(job.getCurrentAlgorithm())
                .phases(phases)
                .algorithmsCompleted(job.getAlgorithmsCompleted())
                .algorithmsTotal(job.getAlgorithmsTotal())
                .currentBestScore(job.getCurrentBestScore())
                .currentBestAlgorithm(job.getCurrentBestAlgorithm())
                .elapsedTimeSeconds(elapsed)
                .estimatedRemainingSeconds(remaining)
                .logs(logs)
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .errorMessage(job.getErrorMessage())
                .build();
    }

    /**
     * Get job results (after completion).
     */
    public AutoMLDTO.ResultsResponse getJobResults(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Job is not completed yet. Current status: " + job.getStatus());
        }

        List<AutoMLDTO.LeaderboardEntry> leaderboard = deserializeLeaderboard(job.getLeaderboardJson());
        List<AutoMLDTO.FeatureImportanceEntry> featureImportance = deserializeFeatureImportance(job.getFeatureImportanceJson());

        Dataset dataset = job.getDataset();
        Model bestModel = job.getBestModel();
        
        // Get deployment info from Deployment entity
        List<Deployment> deployments = deploymentRepository.findByAutoMLJobId(jobId);
        Optional<Deployment> activeDeployment = deployments.stream()
                .filter(d -> d.getStatus() == DeploymentStatus.ACTIVE)
                .findFirst();
        Optional<Deployment> latestDeployment = deployments.stream()
                .max((d1, d2) -> d1.getVersion().compareTo(d2.getVersion()));
        
        boolean isDeployed = !deployments.isEmpty();
        boolean isActiveDeployment = activeDeployment.isPresent();
        Deployment deployment = activeDeployment.orElse(latestDeployment.orElse(null));

        return AutoMLDTO.ResultsResponse.builder()
                .jobId(job.getId())
                .name(job.getName())
                .status(job.getStatus())
                .problemType(job.getProblemType())
                .targetColumn(job.getTargetColumn())
                .datasetInfo(AutoMLDTO.DatasetInfo.builder()
                        .datasetId(dataset.getId())
                        .datasetName(dataset.getName())
                        .totalRows(dataset.getRowCount())
                        .totalFeatures(dataset.getColumnCount())
                        .trainSize((long) (dataset.getRowCount() * 0.8))
                        .testSize((long) (dataset.getRowCount() * 0.2))
                        .build())
                .featureEngineering(AutoMLDTO.FeatureEngineeringInfo.builder()
                        .enabled(job.getEnableFeatureEngineering())
                        .scalingMethod(job.getScalingMethod())
                        .originalFeatures(dataset.getColumnCount())
                        .engineeredFeatures(dataset.getColumnCount() + (job.getEnableFeatureEngineering() ? 5 : 0))
                        .build())
                .leaderboard(leaderboard)
                .bestModel(AutoMLDTO.BestModelInfo.builder()
                        .modelId(bestModel != null ? bestModel.getId() : null)
                        .algorithm(job.getBestAlgorithm())
                        .score(job.getBestScore())
                        .metric(job.getBestMetric())
                        .modelPath(job.getModelPath())
                        .featureEngineerPath(job.getFeatureEngineerPath())
                        .build())
                .featureImportance(featureImportance)
                .comparisonCsvPath(job.getComparisonCsvPath())
                .totalTrainingTimeSeconds(job.getElapsedTimeSeconds())
                .completedAt(job.getCompletedAt())
                // Deployment info from Deployment entity
                .isDeployed(isDeployed)
                .deploymentId(deployment != null ? deployment.getId() : null)
                .deployedModelId(deployment != null && deployment.getModel() != null ? deployment.getModel().getId() : null)
                .deploymentEndpoint(deployment != null ? deployment.getEndpointPath() : null)
                .deployedAt(deployment != null ? deployment.getDeployedAt() : null)
                .deploymentVersion(deployment != null ? deployment.getVersion() : null)
                .deploymentVersionLabel(deployment != null ? deployment.getVersionLabel() : null)
                .isActiveDeployment(isActiveDeployment)
                .build();
    }

    /**
     * List AutoML jobs with pagination.
     */
    public AutoMLDTO.PagedResponse listJobs(String projectId, String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AutoMLJob> jobPage;
        if (projectId != null && status != null) {
            JobStatus jobStatus = JobStatus.valueOf(status.toUpperCase());
            jobPage = autoMLJobRepository.findByProjectIdAndStatus(projectId, jobStatus, pageRequest);
        } else if (projectId != null) {
            jobPage = autoMLJobRepository.findByProjectId(projectId, pageRequest);
        } else {
            jobPage = autoMLJobRepository.findAll(pageRequest);
        }

        List<AutoMLDTO.ListItem> items = jobPage.getContent().stream()
                .map(this::toListItem)
                .toList();

        return AutoMLDTO.PagedResponse.builder()
                .content(items)
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * Stop a running job.
     */
    @Transactional
    public AutoMLDTO.StopResponse stopJob(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
            throw new IllegalStateException("Cannot stop a job that is already " + job.getStatus());
        }

        // Remove from running jobs (will trigger stop in async method)
        CompletableFuture<?> future = runningJobs.remove(jobId);
        if (future != null) {
            future.cancel(true);
        }

        job.setStatus(JobStatus.STOPPED);
        job.setCompletedAt(LocalDateTime.now());
        if (job.getStartedAt() != null) {
            job.setElapsedTimeSeconds(java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).getSeconds());
        }
        addLog(job, "WARN", "Job stopped by user");
        autoMLJobRepository.save(job);

        return AutoMLDTO.StopResponse.builder()
                .jobId(jobId)
                .status(JobStatus.STOPPED)
                .message("AutoML job stopped successfully")
                .algorithmsCompleted(job.getAlgorithmsCompleted())
                .bestScoreAchieved(job.getCurrentBestScore())
                .stoppedAt(job.getCompletedAt())
                .build();
    }

    /**
     * Delete a job.
     */
    @Transactional
    public void deleteJob(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Stop if running
        CompletableFuture<?> future = runningJobs.remove(jobId);
        if (future != null) {
            future.cancel(true);
        }

        autoMLJobRepository.delete(job);
        log.info("Deleted AutoML job: {}", jobId);
    }

    /**
     * Deploy best model from job.
     * Creates a versioned deployment with proper tracking.
     */
    @Transactional
    public AutoMLDTO.DeployResponse deployBestModel(String jobId, AutoMLDTO.DeployRequest request) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Can only deploy models from completed jobs");
        }

        Project project = job.getProject();
        if (project == null) {
            throw new IllegalStateException("AutoML job has no associated project");
        }

        // Create or get model entity
        Model model = job.getBestModel();
        if (model == null) {
            model = Model.builder()
                    .name(request.getDeploymentName() != null ? request.getDeploymentName() : job.getBestAlgorithm() + " - " + job.getName())
                    .algorithm(job.getBestAlgorithm())
                    .algorithmDisplayName(job.getBestAlgorithm())
                    .problemType(job.getProblemType())
                    .project(job.getProject())
                    .datasetId(job.getDataset().getId())
                    .datasetName(job.getDataset().getName())
                    .targetVariable(job.getTargetColumn())
                    .isDeployed(false)
                    .build();
            
            if (job.getProblemType() == ProblemType.REGRESSION) {
                model.setR2Score(job.getBestScore());
            } else {
                model.setAccuracy(job.getBestScore());
            }
            
            model = modelRepository.save(model);
            job.setBestModel(model);
            autoMLJobRepository.save(job);
        }

        // Deactivate current active deployment for this project
        Optional<Deployment> currentActive = deploymentRepository.findActiveByProjectId(project.getId());
        if (currentActive.isPresent()) {
            Deployment active = currentActive.get();
            active.setStatus(DeploymentStatus.INACTIVE);
            active.setDeactivatedAt(LocalDateTime.now());
            active.setDeactivationReason("Replaced by new deployment from AutoML job");
            deploymentRepository.save(active);
            
            // Update old model status
            Model oldModel = active.getModel();
            if (oldModel != null) {
                oldModel.setIsDeployed(false);
                modelRepository.save(oldModel);
            }
            log.info("Deactivated previous deployment v{}", active.getVersion());
        }

        // Get next version number
        Integer nextVersion = deploymentRepository.findMaxVersionByProjectId(project.getId()) + 1;

        // Create deployment name
        String deploymentName = request.getDeploymentName();
        if (deploymentName == null || deploymentName.isBlank()) {
            deploymentName = String.format("%s v%d - %s", job.getBestAlgorithm(), nextVersion, job.getTargetColumn());
        }

        // Create new deployment
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
                .predictionsCount(0L)
                .build();

        deployment = deploymentRepository.save(deployment);

        // Update model deployment status
        model.setIsDeployed(true);
        model.setDeployedAt(LocalDateTime.now());
        modelRepository.save(model);

        log.info("Created deployment v{} for project {} from AutoML job {}", 
                nextVersion, project.getId(), jobId);

        // Format score for display
        String scoreFormatted = job.getProblemType() == ProblemType.REGRESSION 
                ? String.format("R² = %.4f", job.getBestScore())
                : String.format("%.1f%%", job.getBestScore() * 100);

        return AutoMLDTO.DeployResponse.builder()
                .deploymentId(deployment.getId())
                .modelId(model.getId())
                .name(deployment.getName())
                .status("ACTIVE")
                .endpoint(deployment.getEndpointPath())
                .deployedAt(deployment.getDeployedAt())
                .version(deployment.getVersion())
                .versionLabel(deployment.getVersionLabel())
                .algorithm(deployment.getAlgorithm())
                .score(deployment.getScore())
                .scoreFormatted(scoreFormatted)
                .message("Model deployed successfully as " + deployment.getVersionLabel())
                .build();
    }

    /**
     * Get leaderboard for a job.
     */
    public List<AutoMLDTO.LeaderboardEntry> getLeaderboard(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return deserializeLeaderboard(job.getLeaderboardJson());
    }

    /**
     * Get feature importance for a job.
     */
    public List<AutoMLDTO.FeatureImportanceEntry> getFeatureImportance(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return deserializeFeatureImportance(job.getFeatureImportanceJson());
    }

    /**
     * Get logs for a job.
     */
    public List<AutoMLDTO.LogEntry> getJobLogs(String jobId, int limit) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        List<AutoMLDTO.LogEntry> logs = deserializeLogs(job.getLogsJson());
        if (logs.size() > limit) {
            return logs.subList(logs.size() - limit, logs.size());
        }
        return logs;
    }

    /**
     * List available algorithms.
     */
    public List<Map<String, Object>> listAlgorithms(String problemType) {
        List<Map<String, Object>> algorithms = new ArrayList<>();

        if (problemType == null || problemType.equalsIgnoreCase("classification")) {
            algorithms.add(createAlgorithmInfo("XGBoost", "CLASSIFICATION", "Gradient boosting for high accuracy"));
            algorithms.add(createAlgorithmInfo("Random Forest", "CLASSIFICATION", "Ensemble of decision trees"));
            algorithms.add(createAlgorithmInfo("Gradient Boosting", "CLASSIFICATION", "Sequential boosting"));
            algorithms.add(createAlgorithmInfo("Logistic Regression", "CLASSIFICATION", "Fast and interpretable"));
            algorithms.add(createAlgorithmInfo("SVM", "CLASSIFICATION", "Support Vector Machine"));
        }

        if (problemType == null || problemType.equalsIgnoreCase("regression")) {
            algorithms.add(createAlgorithmInfo("XGBoost", "REGRESSION", "Gradient boosting for regression"));
            algorithms.add(createAlgorithmInfo("Random Forest", "REGRESSION", "Ensemble regression"));
            algorithms.add(createAlgorithmInfo("Gradient Boosting", "REGRESSION", "Sequential boosting"));
            algorithms.add(createAlgorithmInfo("Linear Regression", "REGRESSION", "Fast and interpretable"));
            algorithms.add(createAlgorithmInfo("Ridge", "REGRESSION", "L2 regularized regression"));
            algorithms.add(createAlgorithmInfo("Lasso", "REGRESSION", "L1 regularized regression"));
            algorithms.add(createAlgorithmInfo("SVR", "REGRESSION", "Support Vector Regression"));
        }

        return algorithms;
    }

    // ==================== HELPER METHODS ====================

    private int getAlgorithmCount(ProblemType problemType, String accuracyVsSpeed) {
        int base = problemType == ProblemType.REGRESSION ? 7 : 5;
        return switch (accuracyVsSpeed) {
            case "low" -> Math.max(2, base - 2);
            case "high" -> base;
            default -> Math.max(3, base - 1);
        };
    }

    private List<String> getAlgorithmsForProblemType(ProblemType problemType) {
        if (problemType == ProblemType.REGRESSION) {
            return List.of("XGBoost", "Random Forest", "Gradient Boosting", "Linear Regression", "Ridge", "Lasso", "SVR");
        }
        return List.of("XGBoost", "Random Forest", "Gradient Boosting", "Logistic Regression", "SVM");
    }

    private double generateMockScore(ProblemType problemType, String algorithm) {
        double base = switch (algorithm) {
            case "XGBoost" -> 0.92;
            case "Random Forest" -> 0.89;
            case "Gradient Boosting" -> 0.90;
            case "Logistic Regression", "Linear Regression" -> 0.82;
            case "SVM", "SVR" -> 0.85;
            case "Ridge" -> 0.83;
            case "Lasso" -> 0.81;
            default -> 0.80;
        };
        return base + (Math.random() * 0.06 - 0.03); // Add some randomness
    }

    private String formatScore(double score, ProblemType problemType) {
        if (problemType == ProblemType.REGRESSION) {
            return String.format("R² = %.4f", score);
        }
        return String.format("%.1f%% accuracy", score * 100);
    }

    private AutoMLDTO.LeaderboardEntry createLeaderboardEntry(int rank, String algorithm, double score, ProblemType problemType) {
        AutoMLDTO.LeaderboardEntry.LeaderboardEntryBuilder builder = AutoMLDTO.LeaderboardEntry.builder()
                .rank(rank)
                .algorithm(algorithm)
                .cvScore(score)
                .cvStd(Math.random() * 0.02)
                .trainingTimeSeconds((long) (5 + Math.random() * 115));

        if (problemType == ProblemType.REGRESSION) {
            builder.r2(score)
                    .mae(1.5 - score + Math.random() * 0.3)
                    .rmse(2.0 - score + Math.random() * 0.4);
        } else {
            builder.accuracy(score)
                    .precision(score - 0.01 + Math.random() * 0.02)
                    .recall(score - 0.02 + Math.random() * 0.03)
                    .f1Score(score - 0.015 + Math.random() * 0.02)
                    .auc(score + 0.02 + Math.random() * 0.02);
        }

        return builder.build();
    }

    private List<AutoMLDTO.FeatureImportanceEntry> generateMockFeatureImportance() {
        String[] features = {"tenure", "monthly_charges", "total_charges", "contract_type", "payment_method",
                "online_security", "tech_support", "internet_service", "senior_citizen", "partner"};
        List<AutoMLDTO.FeatureImportanceEntry> entries = new ArrayList<>();

        double remaining = 1.0;
        for (int i = 0; i < features.length; i++) {
            double importance = i < features.length - 1 ? remaining * (0.15 + Math.random() * 0.25) : remaining;
            entries.add(AutoMLDTO.FeatureImportanceEntry.builder()
                    .feature(features[i])
                    .importance(Math.round(importance * 1000) / 1000.0)
                    .rank(i + 1)
                    .build());
            remaining -= importance;
        }

        entries.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    private List<AutoMLDTO.PhaseInfo> buildPhaseInfo(AutoMLJob job) {
        List<AutoMLDTO.PhaseInfo> phases = new ArrayList<>();
        String currentPhase = job.getCurrentPhase();

        phases.add(buildPhase("DATA_VALIDATION", "Data Validation", currentPhase, 1));
        phases.add(buildPhase("FEATURE_ENGINEERING", "Feature Engineering", currentPhase, 2));
        phases.add(buildPhase("ALGORITHM_SELECTION", "Algorithm Selection", currentPhase, 3));
        phases.add(buildPhase("MODEL_TRAINING", "Model Training", currentPhase, 4));
        phases.add(buildPhase("EVALUATION", "Evaluation", currentPhase, 5));

        return phases;
    }

    private AutoMLDTO.PhaseInfo buildPhase(String name, String label, String currentPhase, int order) {
        List<String> phaseOrder = List.of("QUEUED", "DATA_VALIDATION", "FEATURE_ENGINEERING", 
                "ALGORITHM_SELECTION", "MODEL_TRAINING", "EVALUATION", "COMPLETED");

        int currentIdx = phaseOrder.indexOf(currentPhase);
        int thisIdx = phaseOrder.indexOf(name);

        String status;
        int progress;
        if (currentPhase.equals("COMPLETED")) {
            status = "COMPLETED";
            progress = 100;
        } else if (thisIdx < currentIdx) {
            status = "COMPLETED";
            progress = 100;
        } else if (thisIdx == currentIdx) {
            status = "RUNNING";
            progress = 50;
        } else {
            status = "PENDING";
            progress = 0;
        }

        return AutoMLDTO.PhaseInfo.builder()
                .name(name)
                .label(label)
                .status(status)
                .progress(progress)
                .build();
    }

    private AutoMLDTO.LogEntry createLogEntry(String level, String message) {
        return AutoMLDTO.LogEntry.builder()
                .timestamp(LocalDateTime.now())
                .level(level)
                .message(message)
                .build();
    }

    private void addLog(AutoMLJob job, String level, String message) {
        List<AutoMLDTO.LogEntry> logs = deserializeLogs(job.getLogsJson());
        logs.add(createLogEntry(level, message));
        job.setLogsJson(serializeJson(logs));
    }

    private Map<String, Object> createAlgorithmInfo(String name, String type, String description) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("type", type);
        info.put("description", description);
        return info;
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize JSON", e);
            return "[]";
        }
    }

    private List<AutoMLDTO.LogEntry> deserializeLogs(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AutoMLDTO.LogEntry>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize logs", e);
            return new ArrayList<>();
        }
    }

    private List<AutoMLDTO.LeaderboardEntry> deserializeLeaderboard(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AutoMLDTO.LeaderboardEntry>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize leaderboard", e);
            return new ArrayList<>();
        }
    }

    private List<AutoMLDTO.FeatureImportanceEntry> deserializeFeatureImportance(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AutoMLDTO.FeatureImportanceEntry>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize feature importance", e);
            return new ArrayList<>();
        }
    }

    private AutoMLDTO.JobResponse toJobResponse(AutoMLJob job, String message) {
        AutoMLDTO.AutoMLConfig config = AutoMLDTO.AutoMLConfig.builder()
                .enableFeatureEngineering(job.getEnableFeatureEngineering())
                .scalingMethod(job.getScalingMethod())
                .polynomialDegree(job.getPolynomialDegree())
                .selectFeatures(job.getSelectFeatures())
                .cvFolds(job.getCvFolds())
                .enableExplainability(job.getEnableExplainability())
                .enableHyperparameterTuning(job.getEnableHyperparameterTuning())
                .tuningMethod(job.getTuningMethod())
                .build();

        return AutoMLDTO.JobResponse.builder()
                .jobId(job.getId())
                .projectId(job.getProject() != null ? job.getProject().getId() : null)
                .datasetId(job.getDataset().getId())
                .datasetName(job.getDataset().getName())
                .name(job.getName())
                .description(job.getDescription())
                .targetColumn(job.getTargetColumn())
                .problemType(job.getProblemType())
                .status(job.getStatus())
                .statusLabel(job.getStatus().name().toLowerCase())
                .maxTrainingTimeMinutes(job.getMaxTrainingTimeMinutes())
                .accuracyVsSpeed(job.getAccuracyVsSpeed())
                .interpretability(job.getInterpretability())
                .config(config)
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .message(message)
                .build();
    }

    private AutoMLDTO.ListItem toListItem(AutoMLJob job) {
        // Get deployment info from Deployment entity
        List<Deployment> deployments = deploymentRepository.findByAutoMLJobId(job.getId());
        
        // Find if there's an active deployment for this job
        Optional<Deployment> activeDeployment = deployments.stream()
                .filter(d -> d.getStatus() == DeploymentStatus.ACTIVE)
                .findFirst();
        
        // Or get the most recent deployment for this job
        Optional<Deployment> latestDeployment = deployments.stream()
                .max((d1, d2) -> d1.getVersion().compareTo(d2.getVersion()));
        
        boolean isDeployed = !deployments.isEmpty();
        boolean isActiveDeployment = activeDeployment.isPresent();
        
        Deployment deployment = activeDeployment.orElse(latestDeployment.orElse(null));
        
        return AutoMLDTO.ListItem.builder()
                .jobId(job.getId())
                .name(job.getName())
                .projectId(job.getProject() != null ? job.getProject().getId() : null)
                .datasetId(job.getDataset().getId())
                .datasetName(job.getDataset().getName())
                .problemType(job.getProblemType())
                .status(job.getStatus())
                .statusLabel(job.getStatus().name().toLowerCase())
                .bestAlgorithm(job.getBestAlgorithm())
                .bestScore(job.getBestScore())
                .algorithmsCount(job.getAlgorithmsTotal())
                .elapsedTimeSeconds(job.getElapsedTimeSeconds())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                // Deployment info
                .isDeployed(isDeployed)
                .deploymentId(deployment != null ? deployment.getId() : null)
                .deployedModelId(deployment != null && deployment.getModel() != null ? deployment.getModel().getId() : null)
                .deploymentEndpoint(deployment != null ? deployment.getEndpointPath() : null)
                .deployedAt(deployment != null ? deployment.getDeployedAt() : null)
                .deploymentVersion(deployment != null ? deployment.getVersion() : null)
                .deploymentVersionLabel(deployment != null ? deployment.getVersionLabel() : null)
                .isActiveDeployment(isActiveDeployment)
                .build();
    }
}
