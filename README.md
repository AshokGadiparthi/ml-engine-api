# ML Engine API v2.0

Enterprise Machine Learning Platform REST API - Phase 1: Project & Dataset Management

## 🚀 Quick Start

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Or
java -jar target/ml-engine-api-2.0.0.jar
```

## 📡 API Endpoints

### Health & Status
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api` | API status |
| GET | `/api/health` | Health check |
| GET | `/api/algorithms` | List available algorithms |

### Projects
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create project |
| GET | `/api/projects` | List all projects |
| GET | `/api/projects/{id}` | Get project details |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |
| GET | `/api/projects/{id}/stats` | Get dashboard statistics |

### Datasets
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasets` | Upload dataset (multipart) |
| GET | `/api/datasets` | List datasets |
| GET | `/api/datasets/{id}` | Get dataset details |
| GET | `/api/datasets/{id}/preview` | Preview data (first N rows) |
| GET | `/api/datasets/{id}/columns` | Get column metadata |
| GET | `/api/datasets/{id}/quality` | Get quality report |
| PUT | `/api/datasets/{id}` | Update dataset |
| DELETE | `/api/datasets/{id}` | Delete dataset |

### Data Sources
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/datasources` | Create data source |
| GET | `/api/datasources` | List data sources |
| GET | `/api/datasources/{id}` | Get data source details |
| POST | `/api/datasources/test` | Test connection (without saving) |
| POST | `/api/datasources/{id}/test` | Test existing connection |
| PUT | `/api/datasources/{id}` | Update data source |
| DELETE | `/api/datasources/{id}` | Delete data source |

## 🔗 URLs

- **API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
- **Health**: http://localhost:8080/actuator/health

## 📊 React UI Mapping

### Dashboard
```
GET /api/projects/{id}/stats
Response:
{
  "modelsCount": 8,
  "deployedModelsCount": 2,
  "datasetsCount": 3,
  "totalDataSize": "2.4 GB",
  "avgAccuracy": 93.5,
  "accuracyTrend": 2.3,
  "predictionsCount": 12450,
  "predictionsThisMonth": 12450
}
```

### Data Management - Upload File
```
POST /api/datasets
Content-Type: multipart/form-data

file: [CSV file]
name: "Customer Transactions"
description: "Customer transaction data"
projectId: "project-uuid"
```

### Data Management - Connect Source
```
POST /api/datasources
{
  "name": "Production Database",
  "sourceType": "POSTGRESQL",
  "host": "localhost",
  "port": 5432,
  "databaseName": "analytics",
  "username": "db_user",
  "password": "********",
  "projectId": "project-uuid"
}
```

## 🗄️ Database

Using H2 embedded database for development:

- **JDBC URL**: `jdbc:h2:file:./data/mlengine`
- **Username**: `sa`
- **Password**: (empty)

For production, switch to PostgreSQL in `application.yml`.

## 📁 Storage

Files are stored in:
```
./storage/
├── datasets/      # Uploaded dataset files
├── models/        # Trained model files
├── temp/          # Temporary files
└── exports/       # Exported reports
```

## 🔒 CORS

Configured for React development:
- http://localhost:3000 (Create React App)
- http://localhost:5173 (Vite)

## 📋 Phase 1 Complete

- ✅ Project CRUD
- ✅ Dataset upload & management
- ✅ Data source connections
- ✅ Dashboard statistics
- ✅ Data preview
- ✅ Quality analysis
- ✅ H2 database persistence

## 🚧 Coming in Phase 2

- Training job management
- Model training with progress
- Algorithm selection & hyperparameters
