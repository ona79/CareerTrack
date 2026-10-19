package com.careertrack.modules.employee.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryHistoryDto {
    private UUID id;
    private UUID employeeId;
    private BigDecimal previousSalary;
    private BigDecimal newSalary;
    private String changeReason;
    private LocalDate effectiveDate;
    private UUID approvedById;
    private String approvedByName;
    private Instant createdAt;
}
