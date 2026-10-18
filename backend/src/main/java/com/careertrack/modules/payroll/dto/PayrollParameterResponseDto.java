package com.careertrack.modules.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollParameterResponseDto {
    private UUID id;
    private String key;
    private String value;
    private String description;
    private String category;
    private String status;
    private String source;
    private LocalDate validFrom;
}
