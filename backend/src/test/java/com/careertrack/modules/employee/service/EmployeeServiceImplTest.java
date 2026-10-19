package com.careertrack.modules.employee.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ConflictException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.payload.PageResponse;
import com.careertrack.common.security.EmployeeSecurityService;
import com.careertrack.modules.document.service.EmployeeDocumentService;
import com.careertrack.modules.employee.dto.*;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.employee.mapper.EmployeeMapper;
import com.careertrack.modules.employee.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SalaryHistoryRepository salaryHistoryRepository;

    @Mock
    private PositionHistoryRepository positionHistoryRepository;

    @Mock
    private EmployeeDocumentService employeeDocumentService;

    @Mock
    private EmployeeSecurityService employeeSecurity;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private UUID employeeId;
    private Employee employee;
    private EmployeeResponseDto responseDto;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        employee = Employee.builder()
                .id(employeeId)
                .employeeNumber("EMP-001")
                .firstName("Fatou")
                .lastName("Ndiaye")
                .email("fatou.ndiaye@careertrack.sn")
                .phone("771234567")
                .address("Dakar, Plateau")
                .birthDate(LocalDate.of(1990, 5, 12))
                .hireDate(LocalDate.of(2022, 1, 15))
                .status(EmployeeStatus.ACTIVE)
                .build();

        responseDto = new EmployeeResponseDto();
        responseDto.setId(employeeId);
        responseDto.setEmployeeNumber("EMP-001");
        responseDto.setFullName("Fatou Ndiaye");
    }

    @Test
    @DisplayName("Création réussie d'un collaborateur")
    void shouldCreateEmployeeSuccessfully() {
        UUID deptId = UUID.randomUUID();
        UUID posId = UUID.randomUUID();

        EmployeeCreateDto dto = new EmployeeCreateDto();
        dto.setEmployeeNumber("EMP-002");
        dto.setEmail("nouveau@careertrack.sn");
        dto.setDepartmentId(deptId);
        dto.setPositionId(posId);

        Department dept = Department.builder().id(deptId).name("Direction RH").build();
        Position pos = Position.builder().id(posId).title("Chargée RH").build();

        when(employeeRepository.findByEmployeeNumber("EMP-002")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmail("nouveau@careertrack.sn")).thenReturn(Optional.empty());
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
        when(positionRepository.findById(posId)).thenReturn(Optional.of(pos));
        when(employeeMapper.toEntity(dto)).thenReturn(employee);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(employeeMapper.toDto(employee)).thenReturn(responseDto);

        EmployeeResponseDto result = employeeService.createEmployee(dto);

        assertThat(result).isNotNull();
        assertThat(result.getEmployeeNumber()).isEqualTo("EMP-001");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Échec si le matricule employé existe déjà")
    void shouldThrowBadRequestWhenEmployeeNumberExists() {
        EmployeeCreateDto dto = new EmployeeCreateDto();
        dto.setEmployeeNumber("EMP-001");

        when(employeeRepository.findByEmployeeNumber("EMP-001")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.createEmployee(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("getAllEmployees rejette les champs de tri non autorisés")
    void shouldRejectInvalidSortField() {
        assertThatThrownBy(() -> employeeService.getAllEmployees(0, 10, null, null, null, "creditCardNumber", "asc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Champ de tri non autorisé");
    }

    @Test
    @DisplayName("getAllEmployees renvoie un DTO réduit avec pagination")
    void shouldReturnReducedSummaryDtoList() {
        when(employeeSecurity.hasRole("DRH")).thenReturn(true);
        Page<Employee> page = new PageImpl<>(List.of(employee));
        when(employeeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<EmployeeSummaryDto> response = employeeService.getAllEmployees(
                0, 10, "Fatou", null, EmployeeStatus.ACTIVE, "lastName", "asc"
        );

        assertThat(response.getContent()).hasSize(1);
        EmployeeSummaryDto summary = response.getContent().get(0);
        assertThat(summary.getEmployeeNumber()).isEqualTo("EMP-001");
        assertThat(summary.getFullName()).isEqualTo("Fatou Ndiaye");
    }

    @Test
    @DisplayName("Détail employé pour DRH inclut les données personnelles et le salaire")
    void shouldReturnFullDetailForDrh() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeSecurity.hasRole("DRH")).thenReturn(true);
        when(employeeSecurity.getCurrentEmployee()).thenReturn(Optional.empty());

        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .contractType(ContractType.CDI)
                .grossSalary(new BigDecimal("800000"))
                .active(true)
                .build();
        when(contractRepository.findByEmployeeIdAndActiveTrue(employeeId)).thenReturn(Optional.of(contract));
        when(salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId)).thenReturn(Collections.emptyList());
        when(positionHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId)).thenReturn(Collections.emptyList());
        when(employeeDocumentService.getDocumentsByEmployee(employeeId)).thenReturn(Collections.emptyList());

        EmployeeDetailDto detail = employeeService.getEmployeeDetail(employeeId);

        assertThat(detail.getIdentity().getFullName()).isEqualTo("Fatou Ndiaye");
        assertThat(detail.getPersonalData()).isNotNull();
        assertThat(detail.getPersonalData().getPhone()).isEqualTo("771234567");
        assertThat(detail.getContract()).isNotNull();
        assertThat(detail.getContract().getGrossSalary()).isEqualTo(new BigDecimal("800000"));
        assertThat(detail.getPermissions().isCanEdit()).isTrue();
    }

    @Test
    @DisplayName("Détail employé pour Manager masque les données personnelles et le salaire")
    void shouldMaskSensitiveDataForManager() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeSecurity.hasRole("DRH")).thenReturn(false);
        when(employeeSecurity.hasRole("MANAGER")).thenReturn(true);

        Employee manager = Employee.builder().id(UUID.randomUUID()).build();
        when(employeeSecurity.getCurrentEmployee()).thenReturn(Optional.of(manager));

        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .contractType(ContractType.CDI)
                .grossSalary(new BigDecimal("800000"))
                .active(true)
                .build();
        when(contractRepository.findByEmployeeIdAndActiveTrue(employeeId)).thenReturn(Optional.of(contract));
        when(positionHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId)).thenReturn(Collections.emptyList());

        EmployeeDetailDto detail = employeeService.getEmployeeDetail(employeeId);

        assertThat(detail.getIdentity().getFullName()).isEqualTo("Fatou Ndiaye");
        assertThat(detail.getPersonalData()).isNull(); // Masqué pour le manager
        assertThat(detail.getContract()).isNotNull();
        assertThat(detail.getContract().getGrossSalary()).isNull(); // Salaire masqué
        assertThat(detail.getSalaryHistory()).isNull(); // Historique salaire masqué
        assertThat(detail.getDocuments()).isNull(); // Documents masqués pour manager
        assertThat(detail.getPositionHistory()).isNotNull(); // Historique de poste visible
    }

    @Test
    @DisplayName("Désactivation impossible si l'employé a des subordonnés actifs (409 Conflict)")
    void shouldBlockDeactivationWhenHasActiveSubordinates() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.countByManagerIdAndStatusNot(employeeId, EmployeeStatus.TERMINATED)).thenReturn(3L);

        EmployeeDeactivateDto dto = EmployeeDeactivateDto.builder().reason("Démission").build();

        assertThatThrownBy(() -> employeeService.deactivateEmployee(employeeId, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("3 collaborateur(s) actif(s)");
    }

    @Test
    @DisplayName("Désactivation réussie clôture le contrat et passe le statut à TERMINATED")
    void shouldDeactivateEmployeeSuccessfully() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.countByManagerIdAndStatusNot(employeeId, EmployeeStatus.TERMINATED)).thenReturn(0L);

        Contract contract = Contract.builder().id(UUID.randomUUID()).active(true).build();
        when(contractRepository.findByEmployeeIdAndActiveTrue(employeeId)).thenReturn(Optional.of(contract));

        EmployeeDeactivateDto dto = EmployeeDeactivateDto.builder()
                .reason("Fin de contrat")
                .departureDate(LocalDate.of(2026, 12, 31))
                .build();

        employeeService.deactivateEmployee(employeeId, dto);

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
        assertThat(contract.isActive()).isFalse();
        assertThat(contract.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("Détection de cycle hiérarchique lors du changement de manager")
    void shouldDetectHierarchyCycleOnUpdate() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        UUID managerId = UUID.randomUUID();
        Employee mgr = Employee.builder().id(managerId).firstName("Manager").lastName("Boss").build();
        mgr.setManager(employee); // Cycle direct: mgr a pour manager employeeId

        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(mgr));

        EmployeeUpdateDto dto = EmployeeUpdateDto.builder()
                .firstName("Fatou")
                .lastName("Ndiaye")
                .nationalId("CNI123")
                .email("fatou@careertrack.sn")
                .birthDate(LocalDate.of(1990, 1, 1))
                .hireDate(LocalDate.of(2022, 1, 1))
                .managerId(managerId)
                .build();

        assertThatThrownBy(() -> employeeService.updateEmployee(employeeId, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cycle hiérarchique");
    }
}
