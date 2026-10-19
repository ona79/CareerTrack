package com.careertrack.modules.employee.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeHistoryDto {
    private List<SalaryHistoryDto> salaryHistories;
    private List<PositionHistoryDto> positionHistories;
}
