package com.careertrack.modules.leave.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.leave.dto.LeaveApprovalDto;
import com.careertrack.modules.leave.dto.LeaveRequestCreateDto;
import com.careertrack.modules.leave.dto.LeaveRequestResponseDto;
import com.careertrack.modules.leave.entity.LeaveBalance;
import com.careertrack.modules.leave.entity.LeaveRequest;
import com.careertrack.modules.leave.entity.LeaveStatus;
import com.careertrack.modules.leave.entity.LeaveType;
import com.careertrack.modules.leave.mapper.LeaveMapper;
import com.careertrack.modules.leave.repository.LeaveBalanceRepository;
import com.careertrack.modules.leave.repository.LeaveRequestRepository;
import com.careertrack.modules.leave.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveMapper leaveMapper;

    @Mock
    private com.careertrack.modules.notification.repository.NotificationRepository notificationRepository;

    @Mock
    private com.careertrack.modules.setting.repository.CompanySettingRepository companySettingRepository;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private UUID employeeId;
    private UUID leaveTypeId;
    private Employee employee;
    private LeaveType leaveType;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        leaveTypeId = UUID.randomUUID();

        employee = Employee.builder()
                .id(employeeId)
                .firstName("Amadou")
                .lastName("Diop")
                .build();

        leaveType = LeaveType.builder()
                .id(leaveTypeId)
                .name("Congé Payé Annuel")
                .code("CONGE_PAYE")
                .build();
    }

    @Test
    @DisplayName("Calcul exact des jours ouvrables (lundi au vendredi, hors weekend)")
    void shouldCalculateWorkingDaysCorrectly() {
        // Du lundi 1er juin 2026 au dimanche 7 juin 2026 : 5 jours ouvrables
        var result = leaveService.calculateWorkingDays(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7));
        assertThat(result.getWorkingDays()).isEqualByComparingTo(new BigDecimal("5"));

        // Du vendredi 5 juin 2026 au lundi 8 juin 2026 : 2 jours ouvrables (ven + lun)
        var result2 = leaveService.calculateWorkingDays(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 8));
        assertThat(result2.getWorkingDays()).isEqualByComparingTo(new BigDecimal("2"));
    }

    @Test
    @DisplayName("Création d'une demande de congé avec solde suffisant")
    void shouldCreateLeaveRequestWhenBalanceIsSufficient() {
        LeaveRequestCreateDto dto = new LeaveRequestCreateDto();
        dto.setEmployeeId(employeeId);
        dto.setLeaveTypeId(leaveTypeId);
        dto.setStartDate(LocalDate.of(2026, 6, 1));
        dto.setEndDate(LocalDate.of(2026, 6, 10));
        dto.setRequestedDays(new BigDecimal("8"));
        dto.setReason("Congé de repos annuel");

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .leaveType(leaveType)
                .remainingDays(new BigDecimal("15"))
                .periodYear(2026)
                .build();

        LeaveRequest entity = new LeaveRequest();
        entity.setId(UUID.randomUUID());

        LeaveRequestResponseDto responseDto = new LeaveRequestResponseDto();
        responseDto.setId(entity.getId());
        responseDto.setStatus(LeaveStatus.SUBMITTED);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(leaveTypeId)).thenReturn(Optional.of(leaveType));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(employeeId, leaveTypeId, 2026))
                .thenReturn(Optional.of(balance));
        when(leaveMapper.toEntity(dto)).thenReturn(entity);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(entity);
        when(leaveMapper.toDto(entity)).thenReturn(responseDto);

        LeaveRequestResponseDto result = leaveService.createLeaveRequest(dto);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.SUBMITTED);
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Échec si date de fin antérieure à date de début")
    void shouldThrowBadRequestWhenEndDateBeforeStartDate() {
        LeaveRequestCreateDto dto = new LeaveRequestCreateDto();
        dto.setStartDate(LocalDate.of(2026, 6, 10));
        dto.setEndDate(LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> leaveService.createLeaveRequest(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("La date de fin ne peut pas précéder la date de début");
    }

    @Test
    @DisplayName("Échec si solde de congés insuffisant")
    void shouldThrowBadRequestWhenBalanceInsufficient() {
        LeaveRequestCreateDto dto = new LeaveRequestCreateDto();
        dto.setEmployeeId(employeeId);
        dto.setLeaveTypeId(leaveTypeId);
        dto.setStartDate(LocalDate.of(2026, 6, 1));
        dto.setEndDate(LocalDate.of(2026, 6, 20));
        dto.setRequestedDays(new BigDecimal("15"));

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .leaveType(leaveType)
                .remainingDays(new BigDecimal("5"))
                .periodYear(2026)
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(leaveTypeId)).thenReturn(Optional.of(leaveType));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(employeeId, leaveTypeId, 2026))
                .thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> leaveService.createLeaveRequest(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Solde de congés insuffisant");
    }

    @Test
    @DisplayName("Validation d'une demande par le manager avec notification et décompte")
    void shouldApproveLeaveRequest() {
        UUID requestId = UUID.randomUUID();
        LeaveRequest request = LeaveRequest.builder()
                .id(requestId)
                .employee(employee)
                .leaveType(leaveType)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 5))
                .requestedDays(new BigDecimal("4.0"))
                .status(LeaveStatus.SUBMITTED)
                .build();

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .leaveType(leaveType)
                .periodYear(2026)
                .usedDays(new BigDecimal("2.0"))
                .remainingDays(new BigDecimal("22.0"))
                .build();

        LeaveApprovalDto approvalDto = new LeaveApprovalDto();
        approvalDto.setStatus(LeaveStatus.APPROVED_N1);
        approvalDto.setManagerComment("Congé accordé");

        Authentication auth = mock(Authentication.class);
        UserPrincipal principal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .username("manager")
                .email("mgr@careertrack.sn")
                .password("hash")
                .enabled(true)
                .authorities(Collections.emptyList())
                .build();

        when(auth.getPrincipal()).thenReturn(principal);
        when(employeeRepository.findByUserId(principal.getId())).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(employee.getId(), leaveType.getId(), 2026))
                .thenReturn(Optional.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequestResponseDto responseDto = new LeaveRequestResponseDto();
        responseDto.setStatus(LeaveStatus.APPROVED_N1);
        when(leaveMapper.toDto(request)).thenReturn(responseDto);

        LeaveRequestResponseDto result = leaveService.approveOrRejectLeave(requestId, approvalDto, auth);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED_N1);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED_N1);
        assertThat(balance.getUsedDays()).isEqualByComparingTo(new BigDecimal("6.0"));
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("18.0"));
    }

    @Test
    @DisplayName("Annulation d'un congé déjà validé avec remboursement du solde")
    void shouldCancelApprovedLeaveAndRefundBalance() {
        UUID requestId = UUID.randomUUID();
        LeaveRequest request = LeaveRequest.builder()
                .id(requestId)
                .employee(employee)
                .leaveType(leaveType)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 10))
                .requestedDays(new BigDecimal("8.0"))
                .status(LeaveStatus.APPROVED_N1)
                .build();

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .leaveType(leaveType)
                .periodYear(2026)
                .usedDays(new BigDecimal("8.0"))
                .remainingDays(new BigDecimal("16.0"))
                .build();

        Authentication auth = mock(Authentication.class);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(employee.getId(), leaveType.getId(), 2026))
                .thenReturn(Optional.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequestResponseDto responseDto = new LeaveRequestResponseDto();
        responseDto.setStatus(LeaveStatus.CANCELLED);
        when(leaveMapper.toDto(request)).thenReturn(responseDto);

        LeaveRequestResponseDto result = leaveService.cancelLeaveRequest(requestId, auth);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("24.0"));
    }

    @Test
    @DisplayName("Détection de conflit si 2 employés ou plus du département sont absents")
    void shouldDetectConflictWhenThresholdReached() {
        UUID deptId = UUID.randomUUID();
        Employee emp1 = Employee.builder().id(UUID.randomUUID()).build();
        Employee emp2 = Employee.builder().id(UUID.randomUUID()).build();

        LeaveRequest lr1 = LeaveRequest.builder().id(UUID.randomUUID()).employee(emp1).build();
        LeaveRequest lr2 = LeaveRequest.builder().id(UUID.randomUUID()).employee(emp2).build();

        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 15);

        when(leaveRequestRepository.findTeamLeavesInRange(eq(deptId), eq(start), eq(end), any()))
                .thenReturn(List.of(lr1, lr2));
        when(leaveMapper.toDtoList(any())).thenReturn(List.of(new LeaveRequestResponseDto(), new LeaveRequestResponseDto()));

        var conflictDto = leaveService.detectTeamConflicts(deptId, start, end);

        assertThat(conflictDto.isHasConflict()).isTrue();
        assertThat(conflictDto.getOverlappingRequestsCount()).isEqualTo(2);
    }
}

