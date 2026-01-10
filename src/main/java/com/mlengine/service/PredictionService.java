package com.mlengine.service;

import com.mlengine.client.MLEngineClient;
import com.mlengine.model.dto.PredictionDTO;
import com.mlengine.model.entity.Activity;
import com.mlengine.model.entity.BatchPredictionJob;
import com.mlengine.model.entity.Model;
import com.mlengine.model.entity.Prediction;
import com.mlengine.model.enums.ProblemType;
import com.mlengine.repository.BatchPredictionJobRepository;
import com.mlengine.repository.ModelRepository;
import com.mlengine.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for Prediction operations.
 * Integrates with FastAPI ML Engine for REAL predictions!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final BatchPredictionJobRepository batchJobRepository;
    private final ModelRepository modelRepository;
    private final MLEngineClient mlEngineClient;
    private final ActivityService activityService;

    // ========== SINGLE PREDICTION ==========

    @Transactional
    public PredictionDTO.SingleResponse predictSingle(PredictionDTO.SingleRequest request) {
        long startTime = System.currentTimeMillis();
        
        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + request.getModelId()));

        log.info("🎯 Making REAL prediction with model: {} ({})", model.getName(), model.getAlgorithm());

        try {
            // Get FastAPI model ID from model path
            String fastApiModelId = model.getModelPath();
            if (fastApiModelId == null || fastApiModelId.isBlank()) {
                throw new IllegalStateException("Model has no FastAPI model ID. Please train with AutoML first.");
            }

            // Call FastAPI for real prediction
            Map<String, Object> fastApiResponse = mlEngineClient.predict(fastApiModelId, request.getFeatures());
            
            // Extract prediction result
            Object predictionRaw = fastApiResponse.get("prediction");
            Object probabilityRaw = fastApiResponse.get("probability");
            Object confidenceRaw = fastApiResponse.get("confidence");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> probabilities = (Map<String, Object>) fastApiResponse.get("probabilities");

            // Process prediction
            String predictedClass;
            Double probability = null;
            Double confidence = null;
            Double predictedValue = null;

            if (model.getProblemType() == ProblemType.CLASSIFICATION) {
                // Classification
                if (predictionRaw instanceof Number) {
                    int pred = ((Number) predictionRaw).intValue();
                    predictedClass = pred == 1 ? "Approved" : "Rejected";
                } else {
                    predictedClass = String.valueOf(predictionRaw);
                }

                if (probabilityRaw instanceof Number) {
                    probability = ((Number) probabilityRaw).doubleValue();
                } else if (probabilities != null && !probabilities.isEmpty()) {
                    // Get probability of predicted class
                    Object prob = probabilities.get(predictedClass);
                    if (prob == null) prob = probabilities.get("1");
                    if (prob instanceof Number) {
                        probability = ((Number) prob).doubleValue();
                    }
                }

                if (confidenceRaw instanceof Number) {
                    confidence = ((Number) confidenceRaw).doubleValue();
                } else if (probability != null) {
                    confidence = probability;
                }
            } else {
                // Regression
                if (predictionRaw instanceof Number) {
                    predictedValue = ((Number) predictionRaw).doubleValue();
                }
                predictedClass = predictedValue != null ? String.format("%.2f", predictedValue) : "N/A";
                confidence = 0.85; // Default confidence for regression
            }

            long processingTime = System.currentTimeMillis() - startTime;

            // Determine risk level
            String riskLevel = "Medium Risk";
            String riskColor = "yellow";
            if (probability != null) {
                if (probability > 0.7) {
                    riskLevel = "Low Risk";
                    riskColor = "green";
                } else if (probability < 0.3) {
                    riskLevel = "High Risk";
                    riskColor = "red";
                }
            }

            // Save prediction to database
            Prediction prediction = Prediction.builder()
                    .modelId(model.getId())
                    .modelName(model.getName())
                    .projectId(model.getProject() != null ? model.getProject().getId() : null)
                    .predictionType("single")
                    .inputJson(request.getFeatures().toString())
                    .predictedClass(predictedClass)
                    .probability(probability)
                    .confidence(confidence)
                    .processingTimeMs(processingTime)
                    .build();
            prediction = predictionRepository.save(prediction);
            
            // Record activity for prediction
            try {
                activityService.recordActivity(
                        Activity.ActivityType.PREDICTION_SINGLE,
                        "Prediction made",
                        model.getName() + " - " + predictedClass,
                        "System", null,
                        prediction.getId(), "PREDICTION", model.getName(),
                        model.getProject() != null ? model.getProject().getId() : null,
                        null
                );
            } catch (Exception activityEx) {
                log.warn("Failed to record prediction activity: {}", activityEx.getMessage());
            }

            log.info("✅ REAL prediction complete: {} with confidence {}", predictedClass, confidence);

            return PredictionDTO.SingleResponse.builder()
                    .predictionId(prediction.getId())
                    .modelId(model.getId())
                    .modelName(model.getName())
                    .predictedClass(predictedClass)
                    .predictedLabel(predictedClass)
                    .probability(probability)
                    .probabilityLabel(probability != null ? String.format("%.1f%%", probability * 100) : null)
                    .confidence(confidence)
                    .confidenceLabel(confidence != null && confidence > 0.7 ? "High Confidence" : "Medium Confidence")
                    .riskLevel(riskLevel)
                    .riskColor(riskColor)
                    .predictedValue(predictedValue)
                    .predictedValueLabel(predictedValue != null ? String.format("%.2f", predictedValue) : null)
                    .inputFeatures(request.getFeatures())
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Prediction failed: {}", e.getMessage(), e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    public PredictionDTO.SingleResponse getPrediction(String id) {
        Prediction prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prediction not found: " + id));

        return PredictionDTO.SingleResponse.builder()
                .predictionId(prediction.getId())
                .modelId(prediction.getModelId())
                .modelName(prediction.getModelName())
                .predictedClass(prediction.getPredictedClass())
                .predictedLabel(prediction.getPredictedClass())
                .probability(prediction.getProbability())
                .probabilityLabel(prediction.getProbability() != null ? 
                        String.format("%.1f%%", prediction.getProbability() * 100) : null)
                .confidence(prediction.getConfidence())
                .processingTimeMs(prediction.getProcessingTimeMs())
                .timestamp(prediction.getCreatedAt())
                .build();
    }

    // ========== BATCH PREDICTION ==========

    @Transactional
    public PredictionDTO.BatchResponse startBatchPrediction(
            String modelId, String jobName, MultipartFile file, String projectId) {
        
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        // Create batch job
        String name = jobName != null ? jobName : "Batch " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        BatchPredictionJob job = BatchPredictionJob.builder()
                .jobName(name)
                .modelId(model.getId())
                .modelName(model.getName())
                .projectId(projectId)
                .status("QUEUED")
                .inputFileName(file.getOriginalFilename())
                .totalRecords(0)
                .processedRecords(0)
                .failedRecords(0)
                .progress(0)
                .build();

        job = batchJobRepository.save(job);

        // Start async processing
        final String jobId = job.getId();
        new Thread(() -> processBatchJob(jobId, file, model)).start();

        return toBatchResponse(job);
    }

    private void processBatchJob(String jobId, MultipartFile file, Model model) {
        try {
            BatchPredictionJob job = batchJobRepository.findById(jobId).orElseThrow();
            job.setStatus("PROCESSING");
            job.setStartedAt(LocalDateTime.now());
            batchJobRepository.save(job);

            String fastApiModelId = model.getModelPath();
            if (fastApiModelId == null || fastApiModelId.isBlank()) {
                throw new IllegalStateException("Model has no FastAPI model ID");
            }

            // Read CSV
            List<Map<String, Object>> records = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String headerLine = reader.readLine();
                String[] headers = headerLine.split(",");

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    Map<String, Object> record = new HashMap<>();
                    for (int i = 0; i < headers.length && i < values.length; i++) {
                        String value = values[i].trim();
                        try {
                            record.put(headers[i].trim(), Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            record.put(headers[i].trim(), value);
                        }
                    }
                    records.add(record);
                }
            }

            job = batchJobRepository.findById(jobId).orElseThrow();
            job.setTotalRecords(records.size());
            batchJobRepository.save(job);

            // Call FastAPI batch prediction
            Map<String, Object> batchResponse = mlEngineClient.predictBatch(fastApiModelId, records);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) batchResponse.get("predictions");

            // Save results to CSV and to database
            Path outputDir = Path.of(System.getProperty("java.io.tmpdir"), "ml-predictions");
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve("predictions_" + jobId + ".csv");
            
            // Get projectId from model or job
            String projectId = job.getProjectId();
            if (projectId == null && model.getProject() != null) {
                projectId = model.getProject().getId();
            }

            try (PrintWriter writer = new PrintWriter(outputFile.toFile())) {
                writer.println("index,prediction,probability,confidence");
                for (int i = 0; i < predictions.size(); i++) {
                    Map<String, Object> pred = predictions.get(i);
                    Object predValue = pred.get("prediction");
                    Double probability = pred.get("probability") != null ? 
                            ((Number) pred.get("probability")).doubleValue() : null;
                    Double confidence = pred.get("confidence") != null ? 
                            ((Number) pred.get("confidence")).doubleValue() : 0.85;
                    
                    writer.printf("%d,%s,%.4f,%.4f%n",
                            i,
                            predValue,
                            probability != null ? probability : 0.0,
                            confidence);
                    
                    // Save to Prediction table (for counting)
                    Prediction prediction = Prediction.builder()
                            .modelId(model.getId())
                            .modelName(model.getName())
                            .projectId(projectId)
                            .predictionType("batch")
                            .batchId(jobId)
                            .batchIndex(i)
                            .predictedClass(predValue != null ? predValue.toString() : "N/A")
                            .probability(probability)
                            .confidence(confidence)
                            .build();
                    predictionRepository.save(prediction);
                }
            }

            // Update job
            job = batchJobRepository.findById(jobId).orElseThrow();
            job.setStatus("COMPLETED");
            job.setProcessedRecords(predictions.size());
            job.setProgress(100);
            job.setOutputFilePath(outputFile.toString());
            job.setOutputFileName("predictions_" + jobId + ".csv");
            job.setCompletedAt(LocalDateTime.now());
            if (job.getStartedAt() != null) {
                job.setProcessingTimeMs(
                        java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis());
            }
            batchJobRepository.save(job);
            
            // Record activity
            try {
                activityService.recordBatchPredictionCompleted(
                        jobId,
                        job.getJobName(),
                        predictions.size(),
                        "System",
                        projectId
                );
            } catch (Exception activityEx) {
                log.warn("Failed to record batch prediction activity: {}", activityEx.getMessage());
            }

            log.info("✅ Batch prediction completed: {} records", predictions.size());

        } catch (Exception e) {
            log.error("❌ Batch prediction failed", e);
            try {
                BatchPredictionJob job = batchJobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    job.setStatus("FAILED");
                    job.setErrorMessage(e.getMessage());
                    job.setCompletedAt(LocalDateTime.now());
                    batchJobRepository.save(job);
                }
            } catch (Exception ex) {
                log.error("Failed to update batch job status", ex);
            }
        }
    }

    public List<PredictionDTO.BatchListItem> getAllBatchJobs(String projectId) {
        List<BatchPredictionJob> jobs;
        if (projectId != null) {
            jobs = batchJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        } else {
            jobs = batchJobRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return jobs.stream()
                .map(this::toBatchListItem)
                .toList();
    }

    public PredictionDTO.BatchResponse getBatchJob(String jobId) {
        BatchPredictionJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + jobId));
        return toBatchResponse(job);
    }

    public String getBatchOutputPath(String jobId) {
        BatchPredictionJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + jobId));

        if (!"COMPLETED".equals(job.getStatus())) {
            throw new IllegalStateException("Batch job is not completed yet");
        }

        return job.getOutputFilePath();
    }

    // ========== PREDICTION HISTORY ==========

    public PredictionDTO.HistoryResponse getHistory(String projectId, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Prediction> predictionPage;
        if (projectId != null) {
            predictionPage = predictionRepository.findByProjectId(projectId, pageRequest);
        } else {
            predictionPage = predictionRepository.findAll(pageRequest);
        }

        List<PredictionDTO.HistoryItem> items = predictionPage.getContent().stream()
                .map(this::toHistoryItem)
                .toList();

        // Quick stats
        long totalPredictions = predictionRepository.count();
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayPredictions = predictionRepository.countByCreatedAtAfter(todayStart);

        return PredictionDTO.HistoryResponse.builder()
                .predictions(items)
                .total((int) predictionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .totalPages(predictionPage.getTotalPages())
                .totalPredictions(totalPredictions)
                .todayPredictions(todayPredictions)
                .build();
    }

    // ========== REALTIME PREDICTION ==========

    public PredictionDTO.RealtimeResponse predictRealtime(String modelId, Map<String, Object> features) {
        long startTime = System.currentTimeMillis();

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        log.info("⚡ Realtime prediction for model: {}", modelId);

        try {
            String fastApiModelId = model.getModelPath();
            if (fastApiModelId == null || fastApiModelId.isBlank()) {
                throw new IllegalStateException("Model has no FastAPI model ID");
            }

            Map<String, Object> response = mlEngineClient.predict(fastApiModelId, features);

            String prediction;
            Object predRaw = response.get("prediction");
            if (predRaw instanceof Number) {
                int pred = ((Number) predRaw).intValue();
                prediction = pred == 1 ? "Approved" : "Rejected";
            } else {
                prediction = String.valueOf(predRaw);
            }

            Double probability = response.get("probability") != null ? 
                    ((Number) response.get("probability")).doubleValue() : null;
            Double confidence = response.get("confidence") != null ? 
                    ((Number) response.get("confidence")).doubleValue() : probability;

            @SuppressWarnings("unchecked")
            Map<String, Object> probsRaw = (Map<String, Object>) response.get("probabilities");
            Map<String, Double> classProbabilities = new HashMap<>();
            if (probsRaw != null) {
                probsRaw.forEach((k, v) -> {
                    if (v instanceof Number) {
                        classProbabilities.put(k, ((Number) v).doubleValue());
                    }
                });
            }

            long latency = System.currentTimeMillis() - startTime;

            log.info("✅ Realtime prediction: {} ({}ms)", prediction, latency);

            return PredictionDTO.RealtimeResponse.builder()
                    .prediction(prediction)
                    .probability(probability)
                    .confidence(confidence)
                    .classProbabilities(classProbabilities)
                    .latencyMs(latency)
                    .build();

        } catch (Exception e) {
            log.error("❌ Realtime prediction failed", e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    // ========== HELPER METHODS ==========

    private PredictionDTO.BatchResponse toBatchResponse(BatchPredictionJob job) {
        return PredictionDTO.BatchResponse.builder()
                .jobId(job.getId())
                .jobName(job.getJobName())
                .modelId(job.getModelId())
                .modelName(job.getModelName())
                .status(job.getStatus())
                .statusLabel(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .processedRecords(job.getProcessedRecords())
                .failedRecords(job.getFailedRecords())
                .progress(job.getProgress())
                .progressLabel(String.format("%d / %d", 
                        job.getProcessedRecords() != null ? job.getProcessedRecords() : 0,
                        job.getTotalRecords() != null ? job.getTotalRecords() : 0))
                .inputFileName(job.getInputFileName())
                .outputFileName(job.getOutputFileName())
                .downloadUrl(job.getOutputFilePath() != null ? 
                        "/api/predictions/batch/" + job.getId() + "/download" : null)
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .processingTimeMs(job.getProcessingTimeMs())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private PredictionDTO.BatchListItem toBatchListItem(BatchPredictionJob job) {
        return PredictionDTO.BatchListItem.builder()
                .jobId(job.getId())
                .jobName(job.getJobName())
                .modelName(job.getModelName())
                .status(job.getStatus())
                .statusLabel(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .progress(job.getProgress())
                .progressLabel(String.format("%d%%", job.getProgress() != null ? job.getProgress() : 0))
                .createdAt(job.getCreatedAt())
                .build();
    }

    private PredictionDTO.HistoryItem toHistoryItem(Prediction prediction) {
        String riskLevel = "Medium Risk";
        String riskColor = "yellow";
        if (prediction.getProbability() != null) {
            if (prediction.getProbability() > 0.7) {
                riskLevel = "Low Risk";
                riskColor = "green";
            } else if (prediction.getProbability() < 0.3) {
                riskLevel = "High Risk";
                riskColor = "red";
            }
        }

        return PredictionDTO.HistoryItem.builder()
                .predictionId(prediction.getId())
                .modelId(prediction.getModelId())
                .modelName(prediction.getModelName())
                .predictionType(prediction.getPredictionType() != null ? prediction.getPredictionType() : "single")
                .predictedClass(prediction.getPredictedClass())
                .probability(prediction.getProbability())
                .probabilityLabel(prediction.getProbability() != null ? 
                        String.format("%.1f%%", prediction.getProbability() * 100) : null)
                .riskLevel(riskLevel)
                .riskColor(riskColor)
                .timestamp(prediction.getCreatedAt())
                .build();
    }
}
