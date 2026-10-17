package com.careertrack.modules.payroll.dto;

import com.careertrack.modules.payroll.entity.PayslipLineType;
import com.careertrack.modules.payroll.entity.PayslipStatus;
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
public class PayslipResponseDto {
    private UUID id;
    private UUID employeeId;
    private String employeeFullName;
    private String employeeNumber;
    private int periodMonth;
    private int periodYear;
    private String period; // Format "YYYY-MM"
    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private BigDecimal employerContributions;
    private PayslipStatus status;
    private LocalDate paymentDate;
    private String pdfUrl;
    private List<PayslipLineDto> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayslipLineDto {
        private UUID id;
        private PayslipLineType lineType;
        private String label;
        private BigDecimal baseAmount;
        private BigDecimal rate;
        private BigDecimal amount;
    }
}
