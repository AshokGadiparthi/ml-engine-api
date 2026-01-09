# ML Engine API v2.0

Enterprise Machine Learning Platform REST API - Phases 1, 2 & 3

## 🚀 Quick Start

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

## 📡 API Endpoints

### Health
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api` | API status |
| GET | `/api/health` | Health check |

### Projects (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List projects |
| GET | `/api/projects/{id}` | Get project |
| GET | `/api/projects/{id}/stats` | Dashboard stats |

### Datasets (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasets` | Upload dataset |
| GET | `/api/datasets` | List datasets |
| GET | `/api/datasets/{id}/preview` | Preview data |
| GET | `/api/datasets/{id}/columns` | Column info |
| GET | `/api/datasets/{id}/quality` | Quality report |

### Data Sources (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasources` | Create connection |
| GET | `/api/datasources` | List sources |
| POST | `/api/datasources/test` | Test connection |

### Training Jobs (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/training/jobs` | Start training |
| GET | `/api/training/jobs` | List jobs |
| GET | `/api/training/jobs/{id}` | Job details |
| GET | `/api/training/jobs/{id}/progress` | Job progress |
| POST | `/api/training/jobs/{id}/stop` | Stop job |

### Algorithms (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/algorithms` | List algorithms |
| GET | `/api/algorithms/{id}/params` | Get hyperparameters |

### Models (Phase 3) ⭐ NEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/models` | List models |
| GET | `/api/models/{id}` | Model details |
| GET | `/api/models/recent` | Recent models |
| GET | `/api/models/compare?ids=` | Compare models |

### Model Evaluation (Phase 3) ⭐ NEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/models/{id}/metrics` | All metrics |
| GET | `/api/models/{id}/confusion-matrix` | Confusion matrix |
| GET | `/api/models/{id}/roc-curve` | ROC curve data |
| GET | `/api/models/{id}/pr-curve` | PR curve data |
| GET | `/api/models/{id}/feature-importance` | Feature importance |
| GET | `/api/models/{id}/learning-curve` | Learning curve |
| GET | `/api/models/{id}/health` | Model health |
| GET | `/api/models/{id}/training-details` | Training details |

### Model Deployment (Phase 3) ⭐ NEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/models/{id}/deploy` | Deploy model |
| POST | `/api/models/{id}/undeploy` | Undeploy model |

## 🔗 URLs

- **API**: http://localhost:8080/api
- **Swagger**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console

## ✅ Phase Status

- ✅ Phase 1: Projects, Datasets, Data Sources
- ✅ Phase 2: Training Jobs, Algorithms
- ✅ Phase 3: Models, Evaluation, Metrics
- 🔜 Phase 4: Interpretability (SHAP/LIME)
- 🔜 Phase 5: Predictions (Single/Batch)
