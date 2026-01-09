# ML Engine API v2.0

Enterprise Machine Learning Platform REST API - Phases 1 & 2

## 🚀 Quick Start

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

## 📡 API Endpoints

### Health & Status
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api` | API status |
| GET | `/api/health` | Health check |

### Projects
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List all projects |
| GET | `/api/projects/{id}` | Get project details |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |
| GET | `/api/projects/{id}/stats` | Dashboard statistics |

### Datasets
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasets` | Upload dataset (multipart) |
| GET | `/api/datasets` | List datasets |
| GET | `/api/datasets/{id}` | Get dataset details |
| GET | `/api/datasets/{id}/preview` | Preview data |
| GET | `/api/datasets/{id}/columns` | Column metadata |
| GET | `/api/datasets/{id}/quality` | Quality report |

### Data Sources
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasources` | Create data source |
| GET | `/api/datasources` | List data sources |
| POST | `/api/datasources/test` | Test connection |

### Training Jobs ⭐ Phase 2
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/training/jobs` | Start training |
| GET | `/api/training/jobs` | List jobs |
| GET | `/api/training/jobs/{id}` | Job details |
| GET | `/api/training/jobs/{id}/progress` | Job progress |
| POST | `/api/training/jobs/{id}/stop` | Stop job |
| POST | `/api/training/jobs/{id}/pause` | Pause job |
| POST | `/api/training/jobs/{id}/resume` | Resume job |

### Algorithms ⭐ Phase 2
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/algorithms` | List all algorithms |
| GET | `/api/algorithms/{id}` | Algorithm info |
| GET | `/api/algorithms/{id}/params` | Hyperparameters |

## 🔗 URLs

- **API**: http://localhost:8080/api
- **Swagger**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console

## 📊 Supported Algorithms (14)

| Algorithm | Classification | Regression | GPU |
|-----------|---------------|------------|-----|
| xgboost | ✅ | ✅ | ✅ |
| lightgbm | ✅ | ✅ | ✅ |
| catboost | ✅ | ✅ | ✅ |
| random_forest | ✅ | ✅ | ❌ |
| gradient_boosting | ✅ | ✅ | ❌ |
| neural_network | ✅ | ✅ | ✅ |
| logistic_regression | ✅ | ❌ | ❌ |
| linear_regression | ❌ | ✅ | ❌ |
| svm | ✅ | ❌ | ❌ |
| knn | ✅ | ✅ | ❌ |
| naive_bayes | ✅ | ❌ | ❌ |
| decision_tree | ✅ | ✅ | ❌ |
| extra_trees | ✅ | ✅ | ❌ |
| adaboost | ✅ | ✅ | ❌ |

## 📋 Example: Start Training

```bash
curl -X POST http://localhost:8080/api/training/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "experimentName": "Churn Prediction v1",
    "datasetId": "your-dataset-id",
    "algorithm": "xgboost",
    "targetVariable": "churn",
    "problemType": "CLASSIFICATION",
    "trainTestSplit": 0.8,
    "crossValidationFolds": 5,
    "hyperparameters": {
      "max_depth": 6,
      "learning_rate": 0.1,
      "n_estimators": 100
    }
  }'
```

## ✅ Phase Status

- ✅ Phase 1: Projects, Datasets, Data Sources
- ✅ Phase 2: Training Jobs, Algorithms  
- 🔜 Phase 3: Model Evaluation
- 🔜 Phase 4: Interpretability (SHAP/LIME)
- 🔜 Phase 5: Predictions
