package com.mlengine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ApiKey entity - stores API keys for model predictions.
 */
@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name")
    private String name;  // User-friendly name

    @Column(name = "key_value", nullable = false, unique = true)
    private String keyValue;  // The actual API key

    @Column(name = "key_prefix")
    private String keyPrefix;  // First 12 chars for display: "mlk_abc1..."

    @Column(name = "model_id")
    private String modelId;  // If key is model-specific

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "user_id")
    private String userId;

    // Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Rate limiting
    @Column(name = "rate_limit_per_hour")
    @Builder.Default
    private Integer rateLimitPerHour = 1000;

    @Column(name = "rate_limit_per_day")
    @Builder.Default
    private Integer rateLimitPerDay = 10000;

    // Usage stats (denormalized for quick access)
    @Column(name = "total_requests")
    @Builder.Default
    private Long totalRequests = 0L;

    @Column(name = "requests_today")
    @Builder.Default
    private Long requestsToday = 0L;

    @Column(name = "requests_this_hour")
    @Builder.Default
    private Long requestsThisHour = 0L;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "last_reset_date")
    private LocalDateTime lastResetDate;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
