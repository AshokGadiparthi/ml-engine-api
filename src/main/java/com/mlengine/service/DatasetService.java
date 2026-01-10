package com.mlengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mlengine.config.MLEngineConfig;
import com.mlengine.model.dto.DatasetDTO;
import com.mlengine.model.entity.Dataset;
import com.mlengine.model.entity.Project;
import com.mlengine.model.enums.DataSourceType;
import com.mlengine.model.enums.DatasetStatus;
import com.mlengine.repository.DatasetRepository;
import com.mlengine.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Dataset operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final ProjectRepository projectRepository;
    private final MLEngineConfig config;
    private final ObjectMapper objectMapper;
    private final ActivityService activityService;

    /**
     * Upload and create dataset from file.
     */
    @Transactional
    public DatasetDTO.Response createFromFile(
            MultipartFile file,
            DatasetDTO.CreateFromFileRequest request
    ) throws Exception {
        log.info("Creating dataset from file: {}", file.getOriginalFilename());

        // Validate project
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        }

        // Create dataset entity
        Dataset dataset = Dataset.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sourceType(DataSourceType.CSV_FILE)
                .sourceName("File Upload")
                .fileName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .status(DatasetStatus.PROCESSING)
                .project(project)
                .build();

        dataset = datasetRepository.save(dataset);

        // Save file
        String filePath = saveFile(file, dataset.getId());
        dataset.setFilePath(filePath);
        dataset = datasetRepository.save(dataset);

        // Process file asynchronously
        processDatasetAsync(dataset.getId());
        
        // Record dataset upload activity
        try {
            activityService.recordDatasetUploaded(
                    dataset.getId(),
                    dataset.getName(),
                    file.getSize() > 1024 * 1024 
                            ? String.format("%.1f MB", file.getSize() / (1024.0 * 1024.0))
                            : String.format("%.1f KB", file.getSize() / 1024.0),
                    "System",
                    project != null ? project.getId() : null
            );
        } catch (Exception e) {
            log.warn("Failed to record activity: {}", e.getMessage());
        }

        return toResponse(dataset);
    }

    /**
     * Get all datasets for a project.
     */
    public List<DatasetDTO.ListItem> getDatasetsByProject(String projectId) {
        List<Dataset> datasets;
        if (projectId != null) {
            datasets = datasetRepository.findActiveByProjectId(projectId);
        } else {
            datasets = datasetRepository.findByStatusOrderByUpdatedAtDesc(DatasetStatus.ACTIVE);
        }

        return datasets.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    /**
     * Get dataset by ID.
     */
    public DatasetDTO.Response getDataset(String id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));
        return toResponse(dataset);
    }

    /**
     * Get dataset preview (first N rows).
     */
    public DatasetDTO.PreviewResponse getPreview(String id, int rows) throws Exception {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));

        if (dataset.getFilePath() == null) {
            throw new IllegalStateException("Dataset file not available");
        }

        List<String> columns = new ArrayList<>();
        List<List<Object>> data = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(Path.of(dataset.getFilePath()));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            columns.addAll(parser.getHeaderNames());

            int count = 0;
            for (CSVRecord record : parser) {
                if (count >= rows) break;

                List<Object> row = new ArrayList<>();
                for (String col : columns) {
                    row.add(record.get(col));
                }
                data.add(row);
                count++;
            }
        }

        return DatasetDTO.PreviewResponse.builder()
                .datasetId(id)
                .columns(columns)
                .rows(data)
                .totalRows(dataset.getRowCount() != null ? dataset.getRowCount().intValue() : 0)
                .previewRows(data.size())
                .build();
    }

    /**
     * Get column information for a dataset.
     */
    public List<DatasetDTO.ColumnInfo> getColumns(String id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));

        if (dataset.getColumnsJson() == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    dataset.getColumnsJson(),
                    new TypeReference<List<DatasetDTO.ColumnInfo>>() {}
            );
        } catch (Exception e) {
            log.error("Failed to parse columns JSON", e);
            return Collections.emptyList();
        }
    }

    /**
     * Update dataset.
     */
    @Transactional
    public DatasetDTO.Response updateDataset(String id, DatasetDTO.UpdateRequest request) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));

        if (request.getName() != null) {
            dataset.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dataset.setDescription(request.getDescription());
        }
        if (request.getSyncFrequency() != null) {
            dataset.setSyncFrequency(request.getSyncFrequency());
        }

        dataset = datasetRepository.save(dataset);
        return toResponse(dataset);
    }

    /**
     * Delete dataset.
     */
    @Transactional
    public void deleteDataset(String id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));

        // Delete file
        if (dataset.getFilePath() != null) {
            try {
                Files.deleteIfExists(Path.of(dataset.getFilePath()));
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", dataset.getFilePath());
            }
        }

        dataset.setStatus(DatasetStatus.DELETED);
        datasetRepository.save(dataset);
        log.info("Deleted dataset: {}", id);
    }

    /**
     * Get data quality report.
     */
    public DatasetDTO.QualityReport getQualityReport(String id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));

        // Get column quality from stored data
        List<DatasetDTO.ColumnInfo> columns = getColumns(id);
        List<DatasetDTO.ColumnQuality> columnQuality = columns.stream()
                .map(col -> DatasetDTO.ColumnQuality.builder()
                        .column(col.getName())
                        .dataType(col.getDataType())
                        .missingPct(col.getMissingPct())
                        .uniqueValues(col.getUniqueValues())
                        .hasOutliers(false)  // Would need more analysis
                        .build())
                .collect(Collectors.toList());

        return DatasetDTO.QualityReport.builder()
                .datasetId(id)
                .overallScore(dataset.getQualityScore())
                .completenessScore(100.0 - (dataset.getMissingValuesPct() != null ? dataset.getMissingValuesPct() : 0))
                .uniquenessScore(100.0 - (dataset.getDuplicateRowsPct() != null ? dataset.getDuplicateRowsPct() : 0))
                .consistencyScore(95.0)  // Placeholder
                .totalRows(dataset.getRowCount())
                .duplicateRows(dataset.getDuplicateRowsPct() != null 
                        ? Math.round(dataset.getRowCount() * dataset.getDuplicateRowsPct() / 100) : 0)
                .missingCells(dataset.getMissingValuesPct() != null 
                        ? Math.round(dataset.getRowCount() * dataset.getColumnCount() * dataset.getMissingValuesPct() / 100) : 0)
                .missingPct(dataset.getMissingValuesPct())
                .columnQuality(columnQuality)
                .build();
    }

    // ========== Private Helper Methods ==========

    private String saveFile(MultipartFile file, String datasetId) throws IOException {
        Path datasetDir = Path.of(config.getStorage().getDatasetsDir(), datasetId);
        Files.createDirectories(datasetDir);

        Path filePath = datasetDir.resolve(file.getOriginalFilename());
        file.transferTo(filePath);

        log.info("Saved file to: {}", filePath);
        return filePath.toString();
    }

    @Async
    protected void processDatasetAsync(String datasetId) {
        try {
            Thread.sleep(100);  // Small delay to ensure transaction is committed
            processDataset(datasetId);
        } catch (Exception e) {
            log.error("Failed to process dataset: {}", datasetId, e);
        }
    }

    private void processDataset(String datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId).orElse(null);
        if (dataset == null) return;

        try {
            log.info("Processing dataset: {}", datasetId);

            Path filePath = Path.of(dataset.getFilePath());
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            // Analyze CSV
            List<DatasetDTO.ColumnInfo> columns = new ArrayList<>();
            long rowCount = 0;
            long missingCells = 0;
            long totalCells = 0;

            try (Reader reader = Files.newBufferedReader(filePath);
                 CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

                List<String> headers = parser.getHeaderNames();
                Map<String, Set<String>> uniqueValues = new HashMap<>();
                Map<String, Long> missingByColumn = new HashMap<>();
                Map<String, List<Double>> numericValues = new HashMap<>();

                for (String header : headers) {
                    uniqueValues.put(header, new HashSet<>());
                    missingByColumn.put(header, 0L);
                    numericValues.put(header, new ArrayList<>());
                }

                for (CSVRecord record : parser) {
                    rowCount++;
                    for (String header : headers) {
                        String value = record.get(header);
                        totalCells++;

                        if (value == null || value.trim().isEmpty()) {
                            missingCells++;
                            missingByColumn.merge(header, 1L, Long::sum);
                        } else {
                            uniqueValues.get(header).add(value);

                            // Try to parse as number
                            try {
                                double numVal = Double.parseDouble(value);
                                numericValues.get(header).add(numVal);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                // Build column info
                for (String header : headers) {
                    List<Double> nums = numericValues.get(header);
                    boolean isNumeric = nums.size() > rowCount * 0.5;  // >50% numeric

                    DatasetDTO.ColumnInfo colInfo = DatasetDTO.ColumnInfo.builder()
                            .name(header)
                            .dataType(isNumeric ? "numeric" : "categorical")
                            .uniqueValues((long) uniqueValues.get(header).size())
                            .missingPct(rowCount > 0 ? (missingByColumn.get(header) * 100.0 / rowCount) : 0)
                            .isFeature(true)
                            .isTarget(false)
                            .build();

                    if (isNumeric && !nums.isEmpty()) {
                        colInfo.setMin(nums.stream().mapToDouble(d -> d).min().orElse(0));
                        colInfo.setMax(nums.stream().mapToDouble(d -> d).max().orElse(0));
                        colInfo.setMean(nums.stream().mapToDouble(d -> d).average().orElse(0));
                    }

                    columns.add(colInfo);
                }

                dataset.setColumnCount(headers.size());
            }

            // Update dataset
            dataset.setRowCount(rowCount);
            dataset.setColumnsJson(objectMapper.writeValueAsString(columns));
            dataset.setMissingValuesPct(totalCells > 0 ? (missingCells * 100.0 / totalCells) : 0);
            dataset.setQualityScore(100.0 - dataset.getMissingValuesPct());
            dataset.setStatus(DatasetStatus.ACTIVE);
            dataset.setLastSyncAt(LocalDateTime.now());

            datasetRepository.save(dataset);
            log.info("Dataset processed successfully: {} ({} rows, {} columns)",
                    datasetId, rowCount, dataset.getColumnCount());

        } catch (Exception e) {
            log.error("Failed to process dataset: {}", datasetId, e);
            dataset.setStatus(DatasetStatus.ERROR);
            dataset.setErrorMessage(e.getMessage());
            datasetRepository.save(dataset);
        }
    }

    private DatasetDTO.Response toResponse(Dataset dataset) {
        List<DatasetDTO.ColumnInfo> columns = Collections.emptyList();
        if (dataset.getColumnsJson() != null) {
            try {
                columns = objectMapper.readValue(
                        dataset.getColumnsJson(),
                        new TypeReference<List<DatasetDTO.ColumnInfo>>() {}
                );
            } catch (Exception ignored) {}
        }

        return DatasetDTO.Response.builder()
                .id(dataset.getId())
                .name(dataset.getName())
                .description(dataset.getDescription())
                .projectId(dataset.getProjectId())
                .sourceType(dataset.getSourceType())
                .sourceName(dataset.getSourceName())
                .sourceTable(dataset.getSourceTable())
                .rowCount(dataset.getRowCount())
                .columnCount(dataset.getColumnCount())
                .fileSize(formatFileSize(dataset.getFileSizeBytes()))
                .fileSizeBytes(dataset.getFileSizeBytes())
                .featureCount(dataset.getColumnCount() != null ? dataset.getColumnCount() - 1 : 0)
                .qualityScore(dataset.getQualityScore())
                .qualityLabel(dataset.getQualityScore() != null ? String.format("%.0f%%", dataset.getQualityScore()) : null)
                .missingValuesPct(dataset.getMissingValuesPct())
                .duplicateRowsPct(dataset.getDuplicateRowsPct())
                .columns(columns)
                .status(dataset.getStatus())
                .statusLabel(dataset.getStatus() != null ? dataset.getStatus().name().toLowerCase() : null)
                .errorMessage(dataset.getErrorMessage())
                .lastSyncAt(dataset.getLastSyncAt())
                .lastSyncLabel(formatTimeAgo(dataset.getLastSyncAt()))
                .syncFrequency(dataset.getSyncFrequency())
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt())
                .build();
    }

    private DatasetDTO.ListItem toListItem(Dataset dataset) {
        return DatasetDTO.ListItem.builder()
                .id(dataset.getId())
                .name(dataset.getName())
                .initials(getInitials(dataset.getName()))
                .sourceType(dataset.getSourceType())
                .sourceName(dataset.getSourceName())
                .rowCount(dataset.getRowCount())
                .rowCountLabel(formatNumber(dataset.getRowCount()) + " records")
                .featureCount(dataset.getColumnCount() != null ? dataset.getColumnCount() - 1 : 0)
                .featureCountLabel((dataset.getColumnCount() != null ? dataset.getColumnCount() - 1 : 0) + " features")
                .fileSize(formatFileSize(dataset.getFileSizeBytes()))
                .qualityScore(dataset.getQualityScore())
                .qualityLabel(dataset.getQualityScore() != null ? String.format("%.0f%%", dataset.getQualityScore()) : null)
                .status(dataset.getStatus())
                .lastSyncAt(dataset.getLastSyncAt())
                .lastSyncLabel(formatTimeAgo(dataset.getLastSyncAt()))
                .build();
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    private String formatNumber(Long number) {
        if (number == null) return "0";
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%,d", number);
        return number.toString();
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        long days = hours / 24;
        return days + " days ago";
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "??";
        String[] words = name.split("\\s+");
        if (words.length >= 2) {
            return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
