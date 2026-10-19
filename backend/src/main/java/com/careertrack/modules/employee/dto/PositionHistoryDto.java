package com.careertrack.modules.employee.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistoryDto {
    private UUID id;
    private UUID employeeId;
    private UUID previousPositionId;
    private String previousPositionTitle;
    private UUID newPositionId;
    private String newPositionTitle;
    private UUID previousDepartmentId;
    private String previousDepartmentName;
    private UUID newDepartmentId;
    private String newDepartmentName;
    private LocalDate effectiveDate;
    private String changeReason;
    private UUID changedById;
    private String changedByName;
    private Instant createdAt;
}
