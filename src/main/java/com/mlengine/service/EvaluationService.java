package com.mlengine.service;

import com.mlengine.client.FastAPIEvaluationClient;
import com.mlengine.model.dto.EvaluationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluation Service - FINAL FIXED VERSION
 * Properly handles snake_case conversion for FastAPI compatibility
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final FastAPIEvaluationClient fastAPIClient;

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
            // Convert evaluation_result to snake_case Map manually
            Map<String, Object> evalResultMap = convertThresholdResponseToSnakeCase(
                    request.getEvaluationResult()
            );

            Map<String, Object> result = fastAPIClient.calculateBusinessImpact(
                    modelId,
                    evalResultMap,
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
            // Convert eval_result to snake_case
            Map<String, Object> evalResultMap = convertThresholdResponseToSnakeCase(
                    request.getEvalResult()
            );

            // Convert business_impact to snake_case
            Map<String, Object> businessImpactMap = convertBusinessImpactToSnakeCase(
                    request.getBusinessImpact()
            );

            Map<String, Object> result = fastAPIClient.assessProductionReadiness(
                    modelId,
                    evalResultMap,
                    request.getLearningCurve(),
                    businessImpactMap,
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

    // ==================== CONVERSION HELPERS ====================

    /**
     * Convert ThresholdEvaluationResponse to snake_case Map for FastAPI
     */
    private Map<String, Object> convertThresholdResponseToSnakeCase(Object response) {
        if (response == null) return null;

        // If it's already a Map, convert the keys
        if (response instanceof Map) {
            return convertMapKeysToSnakeCase((Map<String, Object>) response);
        }

        // If it's a DTO, manually construct the snake_case map
        if (response instanceof EvaluationDTO.ThresholdEvaluationResponse) {
            EvaluationDTO.ThresholdEvaluationResponse r = (EvaluationDTO.ThresholdEvaluationResponse) response;
            Map<String, Object> map = new HashMap<>();
            map.put("modelId", r.getModelId());
            map.put("threshold", r.getThreshold());

            if (r.getConfusionMatrix() != null) {
                map.put("confusion_matrix", convertConfusionMatrixToMap(r.getConfusionMatrix()));
            }
            if (r.getMetrics() != null) {
                map.put("metrics", convertMetricsToMap(r.getMetrics()));
            }
            if (r.getRates() != null) {
                map.put("rates", convertRatesToMap(r.getRates()));
            }

            return map;
        }

        // If it's a plain map, just convert keys
        return convertMapKeysToSnakeCase((Map<String, Object>) response);
    }

    /**
     * Convert BusinessImpactResponse to snake_case Map for FastAPI
     */
    private Map<String, Object> convertBusinessImpactToSnakeCase(Object response) {
        if (response == null) return null;

        if (response instanceof Map) {
            return convertMapKeysToSnakeCase((Map<String, Object>) response);
        }

        if (response instanceof EvaluationDTO.BusinessImpactResponse) {
            EvaluationDTO.BusinessImpactResponse r = (EvaluationDTO.BusinessImpactResponse) response;
            Map<String, Object> map = new HashMap<>();
            map.put("modelId", r.getModelId());

            if (r.getCosts() != null) {
                map.put("costs", convertCostMetricsToMap(r.getCosts()));
            }
            if (r.getRevenue() != null) {
                map.put("revenue", convertRevenueMetricsToMap(r.getRevenue()));
            }
            if (r.getFinancial() != null) {
                map.put("financial", convertFinancialSummaryToMap(r.getFinancial()));
            }

            return map;
        }

        return convertMapKeysToSnakeCase((Map<String, Object>) response);
    }

    /**
     * Recursively convert all map keys from camelCase to snake_case
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertMapKeysToSnakeCase(Map<String, Object> map) {
        if (map == null) return null;

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String snakeCaseKey = camelToSnakeCase(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Map) {
                value = convertMapKeysToSnakeCase((Map<String, Object>) value);
            } else if (value instanceof List) {
                // Lists don't need conversion
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

    // ==================== DTO TO MAP CONVERTERS ====================

    private Map<String, Object> convertConfusionMatrixToMap(EvaluationDTO.ConfusionMatrix cm) {
        if (cm == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("tn", cm.getTn());
        map.put("fp", cm.getFp());
        map.put("fn", cm.getFn());
        map.put("tp", cm.getTp());
        map.put("total", cm.getTotal());
        return map;
    }

    private Map<String, Object> convertMetricsToMap(EvaluationDTO.Metrics m) {
        if (m == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("accuracy", m.getAccuracy());
        map.put("precision", m.getPrecision());
        map.put("recall", m.getRecall());
        map.put("f1_score", m.getF1Score());
        map.put("auc_roc", m.getAucRoc());
        return map;
    }

    private Map<String, Object> convertRatesToMap(EvaluationDTO.Rates r) {
        if (r == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("false_positive_rate", r.getFalsePositiveRate());
        map.put("false_negative_rate", r.getFalseNegativeRate());
        map.put("true_positive_rate", r.getTruePositiveRate());
        map.put("true_negative_rate", r.getTrueNegativeRate());
        return map;
    }

    private Map<String, Object> convertCostMetricsToMap(EvaluationDTO.CostMetrics c) {
        if (c == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("false_positive_cost", c.getFalsePositiveCost());
        map.put("false_negative_cost", c.getFalseNegativeCost());
        map.put("total_cost", c.getTotalCost());
        return map;
    }

    private Map<String, Object> convertRevenueMetricsToMap(EvaluationDTO.RevenueMetrics r) {
        if (r == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("true_positive_revenue", r.getTruePositiveRevenue());
        map.put("revenue_if_optimal", r.getRevenueIfOptimal());
        return map;
    }

    private Map<String, Object> convertFinancialSummaryToMap(EvaluationDTO.FinancialSummary f) {
        if (f == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("profit", f.getProfit());
        map.put("improvement_vs_baseline", f.getImprovementVsBaseline());
        return map;
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

            if (threshold == null) {
                log.error("Missing threshold in response. Keys: {}", result.keySet());
                throw new RuntimeException("Missing threshold field");
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
            return EvaluationDTO.BusinessImpactResponse.builder()
                    .modelId(modelId)
                    .costs(convertCostMetrics((Map<String, Object>) result.get("costs")))
                    .revenue(convertRevenueMetrics((Map<String, Object>) result.get("revenue")))
                    .financial(convertFinancialSummary((Map<String, Object>) result.get("financial")))
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
                log.error("Missing required fields in optimal threshold response. Keys: {}", result.keySet());
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

            if (statusObj == null) {
                log.error("Missing overall_status in response. Keys: {}", result.keySet());
                throw new RuntimeException("Invalid production readiness response");
            }

            return EvaluationDTO.ProductionReadinessResponse.builder()
                    .modelId(modelId)
                    .overallStatus((String) statusObj)
                    .summary(convertReadinessSummary((Map<String, Object>) summaryObj))
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
            Object scoreObj = result.get("overall_score");

            return EvaluationDTO.CompleteEvaluationResponse.builder()
                    .modelId(modelId)
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
