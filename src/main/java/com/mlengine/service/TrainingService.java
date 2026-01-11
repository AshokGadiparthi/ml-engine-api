package com.mlengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.client.MLEngineClient;
import com.mlengine.model.dto.TrainingJobDTO;
import com.mlengine.model.entity.Dataset;
import com.mlengine.model.entity.Model;
import com.mlengine.model.entity.Project;
import com.mlengine.model.entity.TrainingJob;
import com.mlengine.model.enums.JobStatus;
import com.mlengine.model.enums.ProblemType;
import com.mlengine.repository.DatasetRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.ProjectRepository;
import com.mlengine.repository.TrainingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for training job operations.
 * Integrates with Python FastAPI ML Engine for REAL ML training.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingJobRepository trainingJobRepository;
    private final DatasetRepository datasetRepository;
    private final ModelRepository modelRepository;
    private final ProjectRepository projectRepository;
    private final AlgorithmService algorithmService;
    private final MLEngineClient mlEngineClient;
    private final ActivityService activityService;
    private final ObjectMapper objectMapper;

    // Track running jobs for cancellation
    private final Map<String, CompletableFuture<?>> runningJobs = new ConcurrentHashMap<>();
    
    // Map Spring Boot job ID to FastAPI job ID
    private final Map<String, String> jobIdMapping = new ConcurrentHashMap<>();
    
    // Executor for async training
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * Start a new training job via FastAPI.
     */
    @Transactional
    public TrainingJobDTO.Response startTraining(TrainingJobDTO.CreateRequest request) {
        log.info("🚀 Starting REAL training job: {} with algorithm {}", 
                request.getExperimentName(), request.getAlgorithm());

        // Validate dataset
        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + request.getDatasetId()));

        // Validate project - TRY MULTIPLE SOURCES
        Project project = null;
        String projectIdToUse = request.getProjectId();
        
        log.info("Training request - projectId from request: {}", projectIdToUse);
        
        // FALLBACK 1: If projectId not in request, get from dataset
        if (projectIdToUse == null && dataset.getProjectId() != null) {
            projectIdToUse = dataset.getProjectId();
            log.info("ProjectId not in request, using dataset's projectId: {}", projectIdToUse);
        }
        
        // FALLBACK 2: If dataset has project relationship, use that
        if (projectIdToUse == null && dataset.getProject() != null) {
            try {
                projectIdToUse = dataset.getProject().getId();
                log.info("Got projectId from dataset.project: {}", projectIdToUse);
            } catch (Exception e) {
                log.warn("Could not get project from dataset relationship: {}", e.getMessage());
            }
        }
        
        // Use final variable for lambda (Java requirement)
        final String finalProjectId = projectIdToUse;
        if (finalProjectId != null) {
            project = projectRepository.findById(finalProjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + finalProjectId));
        }
        
        log.info("Final project for training job: {} (id: {})", 
                project != null ? project.getName() : "null",
                project != null ? project.getId() : "null");

        // Get algorithm display name
        String algorithmDisplayName = algorithmService.getAlgorithmDisplayName(request.getAlgorithm());

        // Create job name
        String jobName = request.getExperimentName() != null 
                ? request.getExperimentName() 
                : dataset.getName() + " - " + algorithmDisplayName;

        // Convert hyperparameters to JSON
        String hyperparamsJson = null;
        if (request.getHyperparameters() != null) {
            try {
                hyperparamsJson = objectMapper.writeValueAsString(request.getHyperparameters());
            } catch (Exception e) {
                log.warn("Failed to serialize hyperparameters", e);
            }
        }

        // Create training job entity
        TrainingJob job = TrainingJob.builder()
                .jobName(jobName)
                .experimentName(request.getExperimentName())
                .status(JobStatus.QUEUED)
                .progress(0)
                .currentEpoch(0)
                .totalEpochs(100)
                .datasetId(request.getDatasetId())
                .datasetName(dataset.getName())
                .algorithm(request.getAlgorithm())
                .algorithmDisplayName(algorithmDisplayName)
                .targetVariable(request.getTargetVariable())
                .problemType(request.getProblemType())
                .trainTestSplit(request.getTrainTestSplit())
                .crossValidationFolds(request.getCrossValidationFolds())
                .hyperparametersJson(hyperparamsJson)
                .gpuAcceleration(request.getGpuAcceleration())
                .autoHyperparameterTuning(request.getAutoHyperparameterTuning())
                .earlyStopping(request.getEarlyStopping())
                .earlyStoppingPatience(request.getEarlyStoppingPatience())
                .batchSize(request.getBatchSize())
                .evaluationMetric(request.getEvaluationMetric())
                .computeResources(request.getGpuAcceleration() ? "4x GPU" : "CPU")
                .costEstimate(request.getGpuAcceleration() ? 0.42 : 0.05)
                .project(project)
                .projectIdValue(project != null ? project.getId() : null)  // Store for async access
                .build();

        job = trainingJobRepository.save(job);
        log.info("Created training job: {}", job.getId());

        // Record activity
        try {
            activityService.recordTrainingStarted(
                    job.getId(),
                    jobName,
                    algorithmDisplayName,
                    "System",
                    project != null ? project.getId() : null
            );
        } catch (Exception e) {
            log.warn("Failed to record activity: {}", e.getMessage());
        }

        // Start async training via FastAPI AFTER transaction commits
        final String jobId = job.getId();
        final String datasetPath = dataset.getFilePath();
        final TrainingJobDTO.Response response = toResponse(job);
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    try {
                        executeTrainingWithFastAPI(jobId, datasetPath);
                    } catch (Exception e) {
                        log.error("Training execution failed for job: {}", jobId, e);
                    }
                }, executorService);
                runningJobs.put(jobId, future);
            }
        });

        return response;
    }

    /**
     * Execute training using the FastAPI ML Engine - REAL ML!
     */
    private void executeTrainingWithFastAPI(String jobId, String datasetPath) {
        try {
            log.info("🚀 Executing REAL ML training via FastAPI for job: {}", jobId);
            
            // Get job from database
            TrainingJob job = trainingJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found"));

            // Update status to starting
            job.setStatus(JobStatus.STARTING);
            job.setStartedAt(LocalDateTime.now());
            job.setStatusMessage("Connecting to ML Engine...");
            job.setProgress(5);
            trainingJobRepository.save(job);

            // Build config for FastAPI
            Map<String, Object> config = new HashMap<>();
            config.put("train_test_split", job.getTrainTestSplit());
            config.put("cv_folds", job.getCrossValidationFolds());
            config.put("early_stopping", job.getEarlyStopping());
            config.put("early_stopping_patience", job.getEarlyStoppingPatience());
            config.put("evaluation_metric", job.getEvaluationMetric());
            
            // Add hyperparameters if present
            if (job.getHyperparametersJson() != null) {
                try {
                    Map<String, Object> hyperparams = objectMapper.readValue(
                            job.getHyperparametersJson(), 
                            new TypeReference<Map<String, Object>>() {}
                    );
                    config.put("hyperparameters", hyperparams);
                } catch (Exception e) {
                    log.warn("Failed to parse hyperparameters", e);
                }
            }

            // Call FastAPI to start training
            Map<String, Object> fastApiResponse = mlEngineClient.startTraining(
                    datasetPath,
                    job.getTargetVariable(),
                    job.getAlgorithm(),
                    job.getProblemType().name(),
                    config
            );

            String fastApiJobId = (String) fastApiResponse.get("job_id");
            if (fastApiJobId == null) {
                throw new RuntimeException("FastAPI did not return job_id");
            }
            
            jobIdMapping.put(jobId, fastApiJobId);
            log.info("FastAPI training job started: {} -> {}", jobId, fastApiJobId);

            // Update status to training
            job = trainingJobRepository.findById(jobId).orElseThrow();
            job.setStatus(JobStatus.TRAINING);
            job.setStatusMessage("Training in progress...");
            job.setProgress(10);
            trainingJobRepository.save(job);

            // Poll for progress
            boolean completed = false;
            int maxPolls = 300;  // 5 minutes max
            int pollCount = 0;
            
            while (!completed && pollCount < maxPolls) {
                Thread.sleep(1000);  // Poll every second
                pollCount++;
                
                try {
                    Map<String, Object> progress = mlEngineClient.getTrainingProgress(fastApiJobId);
                    
                    String status = (String) progress.get("status");
                    Integer progressValue = progress.get("progress") != null ? 
                            ((Number) progress.get("progress")).intValue() : null;
                    Integer currentEpoch = progress.get("current_epoch") != null ?
                            ((Number) progress.get("current_epoch")).intValue() : null;
                    Integer totalEpochs = progress.get("total_epochs") != null ?
                            ((Number) progress.get("total_epochs")).intValue() : null;
                    Double currentAccuracy = progress.get("current_accuracy") != null ?
                            ((Number) progress.get("current_accuracy")).doubleValue() : null;
                    Double currentLoss = progress.get("current_loss") != null ?
                            ((Number) progress.get("current_loss")).doubleValue() : null;
                    
                    // Update job progress
                    job = trainingJobRepository.findById(jobId).orElseThrow();
                    if (progressValue != null) job.setProgress(progressValue);
                    if (currentEpoch != null) job.setCurrentEpoch(currentEpoch);
                    if (totalEpochs != null) job.setTotalEpochs(totalEpochs);
                    if (currentAccuracy != null) job.setCurrentAccuracy(currentAccuracy);
                    if (currentLoss != null) job.setCurrentLoss(currentLoss);
                    
                    // Calculate ETA
                    if (progressValue != null && progressValue > 0) {
                        long elapsedSeconds = ChronoUnit.SECONDS.between(job.getStartedAt(), LocalDateTime.now());
                        long etaSeconds = (elapsedSeconds * (100 - progressValue)) / progressValue;
                        job.setEtaSeconds(etaSeconds);
                    }
                    
                    job.setStatusMessage("Training epoch " + (currentEpoch != null ? currentEpoch : "?") + 
                            "/" + (totalEpochs != null ? totalEpochs : "100"));
                    trainingJobRepository.save(job);
                    
                    if ("completed".equals(status) || "COMPLETED".equals(status)) {
                        completed = true;
                    } else if ("failed".equals(status) || "FAILED".equals(status)) {
                        throw new RuntimeException("Training failed: " + progress.get("error"));
                    }
                    
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("failed")) {
                        throw e;
                    }
                    log.debug("Progress poll error (may be normal): {}", e.getMessage());
                }
            }

            // Get final results
            Map<String, Object> results = mlEngineClient.getTrainingResults(fastApiJobId);
            
            // Update job with results
            job = trainingJobRepository.findById(jobId).orElseThrow();
            job.setStatus(JobStatus.COMPLETED);
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            job.setStatusMessage("Training completed successfully");
            
            // Extract metrics - FastAPI returns test_score, not accuracy
            Double accuracy = null;
            if (results.get("accuracy") != null) {
                accuracy = ((Number) results.get("accuracy")).doubleValue();
            } else if (results.get("test_score") != null) {
                accuracy = ((Number) results.get("test_score")).doubleValue();
            }
            
            Double precision = results.get("precision") != null ?
                    ((Number) results.get("precision")).doubleValue() : null;
            Double recall = results.get("recall") != null ?
                    ((Number) results.get("recall")).doubleValue() : null;
            Double f1Score = results.get("f1_score") != null ?
                    ((Number) results.get("f1_score")).doubleValue() : null;
            
            if (accuracy != null) job.setCurrentAccuracy(accuracy);
            job.setBestAccuracy(accuracy);
            
            // Store model path from FastAPI
            String modelId = (String) results.get("model_id");
            job.setModelPath(modelId);
            
            // Calculate duration
            if (job.getStartedAt() != null) {
                job.setDurationSeconds(ChronoUnit.SECONDS.between(job.getStartedAt(), job.getCompletedAt()));
            }
            
            trainingJobRepository.save(job);
            
            // Create Model entity
            Model model = createModelFromJob(job, results);
            job.setModelId(model.getId());
            trainingJobRepository.save(job);
            
            runningJobs.remove(jobId);
            jobIdMapping.remove(jobId);
            
            log.info("✅ Training job completed successfully: {} with accuracy: {}", 
                    jobId, accuracy != null ? String.format("%.2f%%", accuracy * 100) : "N/A");
            
            // Record activity
            try {
                activityService.recordTrainingCompleted(
                        jobId,
                        job.getJobName(),
                        accuracy != null ? String.format("%.1f%%", accuracy * 100) : "N/A",
                        "System",
                        job.getProject() != null ? job.getProject().getId() : null
                );
                
                activityService.recordModelCreated(
                        model.getId(),
                        model.getName(),
                        accuracy != null ? String.format("%.1f%%", accuracy * 100) : "N/A",
                        "System",
                        job.getProject() != null ? job.getProject().getId() : null
                );
            } catch (Exception e) {
                log.warn("Failed to record activity: {}", e.getMessage());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Training job interrupted: {}", jobId);
            markJobStopped(jobId);
        } catch (Exception e) {
            log.error("Training job failed: {}", jobId, e);
            try {
                TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    job.setStatus(JobStatus.FAILED);
                    job.setErrorMessage(e.getMessage());
                    job.setStatusMessage("Training failed: " + e.getMessage());
                    job.setCompletedAt(LocalDateTime.now());
                    trainingJobRepository.save(job);
                    
                    // Record failure activity
                    activityService.recordTrainingFailed(
                            jobId,
                            job.getJobName(),
                            e.getMessage() != null ? 
                                    e.getMessage().substring(0, Math.min(100, e.getMessage().length())) : 
                                    "Unknown error",
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
     * Create Model entity from completed training job.
     */
    private Model createModelFromJob(TrainingJob job, Map<String, Object> results) {
        // FastAPI returns test_score, Spring Boot expects accuracy
        Double accuracy = null;
        if (results.get("accuracy") != null) {
            accuracy = ((Number) results.get("accuracy")).doubleValue();
        } else if (results.get("test_score") != null) {
            accuracy = ((Number) results.get("test_score")).doubleValue();
        } else if (job.getCurrentAccuracy() != null) {
            accuracy = job.getCurrentAccuracy();
        }
        
        Double precision = results.get("precision") != null ?
                ((Number) results.get("precision")).doubleValue() : null;
        Double recall = results.get("recall") != null ?
                ((Number) results.get("recall")).doubleValue() : null;
        Double f1Score = results.get("f1_score") != null ?
                ((Number) results.get("f1_score")).doubleValue() : null;
        
        // ========== GET PROJECT ID - TRY MULTIPLE SOURCES ==========
        String projectIdForModel = job.getProjectIdValue();
        log.info("createModelFromJob - projectIdValue: {}", projectIdForModel);
        
        // Fallback 1: Try read-only column
        if (projectIdForModel == null) {
            projectIdForModel = job.getProjectId();
            log.info("createModelFromJob - fallback to projectId column: {}", projectIdForModel);
        }
        
        // Fallback 2: Try to get from dataset
        if (projectIdForModel == null && job.getDatasetId() != null) {
            try {
                Dataset dataset = datasetRepository.findById(job.getDatasetId()).orElse(null);
                if (dataset != null) {
                    if (dataset.getProjectId() != null) {
                        projectIdForModel = dataset.getProjectId();
                        log.info("createModelFromJob - got projectId from dataset.projectId: {}", projectIdForModel);
                    } else if (dataset.getProject() != null) {
                        projectIdForModel = dataset.getProject().getId();
                        log.info("createModelFromJob - got projectId from dataset.project: {}", projectIdForModel);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not get projectId from dataset: {}", e.getMessage());
            }
        }
        
        // Fetch project properly
        Project project = null;
        if (projectIdForModel != null) {
            project = projectRepository.findById(projectIdForModel).orElse(null);
        }
        
        log.info("Creating TRAINING model for job {} with project: {}, modelPath: {}", 
                job.getId(), projectIdForModel, job.getModelPath());
        
        Model model = Model.builder()
                .name(job.getJobName())
                .algorithm(job.getAlgorithm())
                .algorithmDisplayName(job.getAlgorithmDisplayName())
                .problemType(job.getProblemType())
                .project(project)                   // Use fetched project
                .datasetId(job.getDatasetId())
                .datasetName(job.getDatasetName())
                .targetVariable(job.getTargetVariable())
                .modelPath(job.getModelPath())      // FastAPI model ID - CRITICAL for predictions!
                .trainingJobId(job.getId())
                .source("TRAINING")                 // Model created via Model Training
                .sourceJobId(job.getId())           // Link to Training job
                .accuracy(accuracy)
                .precisionScore(precision)
                .recall(recall)
                .f1Score(f1Score)
                // ========== AUTO-DEPLOY LIKE AUTOML ==========
                .isDeployed(true)                   // AUTO-DEPLOY: Same as AutoML!
                .isProductionReady(true)            // Ready for predictions
                .isBest(false)                      // Can be set later via API
                .deployedAt(LocalDateTime.now())    // Set deployment timestamp
                .endpointUrl("/api/predictions/realtime/" + job.getModelPath())  // Set endpoint
                .build();
        
        Model savedModel = modelRepository.save(model);
        log.info("✅ Created & Auto-Deployed TRAINING Model: {} (name: {}) with modelPath: {} for project: {}", 
                savedModel.getId(), savedModel.getName(), savedModel.getModelPath(), 
                project != null ? project.getId() : "none");
        
        // Record deployment activity (same as AutoML)
        try {
            activityService.recordModelDeployed(
                    savedModel.getId(),
                    savedModel.getName(),
                    "System",
                    project != null ? project.getId() : null
            );
        } catch (Exception e) {
            log.warn("Failed to record deployment activity: {}", e.getMessage());
        }
        
        return savedModel;
    }

    private void markJobStopped(String jobId) {
        try {
            TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.setStatus(JobStatus.STOPPED);
                job.setStatusMessage("Training stopped by user");
                job.setCompletedAt(LocalDateTime.now());
                trainingJobRepository.save(job);
            }
        } catch (Exception e) {
            log.error("Failed to mark job as stopped", e);
        }
    }

    /**
     * Get all training jobs.
     */
    public List<TrainingJobDTO.ListItem> getAllJobs(String projectId) {
        List<TrainingJob> jobs = projectId != null
                ? trainingJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                : trainingJobRepository.findAllByOrderByCreatedAtDesc();

        return jobs.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    /**
     * Get training job by ID.
     */
    public TrainingJobDTO.Response getJob(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));
        return toResponse(job);
    }

    /**
     * Get job progress.
     */
    public TrainingJobDTO.ProgressUpdate getProgress(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        return TrainingJobDTO.ProgressUpdate.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .currentEpoch(job.getCurrentEpoch())
                .totalEpochs(job.getTotalEpochs())
                .currentAccuracy(job.getCurrentAccuracy())
                .currentLoss(job.getCurrentLoss())
                .etaSeconds(job.getEtaSeconds())
                .etaLabel(formatEta(job.getEtaSeconds()))
                .message(job.getStatusMessage())
                .build();
    }

    /**
     * Stop a training job.
     */
    @Transactional
    public TrainingJobDTO.Response stopJob(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        // Cancel the async task
        CompletableFuture<?> future = runningJobs.get(jobId);
        if (future != null) {
            future.cancel(true);
            runningJobs.remove(jobId);
        }

        // Stop FastAPI job if running
        String fastApiJobId = jobIdMapping.get(jobId);
        if (fastApiJobId != null) {
            try {
                // Could call mlEngineClient.stopTraining(fastApiJobId) if endpoint exists
                log.info("Stopping FastAPI job: {}", fastApiJobId);
            } catch (Exception e) {
                log.warn("Failed to stop FastAPI job: {}", e.getMessage());
            }
            jobIdMapping.remove(jobId);
        }

        job.setStatus(JobStatus.STOPPED);
        job.setStatusMessage("Training stopped by user");
        job.setCompletedAt(LocalDateTime.now());
        if (job.getStartedAt() != null) {
            job.setDurationSeconds(ChronoUnit.SECONDS.between(job.getStartedAt(), LocalDateTime.now()));
        }
        
        job = trainingJobRepository.save(job);
        log.info("Stopped training job: {}", jobId);

        return toResponse(job);
    }

    /**
     * Pause a training job.
     */
    @Transactional
    public TrainingJobDTO.Response pauseJob(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        if (job.getStatus() != JobStatus.TRAINING) {
            throw new IllegalStateException("Can only pause training jobs");
        }

        job.setStatus(JobStatus.PAUSED);
        job.setStatusMessage("Training paused");
        job = trainingJobRepository.save(job);

        return toResponse(job);
    }

    /**
     * Resume a paused training job.
     */
    @Transactional
    public TrainingJobDTO.Response resumeJob(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        if (job.getStatus() != JobStatus.PAUSED) {
            throw new IllegalStateException("Can only resume paused jobs");
        }

        job.setStatus(JobStatus.TRAINING);
        job.setStatusMessage("Training resumed");
        job = trainingJobRepository.save(job);

        return toResponse(job);
    }

    /**
     * Delete a training job.
     */
    @Transactional
    public void deleteJob(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        // Cancel if running
        CompletableFuture<?> future = runningJobs.remove(jobId);
        if (future != null) {
            future.cancel(true);
        }
        jobIdMapping.remove(jobId);

        trainingJobRepository.delete(job);
        log.info("Deleted training job: {}", jobId);
    }

    // ========== DEPLOYMENT METHODS (Same as AutoML) ==========

    /**
     * Deploy a model from a completed training job.
     * Works exactly like AutoML's deployBestModel.
     */
    @Transactional
    public TrainingJobDTO.DeployResponse deployModel(String jobId, TrainingJobDTO.DeployRequest request) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Can only deploy models from completed training jobs");
        }

        // Get or create model
        Model model = null;
        if (job.getModelId() != null) {
            model = modelRepository.findById(job.getModelId()).orElse(null);
        }

        if (model == null) {
            // Create new model if doesn't exist
            model = createModelFromJobForDeployment(job, request);
        } else {
            // Update existing model to deployed status
            model.setIsDeployed(true);
            model.setDeployedAt(LocalDateTime.now());
            model.setEndpointUrl("/api/predictions/realtime/" + model.getModelPath());
            if (request != null && request.getDeploymentName() != null) {
                model.setName(request.getDeploymentName());
            }
            model = modelRepository.save(model);
        }

        // Update job with model reference
        job.setModelId(model.getId());
        trainingJobRepository.save(job);

        // Record deployment activity
        try {
            String projectId = job.getProjectIdValue() != null ? job.getProjectIdValue() : job.getProjectId();
            activityService.recordModelDeployed(
                    model.getId(),
                    model.getName(),
                    "System",
                    projectId
            );
        } catch (Exception e) {
            log.warn("Failed to record deployment activity: {}", e.getMessage());
        }

        log.info("✅ Deployed TRAINING model: {} from job: {}", model.getId(), jobId);

        return TrainingJobDTO.DeployResponse.builder()
                .deploymentId(model.getId())
                .modelId(model.getId())
                .name(model.getName())
                .algorithm(model.getAlgorithm())
                .algorithmDisplayName(model.getAlgorithmDisplayName())
                .accuracy(model.getAccuracy())
                .accuracyLabel(model.getAccuracy() != null ? String.format("%.1f%%", model.getAccuracy() * 100) : "N/A")
                .endpointUrl(model.getEndpointUrl())
                .status("DEPLOYED")
                .deployedAt(model.getDeployedAt())
                .message("Model deployed successfully")
                .build();
    }

    /**
     * Create model for deployment (used when model doesn't exist yet).
     */
    private Model createModelFromJobForDeployment(TrainingJob job, TrainingJobDTO.DeployRequest request) {
        // Get project
        String projectIdForModel = job.getProjectIdValue();
        if (projectIdForModel == null) {
            projectIdForModel = job.getProjectId();
        }
        if (projectIdForModel == null && job.getDatasetId() != null) {
            Dataset dataset = datasetRepository.findById(job.getDatasetId()).orElse(null);
            if (dataset != null) {
                projectIdForModel = dataset.getProjectId();
            }
        }
        
        Project project = null;
        if (projectIdForModel != null) {
            project = projectRepository.findById(projectIdForModel).orElse(null);
        }

        String modelName = (request != null && request.getDeploymentName() != null) 
                ? request.getDeploymentName() 
                : job.getJobName();

        Model model = Model.builder()
                .name(modelName)
                .algorithm(job.getAlgorithm())
                .algorithmDisplayName(job.getAlgorithmDisplayName())
                .problemType(job.getProblemType())
                .project(project)
                .datasetId(job.getDatasetId())
                .datasetName(job.getDatasetName())
                .targetVariable(job.getTargetVariable())
                .modelPath(job.getModelPath())
                .trainingJobId(job.getId())
                .source("TRAINING")
                .sourceJobId(job.getId())
                .accuracy(job.getBestAccuracy() != null ? job.getBestAccuracy() : job.getCurrentAccuracy())
                .isDeployed(true)
                .isProductionReady(true)
                .isBest(false)
                .deployedAt(LocalDateTime.now())
                .endpointUrl("/api/predictions/realtime/" + job.getModelPath())
                .build();

        return modelRepository.save(model);
    }

    /**
     * Get deployment status for a training job.
     */
    public TrainingJobDTO.DeploymentStatus getDeploymentStatus(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        Model model = null;
        if (job.getModelId() != null) {
            model = modelRepository.findById(job.getModelId()).orElse(null);
        }

        boolean isDeployed = model != null && Boolean.TRUE.equals(model.getIsDeployed());

        return TrainingJobDTO.DeploymentStatus.builder()
                .jobId(jobId)
                .modelId(model != null ? model.getId() : null)
                .isDeployed(isDeployed)
                .deployedAt(model != null ? model.getDeployedAt() : null)
                .endpointUrl(model != null ? model.getEndpointUrl() : null)
                .canDeploy(job.getStatus() == JobStatus.COMPLETED && job.getModelPath() != null)
                .build();
    }

    /**
     * Get results from a completed training job (like AutoML results).
     */
    public TrainingJobDTO.ResultsResponse getResults(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Training job not found: " + jobId));

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new IllegalStateException("Results only available for completed jobs");
        }

        Model model = null;
        if (job.getModelId() != null) {
            model = modelRepository.findById(job.getModelId()).orElse(null);
        }

        return TrainingJobDTO.ResultsResponse.builder()
                .jobId(jobId)
                .jobName(job.getJobName())
                .status(job.getStatus())
                .algorithm(job.getAlgorithm())
                .algorithmDisplayName(job.getAlgorithmDisplayName())
                .problemType(job.getProblemType())
                .accuracy(job.getBestAccuracy() != null ? job.getBestAccuracy() : job.getCurrentAccuracy())
                .accuracyLabel(formatAccuracy(job.getBestAccuracy() != null ? job.getBestAccuracy() : job.getCurrentAccuracy()))
                .modelId(model != null ? model.getId() : null)
                .modelPath(job.getModelPath())
                .isDeployed(model != null && Boolean.TRUE.equals(model.getIsDeployed()))
                .endpointUrl(model != null ? model.getEndpointUrl() : null)
                .datasetId(job.getDatasetId())
                .datasetName(job.getDatasetName())
                .targetVariable(job.getTargetVariable())
                .trainTestSplit(job.getTrainTestSplit())
                .crossValidationFolds(job.getCrossValidationFolds())
                .trainingDuration(job.getDurationSeconds())
                .trainingDurationLabel(formatDuration(job.getDurationSeconds()))
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    private String formatAccuracy(Double accuracy) {
        if (accuracy == null) return "N/A";
        return String.format("%.1f%%", accuracy * 100);
    }

    // ========== DTO CONVERTERS ==========

    private TrainingJobDTO.Response toResponse(TrainingJob job) {
        Map<String, Object> hyperparams = null;
        if (job.getHyperparametersJson() != null) {
            try {
                hyperparams = objectMapper.readValue(
                        job.getHyperparametersJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } catch (Exception e) {
                log.warn("Failed to parse hyperparameters", e);
            }
        }

        return TrainingJobDTO.Response.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .experimentName(job.getExperimentName())
                .status(job.getStatus())
                .statusLabel(formatStatus(job.getStatus()))
                .statusMessage(job.getStatusMessage())
                .progress(job.getProgress())
                .progressLabel(job.getProgress() + "/100")
                .currentEpoch(job.getCurrentEpoch())
                .totalEpochs(job.getTotalEpochs())
                .currentAccuracy(job.getCurrentAccuracy())
                .currentAccuracyLabel(job.getCurrentAccuracy() != null ? 
                        String.format("%.2f%%", job.getCurrentAccuracy() * 100) : null)
                .bestAccuracy(job.getBestAccuracy())
                .currentLoss(job.getCurrentLoss())
                .datasetId(job.getDatasetId())
                .datasetName(job.getDatasetName())
                .algorithm(job.getAlgorithm())
                .algorithmDisplayName(job.getAlgorithmDisplayName())
                .targetVariable(job.getTargetVariable())
                .problemType(job.getProblemType())
                .trainTestSplit(job.getTrainTestSplit())
                .crossValidationFolds(job.getCrossValidationFolds())
                .hyperparameters(hyperparams)
                .gpuAcceleration(job.getGpuAcceleration())
                .autoHyperparameterTuning(job.getAutoHyperparameterTuning())
                .earlyStopping(job.getEarlyStopping())
                .earlyStoppingPatience(job.getEarlyStoppingPatience())
                .batchSize(job.getBatchSize())
                .evaluationMetric(job.getEvaluationMetric())
                .startedAt(job.getStartedAt())
                .startedAtLabel(formatDateTime(job.getStartedAt()))
                .completedAt(job.getCompletedAt())
                .etaSeconds(job.getEtaSeconds())
                .etaLabel(formatEta(job.getEtaSeconds()))
                .durationSeconds(job.getDurationSeconds())
                .durationLabel(formatDuration(job.getDurationSeconds()))
                .modelId(job.getModelId())
                .computeResources(job.getComputeResources())
                .costEstimate(job.getCostEstimate())
                .costLabel(job.getCostEstimate() != null ? 
                        String.format("$%.2f", job.getCostEstimate()) : null)
                .errorMessage(job.getErrorMessage())
                .projectId(job.getProject() != null ? job.getProject().getId() : null)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private TrainingJobDTO.ListItem toListItem(TrainingJob job) {
        return TrainingJobDTO.ListItem.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .algorithm(job.getAlgorithm())
                .algorithmDisplayName(job.getAlgorithmDisplayName())
                .datasetName(job.getDatasetName())
                .status(job.getStatus())
                .statusLabel(formatStatus(job.getStatus()))
                .progress(job.getProgress())
                .progressLabel(job.getProgress() + "/100")
                .currentAccuracy(job.getCurrentAccuracy())
                .currentAccuracyLabel(job.getCurrentAccuracy() != null ? 
                        String.format("%.2f%%", job.getCurrentAccuracy() * 100) : null)
                .startedAt(job.getStartedAt())
                .startedAtLabel(formatDateTime(job.getStartedAt()))
                .etaLabel(formatEta(job.getEtaSeconds()))
                .build();
    }

    // ========== FORMATTERS ==========

    private String formatStatus(JobStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case QUEUED -> "Queued";
            case STARTING -> "Starting";
            case TRAINING -> "Training";
            case VALIDATING -> "Validating";
            case COMPLETED -> "Completed";
            case FAILED -> "Failed";
            case STOPPED -> "Stopped";
            case PAUSED -> "Paused";
        };
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatEta(Long seconds) {
        if (seconds == null || seconds <= 0) return null;
        if (seconds < 60) return seconds + " sec";
        if (seconds < 3600) return (seconds / 60) + " min";
        return (seconds / 3600) + " hr " + ((seconds % 3600) / 60) + " min";
    }

    private String formatDuration(Long seconds) {
        if (seconds == null) return null;
        if (seconds < 60) return seconds + " seconds";
        if (seconds < 3600) return (seconds / 60) + " minutes";
        return (seconds / 3600) + " hours " + ((seconds % 3600) / 60) + " minutes";
    }
}
