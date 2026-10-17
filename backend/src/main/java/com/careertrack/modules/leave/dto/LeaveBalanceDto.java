package com.careertrack.modules.leave.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceDto {
    private UUID id;
    private UUID employeeId;
    private UUID leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeCode;
    private int periodYear;
    private BigDecimal allocatedDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
}
