package com.mlengine.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * EDA Outlier Response DTO
 * Contains outlier detection results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutlierResponseDTO {
    private String featureName;
    private Double lowerBound;
    private Double upperBound;
    private Integer outlierCount;
    private Double outlierPercentage;
    private List<Integer> outlierIndices;
    private String method;
    private Double iqrMultiplier;
}
