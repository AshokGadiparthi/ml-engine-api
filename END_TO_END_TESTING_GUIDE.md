# ML Platform - End-to-End Testing Guide

## Overview

This guide walks you through testing the complete ML Platform from scratch with a fresh database.

### Model Categories

| Source | Description | Created By |
|--------|-------------|------------|
| `AUTOML` | Automatic algorithm selection & hyperparameter tuning | AutoML Engine |
| `TRAINING` | Manual algorithm selection by user | Model Training |

Both model types work identically for predictions - the only difference is how they were created.

---

## Step 0: Clean Database & Restart

### 0.1 Stop All Services
```bash
# Stop Spring Boot (Ctrl+C or kill the process)
# Stop FastAPI if needed
```

### 0.2 Delete Spring Boot H2 Database
```bash
cd /path/to/ml-engine-api
rm -rf data/
```

### 0.3 (Optional) Clean FastAPI Models
If you want a completely fresh start, also clean FastAPI models:
```bash
cd /path/to/ml-engine-fastapi-service
rm -rf models/*
```

### 0.4 Start Services
```bash
# Terminal 1: Start FastAPI
cd /path/to/ml-engine-fastapi-service
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Terminal 2: Start Spring Boot
cd /path/to/ml-engine-api
./mvnw spring-boot:run

# Terminal 3: Start Angular (if not running)
cd /path/to/ml-platform-ui
ng serve --host 0.0.0.0 --port 4200
```

### 0.5 Verify Services
- Spring Boot: http://localhost:8080/api/health
- FastAPI: http://localhost:8000/health
- Angular: http://localhost:4200

---

## Step 1: Create Project

### Via UI
1. Open http://localhost:4200
2. Click **"+ New Project"**
3. Fill in:
   - Name: `Loan Approval Project`
   - Description: `Testing loan approval predictions`
4. Click **Create**

### Via API (alternative)
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Loan Approval Project",
    "description": "Testing loan approval predictions"
  }'
```

### ✅ Verify
- Project appears in sidebar
- Project shows "0 models • 0 datasets"

---

## Step 2: Upload Dataset

### Via UI
1. Go to **Data Management** in sidebar
2. Click **"Upload Dataset"**
3. Upload your CSV file (e.g., `sample_data.csv`)
4. Configure:
   - Name: `Loan Data`
   - Description: `Sample loan application data`
5. Click **Upload**

### ✅ Verify
- Dataset appears in Data Management list
- Click on dataset to see preview
- Check columns are detected correctly

### Sample Dataset Structure
Your CSV should have columns like:
```csv
age,annual_income,credit_score,loan_amount,employment_years,existing_loans,loan_approved
35,75000,720,250000,8,1,1
28,45000,650,150000,3,2,0
...
```

---

## Step 3: Train Model via AutoML (Source: AUTOML)

### Via UI
1. Go to **AutoML Engine** in sidebar
2. Click **"New AutoML Job"**
3. Configure:
   - Dataset: Select `Loan Data`
   - Target Column: `loan_approved`
   - Problem Type: `Classification`
   - Name: `AutoML Loan Model`
4. Click **Start AutoML**
5. Wait for training to complete (watch progress)

### ✅ Verify
- Job status changes: QUEUED → RUNNING → COMPLETED
- Leaderboard shows algorithm comparison
- Best model is highlighted
- **Model is automatically created** with `source = AUTOML`

### Check in H2 Console
```sql
SELECT id, name, source, model_path, accuracy 
FROM models 
WHERE source = 'AUTOML';
```

Expected: Model with `model_path` NOT NULL ✅

---

## Step 4: Train Model via Model Training (Source: TRAINING)

### Via UI
1. Go to **Model Training** in sidebar
2. Click **"New Training Job"**
3. Configure:
   - Dataset: Select `Loan Data`
   - Target Column: `loan_approved`
   - Algorithm: Select `XGBoost` or `Random Forest`
   - Name: `Manual XGBoost Model`
4. Click **Start Training**
5. Wait for training to complete

### ✅ Verify
- Job completes successfully
- Model appears in Models list
- Model has `source = TRAINING`

### Check in H2 Console
```sql
SELECT id, name, source, model_path, accuracy 
FROM models 
WHERE source = 'TRAINING';
```

Expected: Model with `model_path` NOT NULL ✅

---

## Step 5: Verify All Models

### Check Both Model Types Exist
```sql
SELECT id, name, source, source_job_id, model_path, accuracy 
FROM models 
ORDER BY created_at DESC;
```

Expected output:
| name | source | model_path |
|------|--------|------------|
| Random Forest - AutoML Loan Model | AUTOML | uuid-here |
| Manual XGBoost Model | TRAINING | uuid-here |

Both should have `model_path` set! ✅

---

## Step 6: Test Single Prediction

### Via UI
1. Go to **Predictions** in sidebar
2. Select **Single Prediction** tab
3. Select a model (either AUTOML or TRAINING)
4. Fill in features:
   - Age: `35`
   - Annual Income: `75000`
   - Credit Score: `720`
   - Loan Amount: `250000`
   - Employment Years: `8`
   - Existing Loans: `1`
5. Click **"Make Prediction"**

### ✅ Verify
- Prediction result shows (Approved/Rejected)
- Confidence score displays
- Probability distribution chart shows
- No errors in console

### Test Both Models
1. Test with AUTOML model
2. Test with TRAINING model
3. Both should return REAL predictions (not demo/mock)

---

## Step 7: Test Batch Prediction

### Via UI
1. Go to **Predictions** → **Batch Prediction** tab
2. Select a model
3. Upload a CSV file with multiple records
4. Click **"Start Batch Prediction"**
5. Wait for processing

### Sample Batch CSV
```csv
age,annual_income,credit_score,loan_amount,employment_years,existing_loans
35,75000,720,250000,8,1
28,45000,650,150000,3,2
42,95000,780,300000,12,0
```

### ✅ Verify
- Progress bar updates
- Status changes: PENDING → PROCESSING → COMPLETED
- Download results CSV
- Results show predictions for each row

---

## Step 8: Test Prediction History

### Via UI
1. Go to **Predictions** → **History** tab
2. Verify all predictions appear

### Test Filters
1. Filter by **Type**: Single, Batch, API
2. Filter by **Model**: Select specific model
3. Filter by **Result**: Approved, Rejected
4. Filter by **Date Range**

### ✅ Verify
- Predictions list correctly
- Filters work
- Stats cards show correct counts
- Click on prediction to see details modal

---

## Step 9: Test API Integration

### Via UI
1. Go to **Predictions** → **API Integration** tab
2. Select a model
3. Click **"Generate API Key"**
4. Copy the generated key

### Test API Endpoint
```bash
# Replace with your actual values
MODEL_ID="your-model-id"
API_KEY="your-api-key"

