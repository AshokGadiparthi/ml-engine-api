# EDA (Exploratory Data Analysis) Integration Guide - ML Engine Backend API

## Overview

This document provides comprehensive guidance on integrating the EDA (Exploratory Data Analysis) module into the ML Engine Backend API. The EDA module provides data quality assessment, feature analysis, and insights generation for uploaded datasets.

---

## Architecture

### Components Created

1. **EDADTO.java** - Data Transfer Objects for all EDA requests and responses
2. **EDAAnalysis.java** - JPA Entity for persisting EDA results in database
3. **EDAAnalysisRepository.java** - Spring Data JPA Repository with custom queries
4. **EDAService.java** - Business logic service for EDA operations
5. **EDAController.java** - REST API endpoints for EDA functionality

### Technology Stack

- **Framework**: Spring Boot 3.2.1
- **ORM**: JPA/Hibernate
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Validation**: Jakarta Validation
- **JSON Processing**: Jackson ObjectMapper
- **Logging**: SLF4J with Lombok

---

## File Locations

```
src/main/java/com/mlengine/
├── controller/
│   └── EDAController.java              (NEW - REST endpoints)
├── service/
│   └── EDAService.java                 (NEW - Business logic)
├── repository/
│   └── EDAAnalysisRepository.java      (NEW - Database access)
└── model/
    ├── entity/
    │   └── EDAAnalysis.java            (NEW - Database entity)
    └── dto/
        └── EDADTO.java                 (NEW - Request/Response DTOs)
```

---

## REST API Endpoints

### 1. Analyze Dataset
**POST** `/api/eda/analyze`

Perform comprehensive exploratory data analysis on a dataset.

**Request:**
```json
{
  "datasetId": "ds_12345",
  "targetColumn": "target",
  "sampleRows": 5000,
  "projectId": "proj_123"
}
```

**Response:**
```json
{
  "edaId": "eda_abc123",
  "datasetId": "ds_12345",
  "status": "COMPLETED",
  "quality": {
    "overallScore": 85.5,
    "assessment": "Good",
    "completeness": 95.0,
    "uniqueness": 98.0,
    "consistency": 80.0,
    "validity": 85.0,
    "rowCount": 5000,
    "columnCount": 20,
    "missingValues": 250,
    "duplicateRows": 5
  },
  "features": {
    "totalFeatures": 20,
    "numericFeatures": 12,
    "categoricalFeatures": 8,
    "dateTimeFeatures": 0,
    "statistics": [...],
    "correlations": [...]
  },
  "insights": [...],
  "timestamp": "2025-01-12T10:30:00"
}
```

**Status Codes:**
- `200 OK` - Analysis completed successfully
- `400 Bad Request` - Invalid request format
- `404 Not Found` - Dataset not found
- `500 Internal Server Error` - Analysis failed

---

### 2. Get Summary
**GET** `/api/eda/summary/{edaId}`

Retrieve executive summary of an EDA analysis.

**Response:**
```json
{
  "edaId": "eda_abc123",
  "datasetId": "ds_12345",
  "qualityScore": 85.5,
  "assessment": "Good",
  "rowCount": 5000,
  "columnCount": 20,
  "missingPercentage": 5.0,
  "duplicateRowsCount": 5,
  "criticalIssues": 0,
  "highIssues": 1,
  "topConcern": "High Missing Data Detected",
  "recommendation": "Review data quality and address identified issues",
  "timestamp": "2025-01-12T10:30:00"
}
```

---

### 3. Get Quality Metrics
**GET** `/api/eda/quality/{edaId}`

Retrieve detailed quality metrics.

**Response:**
```json
{
  "edaId": "eda_abc123",
  "metrics": {
    "overallScore": 85.5,
    "assessment": "Good",
    "completeness": 95.0,
    "uniqueness": 98.0,
    "consistency": 80.0,
    "validity": 85.0,
    "rowCount": 5000,
    "columnCount": 20,
    "missingValues": 250,
    "duplicateRows": 5
  },
  "assessment": {
    "completenessStatus": "GOOD",
    "consistencyStatus": "GOOD",
    "validityStatus": "GOOD",
    "uniquenessStatus": "EXCELLENT",
    "overallAssessment": "Good"
  },
  "recommendations": [...]
}
```

---

### 4. Get Insights
**GET** `/api/eda/insights/{edaId}`

Retrieve all insights with severity levels.

**Response:**
```json
{
  "edaId": "eda_abc123",
  "totalCount": 3,
  "criticalCount": 0,
  "highCount": 1,
  "mediumCount": 1,
  "lowCount": 1,
  "insights": [
    {
      "id": "insight_12345",
      "title": "High Missing Data Detected",
      "description": "5.0% of data is missing",
      "type": "missing_data",
      "severity": "HIGH",
      "affectedFeatures": ["column_a", "column_b"],
      "recommendation": "Consider imputation strategies"
    }
  ]
}
```

