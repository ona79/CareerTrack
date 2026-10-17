package com.careertrack.modules.leave.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamLeaveConflictDto {
    private UUID departmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean hasConflict;
    private int overlappingRequestsCount;
    private List<LeaveRequestResponseDto> overlappingRequests;
}
