package com.careertrack.modules.dashboard.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadcountKpiDto {
    private long totalEmployees;
    private long activeEmployees;
    private long onLeaveEmployees;
    private long terminatedEmployees;
    private List<ContractTypeBreakdownDto> contractBreakdown;
    private List<DepartmentHeadcountDto> departmentBreakdown;
}
