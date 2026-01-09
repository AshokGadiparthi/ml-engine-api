package com.mlengine.model.dto;

import com.mlengine.model.enums.DataSourceStatus;
import com.mlengine.model.enums.DataSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for DataSource operations.
 * Matches your React UI's "Connect New Data Source" modal.
 */
public class DataSourceDTO {

    /**
     * Request to create a new data source connection.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Connection name is required")
        private String name;

        private String description;

        @NotNull(message = "Source type is required")
        private DataSourceType sourceType;

        private String projectId;

        // Database connections
        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private String password;

        // Cloud connections
        private String bucketName;
        private String region;
        private String credentialsJson;
        private String accessKey;
        private String secretKey;

        // Security
        @Builder.Default
        private Boolean secureConnection = true;
    }

    /**
     * Request to update data source.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String description;
        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private String password;
        private String bucketName;
        private String region;
        private String accessKey;
        private String secretKey;
    }

    /**
     * Request to test connection.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestConnectionRequest {
        @NotNull(message = "Source type is required")
        private DataSourceType sourceType;

        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private String password;

        private String bucketName;
        private String region;
        private String credentialsJson;
        private String accessKey;
        private String secretKey;
    }

    /**
     * Test connection response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestConnectionResponse {
        private Boolean success;
        private String message;
        private Long latencyMs;
        private String serverVersion;
        private Integer tablesCount;
        private List<String> availableTables;
    }

    /**
     * Data source response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String description;
        private DataSourceType sourceType;
        private String sourceTypeLabel;  // "PostgreSQL", "MySQL", etc.
        private String projectId;

        // Connection info (masked for security)
        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private String bucketName;
        private String region;

        // Status
        private DataSourceStatus status;
        private String statusLabel;
        private LocalDateTime lastTestedAt;
        private String lastTestedLabel;
        private LocalDateTime lastUsedAt;
        private String lastUsedLabel;
        private String errorMessage;

        // Statistics
        private Integer datasetsCount;
        private Integer tablesCount;

        // Security
        private Boolean secureConnection;

        // Timestamps
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Data source list item.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private String id;
        private String name;
        private DataSourceType sourceType;
        private String sourceTypeLabel;
        private String host;
        private String databaseName;
        private DataSourceStatus status;
        private String statusLabel;
        private Integer datasetsCount;
        private LocalDateTime lastUsedAt;
        private String lastUsedLabel;
    }

    /**
     * Available tables response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TablesResponse {
        private String dataSourceId;
        private List<TableInfo> tables;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableInfo {
        private String name;
        private String schema;
        private Long rowCount;
        private Integer columnCount;
        private String sizeEstimate;
    }

    /**
     * Browse response - list of tables in data source.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowseResponse {
        private String dataSourceId;
        private String dataSourceName;
        private DataSourceType dataSourceType;
        private List<TableInfo> tables;
        private Integer totalTables;
        private DataSourceStatus connectionStatus;
        private String message;
        private LocalDateTime browsedAt;
    }

    /**
     * Table preview response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TablePreviewResponse {
        private String dataSourceId;
        private String tableName;
        private String schema;
        private List<TableColumnInfo> columns;
        private List<List<Object>> rows;
        private Long totalRows;
        private Integer previewRows;
    }

    /**
     * Column info for table preview.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableColumnInfo {
        private String name;
        private String dataType;
        private Boolean nullable;
        private Boolean primaryKey;
    }
}
