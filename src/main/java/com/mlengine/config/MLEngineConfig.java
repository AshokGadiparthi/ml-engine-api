package com.mlengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * Configuration for ML Engine integration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ml-engine")
public class MLEngineConfig {
    
    private String pythonPath = "python3";
    private String modelsDir = "./models";
    private String dataDir = "./data";
    private String tempDir = "./temp";
    private Defaults defaults = new Defaults();
    
    @Data
    public static class Defaults {
        private String algorithm = "xgboost";
        private String problemType = "classification";
        private double testSize = 0.2;
    }
    
    @PostConstruct
    public void init() {
        // Create directories if they don't exist
        createDirectory(modelsDir);
        createDirectory(dataDir);
        createDirectory(tempDir);
        
        System.out.println("📁 ML Engine directories initialized:");
        System.out.println("   Models: " + new File(modelsDir).getAbsolutePath());
        System.out.println("   Data: " + new File(dataDir).getAbsolutePath());
        System.out.println("   Temp: " + new File(tempDir).getAbsolutePath());
    }
    
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
