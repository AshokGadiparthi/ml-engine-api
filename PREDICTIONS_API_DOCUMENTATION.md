# Predictions API - Complete Documentation

## Overview

This document covers the complete backend API implementation for the **Predictions Module** supporting all 6 UI screens:
1. Model Selection & Header Bar
2. Single Prediction
3. Batch Prediction  
4. History with Filtering
5. Prediction Details Modal
6. API Integration

## ❌ NO FastAPI or ML Engine Changes Required!

The existing FastAPI endpoints are sufficient:
- `POST /api/predictions/realtime/{modelId}` - Single prediction
- `POST /api/predictions/batch/{modelId}` - Batch prediction

All new features (history, filtering, API keys, usage stats) are handled in Spring Boot.

---

## API Endpoints Summary

### Single Prediction
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/predictions/single` | Make single prediction |
| GET | `/api/predictions/{id}` | Get prediction details (for modal) |
| GET | `/api/predictions/models/{modelId}/stats` | Get model stats (for header) |

### Batch Prediction
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/predictions/batch` | Start batch job (multipart) |
| POST | `/api/predictions/batch/validate` | Validate CSV before processing |
| GET | `/api/predictions/batch` | List all batch jobs |
| GET | `/api/predictions/batch/{jobId}` | Get job status |
| GET | `/api/predictions/batch/{jobId}/download` | Download results CSV |
| GET | `/api/predictions/batch/template/{modelId}` | Download template CSV |

### History
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/predictions/history` | Get filtered history with stats |
| GET | `/api/predictions/history/export` | Export history as CSV |

### API Integration
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/predictions/api-integration/{modelId}` | Full API info + code examples |
| POST | `/api/predictions/api-keys` | Create API key |
| POST | `/api/predictions/api-keys/{id}/regenerate` | Regenerate key |
| DELETE | `/api/predictions/api-keys/{id}` | Revoke key |
| GET | `/api/predictions/api-usage/{modelId}` | Usage statistics |
| GET | `/api/predictions/rate-limit/{modelId}` | Rate limit info |

### Public API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/predictions/v1/models/{modelId}/predict` | Public prediction endpoint |

---

## Detailed API Specifications

### 1. Single Prediction

**POST /api/predictions/single**

Request:
```json
{
  "modelId": "model-uuid",
  "projectId": "project-uuid",
  "features": {
    "age": 35,
    "annual_income": 75000,
    "credit_score": 720,
    "loan_amount": 250000,
    "employment_years": 8,
    "existing_loans": 1
  }
}
```

Response:
```json
{
  "predictionId": "pred-uuid",
  "modelId": "model-uuid",
  "modelName": "Loan Approval Model",
  "predictedClass": "Approved",
  "predictedLabel": "Approved",
  "probability": 0.873,
  "probabilityLabel": "87.3%",
  "confidence": 0.873,
  "confidenceLabel": "High Confidence",
  "probabilities": {
    "Approved": 0.873,
    "Rejected": 0.127
  },
  "riskLevel": "Low Risk",
  "riskColor": "green",
  "inputFeatures": { ... },
  "processingTimeMs": 45,
  "timestamp": "2026-01-11T10:30:00"
}
```

### 2. Model Stats (Header Bar)

**GET /api/predictions/models/{modelId}/stats**

Response:
```json
{
  "modelId": "model-uuid",
  "modelName": "Loan Approval Model",
  "algorithm": "xgboost",
  "accuracy": 0.923,
  "accuracyLabel": "92.3%",
  "trainedAt": "2026-01-10T14:30:00",
  "trainedAtLabel": "1/10/2026, 2:30:00 PM",
  "totalPredictions": 12847,
  "resultCounts": {
    "Approved": 9128,
    "Rejected": 3719
  }
}
```

### 3. History with Filtering

**GET /api/predictions/history**

Query Parameters:
- `projectId` - Filter by project
- `modelId` - Filter by model
- `type` - Filter by type: Single, Batch, API
- `result` - Filter by result: Approved, Rejected
- `dateRange` - Preset: today, 7days, 30days
- `startDate` - Custom start date
- `endDate` - Custom end date
- `page` - Page number (default: 0)
- `pageSize` - Items per page (default: 20)

