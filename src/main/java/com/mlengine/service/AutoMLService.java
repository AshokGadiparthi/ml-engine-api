package com.mlengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.client.MLEngineClient;
import com.mlengine.model.dto.AutoMLDTO;
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
 * Integrates with Python FastAPI ML Engine for REAL ML execution.
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
    private final MLEngineClient mlEngineClient;
    private final ActivityService activityService;

    // Track running jobs for cancellation
    private final Map<String, CompletableFuture<?>> runningJobs = new ConcurrentHashMap<>();
    
    // Map Spring Boot job ID to FastAPI job ID
    private final Map<String, String> jobIdMapping = new ConcurrentHashMap<>();
    
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
                .projectIdValue(project != null ? project.getId() : null)  // Store for async access
                .datasetIdValue(dataset.getId())    // Store for async access
                .datasetName(dataset.getName())     // Store for async access
                .dataset(dataset)
                .targetColumn(request.getTargetColumn())
                .problemType(request.getProblemType())
                .status(JobStatus.QUEUED)
                .maxTrainingTimeMinutes(request.getMaxTrainingTimeMinutes())
                .accuracyVsSpeed(request.getAccuracyVsSpeed())
                .interpretability(request.getInterpretability())
                .progress(0)
                .algorithmsCompleted(0)
                .algorithmsTotal(5)
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
        
        // Record training started activity
        try {
            activityService.recordTrainingStarted(
                    job.getId(),
                    job.getName() != null ? job.getName() : "AutoML Run",
                    "AutoML (" + job.getProblemType() + ")",
                    "System",
                    project != null ? project.getId() : null
            );
        } catch (Exception e) {
            log.warn("Failed to record activity: {}", e.getMessage());
        }

        // Start async processing AFTER transaction commits
        final String jobId = job.getId();
        final String datasetPath = dataset.getFilePath();
        final AutoMLDTO.JobResponse response = toJobResponse(job, "AutoML job started successfully");
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    try {
                        executeAutoMLWithFastAPI(jobId, datasetPath);
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
     * Execute AutoML using the FastAPI ML Engine - REAL ML!
     */
    private void executeAutoMLWithFastAPI(String jobId, String datasetPath) {
        try {
            log.info("🚀 Executing REAL AutoML via FastAPI for job: {}", jobId);
            
            // Get job from database
            AutoMLJob job = autoMLJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found"));

            // Update status
            job.setStatus(JobStatus.STARTING);
            job.setStartedAt(LocalDateTime.now());
            job.setCurrentPhase("DATA_VALIDATION");
            job.setProgress(5);
            addLog(job, "INFO", "🚀 Starting REAL ML training via FastAPI");
            autoMLJobRepository.save(job);

            // Call FastAPI to start AutoML
            Map<String, Object> fastApiResponse = mlEngineClient.startAutoML(
                    datasetPath,
                    job.getTargetColumn(),
                    job.getProblemType().name(),
                    job.getCvFolds() != null ? job.getCvFolds() : 5,
                    Boolean.TRUE.equals(job.getEnableFeatureEngineering()),
                    job.getScalingMethod() != null ? job.getScalingMethod() : "standard"
            );

            String fastApiJobId = (String) fastApiResponse.get("job_id");
            if (fastApiJobId == null) {
                throw new RuntimeException("FastAPI did not return job_id");
            }

            // Store mapping
            jobIdMapping.put(jobId, fastApiJobId);
            
            addLog(job, "INFO", "FastAPI job started: " + fastApiJobId);
            autoMLJobRepository.save(job);

            // Poll for progress
            boolean completed = false;
            int maxRetries = 600; // 10 minutes max (1 sec intervals)
            int retries = 0;

            while (!completed && retries < maxRetries) {
                // Check if stopped
                if (!runningJobs.containsKey(jobId)) {
                    try { mlEngineClient.stopAutoML(fastApiJobId); } catch (Exception e) { }
                    markJobStopped(jobId);
                    return;
                }

                Thread.sleep(1000); // Poll every second
                retries++;

                try {
                    Map<String, Object> progress = mlEngineClient.getAutoMLProgress(fastApiJobId);
                    
                    String status = (String) progress.get("status");
                    Integer progressPct = progress.get("progress") != null ? ((Number) progress.get("progress")).intValue() : null;
                    String currentPhase = (String) progress.get("current_phase");
                    String currentAlgorithm = (String) progress.get("current_algorithm");
                    Integer algorithmsCompleted = progress.get("algorithms_completed") != null ? ((Number) progress.get("algorithms_completed")).intValue() : null;
                    Integer algorithmsTotal = progress.get("algorithms_total") != null ? ((Number) progress.get("algorithms_total")).intValue() : null;
                    Double currentBestScore = progress.get("current_best_score") != null 
                            ? ((Number) progress.get("current_best_score")).doubleValue() 
                            : null;
                    String currentBestAlgorithm = (String) progress.get("current_best_algorithm");

                    // Update local job
                    job = autoMLJobRepository.findById(jobId).orElseThrow();
                    
                    if (progressPct != null) job.setProgress(progressPct);
                    if (currentPhase != null) job.setCurrentPhase(mapPhase(currentPhase));
                    if (currentAlgorithm != null) job.setCurrentAlgorithm(currentAlgorithm);
                    if (algorithmsCompleted != null) job.setAlgorithmsCompleted(algorithmsCompleted);
                    if (algorithmsTotal != null) job.setAlgorithmsTotal(algorithmsTotal);
                    if (currentBestScore != null) job.setCurrentBestScore(currentBestScore);
                    if (currentBestAlgorithm != null) job.setCurrentBestAlgorithm(currentBestAlgorithm);
                    
                    // Update status
                    if ("running".equals(status)) {
                        job.setStatus(JobStatus.TRAINING);
                    }
                    
                    autoMLJobRepository.save(job);

                    // Check if completed or failed
                    if ("completed".equals(status)) {
                        completed = true;
                    } else if ("failed".equals(status)) {
                        String error = (String) progress.get("error_message");
                        throw new RuntimeException("FastAPI job failed: " + error);
                    } else if ("stopped".equals(status)) {
                        markJobStopped(jobId);
                        return;
                    }

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("failed")) {
                        throw e;
                    }
                    log.warn("Error polling progress: {}", e.getMessage());
                }
            }

            if (!completed) {
                throw new RuntimeException("AutoML job timed out");
            }

            // Get final results
            Map<String, Object> results = mlEngineClient.getAutoMLResults(fastApiJobId);
            
            // Update job with results
            job = autoMLJobRepository.findById(jobId).orElseThrow();
            job.setStatus(JobStatus.COMPLETED);
            job.setCurrentPhase("COMPLETED");
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            
            // Extract results
            job.setBestAlgorithm((String) results.get("best_algorithm"));
            job.setBestScore(((Number) results.get("best_score")).doubleValue());
            job.setBestMetric((String) results.get("best_metric"));
            
            // Store FastAPI model ID for predictions
            String fastApiModelId = (String) results.get("model_id");
            job.setModelPath(fastApiModelId);
            
            // Convert leaderboard
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> leaderboard = (List<Map<String, Object>>) results.get("leaderboard");
            if (leaderboard != null) {
                List<AutoMLDTO.LeaderboardEntry> entries = convertLeaderboard(leaderboard, job.getProblemType());
                job.setLeaderboardJson(serializeJson(entries));
            }
            
            // Convert feature importance
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> featureImportance = (List<Map<String, Object>>) results.get("feature_importance");
            if (featureImportance != null) {
                List<AutoMLDTO.FeatureImportanceEntry> fiEntries = convertFeatureImportance(featureImportance);
                job.setFeatureImportanceJson(serializeJson(fiEntries));
            }
            
            // Calculate elapsed time
            if (job.getStartedAt() != null) {
                long elapsed = java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).getSeconds();
                job.setElapsedTimeSeconds(elapsed);
            }
            
            addLog(job, "INFO", "✅ AutoML completed! Best: " + job.getBestAlgorithm() + 
                    " with " + String.format("%.2f%%", job.getBestScore() * 100) + " " + job.getBestMetric());
            
            autoMLJobRepository.save(job);
            
            // AUTOMATICALLY CREATE MODEL ENTITY with FastAPI model_id
            // This ensures predictions work immediately without manual deployment
            Model model = createModelFromAutoMLJob(job);
            job.setBestModel(model);
            autoMLJobRepository.save(job);
            log.info("📦 Auto-created Model entity: {} with FastAPI model_id: {}", model.getId(), model.getModelPath());
            
            runningJobs.remove(jobId);
            jobIdMapping.remove(jobId);
            
            log.info("✅ AutoML job completed successfully: {} with best model: {}", 
                    jobId, job.getBestAlgorithm());
            
            // Record activity for completed training
            try {
                activityService.recordTrainingCompleted(
                        jobId,
                        job.getName() != null ? job.getName() : "AutoML Run",
                        String.format("%.1f%%", job.getBestScore() * 100),
                        "System",
                        job.getProject() != null ? job.getProject().getId() : null
                );
                
                // Also record model creation
                activityService.recordModelCreated(
                        model.getId(),
                        model.getName(),
                        String.format("%.1f%%", job.getBestScore() * 100),
                        "System",
                        job.getProject() != null ? job.getProject().getId() : null
                );
            } catch (Exception activityEx) {
                log.warn("Failed to record activity: {}", activityEx.getMessage());
            }

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
                    
                    // Record failure activity
                    activityService.recordTrainingFailed(
                            jobId,
                            job.getName() != null ? job.getName() : "AutoML Run",
                            e.getMessage() != null ? e.getMessage().substring(0, Math.min(100, e.getMessage().length())) : "Unknown error",
                            "System",
                            job.getProject() != null ? job.getProject().getId() : null
                    );
                }
            } catch (Exception ex) {
                log.error("Failed to update job status", ex);
            }
            runningJobs.remove(jobId);
            jobIdMapping.remove(jobId);
        }
    }

    /**
     * Convert FastAPI leaderboard to our DTO format.
     */
    private List<AutoMLDTO.LeaderboardEntry> convertLeaderboard(
            List<Map<String, Object>> fastApiLeaderboard, 
            ProblemType problemType) {
        
        List<AutoMLDTO.LeaderboardEntry> entries = new ArrayList<>();
        
        for (Map<String, Object> item : fastApiLeaderboard) {
            AutoMLDTO.LeaderboardEntry.LeaderboardEntryBuilder builder = AutoMLDTO.LeaderboardEntry.builder()
                    .rank(item.get("rank") != null ? ((Number) item.get("rank")).intValue() : 0)
                    .algorithm((String) item.get("algorithm"))
                    .cvScore(item.get("score") != null ? ((Number) item.get("score")).doubleValue() : 0)
                    .cvStd(item.get("std") != null ? ((Number) item.get("std")).doubleValue() : 0)
                    .trainingTimeSeconds(item.get("training_time_seconds") != null 
                            ? ((Number) item.get("training_time_seconds")).longValue() : 0L);
            
            if (problemType == ProblemType.CLASSIFICATION) {
                builder.accuracy(item.get("accuracy") != null ? ((Number) item.get("accuracy")).doubleValue() : null);
                builder.precision(item.get("precision") != null ? ((Number) item.get("precision")).doubleValue() : null);
                builder.recall(item.get("recall") != null ? ((Number) item.get("recall")).doubleValue() : null);
                builder.f1Score(item.get("f1_score") != null ? ((Number) item.get("f1_score")).doubleValue() : null);
            } else {
                builder.r2(item.get("r2") != null ? ((Number) item.get("r2")).doubleValue() : null);
                builder.mae(item.get("mae") != null ? ((Number) item.get("mae")).doubleValue() : null);
                builder.rmse(item.get("rmse") != null ? ((Number) item.get("rmse")).doubleValue() : null);
            }
            
            entries.add(builder.build());
        }
        
        return entries;
    }

    /**
     * Convert FastAPI feature importance to our DTO format.
     */
    private List<AutoMLDTO.FeatureImportanceEntry> convertFeatureImportance(
            List<Map<String, Object>> fastApiFeatureImportance) {
        
        List<AutoMLDTO.FeatureImportanceEntry> entries = new ArrayList<>();
        
        for (Map<String, Object> item : fastApiFeatureImportance) {
            entries.add(AutoMLDTO.FeatureImportanceEntry.builder()
                    .feature((String) item.get("feature"))
                    .importance(item.get("importance") != null 
                            ? ((Number) item.get("importance")).doubleValue() : 0)
                    .rank(item.get("rank") != null ? ((Number) item.get("rank")).intValue() : 0)
                    .build());
        }
        
        return entries;
    }

    /**
     * Map FastAPI phase names to our phase names.
     */
    private String mapPhase(String fastApiPhase) {
        if (fastApiPhase == null) return "QUEUED";
        
        String phase = fastApiPhase.toLowerCase();
        if (phase.contains("data_loading") || phase.contains("data loading") || 
            phase.contains("data_validation") || phase.contains("data validation")) {
            return "DATA_VALIDATION";
        } else if (phase.contains("feature")) {
            return "FEATURE_ENGINEERING";
        } else if (phase.contains("algorithm") || phase.contains("selection")) {
            return "ALGORITHM_SELECTION";
        } else if (phase.contains("model") || phase.contains("training")) {
            return "MODEL_TRAINING";
        } else if (phase.contains("evaluation")) {
            return "EVALUATION";
        } else if (phase.contains("completed")) {
            return "COMPLETED";
        }
        return fastApiPhase.toUpperCase();
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
            log.error("Failed to mark job as stopped", e);
        }
    }

    // ============ PUBLIC API METHODS ============

    /**
     * Get job progress/status.
     */
    public AutoMLDTO.ProgressResponse getJobProgress(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        List<AutoMLDTO.PhaseInfo> phases = buildPhaseInfo(job);
        List<AutoMLDTO.LogEntry> logs = deserializeLogs(job.getLogsJson());

        Long elapsed = job.getElapsedTimeSeconds();
        if (elapsed == null && job.getStartedAt() != null) {
            elapsed = java.time.Duration.between(job.getStartedAt(), LocalDateTime.now()).getSeconds();
        }

        Long remaining = null;
        if (job.getProgress() != null && job.getProgress() > 0 && elapsed != null && elapsed > 0) {
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
        
        // Get deployment info
        List<Deployment> deployments = deploymentRepository.findByAutoMLJobId(job.getId());
        Optional<Deployment> activeDeployment = deployments.stream()
                .filter(d -> d.getStatus() == DeploymentStatus.ACTIVE)
                .findFirst();
        Optional<Deployment> latestDeployment = deployments.stream()
                .max((d1, d2) -> d1.getVersion().compareTo(d2.getVersion()));
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
                        .engineeredFeatures(dataset.getColumnCount() + (Boolean.TRUE.equals(job.getEnableFeatureEngineering()) ? 5 : 0))
                        .build())
                .leaderboard(leaderboard)
                .bestModel(AutoMLDTO.BestModelInfo.builder()
                        .modelId(job.getModelPath())
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
                // Deployment info
                .isDeployed(!deployments.isEmpty())
                .deploymentId(deployment != null ? deployment.getId() : null)
                .deployedModelId(deployment != null && deployment.getModel() != null ? deployment.getModel().getId() : null)
                .deploymentEndpoint(deployment != null ? deployment.getEndpointPath() : null)
                .deployedAt(deployment != null ? deployment.getDeployedAt() : null)
                .deploymentVersion(deployment != null ? deployment.getVersion() : null)
                .deploymentVersionLabel(deployment != null ? deployment.getVersionLabel() : null)
                .isActiveDeployment(activeDeployment.isPresent())
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
            jobPage = autoMLJobRepository.findByProject_IdAndStatus(projectId, jobStatus, pageRequest);
        } else if (projectId != null) {
            jobPage = autoMLJobRepository.findByProject_Id(projectId, pageRequest);
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

        // Remove from running jobs
        CompletableFuture<?> future = runningJobs.remove(jobId);
        if (future != null) {
            future.cancel(true);
        }
        
        // Stop FastAPI job if exists
        String fastApiJobId = jobIdMapping.remove(jobId);
        if (fastApiJobId != null) {
            try {
                mlEngineClient.stopAutoML(fastApiJobId);
            } catch (Exception e) {
                log.warn("Failed to stop FastAPI job: {}", e.getMessage());
            }
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

        CompletableFuture<?> future = runningJobs.remove(jobId);
        if (future != null) {
            future.cancel(true);
        }
        jobIdMapping.remove(jobId);

        autoMLJobRepository.delete(job);
        log.info("Deleted AutoML job: {}", jobId);
    }

    /**
     * Deploy best model from job.
     */
    @Transactional
    public AutoMLDTO.DeployResponse deployBestModel(String jobId, AutoMLDTO.DeployRequest request) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Can only deploy models from completed jobs");
        }

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
                    .modelPath(job.getModelPath()) // FastAPI model ID for predictions
                    .isDeployed(true)
                    .deployedAt(LocalDateTime.now())
                    .build();
            
            if (job.getProblemType() == ProblemType.REGRESSION) {
                model.setR2Score(job.getBestScore());
            } else {
                model.setAccuracy(job.getBestScore());
            }
            
            model = modelRepository.save(model);
            job.setBestModel(model);
            autoMLJobRepository.save(job);
            
            // Record deployment activity
            try {
                activityService.recordModelDeployed(
                        model.getId(),
                        model.getName(),
                        "System",
                        job.getProject() != null ? job.getProject().getId() : null
                );
            } catch (Exception e) {
                log.warn("Failed to record deployment activity: {}", e.getMessage());
            }
        }

        return AutoMLDTO.DeployResponse.builder()
                .deploymentId(model.getId())
                .modelId(model.getId())
                .name(model.getName())
                .status("DEPLOYED")
                .endpoint("/api/predictions/realtime/" + model.getId())
                .deployedAt(LocalDateTime.now())
                .algorithm(job.getBestAlgorithm())
                .score(job.getBestScore())
                .scoreFormatted(String.format("%.2f%%", job.getBestScore() * 100))
                .message("Model deployed successfully")
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
            algorithms.add(createAlgorithmInfo("Random Forest", "CLASSIFICATION", "Ensemble of decision trees"));
            algorithms.add(createAlgorithmInfo("XGBoost", "CLASSIFICATION", "Gradient boosting for high accuracy"));
            algorithms.add(createAlgorithmInfo("Gradient Boosting", "CLASSIFICATION", "Sequential boosting"));
            algorithms.add(createAlgorithmInfo("Logistic Regression", "CLASSIFICATION", "Fast and interpretable"));
            algorithms.add(createAlgorithmInfo("SVM", "CLASSIFICATION", "Support Vector Machine"));
        }

        if (problemType == null || problemType.equalsIgnoreCase("regression")) {
            algorithms.add(createAlgorithmInfo("Random Forest", "REGRESSION", "Ensemble regression"));
            algorithms.add(createAlgorithmInfo("XGBoost", "REGRESSION", "Gradient boosting for regression"));
            algorithms.add(createAlgorithmInfo("Gradient Boosting", "REGRESSION", "Sequential boosting"));
            algorithms.add(createAlgorithmInfo("Linear Regression", "REGRESSION", "Fast and interpretable"));
            algorithms.add(createAlgorithmInfo("SVR", "REGRESSION", "Support Vector Regression"));
        }

        return algorithms;
    }

    // ==================== HELPER METHODS ====================

    private List<AutoMLDTO.PhaseInfo> buildPhaseInfo(AutoMLJob job) {
        List<AutoMLDTO.PhaseInfo> phases = new ArrayList<>();
        String currentPhase = job.getCurrentPhase();

        phases.add(buildPhase("DATA_VALIDATION", "Data Validation", currentPhase));
        phases.add(buildPhase("FEATURE_ENGINEERING", "Feature Engineering", currentPhase));
        phases.add(buildPhase("ALGORITHM_SELECTION", "Algorithm Selection", currentPhase));
        phases.add(buildPhase("MODEL_TRAINING", "Model Training", currentPhase));
        phases.add(buildPhase("EVALUATION", "Evaluation", currentPhase));

        return phases;
    }

    private AutoMLDTO.PhaseInfo buildPhase(String name, String label, String currentPhase) {
        List<String> phaseOrder = List.of("QUEUED", "DATA_VALIDATION", "FEATURE_ENGINEERING", 
                "ALGORITHM_SELECTION", "MODEL_TRAINING", "EVALUATION", "COMPLETED");

        int currentIdx = currentPhase != null ? phaseOrder.indexOf(currentPhase) : 0;
        int thisIdx = phaseOrder.indexOf(name);

        String status;
        int progress;
        if (currentPhase != null && currentPhase.equals("COMPLETED")) {
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
        List<Deployment> deployments = deploymentRepository.findByAutoMLJobId(job.getId());
        
        Optional<Deployment> activeDeployment = deployments.stream()
                .filter(d -> d.getStatus() == DeploymentStatus.ACTIVE)
                .findFirst();
        
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
     * Create Model entity from completed AutoML job.
     * This ensures the model can be used for predictions immediately.
     */
    private Model createModelFromAutoMLJob(AutoMLJob job) {
        // Check if model already exists for this job
        if (job.getBestModel() != null) {
            return job.getBestModel();
        }

        // Fetch project properly to avoid lazy loading issues in async thread
        Project project = null;
        if (job.getProjectIdValue() != null) {
            project = projectRepository.findById(job.getProjectIdValue()).orElse(null);
        }

        Model model = Model.builder()
                .name(job.getBestAlgorithm() + " - " + (job.getName() != null ? job.getName() : "AutoML"))
                .algorithm(job.getBestAlgorithm())
                .algorithmDisplayName(job.getBestAlgorithm())
                .problemType(job.getProblemType())
                .project(project)                     // Use fetched project
                .datasetId(job.getDatasetIdValue())   // Use stored value - avoids lazy loading!
                .datasetName(job.getDatasetName())    // Use stored value - avoids lazy loading!
                .targetVariable(job.getTargetColumn())
                .modelPath(job.getModelPath())  // CRITICAL: FastAPI model ID for predictions!
                .trainingJobId(job.getId())
                .source("AUTOML")              // Model created via AutoML Engine
                .sourceJobId(job.getId())      // Link to AutoML job
                .isDeployed(false)
                .isProductionReady(true)
                .isBest(true)
                .build();

        // Set accuracy/score based on problem type
        if (job.getProblemType() == ProblemType.REGRESSION) {
            model.setR2Score(job.getBestScore());
        } else {
            model.setAccuracy(job.getBestScore());
        }

        // Set training time
        if (job.getElapsedTimeSeconds() != null) {
            model.setTrainingTimeSeconds(job.getElapsedTimeSeconds());
        }

        model = modelRepository.save(model);
        log.info("✅ Created AUTOML Model: {} with modelPath: {} for project: {}", 
                model.getId(), model.getModelPath(), 
                project != null ? project.getId() : "none");
        
        return model;
    }
}
