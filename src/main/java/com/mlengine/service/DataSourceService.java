package com.mlengine.service;

import com.mlengine.config.MLEngineConfig;
import com.mlengine.model.dto.DataSourceDTO;
import com.mlengine.model.entity.DataSource;
import com.mlengine.model.entity.Project;
import com.mlengine.model.enums.DataSourceStatus;
import com.mlengine.model.enums.DataSourceType;
import com.mlengine.repository.DataSourceRepository;
import com.mlengine.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for DataSource operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final ProjectRepository projectRepository;
    private final MLEngineConfig config;

    @Transactional
    public DataSourceDTO.Response createDataSource(DataSourceDTO.CreateRequest request) {
        log.info("Creating data source: {} ({})", request.getName(), request.getSourceType());

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        }

        DataSource dataSource = DataSource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sourceType(request.getSourceType())
                .host(request.getHost())
                .port(request.getPort())
                .databaseName(request.getDatabaseName())
                .username(request.getUsername())
                .password(request.getPassword())
                .bucketName(request.getBucketName())
                .region(request.getRegion())
                .credentialsJson(request.getCredentialsJson())
                .accessKey(request.getAccessKey())
                .secretKey(request.getSecretKey())
                .secureConnection(request.getSecureConnection())
                .status(DataSourceStatus.DISCONNECTED)
                .project(project)
                .build();

        dataSource = dataSourceRepository.save(dataSource);
        return toResponse(dataSource);
    }

    public List<DataSourceDTO.ListItem> getDataSourcesByProject(String projectId) {
        List<DataSource> dataSources = projectId != null 
                ? dataSourceRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)
                : dataSourceRepository.findAll();
        return dataSources.stream().map(this::toListItem).collect(Collectors.toList());
    }

    public DataSourceDTO.Response getDataSource(String id) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));
        return toResponse(dataSource);
    }

    public DataSourceDTO.TestConnectionResponse testConnection(DataSourceDTO.TestConnectionRequest request) {
        log.info("Testing connection: {}", request.getSourceType());
        long startTime = System.currentTimeMillis();

        try {
            switch (request.getSourceType()) {
                case POSTGRESQL:
                    return testJdbcConnection("jdbc:postgresql", request, 5432, startTime);
                case MYSQL:
                    return testJdbcConnection("jdbc:mysql", request, 3306, startTime);
                case SQLITE:
                    return testSqliteConnection(request, startTime);
                default:
                    return DataSourceDTO.TestConnectionResponse.builder()
                            .success(true)
                            .message("Connection type supported (detailed test requires Python)")
                            .latencyMs(System.currentTimeMillis() - startTime)
                            .build();
            }
        } catch (Exception e) {
            return DataSourceDTO.TestConnectionResponse.builder()
                    .success(false)
                    .message("Connection failed: " + e.getMessage())
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Transactional
    public DataSourceDTO.TestConnectionResponse testConnection(String id) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));

        DataSourceDTO.TestConnectionRequest request = DataSourceDTO.TestConnectionRequest.builder()
                .sourceType(dataSource.getSourceType())
                .host(dataSource.getHost())
                .port(dataSource.getPort())
                .databaseName(dataSource.getDatabaseName())
                .username(dataSource.getUsername())
                .password(dataSource.getPassword())
                .build();

        DataSourceDTO.TestConnectionResponse response = testConnection(request);

        dataSource.setLastTestedAt(LocalDateTime.now());
        dataSource.setStatus(response.getSuccess() ? DataSourceStatus.CONNECTED : DataSourceStatus.ERROR);
        dataSource.setErrorMessage(response.getSuccess() ? null : response.getMessage());
        dataSourceRepository.save(dataSource);

        return response;
    }

    @Transactional
    public DataSourceDTO.Response updateDataSource(String id, DataSourceDTO.UpdateRequest request) {
        DataSource ds = dataSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));

        if (request.getName() != null) ds.setName(request.getName());
        if (request.getHost() != null) ds.setHost(request.getHost());
        if (request.getPort() != null) ds.setPort(request.getPort());
        if (request.getDatabaseName() != null) ds.setDatabaseName(request.getDatabaseName());
        if (request.getUsername() != null) ds.setUsername(request.getUsername());
        if (request.getPassword() != null) ds.setPassword(request.getPassword());

        return toResponse(dataSourceRepository.save(ds));
    }

    @Transactional
    public void deleteDataSource(String id) {
        dataSourceRepository.deleteById(id);
        log.info("Deleted data source: {}", id);
    }

    /**
     * Browse data source - get list of tables.
     */
    @Transactional
    public DataSourceDTO.BrowseResponse browseDataSource(String id) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));

        log.info("Browsing data source: {} ({})", dataSource.getName(), dataSource.getSourceType());

        List<DataSourceDTO.TableInfo> tables = new ArrayList<>();
        String message = "Success";
        DataSourceStatus status = DataSourceStatus.CONNECTED;

        try {
            switch (dataSource.getSourceType()) {
                case POSTGRESQL:
                    tables = browseJdbcTables("jdbc:postgresql", dataSource, 5432,
                            "SELECT table_name, table_schema FROM information_schema.tables WHERE table_schema = 'public'");
                    break;
                case MYSQL:
                    tables = browseJdbcTables("jdbc:mysql", dataSource, 3306,
                            "SELECT table_name, table_schema FROM information_schema.tables WHERE table_schema = DATABASE()");
                    break;
                case SQLITE:
                    tables = browseSqliteTables(dataSource);
                    break;
                default:
                    tables = generateMockTables(dataSource);
                    message = "Mock data - actual browsing requires connector";
            }

            dataSource.setLastUsedAt(LocalDateTime.now());
            dataSource.setStatus(DataSourceStatus.CONNECTED);
            dataSource.setTablesCount(tables.size());
            dataSourceRepository.save(dataSource);

        } catch (Exception e) {
            log.error("Failed to browse data source: {}", id, e);
            message = "Browse failed: " + e.getMessage();
            status = DataSourceStatus.ERROR;
            
            dataSource.setStatus(DataSourceStatus.ERROR);
            dataSource.setErrorMessage(e.getMessage());
            dataSourceRepository.save(dataSource);
        }

        return DataSourceDTO.BrowseResponse.builder()
                .dataSourceId(id)
                .dataSourceName(dataSource.getName())
                .dataSourceType(dataSource.getSourceType())
                .tables(tables)
                .totalTables(tables.size())
                .connectionStatus(status)
                .message(message)
                .browsedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Preview table data from data source.
     */
    public DataSourceDTO.TablePreviewResponse previewTable(String id, String tableName, int rows) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));

        log.info("Previewing table {} from data source: {}", tableName, dataSource.getName());

        try {
            switch (dataSource.getSourceType()) {
                case POSTGRESQL:
                    return previewJdbcTable("jdbc:postgresql", dataSource, 5432, tableName, rows);
                case MYSQL:
                    return previewJdbcTable("jdbc:mysql", dataSource, 3306, tableName, rows);
                case SQLITE:
                    return previewSqliteTable(dataSource, tableName, rows);
                default:
                    return generateMockTablePreview(dataSource, tableName, rows);
            }
        } catch (Exception e) {
            log.error("Failed to preview table: {} from {}", tableName, id, e);
            throw new RuntimeException("Failed to preview table: " + e.getMessage());
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private DataSourceDTO.TestConnectionResponse testJdbcConnection(
            String jdbcPrefix, DataSourceDTO.TestConnectionRequest request, int defaultPort, long startTime) {
        try {
            String url = String.format("%s://%s:%d/%s",
                    jdbcPrefix, request.getHost(),
                    request.getPort() != null ? request.getPort() : defaultPort,
                    request.getDatabaseName());

            try (Connection conn = DriverManager.getConnection(url, request.getUsername(), request.getPassword())) {
                return DataSourceDTO.TestConnectionResponse.builder()
                        .success(true)
                        .message("Connection successful")
                        .latencyMs(System.currentTimeMillis() - startTime)
                        .serverVersion(conn.getMetaData().getDatabaseProductVersion())
                        .build();
            }
        } catch (Exception e) {
            return DataSourceDTO.TestConnectionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private DataSourceDTO.TestConnectionResponse testSqliteConnection(
            DataSourceDTO.TestConnectionRequest request, long startTime) {
        try {
            String url = "jdbc:sqlite:" + request.getDatabaseName();
            try (Connection conn = DriverManager.getConnection(url)) {
                return DataSourceDTO.TestConnectionResponse.builder()
                        .success(true)
                        .message("SQLite connection successful")
                        .latencyMs(System.currentTimeMillis() - startTime)
                        .serverVersion("SQLite " + conn.getMetaData().getDatabaseProductVersion())
                        .build();
            }
        } catch (Exception e) {
            return DataSourceDTO.TestConnectionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private List<DataSourceDTO.TableInfo> browseJdbcTables(String jdbcPrefix, DataSource ds, int defaultPort, String query) {
        List<DataSourceDTO.TableInfo> tables = new ArrayList<>();
        try {
            String url = String.format("%s://%s:%d/%s", jdbcPrefix, ds.getHost(),
                    ds.getPort() != null ? ds.getPort() : defaultPort, ds.getDatabaseName());

            try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword());
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String tableName = rs.getString(1);
                    String schema = rs.getString(2);
                    
                    Long rowCount = getTableRowCount(conn, tableName);
                    Integer colCount = getTableColumnCount(conn, tableName);

                    tables.add(DataSourceDTO.TableInfo.builder()
                            .name(tableName)
                            .schema(schema)
                            .rowCount(rowCount)
                            .columnCount(colCount)
                            .sizeEstimate(formatRowCount(rowCount))
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to browse JDBC tables", e);
            throw new RuntimeException("Failed to browse tables: " + e.getMessage());
        }
        return tables;
    }

    private List<DataSourceDTO.TableInfo> browseSqliteTables(DataSource ds) {
        List<DataSourceDTO.TableInfo> tables = new ArrayList<>();
        try {
            String url = "jdbc:sqlite:" + ds.getDatabaseName();
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {

                while (rs.next()) {
                    String tableName = rs.getString(1);
                    Long rowCount = getTableRowCount(conn, tableName);
                    Integer colCount = getTableColumnCount(conn, tableName);

                    tables.add(DataSourceDTO.TableInfo.builder()
                            .name(tableName)
                            .schema("main")
                            .rowCount(rowCount)
                            .columnCount(colCount)
                            .sizeEstimate(formatRowCount(rowCount))
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to browse SQLite tables", e);
            throw new RuntimeException("Failed to browse tables: " + e.getMessage());
        }
        return tables;
    }

    private DataSourceDTO.TablePreviewResponse previewJdbcTable(String jdbcPrefix, DataSource ds, int defaultPort, String tableName, int maxRows) {
        List<DataSourceDTO.TableColumnInfo> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        long totalRows = 0;

        try {
            String url = String.format("%s://%s:%d/%s", jdbcPrefix, ds.getHost(),
                    ds.getPort() != null ? ds.getPort() : defaultPort, ds.getDatabaseName());

            try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword())) {
                // Get total count
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                    if (rs.next()) totalRows = rs.getLong(1);
                }

                // Get sample rows
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT " + maxRows)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    
                    // Build columns
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        columns.add(DataSourceDTO.TableColumnInfo.builder()
                                .name(meta.getColumnName(i))
                                .dataType(meta.getColumnTypeName(i))
                                .nullable(meta.isNullable(i) == ResultSetMetaData.columnNullable)
                                .primaryKey(false)
                                .build());
                    }
                    
                    // Build rows
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            row.add(rs.getObject(i));
                        }
                        rows.add(row);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to preview JDBC table", e);
            throw new RuntimeException("Failed to preview table: " + e.getMessage());
        }

        return DataSourceDTO.TablePreviewResponse.builder()
                .dataSourceId(ds.getId())
                .tableName(tableName)
                .columns(columns)
                .rows(rows)
                .totalRows(totalRows)
                .previewRows(rows.size())
                .build();
    }

    private DataSourceDTO.TablePreviewResponse previewSqliteTable(DataSource ds, String tableName, int maxRows) {
        List<DataSourceDTO.TableColumnInfo> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        long totalRows = 0;

        try {
            String url = "jdbc:sqlite:" + ds.getDatabaseName();
            try (Connection conn = DriverManager.getConnection(url)) {
                // Get columns using PRAGMA
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
                    while (rs.next()) {
                        columns.add(DataSourceDTO.TableColumnInfo.builder()
                                .name(rs.getString("name"))
                                .dataType(rs.getString("type"))
                                .nullable(rs.getInt("notnull") == 0)
                                .primaryKey(rs.getInt("pk") == 1)
                                .build());
                    }
                }

                // Get total count
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                    if (rs.next()) totalRows = rs.getLong(1);
                }

                // Get sample rows
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT " + maxRows)) {
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= columns.size(); i++) {
                            row.add(rs.getObject(i));
                        }
                        rows.add(row);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to preview SQLite table", e);
            throw new RuntimeException("Failed to preview table: " + e.getMessage());
        }

        return DataSourceDTO.TablePreviewResponse.builder()
                .dataSourceId(ds.getId())
                .tableName(tableName)
                .columns(columns)
                .rows(rows)
                .totalRows(totalRows)
                .previewRows(rows.size())
                .build();
    }

    private Long getTableRowCount(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            log.warn("Failed to get row count for {}: {}", tableName, e.getMessage());
        }
        return 0L;
    }

    private Integer getTableColumnCount(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 1")) {
            return rs.getMetaData().getColumnCount();
        } catch (Exception e) {
            log.warn("Failed to get column count for {}: {}", tableName, e.getMessage());
        }
        return 0;
    }

    private List<DataSourceDTO.TableInfo> generateMockTables(DataSource ds) {
        List<DataSourceDTO.TableInfo> tables = new ArrayList<>();
        String[] mockTableNames = {"customers", "orders", "products", "transactions", "users"};
        
        for (String name : mockTableNames) {
            tables.add(DataSourceDTO.TableInfo.builder()
                    .name(name)
                    .schema("public")
                    .rowCount((long) (Math.random() * 100000))
                    .columnCount((int) (Math.random() * 15) + 5)
                    .sizeEstimate(formatRowCount((long) (Math.random() * 100000)))
                    .build());
        }
        return tables;
    }

    private DataSourceDTO.TablePreviewResponse generateMockTablePreview(DataSource ds, String tableName, int maxRows) {
        List<DataSourceDTO.TableColumnInfo> columns = List.of(
                DataSourceDTO.TableColumnInfo.builder().name("id").dataType("INTEGER").nullable(false).primaryKey(true).build(),
                DataSourceDTO.TableColumnInfo.builder().name("name").dataType("VARCHAR").nullable(true).primaryKey(false).build(),
                DataSourceDTO.TableColumnInfo.builder().name("value").dataType("DECIMAL").nullable(true).primaryKey(false).build(),
                DataSourceDTO.TableColumnInfo.builder().name("created_at").dataType("TIMESTAMP").nullable(true).primaryKey(false).build()
        );

        List<List<Object>> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(maxRows, 10); i++) {
            rows.add(List.of(i + 1, "Sample " + (i + 1), Math.round(Math.random() * 1000 * 100.0) / 100.0, LocalDateTime.now().minusDays(i)));
        }

        return DataSourceDTO.TablePreviewResponse.builder()
                .dataSourceId(ds.getId())
                .tableName(tableName)
                .columns(columns)
                .rows(rows)
                .totalRows((long) (Math.random() * 10000))
                .previewRows(rows.size())
                .build();
    }

    private String formatRowCount(Long rowCount) {
        if (rowCount == null || rowCount == 0) return "0 rows";
        if (rowCount >= 1_000_000) return String.format("%.1fM rows", rowCount / 1_000_000.0);
        if (rowCount >= 1_000) return String.format("%.1fK rows", rowCount / 1_000.0);
        return rowCount + " rows";
    }

    private DataSourceDTO.Response toResponse(DataSource ds) {
        return DataSourceDTO.Response.builder()
                .id(ds.getId())
                .name(ds.getName())
                .description(ds.getDescription())
                .sourceType(ds.getSourceType())
                .sourceTypeLabel(getSourceTypeLabel(ds.getSourceType()))
                .host(ds.getHost())
                .port(ds.getPort())
                .databaseName(ds.getDatabaseName())
                .username(ds.getUsername())
                .bucketName(ds.getBucketName())
                .region(ds.getRegion())
                .status(ds.getStatus())
                .statusLabel(ds.getStatus() != null ? ds.getStatus().name().toLowerCase() : null)
                .lastTestedAt(ds.getLastTestedAt())
                .datasetsCount(ds.getDatasetsCount())
                .secureConnection(ds.getSecureConnection())
                .createdAt(ds.getCreatedAt())
                .updatedAt(ds.getUpdatedAt())
                .build();
    }

    private DataSourceDTO.ListItem toListItem(DataSource ds) {
        return DataSourceDTO.ListItem.builder()
                .id(ds.getId())
                .name(ds.getName())
                .sourceType(ds.getSourceType())
                .sourceTypeLabel(getSourceTypeLabel(ds.getSourceType()))
                .host(ds.getHost())
                .databaseName(ds.getDatabaseName())
                .status(ds.getStatus())
                .datasetsCount(ds.getDatasetsCount())
                .lastUsedAt(ds.getLastUsedAt())
                .build();
    }

    private String getSourceTypeLabel(DataSourceType type) {
        if (type == null) return null;
        return switch (type) {
            case POSTGRESQL -> "PostgreSQL";
            case MYSQL -> "MySQL";
            case SQLITE -> "SQLite";
            case BIGQUERY -> "Google BigQuery";
            case AWS_S3 -> "AWS S3";
            case GCS -> "Google Cloud Storage";
            case CSV_FILE -> "CSV File";
            case API -> "API";
        };
    }
}
