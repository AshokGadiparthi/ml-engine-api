package com.mlengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Prediction operations.
 * Supports ALL features in the Predictions UI screens.
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
    private final ObjectMapper objectMapper;

    // ========== SINGLE PREDICTION ==========

    @Transactional
    public PredictionDTO.SingleResponse predictSingle(PredictionDTO.SingleRequest request) {
        long startTime = System.currentTimeMillis();
        
        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + request.getModelId()));

        log.info("🎯 Making REAL prediction with model: {} ({})", model.getName(), model.getAlgorithm());

        // Validate model has FastAPI model ID
        String fastApiModelId = model.getModelPath();
        if (fastApiModelId == null || fastApiModelId.isBlank()) {
            throw new IllegalStateException(
                "Model '" + model.getName() + "' is not ready for predictions. " +
                "The model needs to be trained via AutoML or Model Training first. " +
                "Please train a new model or select a model that has been properly trained.");
        }

        try {
            // Call FastAPI for REAL prediction
            Map<String, Object> fastApiResponse = mlEngineClient.predict(fastApiModelId, request.getFeatures());
            
            Object predictionRaw = fastApiResponse.get("prediction");
            Object probabilityRaw = fastApiResponse.get("probability");
            Object confidenceRaw = fastApiResponse.get("confidence");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> probsRaw = (Map<String, Object>) fastApiResponse.get("probabilities");

            String predictedClass;
            String predictedLabel;
            Double probability = null;
            Double confidence = null;
            Double predictedValue = null;
            Map<String, Double> probabilities = new HashMap<>();

            if (model.getProblemType() == ProblemType.CLASSIFICATION) {
                if (predictionRaw instanceof Number) {
                    int pred = ((Number) predictionRaw).intValue();
                    predictedClass = pred == 1 ? "Approved" : "Rejected";
                    predictedLabel = predictedClass;
                } else {
                    predictedClass = String.valueOf(predictionRaw);
                    predictedLabel = capitalize(predictedClass);
                }

                if (probabilityRaw instanceof Number) {
                    probability = ((Number) probabilityRaw).doubleValue();
                }

                if (probsRaw != null) {
                    for (Map.Entry<String, Object> entry : probsRaw.entrySet()) {
                        if (entry.getValue() instanceof Number) {
                            probabilities.put(capitalize(entry.getKey()), ((Number) entry.getValue()).doubleValue());
                        }
                    }
                }

                if (probability == null && !probabilities.isEmpty()) {
                    probability = probabilities.get(predictedLabel);
                    if (probability == null) {
                        probability = probabilities.values().stream().max(Double::compareTo).orElse(null);
                    }
                }

                if (confidenceRaw instanceof Number) {
                    confidence = ((Number) confidenceRaw).doubleValue();
                } else if (probability != null) {
                    confidence = probability;
                }
            } else {
                if (predictionRaw instanceof Number) {
                    predictedValue = ((Number) predictionRaw).doubleValue();
                }
                predictedClass = predictedValue != null ? String.format("%.2f", predictedValue) : "N/A";
                predictedLabel = predictedClass;
                confidence = 0.85;
            }

            long processingTime = System.currentTimeMillis() - startTime;

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

            String probabilitiesJson = null;
            try {
                if (!probabilities.isEmpty()) {
                    probabilitiesJson = objectMapper.writeValueAsString(probabilities);
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize probabilities", e);
            }

            Prediction prediction = Prediction.builder()
                    .modelId(model.getId())
                    .modelName(model.getName())
                    .projectId(request.getProjectId() != null ? request.getProjectId() : 
                              (model.getProject() != null ? model.getProject().getId() : null))
                    .predictionType("Single")
                    .source(request.getSource() != null ? request.getSource() : "UI")
                    .inputJson(serializeToJson(request.getFeatures()))
                    .predictedClass(predictedClass)
                    .predictedLabel(predictedLabel)
                    .probability(probability)
                    .confidence(confidence)
                    .probabilitiesJson(probabilitiesJson)
                    .riskLevel(riskLevel)
                    .predictedValue(predictedValue)
                    .processingTimeMs(processingTime)
                    .build();
            prediction = predictionRepository.save(prediction);
            
            try {
                activityService.recordActivity(
                        Activity.ActivityType.PREDICTION_SINGLE,
                        "Prediction made",
                        model.getName() + " - " + predictedLabel,
                        "System", null,
                        prediction.getId(), "PREDICTION", model.getName(),
                        prediction.getProjectId(),
                        null
                );
            } catch (Exception activityEx) {
                log.warn("Failed to record prediction activity: {}", activityEx.getMessage());
            }

            log.info("✅ REAL prediction complete: {} with confidence {}", predictedLabel, confidence);

            return PredictionDTO.SingleResponse.builder()
                    .predictionId(prediction.getId())
                    .modelId(model.getId())
                    .modelName(model.getName())
                    .predictedClass(predictedClass)
                    .predictedLabel(predictedLabel)
                    .probability(probability)
                    .probabilityLabel(probability != null ? String.format("%.1f%%", probability * 100) : null)
                    .confidence(confidence)
                    .confidenceLabel(confidence != null && confidence > 0.7 ? "High Confidence" : "Medium Confidence")
                    .probabilities(probabilities)
                    .riskLevel(riskLevel)
                    .riskColor(riskColor)
                    .predictedValue(predictedValue)
                    .predictedValueLabel(predictedValue != null ? String.format("%.2f", predictedValue) : null)
                    .inputFeatures(request.getFeatures())
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now())
                    .source(prediction.getSource())
                    .build();

        } catch (Exception e) {
            log.error("❌ Prediction failed: {}", e.getMessage(), e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    // ========== GET PREDICTION DETAIL ==========

    public PredictionDTO.PredictionDetail getPredictionDetail(String id) {
        Prediction prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prediction not found: " + id));

        Map<String, Object> inputFeatures = parseJson(prediction.getInputJson());
        
        List<PredictionDTO.FeatureDisplay> inputFeaturesDisplay = new ArrayList<>();
        if (inputFeatures != null) {
            for (Map.Entry<String, Object> entry : inputFeatures.entrySet()) {
                inputFeaturesDisplay.add(PredictionDTO.FeatureDisplay.builder()
                        .name(entry.getKey())
                        .label(formatFeatureName(entry.getKey()))
                        .value(entry.getValue())
                        .valueLabel(formatFeatureValue(entry.getValue()))
                        .build());
            }
        }

        Map<String, Double> probabilities = new HashMap<>();
        if (prediction.getProbabilitiesJson() != null) {
            try {
                probabilities = objectMapper.readValue(prediction.getProbabilitiesJson(), 
                        new TypeReference<Map<String, Double>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse probabilities", e);
            }
        }

        return PredictionDTO.PredictionDetail.builder()
                .predictionId(prediction.getId())
                .modelId(prediction.getModelId())
                .modelName(prediction.getModelName())
                .predictionType(prediction.getPredictionType())
                .predictionTypeLabel(prediction.getPredictionType())
                .timestamp(prediction.getCreatedAt())
                .timestampLabel(formatTimestamp(prediction.getCreatedAt()))
                .inputFeatures(inputFeatures)
                .inputFeaturesDisplay(inputFeaturesDisplay)
                .predictedClass(prediction.getPredictedClass())
                .predictedLabel(prediction.getPredictedLabel() != null ? 
                        prediction.getPredictedLabel() : prediction.getPredictedClass())
                .confidence(prediction.getConfidence())
                .confidenceLabel(prediction.getConfidence() != null ? 
                        String.format("%.1f%%", prediction.getConfidence() * 100) : null)
                .probabilities(probabilities)
                .riskLevel(prediction.getRiskLevel())
                .riskColor(getRiskColor(prediction.getRiskLevel()))
                .processingTimeMs(prediction.getProcessingTimeMs())
                .source(prediction.getSource())
                .build();
    }

    // ========== MODEL PREDICTION STATS ==========

    public PredictionDTO.ModelPredictionStats getModelPredictionStats(String modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));

        Long totalPredictions = predictionRepository.countByModelId(modelId);
        
        Map<String, Long> resultCounts = new LinkedHashMap<>();
        List<Prediction> recentPredictions = predictionRepository.findTop10ByModelIdOrderByCreatedAtDesc(modelId);
        
        List<String> distinctResults = recentPredictions.stream()
                .map(Prediction::getPredictedClass)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        for (String result : distinctResults) {
            long count = recentPredictions.stream()
                    .filter(p -> result.equals(p.getPredictedClass()))
                    .count();
            resultCounts.put(result, count);
        }

        return PredictionDTO.ModelPredictionStats.builder()
                .modelId(model.getId())
                .modelName(model.getName())
                .algorithm(model.getAlgorithm())
                .accuracy(model.getAccuracy())
                .accuracyLabel(model.getAccuracy() != null ? String.format("%.1f%%", model.getAccuracy() * 100) : null)
                .trainedAt(model.getCreatedAt())
                .trainedAtLabel(formatTimestamp(model.getCreatedAt()))
                .totalPredictions(totalPredictions != null ? totalPredictions : 0L)
                .resultCounts(resultCounts)
                .build();
    }

    // ========== PREDICTION HISTORY WITH FILTERING ==========

    public PredictionDTO.HistoryResponse getHistory(PredictionDTO.HistoryFilter filter) {
        int page = filter.getPage() != null ? filter.getPage() : 0;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 20;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        if (filter.getDateRange() != null && startDate == null) {
            LocalDateTime now = LocalDateTime.now();
            switch (filter.getDateRange().toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    endDate = now;
                    break;
                case "7days":
                    startDate = now.minusDays(7);
                    endDate = now;
                    break;
                case "30days":
                    startDate = now.minusDays(30);
                    endDate = now;
                    break;
            }
        }

        Page<Prediction> predictionPage = predictionRepository.findWithFilters(
                filter.getProjectId(),
                filter.getType(),
                filter.getModelId(),
                filter.getResult(),
                startDate,
                endDate,
                pageRequest
        );

        List<PredictionDTO.HistoryItem> items = predictionPage.getContent().stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());

        PredictionDTO.HistoryStats stats = calculateHistoryStats(filter, startDate, endDate);

        return PredictionDTO.HistoryResponse.builder()
                .predictions(items)
                .total((int) predictionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .totalPages(predictionPage.getTotalPages())
                .stats(stats)
                .build();
    }

    private PredictionDTO.HistoryStats calculateHistoryStats(PredictionDTO.HistoryFilter filter, 
                                                              LocalDateTime startDate, LocalDateTime endDate) {
        String projectId = filter.getProjectId();
        
        Long total = predictionRepository.countWithFilters(
                projectId, filter.getType(), filter.getModelId(), filter.getResult(), startDate, endDate);

        List<String> distinctResults = predictionRepository.findDistinctResultsByProject(projectId);
        Map<String, Long> resultCounts = new LinkedHashMap<>();
        Map<String, Double> resultPercentages = new LinkedHashMap<>();
        
        for (String result : distinctResults) {
            Long count = predictionRepository.countByProjectAndResult(projectId, result);
            resultCounts.put(result, count);
            if (total != null && total > 0) {
                resultPercentages.put(result, (count * 100.0) / total);
            }
        }

        Long singleCount = predictionRepository.countByProjectAndType(projectId, "Single");
        Long batchCount = predictionRepository.countByProjectAndType(projectId, "Batch");
        Long apiCount = predictionRepository.countByProjectAndType(projectId, "API");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);

        Long todayCount = predictionRepository.countByProjectSince(projectId, todayStart);
        Long thisWeekCount = predictionRepository.countByProjectSince(projectId, weekStart);
        Long thisMonthCount = predictionRepository.countByProjectSince(projectId, monthStart);

        Double avgConfidence = predictionRepository.avgConfidenceByProject(projectId);

        return PredictionDTO.HistoryStats.builder()
                .totalPredictions(total != null ? total : 0L)
                .totalLabel(formatNumber(total != null ? total : 0L))
                .resultCounts(resultCounts)
                .resultPercentages(resultPercentages)
                .singleCount(singleCount != null ? singleCount : 0L)
                .batchCount(batchCount != null ? batchCount : 0L)
                .apiCount(apiCount != null ? apiCount : 0L)
                .todayCount(todayCount != null ? todayCount : 0L)
                .thisWeekCount(thisWeekCount != null ? thisWeekCount : 0L)
                .thisMonthCount(thisMonthCount != null ? thisMonthCount : 0L)
                .avgConfidence(avgConfidence)
                .avgConfidenceLabel(avgConfidence != null ? String.format("%.1f%%", avgConfidence * 100) : null)
                .build();
    }

    // ========== BATCH PREDICTION ==========

    @Transactional
    public PredictionDTO.BatchResponse startBatchPrediction(
            String modelId, String jobName, MultipartFile file, String projectId,
            boolean includeConfidence, boolean includeProbabilities) {
        
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        // Validate model has FastAPI model ID
        if (model.getModelPath() == null || model.getModelPath().isBlank()) {
            throw new IllegalStateException(
                "Model '" + model.getName() + "' is not ready for predictions. " +
                "Please train the model first using AutoML or Model Training.");
        }

        String name = jobName != null ? jobName : "Batch " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        BatchPredictionJob job = BatchPredictionJob.builder()
                .jobName(name)
                .modelId(model.getId())
                .modelName(model.getName())
                .projectId(projectId)
                .status("PENDING")
                .inputFileName(file.getOriginalFilename())
                .totalRecords(0)
                .processedRecords(0)
                .failedRecords(0)
                .progress(0)
                .build();

        job = batchJobRepository.save(job);

        final String jobId = job.getId();
        new Thread(() -> processBatchJob(jobId, file, model)).start();

        return toBatchResponse(job);
    }

    public PredictionDTO.BatchValidation validateBatchFile(String modelId, MultipartFile file) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> foundColumns = new ArrayList<>();
        List<Map<String, Object>> previewRows = new ArrayList<>();
        int totalRows = 0;
        int rowsWithMissing = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                errors.add("File is empty");
                return buildValidationResult(false, file.getOriginalFilename(), 0, 
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 
                        0, Collections.emptyList(), warnings, errors);
            }

            String[] headers = headerLine.split(",");
            for (String h : headers) {
                foundColumns.add(h.trim());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                totalRows++;
                String[] values = line.split(",");
                
                boolean hasMissing = false;
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String value = i < values.length ? values[i].trim() : "";
                    row.put(headers[i].trim(), value);
                    if (value.isEmpty()) hasMissing = true;
                }
                
                if (hasMissing) rowsWithMissing++;
                
                if (previewRows.size() < 5) {
                    previewRows.add(row);
                }
            }

            if (rowsWithMissing > 0) {
                warnings.add(rowsWithMissing + " rows have missing values (will skip)");
            }

        } catch (Exception e) {
            errors.add("Failed to read file: " + e.getMessage());
        }

        List<String> requiredColumns = new ArrayList<>(foundColumns);
        List<String> missingColumns = Collections.emptyList();

        return buildValidationResult(errors.isEmpty(), file.getOriginalFilename(), totalRows,
                requiredColumns, foundColumns, missingColumns, rowsWithMissing, 
                previewRows, warnings, errors);
    }

    private PredictionDTO.BatchValidation buildValidationResult(
            boolean valid, String fileName, int totalRows, 
            List<String> requiredColumns, List<String> foundColumns, List<String> missingColumns,
            int rowsWithMissing, List<Map<String, Object>> previewRows,
            List<String> warnings, List<String> errors) {
        
        return PredictionDTO.BatchValidation.builder()
                .valid(valid)
                .fileName(fileName)
                .totalRows(totalRows)
                .requiredColumns(requiredColumns)
                .foundColumns(foundColumns)
                .missingColumns(missingColumns)
                .rowsWithMissingValues(rowsWithMissing)
                .previewRows(previewRows)
                .warnings(warnings)
                .errors(errors)
                .build();
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

            Map<String, Object> batchResponse = mlEngineClient.predictBatch(fastApiModelId, records);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) batchResponse.get("predictions");

            Path outputDir = Path.of(System.getProperty("java.io.tmpdir"), "ml-predictions");
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve("predictions_" + jobId + ".csv");
            
            String projectId = job.getProjectId();
            if (projectId == null && model.getProject() != null) {
                projectId = model.getProject().getId();
            }

            Map<String, Integer> resultCounts = new HashMap<>();

            try (PrintWriter writer = new PrintWriter(outputFile.toFile())) {
                writer.println("index,prediction,probability,confidence");
                for (int i = 0; i < predictions.size(); i++) {
                    Map<String, Object> pred = predictions.get(i);
                    Object predValue = pred.get("prediction");
                    Double probability = pred.get("probability") != null ? 
                            ((Number) pred.get("probability")).doubleValue() : null;
                    Double confidence = pred.get("confidence") != null ? 
                            ((Number) pred.get("confidence")).doubleValue() : 0.85;
                    
                    String predClass = predValue != null ? String.valueOf(predValue) : "N/A";
                    String predLabel = predValue instanceof Number ? 
                            (((Number) predValue).intValue() == 1 ? "Approved" : "Rejected") : predClass;
                    
                    writer.printf("%d,%s,%.4f,%.4f%n",
                            i,
                            predLabel,
                            probability != null ? probability : 0.0,
                            confidence);

                    resultCounts.merge(predLabel, 1, Integer::sum);
                    
                    Prediction prediction = Prediction.builder()
                            .modelId(model.getId())
                            .modelName(model.getName())
                            .projectId(projectId)
                            .predictionType("Batch")
                            .source("BATCH")
                            .batchId(jobId)
                            .batchIndex(i)
                            .predictedClass(predLabel)
                            .predictedLabel(predLabel)
                            .probability(probability)
                            .confidence(confidence)
                            .build();
                    predictionRepository.save(prediction);
                }
            }

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
            
            try {
                job.setResultSummary(objectMapper.writeValueAsString(resultCounts));
            } catch (Exception e) {
                log.warn("Failed to serialize result summary", e);
            }
            
            batchJobRepository.save(job);
            
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

    public byte[] generateTemplateCSV(String modelId) {
        StringBuilder sb = new StringBuilder();
        sb.append("age,annual_income,credit_score,loan_amount,employment_years,existing_loans\n");
        sb.append("35,75000,720,250000,8,1\n");
        sb.append("42,92000,680,180000,12,2\n");
        return sb.toString().getBytes();
    }

    // ========== REALTIME PREDICTION (FOR API) ==========

    public PredictionDTO.RealtimeResponse predictRealtime(String modelId, Map<String, Object> features, 
                                                          String apiKeyValue, String projectId) {
        long startTime = System.currentTimeMillis();

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        log.info("⚡ Realtime API prediction for model: {}", modelId);

        // Validate model has FastAPI model ID
        String fastApiModelId = model.getModelPath();
        if (fastApiModelId == null || fastApiModelId.isBlank()) {
            throw new IllegalStateException(
                "Model '" + model.getName() + "' is not ready for predictions. " +
                "Please train the model first using AutoML or Model Training.");
        }

        try {
            // Call FastAPI for REAL prediction
            Map<String, Object> response = mlEngineClient.predict(fastApiModelId, features);

            String prediction;
            Object predRaw = response.get("prediction");
            if (predRaw instanceof Number) {
                int pred = ((Number) predRaw).intValue();
                prediction = pred == 1 ? "approved" : "rejected";
            } else {
                prediction = String.valueOf(predRaw).toLowerCase();
            }

            Double probability = response.get("probability") != null ? 
                    ((Number) response.get("probability")).doubleValue() : null;
            Double confidence = response.get("confidence") != null ? 
                    ((Number) response.get("confidence")).doubleValue() : probability;

            @SuppressWarnings("unchecked")
            Map<String, Object> probsRaw = (Map<String, Object>) response.get("probabilities");
            Map<String, Double> probabilities = new HashMap<>();
            if (probsRaw != null) {
                probsRaw.forEach((k, v) -> {
                    if (v instanceof Number) {
                        probabilities.put(k.toLowerCase(), ((Number) v).doubleValue());
                    }
                });
            }

            long latency = System.currentTimeMillis() - startTime;

            Prediction pred = Prediction.builder()
                    .modelId(modelId)
                    .modelName(model.getName())
                    .projectId(projectId)
                    .predictionType("API")
                    .source("API")
                    .inputJson(serializeToJson(features))
                    .predictedClass(capitalize(prediction))
                    .predictedLabel(capitalize(prediction))
                    .probability(probability)
                    .confidence(confidence)
                    .probabilitiesJson(serializeToJson(probabilities))
                    .processingTimeMs(latency)
                    .apiKeyId(apiKeyValue)
                    .build();
            predictionRepository.save(pred);

            log.info("✅ Realtime prediction: {} ({}ms)", prediction, latency);

            return PredictionDTO.RealtimeResponse.builder()
                    .prediction(prediction)
                    .confidence(confidence)
                    .probabilities(probabilities)
                    .latencyMs(latency)
                    .build();

        } catch (Exception e) {
            log.error("❌ Realtime prediction failed", e);
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    // ========== EXPORT ==========

    public byte[] exportHistory(PredictionDTO.ExportRequest request) {
        PredictionDTO.HistoryFilter filter = PredictionDTO.HistoryFilter.builder()
                .projectId(request.getProjectId())
                .modelId(request.getModelId())
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .page(0)
                .pageSize(10000)
                .build();

        PredictionDTO.HistoryResponse history = getHistory(filter);

        StringBuilder sb = new StringBuilder();
        sb.append("prediction_id,model_name,type,result,confidence,timestamp\n");
        
        for (PredictionDTO.HistoryItem item : history.getPredictions()) {
            sb.append(String.format("%s,%s,%s,%s,%.4f,%s\n",
                    item.getPredictionId(),
                    item.getModelName(),
                    item.getPredictionType(),
                    item.getPredictedClass(),
                    item.getConfidence() != null ? item.getConfidence() : 0,
                    item.getTimestamp()
            ));
        }

        return sb.toString().getBytes();
    }

    // ========== HELPER METHODS ==========

    private PredictionDTO.BatchResponse toBatchResponse(BatchPredictionJob job) {
        PredictionDTO.BatchSummary summary = null;
        if ("COMPLETED".equals(job.getStatus()) && job.getResultSummary() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Integer> resultCounts = objectMapper.readValue(job.getResultSummary(), Map.class);
                
                int total = resultCounts.values().stream().mapToInt(Integer::intValue).sum();
                Map<String, Double> percentages = new HashMap<>();
                for (Map.Entry<String, Integer> entry : resultCounts.entrySet()) {
                    percentages.put(entry.getKey(), total > 0 ? (entry.getValue() * 100.0 / total) : 0);
                }

                summary = PredictionDTO.BatchSummary.builder()
                        .totalPredictions(total)
                        .successfulPredictions(total)
                        .failedPredictions(0)
                        .classCounts(resultCounts.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue())))
                        .classPercentages(percentages)
                        .build();
            } catch (Exception e) {
                log.warn("Failed to parse batch summary", e);
            }
        }

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
                .summary(summary)
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .processingTimeMs(job.getProcessingTimeMs())
                .processingTimeLabel(formatDuration(job.getProcessingTimeMs()))
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
                .createdAtLabel(formatRelativeTime(job.getCreatedAt()))
                .build();
    }

    private PredictionDTO.HistoryItem toHistoryItem(Prediction prediction) {
        Map<String, Object> inputFeatures = parseJson(prediction.getInputJson());
        Map<String, Double> probabilities = null;
        if (prediction.getProbabilitiesJson() != null) {
            try {
                probabilities = objectMapper.readValue(prediction.getProbabilitiesJson(), 
                        new TypeReference<Map<String, Double>>() {});
            } catch (Exception e) {
                // ignore
            }
        }

        return PredictionDTO.HistoryItem.builder()
                .predictionId(prediction.getId())
                .modelId(prediction.getModelId())
                .modelName(prediction.getModelName())
                .predictionType(prediction.getPredictionType())
                .predictionTypeLabel(prediction.getPredictionType())
                .source(prediction.getSource())
                .predictedClass(prediction.getPredictedClass())
                .predictedLabel(prediction.getPredictedLabel() != null ? 
                        prediction.getPredictedLabel() : prediction.getPredictedClass())
                .probability(prediction.getProbability())
                .probabilityLabel(prediction.getProbability() != null ? 
                        String.format("%.1f%%", prediction.getProbability() * 100) : null)
                .confidence(prediction.getConfidence())
                .confidenceLabel(prediction.getConfidence() != null ? 
                        String.format("%.1f%% confidence", prediction.getConfidence() * 100) : null)
                .riskLevel(prediction.getRiskLevel())
                .riskColor(getRiskColor(prediction.getRiskLevel()))
                .inputFeatures(inputFeatures)
                .probabilities(probabilities)
                .batchId(prediction.getBatchId())
                .timestamp(prediction.getCreatedAt())
                .timestampLabel(formatRelativeTime(prediction.getCreatedAt()))
                .processingTimeMs(prediction.getProcessingTimeMs())
                .build();
    }

    private String serializeToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String formatFeatureName(String name) {
        return Arrays.stream(name.split("_"))
                .map(this::capitalize)
                .collect(Collectors.joining(" "));
    }

    private String formatFeatureValue(Object value) {
        if (value == null) return "-";
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d >= 1000) {
                return String.format("%,.0f", d);
            }
            return value.toString();
        }
        return value.toString();
    }

    private String formatTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss a"));
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " mins ago";
        long hours = ChronoUnit.HOURS.between(dateTime, LocalDateTime.now());
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        long days = ChronoUnit.DAYS.between(dateTime, LocalDateTime.now());
        if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private String formatDuration(Long millis) {
        if (millis == null) return null;
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + " seconds";
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + " min " + seconds + " sec";
    }

    private String formatNumber(long number) {
        if (number >= 1000000) return String.format("%.1fM", number / 1000000.0);
        if (number >= 1000) return String.format("%.1fK", number / 1000.0);
        return String.valueOf(number);
    }

    private String getRiskColor(String riskLevel) {
        if (riskLevel == null) return "yellow";
        if (riskLevel.toLowerCase().contains("high")) return "red";
        if (riskLevel.toLowerCase().contains("low")) return "green";
        return "yellow";
    }
}
