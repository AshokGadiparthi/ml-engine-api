package com.mlengine.service;

import com.mlengine.config.MLEngineConfig;
import com.mlengine.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for ML operations using Python ML Engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MLService {
    
    private final MLEngineConfig config;
    
    // Track training jobs
    private final Map<String, TrainResponse> trainingJobs = new ConcurrentHashMap<>();
    
    // Track models
    private final Map<String, ModelInfo> models = new ConcurrentHashMap<>();
    
    /**
     * Train a model asynchronously.
     */
    @Async
    public CompletableFuture<TrainResponse> trainModelAsync(String dataPath, TrainRequest request) {
        String jobId = UUID.randomUUID().toString();
        
        TrainResponse response = TrainResponse.builder()
                .jobId(jobId)
                .status("RUNNING")
                .algorithm(request.getAlgorithm())
                .problemType(request.getProblemType())
                .startTime(LocalDateTime.now())
                .build();
        
        trainingJobs.put(jobId, response);
        
        try {
            log.info("Starting training job: {}", jobId);
            
            // Build Python command
            List<String> command = buildTrainCommand(dataPath, request);
            
            // Execute Python script
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Python: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            
            response.setEndTime(LocalDateTime.now());
            response.setDurationMs(
                java.time.Duration.between(response.getStartTime(), response.getEndTime()).toMillis()
            );
            
            if (exitCode == 0) {
                // Parse results and update response
                parseTrainingResults(response, output.toString());
                response.setStatus("COMPLETED");
                
                // Register model
                String modelId = response.getModelId() != null ? 
                    response.getModelId() : jobId;
                registerModel(modelId, request, response);
                
                log.info("Training completed: {} in {}ms", jobId, response.getDurationMs());
            } else {
                response.setStatus("FAILED");
                response.setError("Training failed with exit code: " + exitCode);
                log.error("Training failed: {}", jobId);
            }
            
        } catch (Exception e) {
            log.error("Training error: {}", e.getMessage(), e);
            response.setStatus("FAILED");
            response.setError(e.getMessage());
            response.setEndTime(LocalDateTime.now());
        }
        
        trainingJobs.put(jobId, response);
        return CompletableFuture.completedFuture(response);
    }
    
    /**
     * Train a model synchronously.
     */
    public TrainResponse trainModel(String dataPath, TrainRequest request) {
        try {
            return trainModelAsync(dataPath, request).get();
        } catch (Exception e) {
            return TrainResponse.builder()
                    .status("FAILED")
                    .error(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Make predictions using a trained model.
     */
    public PredictResponse predict(PredictRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            ModelInfo model = models.get(request.getModelId());
            if (model == null) {
                return PredictResponse.builder()
                        .status("FAILED")
                        .error("Model not found: " + request.getModelId())
                        .build();
            }
            
            // Build prediction command
            List<String> command = buildPredictCommand(request, model);
            
            // Execute
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                PredictResponse response = parsePredictionResults(output.toString(), request);
                response.setModelId(request.getModelId());
                response.setStatus("SUCCESS");
                response.setDurationMs(System.currentTimeMillis() - startTime);
                return response;
            } else {
                return PredictResponse.builder()
                        .modelId(request.getModelId())
                        .status("FAILED")
                        .error("Prediction failed")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Prediction error: {}", e.getMessage(), e);
            return PredictResponse.builder()
                    .modelId(request.getModelId())
                    .status("FAILED")
                    .error(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
    
    /**
     * Get training job status.
     */
    public TrainResponse getTrainingStatus(String jobId) {
        return trainingJobs.get(jobId);
    }
    
    /**
     * List all models.
     */
    public List<ModelInfo> listModels() {
        return new ArrayList<>(models.values());
    }
    
    /**
     * Get model by ID.
     */
    public ModelInfo getModel(String modelId) {
        return models.get(modelId);
    }
    
    /**
     * Delete a model.
     */
    public boolean deleteModel(String modelId) {
        ModelInfo model = models.remove(modelId);
        if (model != null && model.getModelPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(model.getModelPath()));
                log.info("Deleted model: {}", modelId);
                return true;
            } catch (IOException e) {
                log.error("Error deleting model file: {}", e.getMessage());
            }
        }
        return model != null;
    }
    
    // ========== Private Helper Methods ==========
    
    private List<String> buildTrainCommand(String dataPath, TrainRequest request) {
        List<String> command = new ArrayList<>();
        command.add(config.getPythonPath());
        command.add("-m");
        command.add("ml_engine.cli");
        command.add("train");
        command.add("--data=" + dataPath);
        command.add("--target=" + request.getTargetColumn());
        command.add("--algorithm=" + request.getAlgorithm());
        command.add("--problem-type=" + request.getProblemType());
        command.add("--output-dir=" + config.getModelsDir());
        
        if (request.isUseAutoML()) {
            command.add("--auto-ml");
        }
        if (request.isTuneHyperparameters()) {
            command.add("--tune");
        }
        if (request.isUseFeatureEngineering()) {
            command.add("--feature-engineering");
        }
        
        return command;
    }
    
    private List<String> buildPredictCommand(PredictRequest request, ModelInfo model) {
        List<String> command = new ArrayList<>();
        command.add(config.getPythonPath());
        command.add("-c");
        
        // Build inline Python prediction script
        StringBuilder script = new StringBuilder();
        script.append("import joblib; import json; import pandas as pd; ");
        script.append("model = joblib.load('").append(model.getModelPath()).append("'); ");
        script.append("data = ").append(toJsonString(request.getFeatures())).append("; ");
        script.append("df = pd.DataFrame([data]); ");
        script.append("pred = model.predict(df)[0]; ");
        script.append("print(json.dumps({'prediction': str(pred)}))");
        
        command.add(script.toString());
        
        return command;
    }
    
    private void parseTrainingResults(TrainResponse response, String output) {
        // Parse accuracy, metrics, etc. from Python output
        Map<String, Object> metrics = new HashMap<>();
        
        for (String line : output.split("\n")) {
            if (line.contains("Accuracy:")) {
                try {
                    String value = line.split(":")[1].trim().replace("%", "");
                    response.setAccuracy(Double.parseDouble(value) / 100);
                    metrics.put("accuracy", response.getAccuracy());
                } catch (Exception e) {
                    log.debug("Could not parse accuracy: {}", line);
                }
            }
            if (line.contains("Model saved:")) {
                response.setModelPath(line.split(":")[1].trim());
                response.setModelId(UUID.randomUUID().toString());
            }
        }
        
        response.setMetrics(metrics);
    }
    
    private PredictResponse parsePredictionResults(String output, PredictRequest request) {
        // Parse prediction from Python output
        PredictResponse response = new PredictResponse();
        
        for (String line : output.split("\n")) {
            if (line.contains("prediction")) {
                try {
                    // Simple JSON parsing
                    String pred = line.replaceAll(".*prediction[\"']:\\s*[\"']?([^\"'\\}]+)[\"']?.*", "$1");
                    response.setPrediction(pred.trim());
                } catch (Exception e) {
                    log.debug("Could not parse prediction: {}", line);
                }
            }
        }
        
        return response;
    }
    
    private void registerModel(String modelId, TrainRequest request, TrainResponse trainResponse) {
        // Convert metrics to Map<String, Object> for ModelInfo
        Map<String, Object> metricsAsObject = new HashMap<>();
        if (trainResponse.getMetrics() != null) {
            metricsAsObject.putAll(trainResponse.getMetrics());
        }
        
        ModelInfo model = ModelInfo.builder()
                .modelId(modelId)
                .name(request.getModelName() != null ? request.getModelName() : modelId)
                .algorithm(request.getAlgorithm())
                .problemType(request.getProblemType())
                .status("READY")
                .trainedAt(LocalDateTime.now())
                .modelPath(trainResponse.getModelPath())
                .metadata(metricsAsObject)
                .build();
        
        models.put(modelId, model);
        log.info("Registered model: {}", modelId);
    }
    
    private String toJsonString(Object obj) {
        if (obj == null) return "{}";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
