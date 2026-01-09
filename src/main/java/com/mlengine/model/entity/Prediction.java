package com.mlengine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Prediction entity - stores prediction history.
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

    // Prediction type
    @Column(name = "prediction_type")
    private String predictionType;  // "single" or "batch"

    // Input/Output
    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    // Result
    @Column(name = "predicted_class")
    private String predictedClass;

    @Column(name = "probability")
    private Double probability;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "risk_level")
    private String riskLevel;  // "High", "Medium", "Low"

    // Batch specific
    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "batch_index")
    private Integer batchIndex;

    // Metadata
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "project_id")
    private String projectId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
