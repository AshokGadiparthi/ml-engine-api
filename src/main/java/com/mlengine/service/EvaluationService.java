package com.mlengine.service;

import com.mlengine.client.FastAPIEvaluationClient;
import com.mlengine.model.dto.EvaluationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * Evaluation Service
 * Handles business logic for model evaluation, business impact analysis,
 * production readiness assessment, and threshold optimization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final FastAPIEvaluationClient fastAPIClient;

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
            log.error("Error evaluating model with threshold: {}", e.getMessage());
            throw new RuntimeException("Failed to evaluate model at threshold", e);
        }
    }

    public EvaluationDTO.BusinessImpactResponse calculateBusinessImpact(
            String modelId,
            EvaluationDTO.BusinessImpactRequest request
    ) {
        log.info("Calculating business impact for model {}", modelId);

        try {
            Map<String, Object> result = fastAPIClient.calculateBusinessImpact(
                    modelId,
                    request.getEvaluationResult(),
                    request.getCostFalsePositive(),
                    request.getCostFalseNegative(),
                    request.getRevenueTruePositive(),
                    request.getVolume()
            );

            return convertToBusinessImpactResponse(result, modelId);

        } catch (Exception e) {
            log.error("Error calculating business impact: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate business impact", e);
        }
    }

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
            log.error("Error finding optimal threshold: {}", e.getMessage());
            throw new RuntimeException("Failed to find optimal threshold", e);
        }
    }

    public EvaluationDTO.ProductionReadinessResponse assessProductionReadiness(
            String modelId,
            EvaluationDTO.ProductionReadinessRequest request
    ) {
        log.info("Assessing production readiness for model {}", modelId);

        try {
            Map<String, Object> result = fastAPIClient.assessProductionReadiness(
                    modelId,
                    request.getEvalResult(),
                    request.getLearningCurve(),
                    request.getBusinessImpact(),
                    request.getFeatureImportance()
            );

            return convertToProductionReadinessResponse(result, modelId);

        } catch (Exception e) {
            log.error("Error assessing production readiness: {}", e.getMessage());
            throw new RuntimeException("Failed to assess production readiness", e);
        }
    }

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
            log.error("Error in complete evaluation: {}", e.getMessage());
            throw new RuntimeException("Failed to complete evaluation", e);
        }
    }

    public boolean isServiceAvailable() {
        return fastAPIClient.isServiceAvailable();
    }

    public Map<String, Object> getHealthStatus() {
        return fastAPIClient.getHealthStatus();
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.ThresholdEvaluationResponse convertToThresholdResponse(
            Map<String, Object> result,
            String modelId
    ) {
        return EvaluationDTO.ThresholdEvaluationResponse.builder()
                .modelId(modelId)
                .threshold(((Number) result.get("threshold")).doubleValue())
                .confusionMatrix(convertConfusionMatrix((Map<String, Object>) result.get("confusion_matrix")))
                .metrics(convertMetrics((Map<String, Object>) result.get("metrics")))
                .rates(convertRates((Map<String, Object>) result.get("rates")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.BusinessImpactResponse convertToBusinessImpactResponse(
            Map<String, Object> result,
            String modelId
    ) {
        return EvaluationDTO.BusinessImpactResponse.builder()
                .modelId(modelId)
                .costs(convertCostMetrics((Map<String, Object>) result.get("costs")))
                .revenue(convertRevenueMetrics((Map<String, Object>) result.get("revenue")))
                .financial(convertFinancialSummary((Map<String, Object>) result.get("financial")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.OptimalThresholdResponse convertToOptimalThresholdResponse(
            Map<String, Object> result,
            String modelId
    ) {
        return EvaluationDTO.OptimalThresholdResponse.builder()
                .modelId(modelId)
                .optimalThreshold(((Number) result.get("optimal_threshold")).doubleValue())
                .expectedProfit(((Number) result.get("expected_profit")).doubleValue())
                .metricsAtThreshold(convertMetrics((Map<String, Object>) result.get("metrics_at_threshold")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.ProductionReadinessResponse convertToProductionReadinessResponse(
            Map<String, Object> result,
            String modelId
    ) {
        return EvaluationDTO.ProductionReadinessResponse.builder()
                .modelId(modelId)
                .overallStatus((String) result.get("overall_status"))
                .summary(convertReadinessSummary((Map<String, Object>) result.get("summary")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private EvaluationDTO.CompleteEvaluationResponse convertToCompleteEvaluationResponse(
            Map<String, Object> result,
            String modelId
    ) {
        return EvaluationDTO.CompleteEvaluationResponse.builder()
                .modelId(modelId)
                .overallScore(((Number) result.get("overall_score")).doubleValue())
                .build();
    }

    private EvaluationDTO.ConfusionMatrix convertConfusionMatrix(Map<String, Object> data) {
        if (data == null) return null;

        return EvaluationDTO.ConfusionMatrix.builder()
                .tn(((Number) data.get("tn")).longValue())
                .fp(((Number) data.get("fp")).longValue())
                .fn(((Number) data.get("fn")).longValue())
                .tp(((Number) data.get("tp")).longValue())
                .total(((Number) data.get("total")).longValue())
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

        Integer passed = ((Number) data.get("passed")).intValue();
        Integer total = ((Number) data.get("total_criteria")).intValue();

        return EvaluationDTO.ReadinessSummary.builder()
                .passed(passed)
                .totalCriteria(total)
                .passPercentage(total > 0 ? (passed * 100.0) / total : 0.0)
                .build();
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        return ((Number) value).doubleValue();
    }
}
