package com.careertrack.modules.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationManagerSubmitDto {

    @DecimalMin(value = "0.0", message = "La note manager doit être >= 0")
    @DecimalMax(value = "5.0", message = "La note manager doit être <= 5")
    private BigDecimal managerScore;

    private String managerFeedback;

    private LocalDate interviewDate;

    private List<ManagerObjectiveRateDto> objectiveRates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerObjectiveRateDto {
        private UUID objectiveId;
        private BigDecimal achievementRate;
        private String status;
    }
}
