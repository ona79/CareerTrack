package com.careertrack.modules.promotion.dto;

import com.careertrack.modules.promotion.entity.PromotionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionProposalDto {
    private UUID id;
    private UUID employeeId;
    private String employeeFullName;
    private UUID proposedPositionId;
    private String proposedPositionTitle;
    private BigDecimal proposedSalary;
    private BigDecimal currentSalary;
    private BigDecimal percentageIncrease;
    private String justification;
    private PromotionStatus status;
    private LocalDate effectiveDate;
}
