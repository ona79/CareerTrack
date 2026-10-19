package com.careertrack.modules.dashboard.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveKpiDto {
    private long pendingRequests;
    private long approvedRequests;
    private long rejectedRequests;
    private BigDecimal totalDaysTaken;
}
