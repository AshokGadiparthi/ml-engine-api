package com.mlengine.service;

import com.mlengine.client.FastAPIEvaluationClient;
import com.mlengine.model.dto.EvaluationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluation Service - FIXED VERSION
 * Handles business logic for model evaluation with proper snake_case handling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final FastAPIEvaluationClient fastAPIClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== THRESHOLD EVALUATION ====================

    public EvaluationDTO.ThresholdEvaluationResponse evaluateWithThreshold(
            String modelId,
            EvaluationDTO.ThresholdEvaluationRequest request
    ) {
        log.info("Evaluating model {} at threshold {}", modelId, request.getThreshold());

        try {
            Map<String, Object> result = fastAPIClient.evaluateWithThreshold(
                    modelId,
                    request.getYTrue(),
                    request.getYPredProba(),
                    request.getThreshold(),
                    request.getTargetNames()
            );

            return convertToThresholdResponse(result, modelId);

        } catch (Exception e) {
            log.error("Error evaluating model with threshold: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to evaluate model at threshold", e);
        }
    }

    // ==================== BUSINESS IMPACT ====================

    public EvaluationDTO.BusinessImpactResponse calculateBusinessImpact(
            String modelId,
            EvaluationDTO.BusinessImpactRequest request
    ) {
        log.info("Calculating business impact for model {}", modelId);

        try {
            // Convert the DTO evaluation_result back to snake_case for FastAPI
            Map<String, Object> evalResultSnakeCase = convertDTOToSnakeCaseMap(request.getEvaluationResult());

            Map<String, Object> result = fastAPIClient.calculateBusinessImpact(
                    modelId,
                    evalResultSnakeCase,
                    request.getCostFalsePositive(),
                    request.getCostFalseNegative(),
                    request.getRevenueTruePositive(),
                    request.getVolume()
            );

            return convertToBusinessImpactResponse(result, modelId);

        } catch (Exception e) {
            log.error("Error calculating business impact: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate business impact", e);
        }
    }

    // ==================== OPTIMAL THRESHOLD ====================

    public EvaluationDTO.OptimalThresholdResponse findOptimalThreshold(
            String modelId,
            EvaluationDTO.OptimalThresholdRequest request
    ) {
        log.info("Finding optimal threshold for model {}", modelId);

        try {
            Map<String, Object> result = fastAPIClient.getOptimalThreshold(
                    modelId,
                    request.getYTrue(),
                    request.getYPredProba(),
                    request.getCostFalsePositive(),
                    request.getCostFalseNegative(),
                    request.getRevenueTruePositive()
            );

            return convertToOptimalThresholdResponse(result, modelId);

        } catch (Exception e) {
            log.error("Error finding optimal threshold: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find optimal threshold", e);
        }
    }

    // ==================== PRODUCTION READINESS ====================

    public EvaluationDTO.ProductionReadinessResponse assessProductionReadiness(
            String modelId,
            EvaluationDTO.ProductionReadinessRequest request
    ) {
        log.info("Assessing production readiness for model {}", modelId);

        try {
            // Convert DTOs back to snake_case for FastAPI
            Map<String, Object> evalResultSnakeCase = convertDTOToSnakeCaseMap(request.getEvalResult());
            Map<String, Object> businessImpactSnakeCase = convertDTOToSnakeCaseMap(request.getBusinessImpact());

            Map<String, Object> result = fastAPIClient.assessProductionReadiness(
                    modelId,
                    evalResultSnakeCase,
                    request.getLearningCurve(),
                    businessImpactSnakeCase,
                    request.getFeatureImportance()
            );

            return convertToProductionReadinessResponse(result, modelId);

        } catch (Exception e) {
            log.error("Error assessing production readiness: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assess production readiness", e);
        }
    }

    // ==================== COMPLETE EVALUATION ====================

    public EvaluationDTO.CompleteEvaluationResponse completeEvaluation(
            String modelId,
            EvaluationDTO.CompleteEvaluationRequest request
    ) {
        log.info("Running complete evaluation for model {}", modelId);

        try {
            Map<String, Object> result = fastAPIClient.completeEvaluation(
                    modelId,
                    request.getXTest(),
                    request.getYTest(),
                    request.getYPredProba(),
                    request.getYTrain(),
                    request.getYPredTrain(),
                    request.getThreshold(),
                    request.getCostFp(),
                    request.getCostFn(),
                    request.getRevenueTp()
            );

            return convertToCompleteEvaluationResponse(result, modelId);

        } catch (Exception e) {
            log.error("Error in complete evaluation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to complete evaluation", e);
        }
    }

    // ==================== HEALTH & STATUS ====================

    public boolean isServiceAvailable() {
        return fastAPIClient.isServiceAvailable();
    }

    public Map<String, Object> getHealthStatus() {
        return fastAPIClient.getHealthStatus();
    }

    // ==================== CONVERSION METHODS ====================

    /**
     * Convert DTO objects back to snake_case Map for FastAPI
     */
    private Map<String, Object> convertDTOToSnakeCaseMap(Object dto) {
        if (dto == null) return null;
        // Convert DTO to Map using Jackson, then convert keys to snake_case
        Map<String, Object> map = objectMapper.convertValue(dto, Map.class);
        return convertKeysToSnakeCase(map);
    }

    /**
     * Recursively convert all map keys from camelCase to snake_case
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertKeysToSnakeCase(Map<String, Object> map) {
        if (map == null) return null;

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String snakeCaseKey = camelToSnakeCase(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Map) {
                value = convertKeysToSnakeCase((Map<String, Object>) value);
            } else if (value instanceof List) {
                // Lists are fine as-is
            }

            result.put(snakeCaseKey, value);
        }
        return result;
    }

    /**
     * Convert camelCase to snake_case
     */
    private String camelToSnakeCase(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    // ==================== RESPONSE CONVERTERS ====================

    @SuppressWarnings("unchecked")
    private EvaluationDTO.ThresholdEvaluationResponse convertToThresholdResponse(
            Map<String, Object> result,
            String modelId
    ) {
        try {
            Object threshold = result.get("threshold");
            Object confusionMatrixObj = result.get("confusion_matrix");
            Object metricsObj = result.get("metrics");
            Object ratesObj = result.get("rates");

            if (threshold == null || confusionMatrixObj == null || metricsObj == null || ratesObj == null) {
                log.error("Missing required fields in threshold response: {}", result.keySet());
                throw new RuntimeException("Invalid response structure from FastAPI");
            }

            return EvaluationDTO.ThresholdEvaluationResponse.builder()
                    .modelId(modelId)
                    .threshold(((Number) threshold).doubleValue())
                    .confusionMatrix(convertConfusionMatrix((Map<String, Object>) confusionMatrixObj))
                    .metrics(convertMetrics((Map<String, Object>) metricsObj))
                    .rates(convertRates((Map<String, Object>) ratesObj))
                    .build();
        } catch (Exception e) {
            log.error("Error converting threshold response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert threshold response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.BusinessImpactResponse convertToBusinessImpactResponse(
            Map<String, Object> result,
            String modelId
    ) {
        try {
            Object costsObj = result.get("costs");
            Object revenueObj = result.get("revenue");
            Object financialObj = result.get("financial");

            if (costsObj == null || revenueObj == null || financialObj == null) {
                throw new RuntimeException("Invalid business impact response structure");
            }

            return EvaluationDTO.BusinessImpactResponse.builder()
                    .modelId(modelId)
                    .costs(convertCostMetrics((Map<String, Object>) costsObj))
                    .revenue(convertRevenueMetrics((Map<String, Object>) revenueObj))
                    .financial(convertFinancialSummary((Map<String, Object>) financialObj))
                    .build();
        } catch (Exception e) {
            log.error("Error converting business impact response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert business impact response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.OptimalThresholdResponse convertToOptimalThresholdResponse(
            Map<String, Object> result,
            String modelId
    ) {
        try {
            Object optimalThreshold = result.get("optimal_threshold");
            Object expectedProfit = result.get("expected_profit");
            Object metricsObj = result.get("metrics_at_threshold");

            if (optimalThreshold == null || expectedProfit == null) {
                log.warn("Missing fields in optimal threshold response. Keys: {}", result.keySet());
                throw new RuntimeException("Invalid optimal threshold response");
            }

            return EvaluationDTO.OptimalThresholdResponse.builder()
                    .modelId(modelId)
                    .optimalThreshold(((Number) optimalThreshold).doubleValue())
                    .expectedProfit(((Number) expectedProfit).doubleValue())
                    .metricsAtThreshold(metricsObj != null ? convertMetrics((Map<String, Object>) metricsObj) : null)
                    .build();
        } catch (Exception e) {
            log.error("Error converting optimal threshold response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert optimal threshold response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.ProductionReadinessResponse convertToProductionReadinessResponse(
            Map<String, Object> result,
            String modelId
    ) {
        try {
            Object statusObj = result.get("overall_status");
            Object summaryObj = result.get("summary");
            Object criteriaObj = result.get("criteria");

            if (statusObj == null || summaryObj == null) {
                throw new RuntimeException("Invalid production readiness response");
            }

            return EvaluationDTO.ProductionReadinessResponse.builder()
                    .modelId(modelId)
                    .overallStatus((String) statusObj)
                    .summary(convertReadinessSummary((Map<String, Object>) summaryObj))
                    .criteria(criteriaObj != null ? (List<EvaluationDTO.ReadinessCriterion>) criteriaObj : null)
                    .build();
        } catch (Exception e) {
            log.error("Error converting production readiness response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert production readiness response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.CompleteEvaluationResponse convertToCompleteEvaluationResponse(
            Map<String, Object> result,
            String modelId
    ) {
        try {
            Object thresholdObj = result.get("threshold_evaluation");
            Object impactObj = result.get("business_impact");
            Object readinessObj = result.get("production_readiness");
            Object scoreObj = result.get("overall_score");

            return EvaluationDTO.CompleteEvaluationResponse.builder()
                    .modelId(modelId)
                    .thresholdEvaluation(thresholdObj != null ?
                            convertToThresholdResponse((Map<String, Object>) thresholdObj, modelId) : null)
                    .businessImpact(impactObj != null ?
                            convertToBusinessImpactResponse((Map<String, Object>) impactObj, modelId) : null)
                    .productionReadiness(readinessObj != null ?
                            convertToProductionReadinessResponse((Map<String, Object>) readinessObj, modelId) : null)
                    .overallScore(scoreObj != null ? ((Number) scoreObj).doubleValue() : null)
                    .build();
        } catch (Exception e) {
            log.error("Error converting complete evaluation response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert complete evaluation response", e);
        }
    }

    // ==================== HELPER CONVERTERS ====================

    private EvaluationDTO.ConfusionMatrix convertConfusionMatrix(Map<String, Object> data) {
        if (data == null) return null;

        return EvaluationDTO.ConfusionMatrix.builder()
                .tn(toLong(data.get("tn")))
                .fp(toLong(data.get("fp")))
                .fn(toLong(data.get("fn")))
                .tp(toLong(data.get("tp")))
                .total(toLong(data.get("total")))
                .build();
    }

    private EvaluationDTO.Metrics convertMetrics(Map<String, Object> data) {
        if (data == null) return null;

        return EvaluationDTO.Metrics.builder()
                .accuracy(toDouble(data.get("accuracy")))
                .precision(toDouble(data.get("precision")))
                .recall(toDouble(data.get("recall")))
                .f1Score(toDouble(data.get("f1_score")))
                .aucRoc(toDouble(data.get("auc_roc")))
                .build();
    }

    private EvaluationDTO.Rates convertRates(Map<String, Object> data) {
        if (data == null) return null;

        return EvaluationDTO.Rates.builder()
                .falsePositiveRate(toDouble(data.get("false_positive_rate")))
                .falseNegativeRate(toDouble(data.get("false_negative_rate")))
                .truePositiveRate(toDouble(data.get("true_positive_rate")))
                .trueNegativeRate(toDouble(data.get("true_negative_rate")))
                .build();
    }

    private EvaluationDTO.CostMetrics convertCostMetrics(Map<String, Object> data) {
        if (data == null) return null;

        return EvaluationDTO.CostMetrics.builder()
                .falsePositiveCost(toDouble(data.get("false_positive_cost")))
                .falseNegativeCost(toDouble(data.get("false_negative_cost")))
                .totalCost(toDouble(data.get("total_cost")))
                .build();
    }

    private EvaluationDTO.RevenueMetrics convertRevenueMetrics(Map<String, Object> data) {
        if (data == null) return null;

        return EvaluationDTO.RevenueMetrics.builder()
                .truePositiveRevenue(toDouble(data.get("true_positive_revenue")))
                .revenueIfOptimal(toDouble(data.get("revenue_if_optimal")))
                .build();
    }

    private EvaluationDTO.FinancialSummary convertFinancialSummary(Map<String, Object> data) {
        if (data == null) return null;

        return EvaluationDTO.FinancialSummary.builder()
                .profit(toDouble(data.get("profit")))
                .improvementVsBaseline(toDouble(data.get("improvement_vs_baseline")))
                .build();
    }

    private EvaluationDTO.ReadinessSummary convertReadinessSummary(Map<String, Object> data) {
        if (data == null) return null;

        Integer passed = toLong(data.get("passed")).intValue();
        Integer total = toLong(data.get("total_criteria")).intValue();

        return EvaluationDTO.ReadinessSummary.builder()
                .passed(passed)
                .totalCriteria(total)
                .passPercentage(total > 0 ? (passed * 100.0) / total : 0.0)
                .build();
    }

    // ==================== TYPE CONVERSION HELPERS ====================

    private Double toDouble(Object value) {
        if (value == null) return null;
        return ((Number) value).doubleValue();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }
}
