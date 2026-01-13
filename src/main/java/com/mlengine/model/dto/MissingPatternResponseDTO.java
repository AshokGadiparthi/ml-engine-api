package com.mlengine.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * EDA Missing Pattern Response DTO
 * Contains missing data pattern analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissingPatternResponseDTO {
    private Integer totalRows;
    private Integer totalColumns;
    private Integer totalMissing;
    private Double totalMissingPercentage;
    private Map<String, MissingColumnInfo> missingPerColumn;
    private Integer rowsWithMissing;
    private String qualityRating;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingColumnInfo {
        private Integer count;
        private Double percentage;
    }
}