Response:
```json
{
  "predictions": [
    {
      "predictionId": "pred-uuid",
      "modelId": "model-uuid",
      "modelName": "Loan Approval Model",
      "predictionType": "Single",
      "predictionTypeLabel": "Single",
      "source": "UI",
      "predictedClass": "Approved",
      "predictedLabel": "Approved",
      "probability": 0.873,
      "probabilityLabel": "87.3%",
      "confidence": 0.873,
      "confidenceLabel": "87.3% confidence",
      "riskLevel": "Low Risk",
      "riskColor": "green",
      "timestamp": "2026-01-11T10:30:00",
      "timestampLabel": "2 hours ago",
      "processingTimeMs": 45
    }
  ],
  "total": 12847,
  "page": 0,
  "pageSize": 20,
  "totalPages": 643,
  "stats": {
    "totalPredictions": 12847,
    "totalLabel": "12.8K",
    "resultCounts": {
      "Approved": 9128,
      "Rejected": 3719
    },
    "resultPercentages": {
      "Approved": 71.0,
      "Rejected": 29.0
    },
    "singleCount": 8234,
    "batchCount": 4128,
    "apiCount": 485,
    "todayCount": 156,
    "thisWeekCount": 892,
    "thisMonthCount": 3456,
    "avgConfidence": 0.847,
    "avgConfidenceLabel": "84.7%"
  }
}
```

### 4. API Integration Info

**GET /api/predictions/api-integration/{modelId}**

Response:
```json
{
  "modelId": "model-uuid",
  "modelName": "Loan Approval Model",
  "endpoint": "http://localhost:8080/api/predictions/v1/models/model-uuid/predict",
  "method": "POST",
  "apiKeyId": "key-uuid",
  "apiKeyPrefix": "mlk_abc12345...",
  "apiKeyCreatedAt": "2026-01-10T14:30:00",
  "usageStats": {
    "todayRequests": 420,
    "todayLabel": "420",
    "monthRequests": 12847,
    "monthLabel": "12.8K",
    "avgLatencyMs": 45.2,
    "avgLatencyLabel": "45ms",
    "successRate": 99.8,
    "successRateLabel": "99.8%",
    "dailyUsage": [...],
    "hourlyUsage": [...]
  },
  "rateLimit": {
    "limitPerHour": 1000,
    "usedThisHour": 420,
    "remainingThisHour": 580,
    "usagePercentage": 42.0,
    "usageLabel": "420 / 1,000 requests/hour"
  },
  "codeExamples": {
    "python": "import requests...",
    "javascript": "const response = await fetch...",
    "curl": "curl -X POST...",
    "java": "HttpClient client..."
  },
  "sampleRequest": {
    "features": {
      "age": 35,
      "annual_income": 75000,
      ...
    }
  },
  "sampleResponse": {
    "prediction": "approved",
    "confidence": 0.873,
    "probabilities": {...}
  }
}
```

### 5. Public API Endpoint

**POST /api/predictions/v1/models/{modelId}/predict**

Headers:
```
Authorization: Bearer mlk_your_api_key_here
Content-Type: application/json
```

Request:
```json
{
  "features": {
    "age": 35,
    "annual_income": 75000,
    "credit_score": 720,
    "loan_amount": 250000,
    "employment_years": 8,
    "existing_loans": 1
  }
}
```

Response:
```json
{
  "prediction": "approved",
  "confidence": 0.873,
  "probabilities": {
    "approved": 0.873,
    "rejected": 0.127
  },
  "latencyMs": 45
}
```

---

## Database Schema

### New Tables

```sql
-- API Keys table
CREATE TABLE api_keys (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    key_value VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(20),
    model_id VARCHAR(36),
    project_id VARCHAR(36),
    user_id VARCHAR(36),
    is_active BOOLEAN DEFAULT TRUE,
    rate_limit_per_hour INT DEFAULT 1000,
    rate_limit_per_day INT DEFAULT 10000,
    total_requests BIGINT DEFAULT 0,
    requests_today BIGINT DEFAULT 0,
    requests_this_hour BIGINT DEFAULT 0,
    last_used_at TIMESTAMP,
    last_reset_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP
);

-- API Usage Statistics table
CREATE TABLE api_usage_stats (
    id VARCHAR(36) PRIMARY KEY,
    api_key_id VARCHAR(36),
    model_id VARCHAR(36),
    project_id VARCHAR(36),
    stat_date DATE,
    stat_hour INT,
    request_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    total_latency_ms BIGINT DEFAULT 0,
    min_latency_ms BIGINT,
    max_latency_ms BIGINT,
    avg_latency_ms DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Updated Tables

```sql
-- Predictions table (new columns)
ALTER TABLE predictions ADD COLUMN source VARCHAR(20) DEFAULT 'UI';
ALTER TABLE predictions ADD COLUMN predicted_label VARCHAR(100);
ALTER TABLE predictions ADD COLUMN probabilities_json TEXT;
ALTER TABLE predictions ADD COLUMN predicted_value DOUBLE;
ALTER TABLE predictions ADD COLUMN api_key_id VARCHAR(36);

