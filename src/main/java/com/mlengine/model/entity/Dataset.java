package com.mlengine.model.entity;

import com.mlengine.model.enums.DatasetStatus;
import com.mlengine.model.enums.DataSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Dataset entity - represents a dataset in the system.
 * Maps to your React UI's Data Management section.
 */
@Entity
@Table(name = "datasets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    // Source information
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private DataSourceType sourceType;

    @Column(name = "source_name")
    private String sourceName;  // e.g., "Production PostgreSQL", "Data Lake S3"

    @Column(name = "source_table")
    private String sourceTable;  // e.g., "customers.transactions"

    @Column(name = "source_path")
    private String sourcePath;  // For files: path, For S3: s3://bucket/path

    // File storage
    @Column(name = "file_path")
    private String filePath;  // Local storage path

    @Column(name = "file_name")
    private String fileName;

    // Statistics
    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "column_count")
    private Integer columnCount;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "quality_score")
    private Double qualityScore;  // 0-100%

    @Column(name = "missing_values_pct")
    private Double missingValuesPct;

    @Column(name = "duplicate_rows_pct")
    private Double duplicateRowsPct;

    // Column metadata (stored as JSON)
    @Column(name = "columns_json", columnDefinition = "TEXT")
    private String columnsJson;  // JSON array of column info

    // Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DatasetStatus status = DatasetStatus.PROCESSING;

    @Column(name = "error_message")
    private String errorMessage;

    // Sync information
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "sync_frequency")
    private String syncFrequency;  // "manual", "hourly", "daily"

    // Project relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "project_id", insertable = false, updatable = false)
    private String projectId;

    // Data source relationship (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id")
    private DataSource dataSource;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
