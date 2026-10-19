package com.careertrack.modules.employee.dto;

import com.careertrack.modules.employee.entity.EmployeeStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSummaryDto {
    private UUID id;
    private String employeeNumber;
    private String fullName;
    private String positionTitle;
    private String departmentName;
    private EmployeeStatus status;
}
