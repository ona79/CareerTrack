package com.careertrack.modules.leave.dto;

import com.careertrack.modules.leave.entity.LeaveStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestResponseDto {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private String departmentName;

    private UUID leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeCode;

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal requestedDays;
    private String reason;
    private LeaveStatus status;
    private String managerComment;

    private UUID approvedById;
    private String approvedByName;
    private Instant approvedAt;
    private Instant createdAt;
}
