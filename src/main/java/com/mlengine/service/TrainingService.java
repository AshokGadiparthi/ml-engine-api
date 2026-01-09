package com.mlengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.config.MLEngineConfig;
import com.mlengine.model.dto.TrainingJobDTO;
import com.mlengine.model.entity.Dataset;
import com.mlengine.model.entity.Model;
import com.mlengine.model.entity.Project;
import com.mlengine.model.entity.TrainingJob;
import com.mlengine.model.enums.JobStatus;
import com.mlengine.repository.DatasetRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.ProjectRepository;
import com.mlengine.repository.TrainingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for training job operations.
 * Handles async training with Python ML Engine.
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
    private final MLEngineConfig config;
    private final ObjectMapper objectMapper;

    // Track running processes
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    /**
     * Start a new training job.
     */
    @Transactional
    public TrainingJobDTO.Response startTraining(TrainingJobDTO.CreateRequest request) {
        log.info("Starting training job: {} with algorithm {}", request.getExperimentName(), request.getAlgorithm());

        // Validate dataset
        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + request.getDatasetId()));

        // Validate project
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));
        }

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

        // Create training job
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
                .build();

        job = trainingJobRepository.save(job);
        log.info("Created training job: {}", job.getId());

        // Start training asynchronously
        executeTrainingAsync(job.getId());

        return toResponse(job);
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

        // Stop the process if running
        Process process = runningProcesses.get(jobId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningProcesses.remove(jobId);
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

        job.setStatus(JobStatus.QUEUED);
        job.setStatusMessage("Resuming training");
        job = trainingJobRepository.save(job);

        // Restart training
        executeTrainingAsync(jobId);

        return toResponse(job);
    }

    /**
     * Delete a training job.
     */
    @Transactional
    public void deleteJob(String jobId) {
        // Stop if running
        Process process = runningProcesses.get(jobId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningProcesses.remove(jobId);
        }

        trainingJobRepository.deleteById(jobId);
        log.info("Deleted training job: {}", jobId);
    }

    // ========== ASYNC TRAINING EXECUTION ==========

    @Async
    protected void executeTrainingAsync(String jobId) {
        try {
            Thread.sleep(100);  // Ensure transaction is committed
            executeTraining(jobId);
        } catch (Exception e) {
            log.error("Training execution failed", e);
            updateJobStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }

    private void executeTraining(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        try {
            log.info("Starting training execution for job: {}", jobId);

            // Update status to training
            job.setStatus(JobStatus.TRAINING);
            job.setStartedAt(LocalDateTime.now());
            job.setStatusMessage("Training started");
            trainingJobRepository.save(job);

            // Get dataset
            Dataset dataset = datasetRepository.findById(job.getDatasetId()).orElse(null);
            if (dataset == null || dataset.getFilePath() == null) {
                throw new IllegalStateException("Dataset file not found");
            }

            // Build Python command
            String command = buildTrainingCommand(job, dataset);
            log.info("Training command: {}", command);

            // Execute Python training
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            runningProcesses.put(jobId, process);

            // Read output and update progress
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Training output: {}", line);
                    parseTrainingOutput(jobId, line);
                }
            }

            int exitCode = process.waitFor();
            runningProcesses.remove(jobId);

            // Update final status
            job = trainingJobRepository.findById(jobId).orElse(job);
            if (exitCode == 0) {
                job.setStatus(JobStatus.COMPLETED);
                job.setProgress(100);
                job.setStatusMessage("Training completed successfully");

                // Create model record
                createModelFromJob(job);
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setStatusMessage("Training failed with exit code: " + exitCode);
            }

            job.setCompletedAt(LocalDateTime.now());
            job.setDurationSeconds(ChronoUnit.SECONDS.between(job.getStartedAt(), LocalDateTime.now()));
            trainingJobRepository.save(job);

            log.info("Training completed for job: {} with status: {}", jobId, job.getStatus());

        } catch (Exception e) {
            log.error("Training failed for job: {}", jobId, e);
            updateJobStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }

    private String buildTrainingCommand(TrainingJob job, Dataset dataset) {
        StringBuilder cmd = new StringBuilder();
        cmd.append(config.getPythonPath()).append(" -c \"");
        cmd.append("from ml_engine import train; ");
        cmd.append("result = train(");
        cmd.append("data_path='").append(dataset.getFilePath()).append("', ");
        cmd.append("target='").append(job.getTargetVariable()).append("', ");
        cmd.append("algorithm='").append(job.getAlgorithm()).append("', ");
        cmd.append("problem_type='").append(job.getProblemType().name().toLowerCase()).append("', ");
        cmd.append("test_size=").append(1.0 - job.getTrainTestSplit()).append(", ");
        cmd.append("output_dir='").append(config.getModelsDir()).append("'");
        cmd.append("); ");
        cmd.append("print('RESULT:', result)");
        cmd.append("\"");

        return cmd.toString();
    }

    private void parseTrainingOutput(String jobId, String line) {
        try {
            TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
            if (job == null) return;

            // Parse progress updates
            if (line.contains("PROGRESS:")) {
                int progress = Integer.parseInt(line.split("PROGRESS:")[1].trim().split(" ")[0]);
                job.setProgress(progress);
            }
            if (line.contains("EPOCH:")) {
                String[] parts = line.split("EPOCH:")[1].trim().split("/");
                job.setCurrentEpoch(Integer.parseInt(parts[0]));
                if (parts.length > 1) {
                    job.setTotalEpochs(Integer.parseInt(parts[1].split(" ")[0]));
                }
            }
            if (line.contains("ACCURACY:")) {
                double accuracy = Double.parseDouble(line.split("ACCURACY:")[1].trim().split(" ")[0]);
                job.setCurrentAccuracy(accuracy);
                if (job.getBestAccuracy() == null || accuracy > job.getBestAccuracy()) {
                    job.setBestAccuracy(accuracy);
                }
            }
            if (line.contains("LOSS:")) {
                double loss = Double.parseDouble(line.split("LOSS:")[1].trim().split(" ")[0]);
                job.setCurrentLoss(loss);
            }
            if (line.contains("RESULT:")) {
                job.setMetricsJson(line.split("RESULT:")[1].trim());
            }

            trainingJobRepository.save(job);

        } catch (Exception e) {
            log.debug("Failed to parse training output: {}", line);
        }
    }

    private void createModelFromJob(TrainingJob job) {
        try {
            // Parse metrics from job
            Map<String, Object> metrics = new HashMap<>();
            if (job.getMetricsJson() != null) {
                try {
                    metrics = objectMapper.readValue(job.getMetricsJson(), new TypeReference<>() {});
                } catch (Exception ignored) {}
            }

            String algorithmDisplayName = algorithmService.getAlgorithmDisplayName(job.getAlgorithm());

            Model model = Model.builder()
                    .name(algorithmDisplayName + " v1.0")
                    .description("Trained on " + job.getDatasetName())
                    .version("1.0")
                    .algorithm(job.getAlgorithm())
                    .algorithmDisplayName(algorithmDisplayName)
                    .problemType(job.getProblemType())
                    .trainingJobId(job.getId())
                    .datasetId(job.getDatasetId())
                    .datasetName(job.getDatasetName())
                    .targetVariable(job.getTargetVariable())
                    .accuracy(job.getBestAccuracy())
                    .metricsJson(job.getMetricsJson())
                    .hyperparametersJson(job.getHyperparametersJson())
                    .trainingTimeSeconds(job.getDurationSeconds())
                    .crossValidationFolds(job.getCrossValidationFolds())
                    .project(job.getProject())
                    .build();

            // Set additional metrics if available
            if (metrics.containsKey("f1_score")) {
                model.setF1Score((Double) metrics.get("f1_score"));
            }
            if (metrics.containsKey("precision")) {
                model.setPrecisionScore((Double) metrics.get("precision"));
            }
            if (metrics.containsKey("recall")) {
                model.setRecall((Double) metrics.get("recall"));
            }

            model = modelRepository.save(model);
            job.setModelId(model.getId());
            trainingJobRepository.save(job);

            log.info("Created model {} from training job {}", model.getId(), job.getId());

        } catch (Exception e) {
            log.error("Failed to create model from job", e);
        }
    }

    private void updateJobStatus(String jobId, JobStatus status, String message) {
        try {
            TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.setStatus(status);
                job.setStatusMessage(message);
                job.setErrorMessage(message);
                job.setCompletedAt(LocalDateTime.now());
                trainingJobRepository.save(job);
            }
        } catch (Exception e) {
            log.error("Failed to update job status", e);
        }
    }

    // ========== DTO CONVERTERS ==========

    private TrainingJobDTO.Response toResponse(TrainingJob job) {
        Map<String, Object> hyperparams = null;
        if (job.getHyperparametersJson() != null) {
            try {
                hyperparams = objectMapper.readValue(job.getHyperparametersJson(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        Map<String, Object> metrics = null;
        if (job.getMetricsJson() != null) {
            try {
                metrics = objectMapper.readValue(job.getMetricsJson(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }

        return TrainingJobDTO.Response.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .experimentName(job.getExperimentName())
                .status(job.getStatus())
                .statusLabel(formatStatus(job.getStatus()))
                .statusMessage(job.getStatusMessage())
                .progress(job.getProgress())
                .progressLabel(job.getCurrentEpoch() + "/" + job.getTotalEpochs())
                .currentEpoch(job.getCurrentEpoch())
                .totalEpochs(job.getTotalEpochs())
                .currentAccuracy(job.getCurrentAccuracy())
                .currentAccuracyLabel(formatPercent(job.getCurrentAccuracy()))
                .bestAccuracy(job.getBestAccuracy())
                .currentLoss(job.getCurrentLoss())
                .datasetId(job.getDatasetId())
                .datasetName(job.getDatasetName())
                .algorithm(job.getAlgorithm())
                .algorithmDisplayName(algorithmService.getAlgorithmDisplayName(job.getAlgorithm()))
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
                .metrics(metrics)
                .computeResources(job.getComputeResources())
                .costEstimate(job.getCostEstimate())
                .costLabel(job.getCostEstimate() != null ? String.format("$%.2f", job.getCostEstimate()) : null)
                .errorMessage(job.getErrorMessage())
                .projectId(job.getProjectId())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private TrainingJobDTO.ListItem toListItem(TrainingJob job) {
        return TrainingJobDTO.ListItem.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .algorithm(job.getAlgorithm())
                .algorithmDisplayName(algorithmService.getAlgorithmDisplayName(job.getAlgorithm()))
                .datasetName(job.getDatasetName())
                .status(job.getStatus())
                .statusLabel(formatStatus(job.getStatus()))
                .progress(job.getProgress())
                .progressLabel(job.getCurrentEpoch() + "/" + job.getTotalEpochs())
                .currentAccuracy(job.getCurrentAccuracy())
                .currentAccuracyLabel(formatPercent(job.getCurrentAccuracy()))
                .startedAt(job.getStartedAt())
                .startedAtLabel(formatDateTime(job.getStartedAt()))
                .etaLabel(formatEta(job.getEtaSeconds()))
                .build();
    }

    private String formatStatus(JobStatus status) {
        if (status == null) return "Unknown";
        return status.name().charAt(0) + status.name().substring(1).toLowerCase();
    }

    private String formatPercent(Double value) {
        if (value == null) return "0%";
        return String.format("%.1f%%", value * 100);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatEta(Long seconds) {
        if (seconds == null) return "Calculating...";
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
