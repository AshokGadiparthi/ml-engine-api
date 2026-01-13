package com.mlengine.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * EDA Categorical Response DTO
 * Contains categorical distribution analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoricalResponseDTO {
    private String featureName;
    private Integer uniqueValues;
    private String mode;
    private Integer modeFrequency;
    private Map<String, Integer> topValues;
    private Integer missingCount;
    private Double missingPercentage;
    private Double entropy;
    private Double balanceRatio;
}
