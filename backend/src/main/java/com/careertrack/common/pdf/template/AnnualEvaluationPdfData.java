package com.careertrack.common.pdf.template;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Objet de transfert pour le gabarit PDF d'entretien annuel (Lot 4).
 */
public record AnnualEvaluationPdfData(
        String campaignTitle,
        String employeeName,
        String employeeNumber,
        String positionTitle,
        String departmentName,
        String evaluatorName,
        LocalDate interviewDate,
        BigDecimal finalScore,
        String generalComments,
        List<ObjectiveItem> objectives
) {
    public record ObjectiveItem(
            String title,
            String description,
            int weight,
            int achievementRate,
            String kpi
    ) {}
}
