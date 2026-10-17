package com.careertrack.modules.leave.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.leave.dto.*;
import com.careertrack.modules.leave.entity.LeaveBalance;
import com.careertrack.modules.leave.entity.LeaveRequest;
import com.careertrack.modules.leave.entity.LeaveStatus;
import com.careertrack.modules.leave.entity.LeaveType;
import com.careertrack.modules.leave.mapper.LeaveMapper;
import com.careertrack.modules.leave.repository.LeaveBalanceRepository;
import com.careertrack.modules.leave.repository.LeaveRequestRepository;
import com.careertrack.modules.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;

    public LeaveServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            LeaveMapper leaveMapper) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.leaveMapper = leaveMapper;
    }

    @Override
    @Transactional
    public LeaveRequestResponseDto createLeaveRequest(LeaveRequestCreateDto createDto) {
        if (createDto.getEndDate().isBefore(createDto.getStartDate())) {
            throw new BadRequestException("La date de fin ne peut pas précéder la date de début");
        }

        Employee employee = employeeRepository.findById(createDto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", createDto.getEmployeeId()));

        LeaveType leaveType = leaveTypeRepository.findById(createDto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de congé", "id", createDto.getLeaveTypeId()));

        // Vérification du solde restant pour les congés payés annuels
        int currentYear = createDto.getStartDate().getYear();
        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(employee.getId(), leaveType.getId(), currentYear)
                .ifPresent(balance -> {
                    if (balance.getRemainingDays().compareTo(createDto.getRequestedDays()) < 0) {
                        throw new BadRequestException("Solde de congés insuffisant : " + balance.getRemainingDays() + " jours restants");
                    }
                });

        LeaveRequest request = leaveMapper.toEntity(createDto);
        request.setEmployee(employee);
        request.setLeaveType(leaveType);
        request.setStatus(LeaveStatus.SUBMITTED);

        LeaveRequest saved = leaveRequestRepository.save(request);
        return leaveMapper.toDto(saved);
    }

    @Override
    @Transactional
    public LeaveRequestResponseDto approveOrRejectLeave(UUID requestId, LeaveApprovalDto approvalDto, Authentication authentication) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé", "id", requestId));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Employee reviewer = employeeRepository.findByUserId(principal.getId()).orElse(null);

        request.setStatus(approvalDto.getStatus());
        request.setManagerComment(approvalDto.getManagerComment());
        request.setApprovedBy(reviewer);
        request.setApprovedAt(Instant.now());

        // Déduction automatique du compteur de jours si approbation finale (N+1 ou DRH)
        if ((approvalDto.getStatus() == LeaveStatus.APPROVED_N1 || approvalDto.getStatus() == LeaveStatus.APPROVED_DRH)
                && request.getStartDate() != null && request.getRequestedDays() != null) {
            int year = request.getStartDate().getYear();
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(
                    request.getEmployee().getId(), request.getLeaveType().getId(), year
            ).ifPresent(balance -> {
                balance.setUsedDays(balance.getUsedDays().add(request.getRequestedDays()));
                balance.setRemainingDays(balance.getRemainingDays().subtract(request.getRequestedDays()));
                leaveBalanceRepository.save(balance);
            });
        }

        LeaveRequest updated = leaveRequestRepository.save(request);
        return leaveMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getEmployeeLeaveRequests(UUID employeeId) {
        return leaveMapper.toDtoList(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getPendingRequestsForManager(UUID managerId) {
        return leaveMapper.toDtoList(leaveRequestRepository.findByEmployeeManagerIdAndStatus(managerId, LeaveStatus.SUBMITTED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> getEmployeeBalances(UUID employeeId, int year) {
        return leaveMapper.toBalanceDtoList(leaveBalanceRepository.findByEmployeeIdAndPeriodYear(employeeId, year));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamLeaveConflictDto detectTeamConflicts(UUID departmentId, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> overlapping = leaveRequestRepository.findTeamLeavesInRange(departmentId, startDate, endDate);
        List<LeaveRequestResponseDto> overlappingDtos = leaveMapper.toDtoList(overlapping);

        return TeamLeaveConflictDto.builder()
                .departmentId(departmentId)
                .startDate(startDate)
                .endDate(endDate)
                .hasConflict(!overlappingDtos.isEmpty())
                .overlappingRequestsCount(overlappingDtos.size())
                .overlappingRequests(overlappingDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getAllLeaveRequests() {
        return leaveMapper.toDtoList(leaveRequestRepository.findAll());
    }
}
