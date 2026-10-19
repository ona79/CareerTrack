package com.careertrack.modules.leave.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingDaysCalculationDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal workingDays;
}
