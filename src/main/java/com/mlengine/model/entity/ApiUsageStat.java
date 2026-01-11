package com.mlengine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ApiUsageStat entity - tracks API usage statistics per day/hour.
 */
@Entity
@Table(name = "api_usage_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageStat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "api_key_id")
    private String apiKeyId;

    @Column(name = "model_id")
    private String modelId;

    @Column(name = "project_id")
    private String projectId;

    // Time period
    @Column(name = "stat_date")
    private LocalDate statDate;

    @Column(name = "stat_hour")
    private Integer statHour;  // 0-23, null for daily aggregate

    // Counts
    @Column(name = "request_count")
    @Builder.Default
    private Long requestCount = 0L;

    @Column(name = "success_count")
    @Builder.Default
    private Long successCount = 0L;

    @Column(name = "error_count")
    @Builder.Default
    private Long errorCount = 0L;

    // Latency stats
    @Column(name = "total_latency_ms")
    @Builder.Default
    private Long totalLatencyMs = 0L;

    @Column(name = "min_latency_ms")
    private Long minLatencyMs;

    @Column(name = "max_latency_ms")
    private Long maxLatencyMs;

    @Column(name = "avg_latency_ms")
    private Double avgLatencyMs;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