---

### 5. Get Feature Analysis
**GET** `/api/eda/features/{edaId}`

Retrieve feature-level statistics.

**Response:**
```json
{
  "edaId": "eda_abc123",
  "analysis": {
    "totalFeatures": 20,
    "numericFeatures": 12,
    "categoricalFeatures": 8,
    "dateTimeFeatures": 0,
    "statistics": [
      {
        "name": "age",
        "dataType": "numeric",
        "missingCount": 10,
        "missingPercentage": 0.2,
        "uniqueCount": 85,
        "mean": 42.5,
        "stdDev": 15.3,
        "min": 18.0,
        "median": 41.0,
        "max": 90.0
      }
    ],
    "correlations": [
      {
        "feature1": "age",
        "feature2": "income",
        "correlationValue": 0.75,
        "strength": "Strong"
      }
    ]
  }
}
```

---

### 6. Get Feature Importance
**GET** `/api/eda/importance/{edaId}?limit=10`

Retrieve ranked features by importance.

**Response:**
```json
{
  "edaId": "eda_abc123",
  "rankings": [
    {
      "rank": 1,
      "feature": "income",
      "importance": 0.98,
      "dataType": "numeric",
      "missingPercentage": 0.5
    },
    {
      "rank": 2,
      "feature": "age",
      "importance": 0.95,
      "dataType": "numeric",
      "missingPercentage": 0.2
    }
  ]
}
```

---

### 7. Latest Analysis for Dataset
**GET** `/api/eda/dataset/{datasetId}/latest`

Retrieve most recent analysis for a dataset.

---

### 8. List Project Analyses
**GET** `/api/eda/project/{projectId}?page=0&size=10&sort=createdAt&direction=DESC`

List all analyses for a project with pagination.

**Response:**
```json
{
  "content": [
    {
      "edaId": "eda_abc123",
      "datasetId": "ds_12345",
      "datasetName": "Customer Data",
      "qualityScore": 85.5,
      "timestamp": "2025-01-12T10:30:00",
      "insightsCount": 3
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "number": 0,
  "size": 10
}
```

---

### 9. Health Check
**GET** `/api/eda/health`

Check EDA service status.

**Response:**
```json
{
  "status": "UP",
  "available": true,
  "message": "EDA service is operational",
  "timestamp": "2025-01-12T10:35:00"
}
```

---

### 10. Compare Analyses
**GET** `/api/eda/compare?edaId1=eda_123&edaId2=eda_456`

Compare quality metrics between two datasets.

---

### 11. Get Recommendations
**GET** `/api/eda/{edaId}/recommendations`

Get actionable improvement recommendations.

---

## Data Models

### Quality Metrics
- **overallScore** (0-100): Weighted average of all metrics
- **completeness**: Percentage of non-missing values
- **uniqueness**: Percentage of unique rows
- **consistency**: Data consistency score
- **validity**: Data validity score

### Insight Severity Levels
- **CRITICAL**: Requires immediate action
- **HIGH**: Should be addressed soon
- **MEDIUM**: Should be considered
- **LOW**: Minor issues
- **INFO**: Informational only

### Feature Data Types
- **numeric**: Numeric values (int, float, decimal)
- **categorical**: Category values (string, enum)
- **datetime**: Date and time values
- **string**: Text/string data

---

## Database Schema

### eda_analyses Table

```sql
CREATE TABLE eda_analyses (
  id VARCHAR(36) PRIMARY KEY,
  eda_id VARCHAR(255) UNIQUE NOT NULL,
  dataset_id VARCHAR(255) NOT NULL,
  dataset_name VARCHAR(255) NOT NULL,
  project_id VARCHAR(255) NOT NULL,
  overall_quality_score DOUBLE NOT NULL,
  quality_assessment VARCHAR(50),
  completeness DOUBLE,
  uniqueness DOUBLE,
  consistency DOUBLE,
  validity DOUBLE,
  row_count BIGINT NOT NULL,
  column_count INT NOT NULL,
  missing_values BIGINT,
  duplicate_rows BIGINT,
  missing_percentage DOUBLE,
  numeric_features INT,
  categorical_features INT,
  datetime_features INT,
  total_insights INT,
  critical_insights INT,
  high_insights INT,
  medium_insights INT,
  low_insights INT,
  top_concern VARCHAR(500),
  recommendation VARCHAR(1000),
  quality_metrics_json TEXT,
  features_analysis_json TEXT,
  insights_json TEXT,
  correlations_json TEXT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  status VARCHAR(50),
  analysis_type VARCHAR(50),
  sample_rows INT,
  analysis_time_ms BIGINT,
  
  INDEX idx_dataset_id (dataset_id),
  INDEX idx_quality_score (overall_quality_score),
  INDEX idx_created_at (created_at)
);
```

