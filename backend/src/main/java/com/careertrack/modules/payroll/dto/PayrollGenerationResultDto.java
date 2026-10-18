package com.careertrack.modules.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollGenerationResultDto {
    private String period;
    private int totalProcessed;
    private int totalGenerated;
    private int totalSkipped;
    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private BigDecimal totalEmployerContributions;
    private List<PayslipResponseDto> payslips;
}
