package com.careertrack.modules.dashboard.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private int periodYear;
    private HeadcountKpiDto headcount;
    private List<AgePyramidBracketDto> agePyramid;
    private TurnoverKpiDto turnover;
    private PayrollKpiDto payroll;
    private TrainingKpiDto training;
    private LeaveKpiDto leaves;
}
