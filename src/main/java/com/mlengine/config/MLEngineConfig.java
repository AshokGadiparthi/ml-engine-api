package com.mlengine.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for ML Engine settings.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ml-engine")
public class MLEngineConfig {

    private String pythonPath = "python3";
    private Storage storage = new Storage();

    @Data
    public static class Storage {
        private String baseDir = "./storage";
        private String datasetsDir = "./storage/datasets";
        private String modelsDir = "./storage/models";
        private String tempDir = "./storage/temp";
        private String exportsDir = "./storage/exports";
    }

    // Convenience methods for direct access
    public String getBaseDir() {
        return storage.getBaseDir();
    }

    public String getDatasetsDir() {
        return storage.getDatasetsDir();
    }

    public String getModelsDir() {
        return storage.getModelsDir();
    }

    public String getTempDir() {
        return storage.getTempDir();
    }

    public String getExportsDir() {
        return storage.getExportsDir();
    }

    @PostConstruct
    public void init() {
        // Create all storage directories
        createDirectory(storage.getBaseDir());
        createDirectory(storage.getDatasetsDir());
        createDirectory(storage.getModelsDir());
        createDirectory(storage.getTempDir());
        createDirectory(storage.getExportsDir());
        
        // Make paths absolute
        storage.setBaseDir(toAbsolutePath(storage.getBaseDir()));
        storage.setDatasetsDir(toAbsolutePath(storage.getDatasetsDir()));
        storage.setModelsDir(toAbsolutePath(storage.getModelsDir()));
        storage.setTempDir(toAbsolutePath(storage.getTempDir()));
        storage.setExportsDir(toAbsolutePath(storage.getExportsDir()));

        System.out.println("📁 Storage directories initialized:");
        System.out.println("   Base:     " + storage.getBaseDir());
        System.out.println("   Datasets: " + storage.getDatasetsDir());
        System.out.println("   Models:   " + storage.getModelsDir());
        System.out.println("   Temp:     " + storage.getTempDir());
        System.out.println("   Exports:  " + storage.getExportsDir());
    }

    private void createDirectory(String path) {
        try {
            Path dirPath = Path.of(path);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to create directory: " + path + " - " + e.getMessage());
        }
    }

    private String toAbsolutePath(String path) {
        return Path.of(path).toAbsolutePath().toString();
    }
}
