# ML Engine Spring Boot API

A REST API to expose the ML Engine for web/mobile applications.

## 🏗️ Project Structure

```
ml-engine-api/
├── pom.xml                          # Maven dependencies
├── src/main/java/com/mlengine/
│   ├── MlEngineApplication.java     # Main application
│   ├── config/
│   │   └── PythonConfig.java        # Python integration config
│   ├── controller/
│   │   ├── TrainController.java     # Training endpoints
│   │   ├── PredictController.java   # Prediction endpoints
│   │   └── ModelController.java     # Model management
│   ├── service/
│   │   ├── MLService.java           # ML operations
│   │   └── PythonBridgeService.java # Python integration
│   ├── model/
│   │   ├── TrainRequest.java        # Training request DTO
│   │   ├── PredictRequest.java      # Prediction request DTO
│   │   └── ModelInfo.java           # Model info DTO
│   └── exception/
│       └── MLException.java         # Custom exceptions
├── src/main/resources/
│   └── application.yml              # Configuration
└── README.md
```

## 🚀 Quick Start

```bash
# Build
mvn clean package

# Run
java -jar target/ml-engine-api.jar

# Or with Maven
mvn spring-boot:run
```

## 📡 API Endpoints

### Training
- `POST /api/train` - Train a new model
- `GET /api/train/status/{jobId}` - Get training status

### Predictions
- `POST /api/predict` - Make predictions
- `POST /api/predict/batch` - Batch predictions

### Models
- `GET /api/models` - List all models
- `GET /api/models/{id}` - Get model details
- `DELETE /api/models/{id}` - Delete model

## 🔧 Configuration

```yaml
# application.yml
ml-engine:
  python-path: /usr/bin/python3
  models-dir: ./models
  max-file-size: 100MB
```

## 📋 Requirements

- Java 17+
- Maven 3.8+
- Python 3.10+ (with ml-engine installed)
