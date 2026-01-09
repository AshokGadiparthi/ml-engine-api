package com.mlengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ML Engine API - Enterprise Machine Learning Platform
 * Version 2.0
 * 
 * Full-featured REST API supporting:
 * - Project Management
 * - Dataset Management  
 * - Data Source Connections (PostgreSQL, MySQL, BigQuery, S3, GCS)
 * - Model Training with 13+ Algorithms
 * - AutoML
 * - Model Evaluation & Metrics
 * - SHAP & LIME Explainability
 * - Single & Batch Predictions
 * - Activity Tracking
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MlEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlEngineApplication.class, args);
        printBanner();
    }
    
    private static void printBanner() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("   __  __ _       _____ _   _  ____ ___ _   _ _____                ");
        System.out.println("  |  \\/  | |     | ____| \\ | |/ ___|_ _| \\ | | ____|              ");
        System.out.println("  | |\\/| | |     |  _| |  \\| | |  _ | ||  \\| |  _|                ");
        System.out.println("  | |  | | |___  | |___| |\\  | |_| || || |\\  | |___               ");
        System.out.println("  |_|  |_|_____| |_____|_| \\_|\\____|___|_| \\_|_____|              ");
        System.out.println("                                                                    ");
        System.out.println("  Enterprise ML Platform API v2.0                                   ");
        System.out.println("=".repeat(70));
        System.out.println("  🚀 API:        http://localhost:8080/api");
        System.out.println("  📚 Swagger:    http://localhost:8080/swagger-ui.html");
        System.out.println("  🗄️  H2 Console: http://localhost:8080/h2-console");
        System.out.println("  ❤️  Health:     http://localhost:8080/actuator/health");
        System.out.println("=".repeat(70) + "\n");
    }
}