curl -X POST "http://localhost:8080/api/predictions/v1/models/${MODEL_ID}/predict" \
  -H "Authorization: Bearer ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "features": {
      "age": 35,
      "annual_income": 75000,
      "credit_score": 720,
      "loan_amount": 250000,
      "employment_years": 8,
      "existing_loans": 1
    }
  }'
```

### ✅ Verify
- API returns prediction result
- Response includes prediction, probability, confidence
- Check API usage stats update in UI

---

## Step 10: Verify in H2 Console

### Access H2 Console
URL: http://localhost:8080/h2-console

Settings:
- JDBC URL: `jdbc:h2:file:./data/mlengine`
- Username: `sa`
- Password: `password` (or blank)

### Verification Queries

#### Check Models
```sql
SELECT id, name, source, model_path, accuracy, created_at
FROM models 
ORDER BY created_at DESC;
```

#### Check Predictions
```sql
SELECT id, model_name, prediction_type, source, predicted_class, confidence, created_at
FROM predictions 
ORDER BY created_at DESC 
LIMIT 20;
```

#### Check API Keys
```sql
SELECT id, key_prefix, model_id, is_active, total_requests
FROM api_keys;
```

#### Check Batch Jobs
```sql
SELECT id, job_name, model_name, status, total_records, processed_records
FROM batch_prediction_jobs;
```

---

## Troubleshooting

### Issue: "Model is not ready for predictions"
**Cause**: Model doesn't have `model_path` set
**Fix**: 
1. Check if training completed successfully
2. Verify FastAPI returned `model_id`
3. Re-train the model

### Issue: Prediction fails with connection error
**Cause**: FastAPI is not running
**Fix**: Start FastAPI service on port 8000

### Issue: Models list is empty
**Cause**: No models trained yet
**Fix**: Complete Step 3 or Step 4 first

### Issue: Demo mode appears
**Cause**: Using old code with demo fallback
**Fix**: Deploy the latest code (this package)

---

## Expected Final State

After completing all steps:

| Table | Expected Count |
|-------|---------------|
| projects | 1 |
| datasets | 1 |
| models | 2+ (1 AUTOML + 1 TRAINING) |
| predictions | Multiple (from testing) |
| api_keys | 1+ |
| batch_prediction_jobs | 1+ |

All models should have:
- `model_path` = FastAPI model ID (NOT NULL)
- `source` = either "AUTOML" or "TRAINING"
- `source_job_id` = ID of the job that created it

---

## Quick Commands

```bash
# Check FastAPI models
curl http://localhost:8000/api/models

# Check Spring Boot health
curl http://localhost:8080/api/health

# List all models via API
curl http://localhost:8080/api/models

# Make prediction via API
curl -X POST http://localhost:8080/api/predictions/single \
  -H "Content-Type: application/json" \
  -d '{
    "modelId": "YOUR_MODEL_ID",
    "features": {
      "age": 35,
      "annual_income": 75000,
      "credit_score": 720
    }
  }'
```

---

## Success Criteria ✅

- [ ] Fresh database created
- [ ] Project created successfully
- [ ] Dataset uploaded and visible
- [ ] AutoML training completes with model created
- [ ] Manual training completes with model created
- [ ] Both models have `model_path` set
- [ ] Single prediction works with REAL data
- [ ] Batch prediction works
- [ ] History shows all predictions
- [ ] API key generation works
- [ ] API prediction endpoint works
- [ ] No mock/demo data anywhere

**All done? Congratulations! Your ML Platform is fully functional! 🎉**
