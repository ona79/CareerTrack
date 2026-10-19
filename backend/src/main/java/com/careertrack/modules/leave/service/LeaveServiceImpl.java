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
import com.careertrack.modules.notification.entity.Notification;
import com.careertrack.modules.notification.entity.NotificationChannel;
import com.careertrack.modules.notification.entity.NotificationType;
import com.careertrack.modules.notification.repository.NotificationRepository;
import com.careertrack.modules.setting.repository.CompanySettingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class LeaveServiceImpl implements LeaveService {

    private static final int DEFAULT_TEAM_CONFLICT_THRESHOLD = 2;
    private static final List<LeaveStatus> ACTIVE_LEAVE_STATUSES = List.of(
            LeaveStatus.SUBMITTED,
            LeaveStatus.EN_ATTENTE,
            LeaveStatus.APPROVED_N1,
            LeaveStatus.APPROVED_DRH,
            LeaveStatus.VALIDE
    );

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;
    private final NotificationRepository notificationRepository;
    private final CompanySettingRepository companySettingRepository;

    public LeaveServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            LeaveMapper leaveMapper,
            NotificationRepository notificationRepository,
            CompanySettingRepository companySettingRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.leaveMapper = leaveMapper;
        this.notificationRepository = notificationRepository;
        this.companySettingRepository = companySettingRepository;
    }

    @Override
    @Transactional
    public LeaveRequestResponseDto createLeaveRequest(LeaveRequestCreateDto createDto) {
        if (createDto.getEndDate().isBefore(createDto.getStartDate())) {
            throw new BadRequestException("La date de fin ne peut pas précéder la date de début");
        }

        // 1. Calcul strict des jours ouvrables (lundi au vendredi)
        BigDecimal workingDays = calculateWorkingDays(createDto.getStartDate(), createDto.getEndDate()).getWorkingDays();
        if (workingDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("La période sélectionnée ne comporte aucun jour ouvrable");
        }

        if (createDto.getRequestedDays() == null || createDto.getRequestedDays().compareTo(BigDecimal.ZERO) <= 0) {
            createDto.setRequestedDays(workingDays);
        } else if (createDto.getRequestedDays().compareTo(workingDays) > 0) {
            createDto.setRequestedDays(workingDays);
        }

        Employee employee = employeeRepository.findById(createDto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", "id", createDto.getEmployeeId()));

        LeaveType leaveType = leaveTypeRepository.findById(createDto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de congé", "id", createDto.getLeaveTypeId()));

        // 2. Vérification et initialisation dynamique du solde
        int currentYear = createDto.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(
                employee.getId(), leaveType.getId(), currentYear
        ).orElseGet(() -> initializeBalanceForEmployee(employee, leaveType, currentYear));

        if (balance.getRemainingDays().compareTo(createDto.getRequestedDays()) < 0) {
            throw new BadRequestException(String.format(
                    "Solde de congés insuffisant : %.1f jours restants, %.1f demandés",
                    balance.getRemainingDays(), createDto.getRequestedDays()
            ));
        }

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

        if (request.getStatus().isCancelled()) {
            throw new BadRequestException("Impossible de statuer sur une demande annulée");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Employee reviewer = employeeRepository.findByUserId(principal.getId()).orElse(null);

        LeaveStatus targetStatus = approvalDto.getStatus();
        request.setStatus(targetStatus);
        request.setManagerComment(approvalDto.getManagerComment());
        request.setApprovedBy(reviewer);
        request.setApprovedAt(Instant.now());

        // Déduction automatique du compteur de jours si décision favorable
        if (targetStatus.isApproved() && request.getStartDate() != null && request.getRequestedDays() != null) {
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

        // Notification in-app au collaborateur
        sendLeaveDecisionNotification(updated, reviewer, approvalDto.getManagerComment());

        return leaveMapper.toDto(updated);
    }

    @Override
    @Transactional
    public LeaveRequestResponseDto cancelLeaveRequest(UUID requestId, Authentication authentication) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé", "id", requestId));

        if (request.getStatus().isCancelled()) {
            throw new BadRequestException("Cette demande de congé est déjà annulée");
        }

        // Si la demande avait déjà été approuvée, restituer les jours au solde
        if (request.getStatus().isApproved() && request.getStartDate() != null && request.getRequestedDays() != null) {
            int year = request.getStartDate().getYear();
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(
                    request.getEmployee().getId(), request.getLeaveType().getId(), year
            ).ifPresent(balance -> {
                balance.setUsedDays(balance.getUsedDays().subtract(request.getRequestedDays()));
                balance.setRemainingDays(balance.getRemainingDays().add(request.getRequestedDays()));
                leaveBalanceRepository.save(balance);
            });
        }

        request.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest saved = leaveRequestRepository.save(request);
        return leaveMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getEmployeeLeaveRequests(UUID employeeId) {
        return leaveMapper.toDtoList(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getPendingRequestsForManager(UUID managerId) {
        List<LeaveRequest> requests = leaveRequestRepository.findByManagerIdAndStatuses(
                managerId, List.of(LeaveStatus.SUBMITTED, LeaveStatus.EN_ATTENTE)
        );
        if (requests.isEmpty()) {
            Employee emp = employeeRepository.findById(managerId).orElse(null);
            if (emp != null && emp.getUser() != null && emp.getUser().getRoles().stream().anyMatch(
                    r -> r.getName().name().equals("ROLE_DRH") || r.getName().name().equals("ROLE_DIRECTION") || r.getName().name().equals("ROLE_ADMIN"))) {
                return leaveMapper.toDtoList(leaveRequestRepository.findByStatuses(List.of(LeaveStatus.SUBMITTED, LeaveStatus.EN_ATTENTE)));
            }
        }
        return leaveMapper.toDtoList(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getPendingRequests(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.careertrack.common.security.UserPrincipal principal) {
            boolean isDrhOrAdmin = principal.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_DRH") || a.getAuthority().equals("ROLE_DIRECTION") || a.getAuthority().equals("ROLE_ADMIN"));
            if (isDrhOrAdmin) {
                return leaveMapper.toDtoList(leaveRequestRepository.findByStatuses(List.of(LeaveStatus.SUBMITTED, LeaveStatus.EN_ATTENTE)));
            }
            Employee emp = employeeRepository.findByUserId(principal.getId()).orElse(null);
            if (emp != null) {
                return getPendingRequestsForManager(emp.getId());
            }
        }
        return leaveMapper.toDtoList(leaveRequestRepository.findByStatuses(List.of(LeaveStatus.SUBMITTED, LeaveStatus.EN_ATTENTE)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveTypeDto> getAllLeaveTypes() {
        return leaveTypeRepository.findAll().stream()
                .map(lt -> LeaveTypeDto.builder()
                        .id(lt.getId())
                        .code(lt.getCode())
                        .name(lt.getName())
                        .maxDaysPerYear(lt.getMaxDaysPerYear())
                        .isPaid(lt.isPaid())
                        .requiresJustification(lt.isRequiresJustification())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> getEmployeeBalances(UUID employeeId, int year) {
        return leaveMapper.toBalanceDtoList(leaveBalanceRepository.findByEmployeeIdAndPeriodYear(employeeId, year));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamLeaveConflictDto detectTeamConflicts(UUID departmentId, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> overlapping = leaveRequestRepository.findTeamLeavesInRange(
                departmentId, startDate, endDate, ACTIVE_LEAVE_STATUSES
        );

        long distinctEmployees = overlapping.stream()
                .map(lr -> lr.getEmployee().getId())
                .distinct()
                .count();

        boolean hasConflict = distinctEmployees >= DEFAULT_TEAM_CONFLICT_THRESHOLD;
        List<LeaveRequestResponseDto> overlappingDtos = leaveMapper.toDtoList(overlapping);

        return TeamLeaveConflictDto.builder()
                .departmentId(departmentId)
                .startDate(startDate)
                .endDate(endDate)
                .threshold(DEFAULT_TEAM_CONFLICT_THRESHOLD)
                .hasConflict(hasConflict)
                .overlappingRequestsCount(overlappingDtos.size())
                .overlappingRequests(overlappingDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamCalendarItemDto> getTeamCalendar(UUID departmentId, LocalDate startDate, LocalDate endDate, Authentication authentication) {
        List<LeaveRequest> leaves = leaveRequestRepository.findCalendarLeaves(
                departmentId, startDate, endDate, ACTIVE_LEAVE_STATUSES
        );

        // Pour chaque congé, vérifier s'il est en conflit avec au moins 1 autre collègue de son département sur sa période
        List<TeamCalendarItemDto> items = new ArrayList<>();
        for (LeaveRequest lr : leaves) {
            UUID deptId = (lr.getEmployee() != null && lr.getEmployee().getDepartment() != null)
                    ? lr.getEmployee().getDepartment().getId()
                    : null;

            boolean isConflicting = false;
            if (deptId != null) {
                long overlaps = leaves.stream()
                        .filter(other -> other.getEmployee() != null
                                && other.getEmployee().getDepartment() != null
                                && deptId.equals(other.getEmployee().getDepartment().getId())
                                && !other.getEmployee().getId().equals(lr.getEmployee().getId())
                                && other.getStartDate().isBefore(lr.getEndDate().plusDays(1))
                                && other.getEndDate().isAfter(lr.getStartDate().minusDays(1)))
                        .count();
                isConflicting = overlaps >= 1; // Au moins un autre collègue absent = >= 2 absents
            }

            items.add(TeamCalendarItemDto.builder()
                    .requestId(lr.getId())
                    .employeeId(lr.getEmployee() != null ? lr.getEmployee().getId() : null)
                    .employeeName(lr.getEmployee() != null ? lr.getEmployee().getFullName() : null)
                    .employeeNumber(lr.getEmployee() != null ? lr.getEmployee().getEmployeeNumber() : null)
                    .departmentId(deptId)
                    .departmentName(lr.getEmployee() != null && lr.getEmployee().getDepartment() != null ? lr.getEmployee().getDepartment().getName() : null)
                    .positionTitle(lr.getEmployee() != null && lr.getEmployee().getPosition() != null ? lr.getEmployee().getPosition().getTitle() : null)
                    .leaveTypeId(lr.getLeaveType() != null ? lr.getLeaveType().getId() : null)
                    .leaveTypeName(lr.getLeaveType() != null ? lr.getLeaveType().getName() : null)
                    .leaveTypeCode(lr.getLeaveType() != null ? lr.getLeaveType().getCode() : null)
                    .startDate(lr.getStartDate())
                    .endDate(lr.getEndDate())
                    .requestedDays(lr.getRequestedDays())
                    .status(lr.getStatus())
                    .conflicting(isConflicting)
                    .build());
        }

        return items;
    }

    @Override
    public WorkingDaysCalculationDto calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("La date de fin ne peut pas précéder la date de début");
        }

        long count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }

        return WorkingDaysCalculationDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .workingDays(BigDecimal.valueOf(count))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getAllLeaveRequests() {
        return leaveMapper.toDtoList(leaveRequestRepository.findAll());
    }

    private LeaveBalance initializeBalanceForEmployee(Employee employee, LeaveType leaveType, int year) {
        BigDecimal daysPerMonth = companySettingRepository.findBySettingKey("LEAVE_DAYS_PER_MONTH")
                .map(setting -> {
                    try {
                        return new BigDecimal(setting.getSettingValue());
                    } catch (Exception e) {
                        return BigDecimal.valueOf(2.0);
                    }
                })
                .orElse(BigDecimal.valueOf(2.0));

        BigDecimal allocated;
        if ("CONGE_PAYE".equalsIgnoreCase(leaveType.getCode())) {
            allocated = daysPerMonth.multiply(BigDecimal.valueOf(12));
        } else if (leaveType.getMaxDaysPerYear() != null) {
            allocated = BigDecimal.valueOf(leaveType.getMaxDaysPerYear());
        } else {
            allocated = BigDecimal.valueOf(30);
        }

        LeaveBalance newBalance = LeaveBalance.builder()
                .employee(employee)
                .leaveType(leaveType)
                .periodYear(year)
                .allocatedDays(allocated)
                .usedDays(BigDecimal.ZERO)
                .remainingDays(allocated)
                .build();

        return leaveBalanceRepository.save(newBalance);
    }

    private void sendLeaveDecisionNotification(LeaveRequest request, Employee reviewer, String comment) {
        if (request.getEmployee() == null || request.getEmployee().getUser() == null) {
            return;
        }

        boolean isApproved = request.getStatus().isApproved();
        String reviewerName = (reviewer != null) ? reviewer.getFullName() : "Le manager";
        String title = isApproved ? "Demande de congé validée" : "Demande de congé refusée";
        String message = isApproved
                ? String.format("Votre demande de congé du %s au %s (%.1f j) a été validée par %s.",
                request.getStartDate(), request.getEndDate(), request.getRequestedDays(), reviewerName)
                : String.format("Votre demande de congé du %s au %s a été refusée par %s.%s",
                request.getStartDate(), request.getEndDate(), reviewerName,
                (comment != null && !comment.isBlank() ? " Motif : " + comment : ""));

        Notification notification = Notification.builder()
                .recipient(request.getEmployee().getUser())
                .title(title)
                .message(message)
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.LEAVE_STATUS)
                .read(false)
                .sentAt(Instant.now())
                .build();

        notificationRepository.save(notification);
    }
}

