package com.mlengine.controller;

import com.mlengine.config.MLEngineConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for health and status endpoints.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health and status endpoints")
@CrossOrigin
public class HealthController {

    private final MLEngineConfig config;

    @GetMapping
    @Operation(summary = "API status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("name", "ML Engine API");
        status.put("version", "2.0.0");
        status.put("description", "Enterprise ML Platform REST API");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "UP");

        // Check Python
        try {
            ProcessBuilder pb = new ProcessBuilder(config.getPythonPath(), "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            health.put("python", exitCode == 0 ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("python", "DOWN");
        }

        // Check ML Engine
        try {
            ProcessBuilder pb = new ProcessBuilder(config.getPythonPath(), "-c", "import ml_engine; print('ok')");
            Process process = pb.start();
            int exitCode = process.waitFor();
            health.put("mlEngine", exitCode == 0 ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("mlEngine", "DOWN");
        }

        return ResponseEntity.ok(health);
    }
}
