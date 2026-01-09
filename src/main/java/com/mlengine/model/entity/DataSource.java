package com.mlengine.model.entity;

import com.mlengine.model.enums.DataSourceStatus;
import com.mlengine.model.enums.DataSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DataSource entity - represents a data source connection.
 * Maps to your React UI's "Connect New Data Source" modal.
 */
@Entity
@Table(name = "data_sources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;  // e.g., "Production Database"

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private DataSourceType sourceType;

    // Connection details (encrypted in production)
    @Column
    private String host;

    @Column
    private Integer port;

    @Column(name = "database_name")
    private String databaseName;

    @Column
    private String username;

    @Column
    private String password;  // Should be encrypted

    // For cloud sources
    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name = "region")
    private String region;

    @Column(name = "credentials_json", columnDefinition = "TEXT")
    private String credentialsJson;  // For BigQuery service account

    @Column(name = "access_key")
    private String accessKey;  // For S3

    @Column(name = "secret_key")
    private String secretKey;  // For S3

    // Connection status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DataSourceStatus status = DataSourceStatus.DISCONNECTED;

    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "error_message")
    private String errorMessage;

    // Statistics
    @Column(name = "datasets_count")
    @Builder.Default
    private Integer datasetsCount = 0;

    @Column(name = "tables_count")
    private Integer tablesCount;

    // Project relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "project_id", insertable = false, updatable = false)
    private String projectId;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Security flag
    @Column(name = "secure_connection")
    @Builder.Default
    private Boolean secureConnection = true;
}
