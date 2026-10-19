package com.careertrack.modules.evaluation.dto;

import com.careertrack.modules.evaluation.entity.ObjectiveCategory;
import jakarta.validation.constraints.NotNull;
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
public class EvaluationCreateDto {

    @NotNull(message = "L'identifiant de la campagne est obligatoire")
    private UUID campaignId;

    @NotNull(message = "L'identifiant de l'employé est obligatoire")
    private UUID employeeId;

    private UUID evaluatorId;

    private LocalDate interviewDate;

    private List<ObjectiveInputDto> objectives;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObjectiveInputDto {
        private String title;
        private String description;
        private ObjectiveCategory category;
        private BigDecimal weight;
        private LocalDate targetDate;
        private BigDecimal achievementRate;
    }
}