---

## Integration Steps

### Step 1: Add Files
Copy all created files to their respective locations in your project.

### Step 2: Update Dependencies
The required dependencies are already in `pom.xml`:
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Jackson for JSON processing
- Lombok for code generation

No additional Maven dependencies needed.

### Step 3: Database Migration
Run the SQL schema to create the `eda_analyses` table, or use Hibernate auto-schema-generation by adding to `application.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

### Step 4: Configure Application
Add to `application.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

logging:
  level:
    com.mlengine.service.EDAService: DEBUG
```

### Step 5: Build and Test
```bash
mvn clean install
mvn spring-boot:run
```

---

## Usage Examples

### Example 1: Analyze a Dataset
```bash
curl -X POST http://localhost:8080/api/eda/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "datasetId": "ds_12345",
    "projectId": "proj_123",
    "targetColumn": "target"
  }'
```

### Example 2: Get Quality Metrics
```bash
curl -X GET http://localhost:8080/api/eda/quality/eda_abc123
```

### Example 3: Get Insights
```bash
curl -X GET http://localhost:8080/api/eda/insights/eda_abc123
```

### Example 4: List Project Analyses
```bash
curl -X GET "http://localhost:8080/api/eda/project/proj_123?page=0&size=10"
```

---

## Integration with Frontend

### React Example
```javascript
// Call EDA analysis
const analyzeDataset = async (datasetId, projectId) => {
  try {
    const response = await fetch('/api/eda/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        datasetId,
        projectId,
        targetColumn: 'target'
      })
    });
    const data = await response.json();
    console.log('EDA Results:', data);
    return data;
  } catch (error) {
    console.error('EDA failed:', error);
  }
};

// Get quality metrics
const getQuality = async (edaId) => {
  const response = await fetch(`/api/eda/quality/${edaId}`);
  return response.json();
};

// Get insights
const getInsights = async (edaId) => {
  const response = await fetch(`/api/eda/insights/${edaId}`);
  return response.json();
};
```

---

## Performance Considerations

### Database Indexing
The `eda_analyses` table includes strategic indexes:
- `dataset_id` - for quick lookups by dataset
- `overall_quality_score` - for filtering by quality
- `created_at` - for time-based queries

### Query Optimization
- Use pagination for listing large numbers of analyses
- Filter by project before retrieving individual records
- Cache frequently accessed results

### JSON Storage
Large JSON objects (features, insights, correlations) are stored as TEXT:
- Provides flexibility for schema changes
- Allows querying details without modifying table schema
- Indexed searches require parsing

---

## Error Handling

### Common Errors

| Error | Status | Cause | Solution |
|-------|--------|-------|----------|
| Dataset not found | 404 | Invalid dataset ID | Verify dataset exists |
| EDA analysis not found | 404 | Invalid EDA ID | Check analysis ID |
| Analysis failed | 500 | Processing error | Check logs for details |
| Invalid request | 400 | Malformed JSON | Verify request format |

---

## Monitoring and Logging

### Key Metrics to Monitor
- Analysis completion time
- Quality score distribution
- Insight types and frequencies
- Dataset characteristics

### Log Levels
```
DEBUG: Detailed analysis steps
INFO: Analysis start/completion
WARN: Quality concerns
ERROR: Processing failures
```

---

## Future Enhancements

1. **Advanced Statistics**
   - Normality tests
   - Outlier detection
   - Skewness and kurtosis analysis

2. **Machine Learning Integration**
   - Feature selection algorithms
   - Dimensionality reduction recommendations
   - Automated feature engineering

3. **Visualization**
   - Distribution plots
   - Correlation heatmaps
   - Quality scorecards

4. **Batch Processing**
   - Analyze multiple datasets
   - Scheduled EDA jobs
   - Automated quality monitoring

5. **Comparison Tools**
   - Trend analysis over time
   - Before/after data quality comparison
   - Dataset similarity analysis

---

## Troubleshooting

### Issue: Analysis takes too long
**Solution:**
- Reduce `sampleRows` parameter
- Increase database connection pool
- Check system resources

### Issue: Out of memory
**Solution:**
- Reduce `sampleRows`
- Increase JVM heap size
- Process in batches

### Issue: Insights not generated
**Solution:**
- Check dataset structure
- Verify data types are correct
- Review error logs

---

## Support

For issues or questions:
1. Check application logs
2. Verify database connectivity
3. Review endpoint documentation
4. Test with curl/Postman first

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-01-12 | Initial EDA module release |

---

## License

This EDA module is part of the ML Engine Backend API project.

---

**Last Updated**: January 12, 2025
**Module Version**: 1.0.0
