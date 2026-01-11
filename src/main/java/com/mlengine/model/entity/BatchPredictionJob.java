package com.mlengine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * BatchPredictionJob entity - tracks batch prediction jobs.
 */
@Entity
@Table(name = "batch_prediction_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPredictionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "model_name")
    private String modelName;

    // Status
    @Column(name = "status")
    @Builder.Default
    private String status = "QUEUED";  // QUEUED, PROCESSING, COMPLETED, FAILED

    @Column(name = "status_message")
    private String statusMessage;

    // Progress
    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "processed_records")
    @Builder.Default
    private Integer processedRecords = 0;

    @Column(name = "failed_records")
    @Builder.Default
    private Integer failedRecords = 0;

    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;  // 0-100

    // Files
    @Column(name = "input_file_path")
    private String inputFilePath;

    @Column(name = "input_file_name")
    private String inputFileName;

    @Column(name = "output_file_path")
    private String outputFilePath;

    @Column(name = "output_file_name")
    private String outputFileName;

    // Results summary
    @Column(name = "positive_count")
    private Integer positiveCount;

    @Column(name = "negative_count")
    private Integer negativeCount;

    @Column(name = "avg_confidence")
    private Double avgConfidence;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;  // JSON with class counts: {"Approved": 1128, "Rejected": 372}

    // Timing
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    // Error
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Project
    @Column(name = "project_id")
    private String projectId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
