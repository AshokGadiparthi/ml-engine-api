package com.mlengine.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * EDA Correlation Response DTO
 * Contains correlation analysis results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CorrelationResponseDTO {
    private Map<String, Map<String, Double>> correlationMatrix;
    private List<StrongCorrelation> strongCorrelations;
    private Integer totalStrongCorrelations;
    private Double strongThreshold;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrongCorrelation {
        private String feature1;
        private String feature2;
        private Double correlation;
    }
}
