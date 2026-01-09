package com.mlengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.client.MLEngineClient;
import com.mlengine.model.dto.AutoMLDTO;
import com.mlengine.model.entity.AutoMLJob;
import com.mlengine.model.entity.Dataset;
import com.mlengine.model.entity.Deployment;
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
import org.springframework.beans.factory.annotation.Value;
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
 * Now integrates with Python FastAPI ML Engine for REAL ML execution!
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

    @Value("${ml-engine.api.enabled:true}")
    private boolean mlEngineEnabled;

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

        // Start async processing AFTER transaction commits
        final String jobId = job.getId();
        final String datasetPath = dataset.getFilePath();
        final AutoMLDTO.JobResponse response = toJobResponse(job, "AutoML job started successfully");
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    try {
                        if (mlEngineEnabled) {
                            executeAutoMLWithFastAPI(jobId, datasetPath);
                        } else {
                            executeAutoMLMock(jobId);
                        }
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
                    mlEngineClient.stopAutoML(fastApiJobId);
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
            
            runningJobs.remove(jobId);
            jobIdMapping.remove(jobId);
            
            log.info("✅ AutoML job completed successfully: {} with best model: {}", 
                    jobId, job.getBestAlgorithm());

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
            AutoMLDTO.LeaderboardEntry entry = AutoMLDTO.LeaderboardEntry.builder()
                    .rank(item.get("rank") != null ? ((Number) item.get("rank")).intValue() : 0)
                    .algorithm((String) item.get("algorithm"))
                    .score(item.get("score") != null ? ((Number) item.get("score")).doubleValue() : 0)
                    .std(item.get("std") != null ? ((Number) item.get("std")).doubleValue() : 0)
                    .trainingTimeSeconds(item.get("training_time_seconds") != null 
                            ? ((Number) item.get("training_time_seconds")).doubleValue() : 0)
                    .build();
            
            if (problemType == ProblemType.CLASSIFICATION) {
                entry.setAccuracy(item.get("accuracy") != null ? ((Number) item.get("accuracy")).doubleValue() : null);
                entry.setPrecision(item.get("precision") != null ? ((Number) item.get("precision")).doubleValue() : null);
                entry.setRecall(item.get("recall") != null ? ((Number) item.get("recall")).doubleValue() : null);
                entry.setF1Score(item.get("f1_score") != null ? ((Number) item.get("f1_score")).doubleValue() : null);
            } else {
                entry.setR2(item.get("r2") != null ? ((Number) item.get("r2")).doubleValue() : null);
                entry.setMae(item.get("mae") != null ? ((Number) item.get("mae")).doubleValue() : null);
                entry.setRmse(item.get("rmse") != null ? ((Number) item.get("rmse")).doubleValue() : null);
            }
            
            entries.add(entry);
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
     * Mock execution for when ML Engine is disabled (fallback).
     */
    private void executeAutoMLMock(String jobId) {
        try {
            log.info("Executing MOCK AutoML job: {}", jobId);
            
            AutoMLJob job = autoMLJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found"));

            job.setStatus(JobStatus.STARTING);
            job.setStartedAt(LocalDateTime.now());
            job.setCurrentPhase("DATA_VALIDATION");
            job.setProgress(5);
            addLog(job, "INFO", "AutoML job started (MOCK MODE)");
            autoMLJobRepository.save(job);

            // Simulate phases
            String[] phases = {"DATA_VALIDATION", "FEATURE_ENGINEERING", "ALGORITHM_SELECTION", "MODEL_TRAINING", "EVALUATION"};
            int[] progress = {15, 25, 70, 90, 95};
            
            for (int i = 0; i < phases.length; i++) {
                Thread.sleep(2000);
                
                if (!runningJobs.containsKey(jobId)) {
                    markJobStopped(jobId);
                    return;
                }
                
                job = autoMLJobRepository.findById(jobId).orElseThrow();
                job.setCurrentPhase(phases[i]);
                job.setProgress(progress[i]);
                job.setStatus(JobStatus.TRAINING);
                addLog(job, "INFO", "Phase: " + phases[i]);
                autoMLJobRepository.save(job);
            }

            // Complete
            job = autoMLJobRepository.findById(jobId).orElseThrow();
            job.setStatus(JobStatus.COMPLETED);
            job.setCurrentPhase("COMPLETED");
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            job.setBestAlgorithm("Random Forest (Mock)");
            job.setBestScore(0.85 + Math.random() * 0.1);
            job.setBestMetric(job.getProblemType() == ProblemType.REGRESSION ? "R²" : "Accuracy");
            job.setLeaderboardJson(serializeJson(generateMockLeaderboard(job.getProblemType())));
            job.setFeatureImportanceJson(serializeJson(generateMockFeatureImportance()));
            
            addLog(job, "INFO", "AutoML completed (MOCK)");
            autoMLJobRepository.save(job);

            runningJobs.remove(jobId);

        } catch (Exception e) {
            log.error("Mock AutoML failed: {}", jobId, e);
            runningJobs.remove(jobId);
        }
    }

    private List<AutoMLDTO.LeaderboardEntry> generateMockLeaderboard(ProblemType problemType) {
        List<AutoMLDTO.LeaderboardEntry> entries = new ArrayList<>();
        String[] algorithms = {"Random Forest", "XGBoost", "Gradient Boosting", "SVM", "Logistic Regression"};
        
        for (int i = 0; i < algorithms.length; i++) {
            double score = 0.95 - (i * 0.03) + (Math.random() * 0.02);
            entries.add(AutoMLDTO.LeaderboardEntry.builder()
                    .rank(i + 1)
                    .algorithm(algorithms[i])
                    .score(score)
                    .accuracy(problemType == ProblemType.CLASSIFICATION ? score : null)
                    .r2(problemType == ProblemType.REGRESSION ? score : null)
                    .trainingTimeSeconds(1.0 + Math.random() * 3)
                    .build());
        }
        
        return entries;
    }

    private List<AutoMLDTO.FeatureImportanceEntry> generateMockFeatureImportance() {
        List<AutoMLDTO.FeatureImportanceEntry> entries = new ArrayList<>();
        String[] features = {"credit_score", "income", "age", "employment", "education"};
        double[] importances = {0.35, 0.25, 0.20, 0.12, 0.08};
        
        for (int i = 0; i < features.length; i++) {
            entries.add(AutoMLDTO.FeatureImportanceEntry.builder()
                    .feature(features[i])
                    .importance(importances[i])
                    .rank(i + 1)
                    .build());
        }
        
        return entries;
    }

    // ============ REST OF THE SERVICE ============

    /**
     * Get job progress.
     */
    public AutoMLDTO.ProgressResponse getProgress(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        return AutoMLDTO.ProgressResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .statusLabel(job.getStatus().name().toLowerCase())
                .progress(job.getProgress())
                .currentPhase(job.getCurrentPhase())
                .currentAlgorithm(job.getCurrentAlgorithm())
                .phases(buildPhaseInfo(job))
                .algorithmsCompleted(job.getAlgorithmsCompleted())
                .algorithmsTotal(job.getAlgorithmsTotal())
                .currentBestScore(job.getCurrentBestScore())
                .currentBestAlgorithm(job.getCurrentBestAlgorithm())
                .elapsedSeconds(calculateElapsedSeconds(job))
                .estimatedRemainingSeconds(estimateRemainingSeconds(job))
                .logs(deserializeLogs(job.getLogsJson()))
                .errorMessage(job.getErrorMessage())
                .build();
    }

    /**
     * Get completed job results.
     */
    public AutoMLDTO.ResultsResponse getResults(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Job not completed. Current status: " + job.getStatus());
        }

        return AutoMLDTO.ResultsResponse.builder()
                .jobId(job.getId())
                .name(job.getName())
                .status(job.getStatus())
                .problemType(job.getProblemType())
                .targetColumn(job.getTargetColumn())
                .bestAlgorithm(job.getBestAlgorithm())
                .bestScore(job.getBestScore())
                .bestMetric(job.getBestMetric())
                .leaderboard(deserializeLeaderboard(job.getLeaderboardJson()))
                .featureImportance(deserializeFeatureImportance(job.getFeatureImportanceJson()))
                .totalTrainingTimeSeconds(job.getElapsedTimeSeconds())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .modelPath(job.getModelPath())
                .build();
    }

    /**
     * Stop a running job.
     */
    public AutoMLDTO.JobResponse stopJob(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.QUEUED && 
            job.getStatus() != JobStatus.STARTING &&
            job.getStatus() != JobStatus.TRAINING) {
            throw new IllegalStateException("Job cannot be stopped. Current status: " + job.getStatus());
        }

        // Remove from running jobs to signal stop
        CompletableFuture<?> future = runningJobs.remove(jobId);
        
        // Stop FastAPI job if exists
        String fastApiJobId = jobIdMapping.get(jobId);
        if (fastApiJobId != null && mlEngineEnabled) {
            try {
                mlEngineClient.stopAutoML(fastApiJobId);
            } catch (Exception e) {
                log.warn("Failed to stop FastAPI job: {}", e.getMessage());
            }
        }
        
        if (future != null) {
            future.cancel(true);
        }

        job.setStatus(JobStatus.STOPPING);
        addLog(job, "WARN", "Stop requested");
        job = autoMLJobRepository.save(job);

        return toJobResponse(job, "Job stop requested");
    }

    /**
     * List all AutoML jobs with pagination.
     */
    public Page<AutoMLDTO.ListItem> listJobs(
            String projectId, 
            JobStatus status, 
            int page, 
            int size,
            String sortBy,
            String sortDir) {
        
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? 
                Sort.Direction.ASC : Sort.Direction.DESC, 
                sortBy != null ? sortBy : "createdAt");
        
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        
        Page<AutoMLJob> jobsPage;
        if (projectId != null && status != null) {
            jobsPage = autoMLJobRepository.findByProjectIdAndStatus(projectId, status, pageRequest);
        } else if (projectId != null) {
            jobsPage = autoMLJobRepository.findByProjectId(projectId, pageRequest);
        } else if (status != null) {
            jobsPage = autoMLJobRepository.findByStatus(status, pageRequest);
        } else {
            jobsPage = autoMLJobRepository.findAll(pageRequest);
        }

        return jobsPage.map(this::toListItem);
    }

    /**
     * Get a single job.
     */
    public AutoMLDTO.JobResponse getJob(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return toJobResponse(job, null);
    }

    /**
     * Delete a job.
     */
    @Transactional
    public void deleteJob(String jobId) {
        AutoMLJob job = autoMLJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() == JobStatus.TRAINING || job.getStatus() == JobStatus.STARTING) {
            throw new IllegalStateException("Cannot delete running job. Stop it first.");
        }

        autoMLJobRepository.delete(job);
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

    // ============ HELPER METHODS ============

    private int getAlgorithmCount(ProblemType problemType, Integer accuracyVsSpeed) {
        if (accuracyVsSpeed != null && accuracyVsSpeed < 50) {
            return 3; // Quick mode
        }
        return problemType == ProblemType.CLASSIFICATION ? 5 : 6;
    }

    private long calculateElapsedSeconds(AutoMLJob job) {
        if (job.getStartedAt() == null) return 0;
        LocalDateTime endTime = job.getCompletedAt() != null ? job.getCompletedAt() : LocalDateTime.now();
        return java.time.Duration.between(job.getStartedAt(), endTime).getSeconds();
    }

    private Long estimateRemainingSeconds(AutoMLJob job) {
        if (job.getProgress() == null || job.getProgress() == 0) return null;
        long elapsed = calculateElapsedSeconds(job);
        if (elapsed == 0) return null;
        double rate = job.getProgress() / (double) elapsed;
        if (rate == 0) return null;
        return (long) ((100 - job.getProgress()) / rate);
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
}
