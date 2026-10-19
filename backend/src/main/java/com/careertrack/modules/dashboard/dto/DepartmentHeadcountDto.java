package com.careertrack.modules.dashboard.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentHeadcountDto {
    private UUID departmentId;
    private String departmentName;
    private long employeeCount;
}
