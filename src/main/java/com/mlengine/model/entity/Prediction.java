package com.mlengine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Prediction entity - stores prediction history.
 * Supports Single, Batch, and API predictions.
 */
@Entity
@Table(name = "predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "project_id")
    private String projectId;

    // Prediction type: Single, Batch, API
    @Column(name = "prediction_type")
    private String predictionType;

    // Source: UI, API, BATCH
    @Column(name = "source")
    @Builder.Default
    private String source = "UI";

    // Input/Output as JSON
    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    // Result
    @Column(name = "predicted_class")
    private String predictedClass;

    @Column(name = "predicted_label")
    private String predictedLabel;

    @Column(name = "probability")
    private Double probability;

    @Column(name = "confidence")
    private Double confidence;

    // Probabilities JSON: {"Approved": 0.87, "Rejected": 0.13}
    @Column(name = "probabilities_json", columnDefinition = "TEXT")
    private String probabilitiesJson;

    @Column(name = "risk_level")
    private String riskLevel;  // "High Risk", "Medium Risk", "Low Risk"

    // For regression
    @Column(name = "predicted_value")
    private Double predictedValue;

    // Batch specific
    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "batch_index")
    private Integer batchIndex;

    // API specific
    @Column(name = "api_key_id")
    private String apiKeyId;

    // Metadata
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
