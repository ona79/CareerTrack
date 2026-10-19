package com.careertrack.modules.leave.dto;

import com.careertrack.modules.leave.entity.LeaveStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamCalendarItemDto {
    private UUID requestId;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private UUID departmentId;
    private String departmentName;
    private String positionTitle;
    private UUID leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal requestedDays;
    private LeaveStatus status;
    private boolean conflicting;
}
