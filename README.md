# ML Engine API v2.0

Enterprise Machine Learning Platform REST API - Phases 1-4

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
| GET | `/api/projects/{id}/stats` | Dashboard stats |

### Datasets (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasets` | Upload dataset |
| GET | `/api/datasets/{id}/preview` | Preview data |
| GET | `/api/datasets/{id}/quality` | Quality report |

### Training Jobs (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/training/jobs` | Start training |
| GET | `/api/training/jobs/{id}/progress` | Job progress |

### Algorithms (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/algorithms` | List algorithms |
| GET | `/api/algorithms/{id}/params` | Hyperparameters |

### Models (Phase 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/models` | List models |
| GET | `/api/models/{id}/metrics` | All metrics |
| GET | `/api/models/{id}/confusion-matrix` | Confusion matrix |
| GET | `/api/models/{id}/roc-curve` | ROC curve |
| GET | `/api/models/{id}/feature-importance` | Feature importance |
| GET | `/api/models/{id}/learning-curve` | Learning curve |
| POST | `/api/models/{id}/deploy` | Deploy model |

### SHAP (Phase 4) ⭐ NEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/models/{id}/shap/global` | Global feature importance |
| GET | `/api/models/{id}/shap/summary` | Summary plot data |
| POST | `/api/models/{id}/shap/local` | Local explanation |

### LIME (Phase 4) ⭐ NEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/models/{id}/lime` | LIME explanation |

### PDP & ICE (Phase 4) ⭐ NEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/models/{id}/pdp/{feature}` | Partial Dependence Plot |
| GET | `/api/models/{id}/ice/{feature}` | ICE Plot |

### What-If Analysis (Phase 4) ⭐ NEW
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/models/{id}/whatif` | What-If analysis |
| POST | `/api/models/{id}/counterfactual` | Counterfactual explanation |

## 🔗 URLs

- **API**: http://localhost:8080/api
- **Swagger**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console

## ✅ Phase Status

- ✅ Phase 1: Projects, Datasets, Data Sources
- ✅ Phase 2: Training Jobs, Algorithms  
- ✅ Phase 3: Models, Evaluation, Metrics
- ✅ Phase 4: SHAP, LIME, PDP, What-If
- 🔜 Phase 5: Predictions (Single/Batch)
