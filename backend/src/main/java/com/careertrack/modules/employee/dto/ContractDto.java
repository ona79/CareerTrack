package com.careertrack.modules.employee.dto;

import com.careertrack.modules.employee.entity.ContractType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractDto {
    private UUID id;
    private UUID employeeId;
    private ContractType contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal grossSalary;
    private boolean active;
    private String documentUrl;
}
