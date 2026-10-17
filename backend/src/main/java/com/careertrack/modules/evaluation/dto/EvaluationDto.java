package com.careertrack.modules.evaluation.dto;

import com.careertrack.modules.evaluation.entity.EvaluationStatus;
import com.careertrack.modules.evaluation.entity.ObjectiveCategory;
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
public class EvaluationDto {
    private UUID id;
    private UUID campaignId;
    private String campaignTitle;
    private UUID employeeId;
    private String employeeFullName;
    private UUID evaluatorId;
    private String evaluatorFullName;
    private EvaluationStatus status;
    private BigDecimal selfScore;
    private BigDecimal managerScore;
    private BigDecimal finalScore;
    private String feedback;
    private String summaryPdfUrl;
    private LocalDate interviewDate;
    private List<SmartObjectiveDto> objectives;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmartObjectiveDto {
        private UUID id;
        private String title;
        private String description;
        private ObjectiveCategory category;
        private BigDecimal weight;
        private LocalDate targetDate;
        private BigDecimal achievementRate;
        private String status;
    }
}
