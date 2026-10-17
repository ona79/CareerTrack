package com.careertrack.modules.employee.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionDto {
    private UUID id;
    private String code;
    private String title;
    private UUID departmentId;
    private String departmentName;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String description;
}
