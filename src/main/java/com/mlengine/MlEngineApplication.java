package com.mlengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ML Engine API - Main Application
 * 
 * REST API for training ML models and making predictions
 * using the Python ML Engine.
 */
@SpringBootApplication
@EnableAsync
public class MlEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlEngineApplication.class, args);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🚀 ML ENGINE API STARTED!");
        System.out.println("=".repeat(60));
        System.out.println("📡 API: http://localhost:8080/api");
        System.out.println("📚 Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("❤️  Health: http://localhost:8080/actuator/health");
        System.out.println("=".repeat(60) + "\n");
    }
}
