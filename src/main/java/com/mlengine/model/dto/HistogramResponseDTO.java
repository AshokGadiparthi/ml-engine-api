package com.mlengine.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * EDA Histogram Response DTO
 * Contains histogram analysis for numeric features
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistogramResponseDTO {
    private String featureName;
    private Double mean;
    private Double median;
    private Double std;
    private Double min;
    private Double max;
    private Double q25;
    private Double q75;
    private Double skewness;
    private Double kurtosis;
    private Integer count;
    private String distribution;
    private Map<String, Object> additionalStats;
}
