package com.careertrack.modules.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalaryHistoryDto(
        UUID id,
        BigDecimal previousSalary,
        BigDecimal newSalary,
        String changeReason,
        LocalDate effectiveDate,
        String approvedByName) {
}