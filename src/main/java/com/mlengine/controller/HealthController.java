package com.mlengine.controller;

import com.mlengine.config.MLEngineConfig;
import com.mlengine.service.MLService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for health and status.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health and status endpoints")
public class HealthController {
    
    private final MLService mlService;
    private final MLEngineConfig config;
    
    /**
     * API status.
     */
    @GetMapping
    @Operation(summary = "API status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("name", "ML Engine API");
        status.put("version", "0.1.0");
        status.put("modelsCount", mlService.listModels().size());
        return ResponseEntity.ok(status);
    }
    
    /**
     * Health check.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        
        // Check Python availability
        try {
            ProcessBuilder pb = new ProcessBuilder(config.getPythonPath(), "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                health.put("python", "UP");
            } else {
                health.put("python", "DOWN");
                health.put("status", "DEGRADED");
            }
        } catch (Exception e) {
            health.put("python", "DOWN");
            health.put("pythonError", e.getMessage());
            health.put("status", "DEGRADED");
        }
        
        // Check ML Engine
        try {
            ProcessBuilder pb = new ProcessBuilder(
                config.getPythonPath(), "-c", "import ml_engine; print('ok')"
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                health.put("mlEngine", "UP");
            } else {
                health.put("mlEngine", "DOWN");
                health.put("status", "DEGRADED");
            }
        } catch (Exception e) {
            health.put("mlEngine", "DOWN");
            health.put("mlEngineError", e.getMessage());
            health.put("status", "DEGRADED");
        }
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get configuration (non-sensitive).
     */
    @GetMapping("/config")
    @Operation(summary = "Get configuration")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("modelsDir", config.getModelsDir());
        configInfo.put("dataDir", config.getDataDir());
        configInfo.put("defaults", Map.of(
            "algorithm", config.getDefaults().getAlgorithm(),
            "problemType", config.getDefaults().getProblemType(),
            "testSize", config.getDefaults().getTestSize()
        ));
        return ResponseEntity.ok(configInfo);
    }
}