-- Batch Prediction Jobs (new column)
ALTER TABLE batch_prediction_jobs ADD COLUMN result_summary TEXT;
```

---

## Files Added/Modified

### New Files
- `ApiKey.java` - Entity for API keys
- `ApiUsageStat.java` - Entity for usage statistics
- `ApiKeyRepository.java` - Repository with key queries
- `ApiUsageStatRepository.java` - Repository with stats queries
- `ApiKeyService.java` - API key management and usage tracking

### Modified Files
- `Prediction.java` - Added source, probabilities, apiKeyId fields
- `BatchPredictionJob.java` - Added resultSummary field
- `PredictionRepository.java` - Added 30+ filtering/stats queries
- `PredictionDTO.java` - Added all DTOs for UI screens
- `PredictionService.java` - Complete rewrite (~1000 lines)
- `PredictionController.java` - 16 endpoints for all features

---

## React Integration Example

```typescript
// predictions.service.ts

const API_URL = 'http://localhost:8080/api/predictions';

// Single Prediction
export const makePrediction = async (modelId: string, features: Record<string, any>) => {
  const response = await fetch(`${API_URL}/single`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ modelId, features })
  });
  return response.json();
};

// Model Stats
export const getModelStats = async (modelId: string) => {
  const response = await fetch(`${API_URL}/models/${modelId}/stats`);
  return response.json();
};

// History with filters
export const getHistory = async (filters: HistoryFilter) => {
  const params = new URLSearchParams(filters as any);
  const response = await fetch(`${API_URL}/history?${params}`);
  return response.json();
};

// Prediction Detail (for modal)
export const getPredictionDetail = async (id: string) => {
  const response = await fetch(`${API_URL}/${id}`);
  return response.json();
};

// API Integration Info
export const getApiIntegrationInfo = async (modelId: string) => {
  const response = await fetch(`${API_URL}/api-integration/${modelId}`);
  return response.json();
};

// Batch Prediction
export const startBatchPrediction = async (modelId: string, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('modelId', modelId);
  
  const response = await fetch(`${API_URL}/batch`, {
    method: 'POST',
    body: formData
  });
  return response.json();
};
```

---

## Deployment Instructions

1. **Delete H2 database** to reset schema:
   ```bash
   rm -rf data/
   ```

2. **Copy new files** to your project

3. **Run Spring Boot**:
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Test endpoints** with Swagger UI:
   ```
   http://localhost:8080/swagger-ui.html
   ```

---

## UI Features Checklist

| Feature | Endpoint | Status |
|---------|----------|--------|
| Model stats header | GET /models/{id}/stats | ✅ |
| Single prediction form | POST /single | ✅ |
| Prediction result with confidence | POST /single | ✅ |
| Probability distribution | POST /single | ✅ |
| Risk level indicator | POST /single | ✅ |
| Batch file upload | POST /batch | ✅ |
| CSV validation | POST /batch/validate | ✅ |
| Download template | GET /batch/template/{id} | ✅ |
| Batch progress tracking | GET /batch/{jobId} | ✅ |
| Batch results summary | GET /batch/{jobId} | ✅ |
| Download results CSV | GET /batch/{jobId}/download | ✅ |
| History filtering by type | GET /history?type= | ✅ |
| History filtering by model | GET /history?modelId= | ✅ |
| History filtering by result | GET /history?result= | ✅ |
| History date range filter | GET /history?dateRange= | ✅ |
| History statistics cards | GET /history | ✅ |
| History pagination | GET /history?page=&pageSize= | ✅ |
| Export history CSV | GET /history/export | ✅ |
| Prediction detail modal | GET /{id} | ✅ |
| Input features grid | GET /{id} | ✅ |
| API endpoint display | GET /api-integration/{id} | ✅ |
| API key management | POST/DELETE /api-keys | ✅ |
| Regenerate API key | POST /api-keys/{id}/regenerate | ✅ |
| Usage statistics | GET /api-usage/{id} | ✅ |
| Rate limit progress bar | GET /rate-limit/{id} | ✅ |
| Code examples (4 languages) | GET /api-integration/{id} | ✅ |
| Sample request/response | GET /api-integration/{id} | ✅ |
| Public API endpoint | POST /v1/models/{id}/predict | ✅ |
