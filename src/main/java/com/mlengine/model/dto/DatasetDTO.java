package com.mlengine.model.dto;

import com.mlengine.model.enums.DatasetStatus;
import com.mlengine.model.enums.DataSourceType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Dataset operations.
 */
public class DatasetDTO {

    /**
     * Request to create dataset from file upload.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFromFileRequest {
        @NotBlank(message = "Dataset name is required")
        private String name;

        private String description;
        private String projectId;
    }

    /**
     * Request to create dataset from data source.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFromSourceRequest {
        @NotBlank(message = "Dataset name is required")
        private String name;

        private String description;
        private String projectId;

        @NotBlank(message = "Data source ID is required")
        private String dataSourceId;

        private String tableName;
        private String query;  // Custom SQL query
        private String syncFrequency;  // "manual", "hourly", "daily"
    }

    /**
     * Request to update dataset.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String description;
        private String syncFrequency;
    }

    /**
     * Dataset response - matches React UI Data Management.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String description;
        private String projectId;

        // Source info
        private DataSourceType sourceType;
        private String sourceName;
        private String sourceTable;

        // Statistics
        private Long rowCount;
        private Integer columnCount;
        private String fileSize;  // Formatted: "2.4 GB"
        private Long fileSizeBytes;
        private Integer featureCount;  // Same as columnCount - 1 (minus target)

        // Quality
        private Double qualityScore;
        private String qualityLabel;  // "98%"
        private Double missingValuesPct;
        private Double duplicateRowsPct;

        // Columns
        private List<ColumnInfo> columns;

        // Status
        private DatasetStatus status;
        private String statusLabel;  // "active", "processing"
        private String errorMessage;

        // Sync
        private LocalDateTime lastSyncAt;
        private String lastSyncLabel;  // "2 hours ago"
        private String syncFrequency;

        // Timestamps
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Dataset list item for Data Management page.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String id;
        private String name;
        private String initials;  // "CU" for "Customer Transactions"
        private DataSourceType sourceType;
        private String sourceName;
        private Long rowCount;
        private String rowCountLabel;  // "1,200,000 records"
        private Integer featureCount;
        private String featureCountLabel;  // "45 features"
        private String fileSize;
        private Double qualityScore;
        private String qualityLabel;
        private DatasetStatus status;
        private LocalDateTime lastSyncAt;
        private String lastSyncLabel;
    }

    /**
     * Column information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfo {
        private String name;
        private String dataType;  // "numeric", "categorical", "datetime", "text"
        private String originalType;  // Original pandas/SQL type
        private Long uniqueValues;
        private Double missingPct;
        private Double min;
        private Double max;
        private Double mean;
        private Double std;
        private List<String> sampleValues;
        private Boolean isTarget;
        private Boolean isFeature;
    }

    /**
     * Data preview response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewResponse {
        private String datasetId;
        private List<String> columns;
        private List<List<Object>> rows;
        private Integer totalRows;
        private Integer previewRows;
    }

    /**
     * Data quality analysis response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityReport {
        private String datasetId;
        private Double overallScore;
        private Double completenessScore;
        private Double uniquenessScore;
        private Double consistencyScore;

        private Long totalRows;
        private Long duplicateRows;
        private Long missingCells;
        private Double missingPct;

        private List<ColumnQuality> columnQuality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnQuality {
        private String column;
        private String dataType;
        private Double missingPct;
        private Long uniqueValues;
        private Boolean hasOutliers;
        private String qualityIssue;  // null if no issues
    }
}
