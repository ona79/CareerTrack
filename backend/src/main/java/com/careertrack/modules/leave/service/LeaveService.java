package com.careertrack.modules.leave.service;

import com.careertrack.modules.leave.dto.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveService {
    LeaveRequestResponseDto createLeaveRequest(LeaveRequestCreateDto createDto);
    LeaveRequestResponseDto approveOrRejectLeave(UUID requestId, LeaveApprovalDto approvalDto, Authentication authentication);
    List<LeaveRequestResponseDto> getEmployeeLeaveRequests(UUID employeeId);
    List<LeaveRequestResponseDto> getPendingRequestsForManager(UUID managerId);
    List<LeaveBalanceDto> getEmployeeBalances(UUID employeeId, int year);
    TeamLeaveConflictDto detectTeamConflicts(UUID departmentId, LocalDate startDate, LocalDate endDate);
    List<LeaveRequestResponseDto> getAllLeaveRequests();
}
