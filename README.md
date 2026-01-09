# ML Engine API v2.0

Enterprise Machine Learning Platform REST API - Complete with Data Management

## 🚀 Quick Start

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

**URLs:**
- API: http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

## 📡 Complete API Endpoints

### Health
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api` | API status |
| GET | `/api/health` | Health check |

### Projects
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List projects |
| GET | `/api/projects/{id}` | Get project |
| GET | `/api/projects/{id}/stats` | Dashboard stats |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |

### Data Sources ⭐ NEW BROWSE/PREVIEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasources` | Create data source |
| GET | `/api/datasources` | List data sources |
| GET | `/api/datasources/{id}` | Get data source |
| POST | `/api/datasources/test` | Test new connection |
| POST | `/api/datasources/{id}/test` | Test existing connection |
| GET | `/api/datasources/{id}/browse` | ⭐ **Browse tables** |
| GET | `/api/datasources/{id}/tables/{name}/preview` | ⭐ **Preview table data** |
| PUT | `/api/datasources/{id}` | Update data source |
| DELETE | `/api/datasources/{id}` | Delete data source |

### Datasets
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasets` | Upload dataset (multipart) |
| GET | `/api/datasets` | List datasets |
| GET | `/api/datasets/{id}` | Get dataset |
| GET | `/api/datasets/{id}/preview` | Preview data |
| GET | `/api/datasets/{id}/columns` | Column info |
| GET | `/api/datasets/{id}/quality` | Quality report |
| PUT | `/api/datasets/{id}` | Update dataset |
| DELETE | `/api/datasets/{id}` | Delete dataset |

### Training Jobs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/training/jobs` | Start training |
| GET | `/api/training/jobs` | List jobs |
| GET | `/api/training/jobs/{id}` | Get job |
| GET | `/api/training/jobs/{id}/progress` | Job progress |
| POST | `/api/training/jobs/{id}/stop` | Stop training |

### Algorithms
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/algorithms` | List 14 algorithms |
| GET | `/api/algorithms/{id}` | Get algorithm |
| GET | `/api/algorithms/{id}/params` | Hyperparameters |

### Models
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/models` | List models |
| GET | `/api/models/{id}` | Get model |
| GET | `/api/models/{id}/metrics` | All metrics |
| GET | `/api/models/{id}/confusion-matrix` | Confusion matrix |
| GET | `/api/models/{id}/roc-curve` | ROC curve |
| GET | `/api/models/{id}/feature-importance` | Feature importance |
| POST | `/api/models/{id}/deploy` | Deploy model |

### Explainability (SHAP/LIME)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/models/{id}/shap/global` | SHAP global importance |
| POST | `/api/models/{id}/shap/local` | SHAP local explanation |
| POST | `/api/models/{id}/lime` | LIME explanation |
| GET | `/api/models/{id}/pdp/{feature}` | Partial Dependence Plot |
| POST | `/api/models/{id}/whatif` | What-If analysis |

### Predictions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/predictions/single` | Single prediction |
| POST | `/api/predictions/batch` | Batch predictions (CSV) |
| GET | `/api/predictions/batch/{id}` | Batch job status |
| GET | `/api/predictions/batch/{id}/download` | Download results |
| GET | `/api/predictions/history` | Prediction history |
| POST | `/api/predictions/realtime/{modelId}` | Low-latency prediction |

### Activities
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/activities` | List activities |
| GET | `/api/activities/recent` | Recent activities |

## ✅ All Features Complete

| Feature | Status | Description |
|---------|--------|-------------|
| Projects | ✅ | CRUD + Dashboard stats |
| Data Sources | ✅ | CRUD + Test + **Browse** + **Preview** |
| Datasets | ✅ | Upload + Preview + Quality |
| Training | ✅ | 14 Algorithms + Progress |
| Models | ✅ | Metrics + Deploy |
| Explainability | ✅ | SHAP + LIME + PDP |
| Predictions | ✅ | Single + Batch |
| Activities | ✅ | Activity feed |

## 🗄️ Supported Data Sources

| Type | Test | Browse | Preview |
|------|------|--------|---------|
| PostgreSQL | ✅ | ✅ | ✅ |
| MySQL | ✅ | ✅ | ✅ |
| SQLite | ✅ | ✅ | ✅ |
| BigQuery | Mock | Mock | Mock |
| AWS S3 | Mock | Mock | Mock |
| GCS | Mock | Mock | Mock |

## 📊 Supported Algorithms (14)

- XGBoost
- LightGBM  
- CatBoost
- Random Forest
- Gradient Boosting
- Neural Network (MLP)
- Logistic Regression
- Linear Regression
- SVM
- KNN
- Naive Bayes
- Decision Tree
- Extra Trees
- AdaBoost

## 🧪 Test Commands

```bash
# Test Browse Data Source
curl http://localhost:8080/api/datasources/{id}/browse

# Test Table Preview
curl http://localhost:8080/api/datasources/{id}/tables/customers/preview?rows=10

# Test Dataset Quality
curl http://localhost:8080/api/datasets/{id}/quality

# Test Connection
curl -X POST http://localhost:8080/api/datasources/{id}/test
```
