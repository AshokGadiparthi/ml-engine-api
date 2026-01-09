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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
