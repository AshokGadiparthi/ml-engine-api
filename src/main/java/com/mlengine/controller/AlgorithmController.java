package com.mlengine.controller;

import com.mlengine.model.dto.AlgorithmDTO;
import com.mlengine.service.AlgorithmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Algorithm information.
 * Matches React UI Algorithm Selection dropdown and hyperparameter forms.
 */
@RestController
@RequestMapping("/api/algorithms")
@RequiredArgsConstructor
@Tag(name = "Algorithms", description = "ML algorithm information and parameters")
@CrossOrigin
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    @GetMapping
    @Operation(summary = "Get all algorithms",
               description = "Returns all available algorithms grouped by problem type")
    public ResponseEntity<AlgorithmDTO.ListResponse> getAllAlgorithms() {
        return ResponseEntity.ok(algorithmService.getAllAlgorithms());
    }

    @GetMapping("/ids")
    @Operation(summary = "Get algorithm IDs",
               description = "Returns list of all algorithm IDs")
    public ResponseEntity<List<String>> getAlgorithmIds() {
        return ResponseEntity.ok(algorithmService.getAlgorithmIds());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get algorithm info",
               description = "Returns information about a specific algorithm")
    public ResponseEntity<AlgorithmDTO.Info> getAlgorithm(@PathVariable String id) {
        return ResponseEntity.ok(algorithmService.getAlgorithm(id));
    }

    @GetMapping("/{id}/params")
    @Operation(summary = "Get algorithm parameters",
               description = "Returns hyperparameter schema for an algorithm")
    public ResponseEntity<AlgorithmDTO.Parameters> getAlgorithmParameters(@PathVariable String id) {
        return ResponseEntity.ok(algorithmService.getAlgorithmParameters(id));
    }
}
