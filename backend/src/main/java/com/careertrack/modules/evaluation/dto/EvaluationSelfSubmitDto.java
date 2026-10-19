package com.careertrack.modules.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationSelfSubmitDto {

    @DecimalMin(value = "0.0", message = "La note personnelle doit être >= 0")
    @DecimalMax(value = "5.0", message = "La note personnelle doit être <= 5")
    private BigDecimal selfScore;

    private String selfFeedback;

    private List<ObjectiveRateDto> objectiveRates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObjectiveRateDto {
        private UUID objectiveId;
        private BigDecimal selfRate;
        private String selfComment;
    }
}
