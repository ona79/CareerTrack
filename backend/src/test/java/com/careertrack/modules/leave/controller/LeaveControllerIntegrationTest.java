package com.careertrack.modules.leave.controller;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.leave.dto.LeaveApprovalDto;
import com.careertrack.modules.leave.dto.LeaveRequestCreateDto;
import com.careertrack.modules.leave.entity.LeaveBalance;
import com.careertrack.modules.leave.entity.LeaveRequest;
import com.careertrack.modules.leave.entity.LeaveStatus;
import com.careertrack.modules.leave.entity.LeaveType;
import com.careertrack.modules.leave.repository.LeaveBalanceRepository;
import com.careertrack.modules.leave.repository.LeaveRequestRepository;
import com.careertrack.modules.leave.repository.LeaveTypeRepository;
import com.careertrack.modules.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaveControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String empToken;
    private String mgrToken;
    private String drhToken;

    private Employee employee;
    private Employee manager;
    private Department department;
    private LeaveType leaveType;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();

        // 1. Rôles
        Role roleEmp = roleRepository.findByName(RoleType.ROLE_EMPLOYE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).description("Employé").build()));
        Role roleMgr = roleRepository.findByName(RoleType.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_MANAGER).description("Manager").build()));
        Role roleDrh = roleRepository.findByName(RoleType.ROLE_DRH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).description("DRH").build()));

        // 2. Utilisateurs
        User userEmp = userRepository.findByUsername("test.leave.emp")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("test.leave.emp")
                        .email("emp@careertrack.sn")
                        .password("hash")
                        .roles(Set.of(roleEmp))
                        .enabled(true)
                        .build()));

        User userMgr = userRepository.findByUsername("test.leave.mgr")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("test.leave.mgr")
                        .email("mgr@careertrack.sn")
                        .password("hash")
                        .roles(Set.of(roleMgr))
                        .enabled(true)
                        .build()));

        User userDrh = userRepository.findByUsername("test.leave.drh")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("test.leave.drh")
                        .email("drh@careertrack.sn")
                        .password("hash")
                        .roles(Set.of(roleDrh))
                        .enabled(true)
                        .build()));

        // 3. Tokens JWT
        empToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder().id(userEmp.getId()).username(userEmp.getUsername())
                        .email(userEmp.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE"))).build());

        mgrToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder().id(userMgr.getId()).username(userMgr.getUsername())
                        .email(userMgr.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).build());

        drhToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder().id(userDrh.getId()).username(userDrh.getUsername())
                        .email(userDrh.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH"))).build());

        // 4. Département et Poste
        department = departmentRepository.findByCode("LEAVE_DEPT")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code("LEAVE_DEPT")
                        .name("Département Congés Test")
                        .description("Test")
                        .build()));

        Position position = positionRepository.findByCode("LEAVE_POS")
                .orElseGet(() -> positionRepository.save(Position.builder()
                        .code("LEAVE_POS")
                        .title("Poste Test Congés")
                        .department(department)
                        .minSalary(new BigDecimal("500000"))
                        .maxSalary(new BigDecimal("900000"))
                        .build()));

        // 5. Employés (Manager et Collaborateur)
        manager = employeeRepository.findByEmail("mgr@careertrack.sn")
                .orElseGet(() -> employeeRepository.save(Employee.builder()
                        .employeeNumber("MGR-LEAVE-01")
                        .firstName("Samba")
                        .lastName("Sow")
                        .nationalId("CNI-MGR-LEAVE")
                        .email("mgr@careertrack.sn")
                        .birthDate(LocalDate.of(1985, 3, 15))
                        .hireDate(LocalDate.of(2020, 1, 1))
                        .department(department)
                        .position(position)
                        .user(userMgr)
                        .status(EmployeeStatus.ACTIVE)
                        .build()));

        employee = employeeRepository.findByEmail("emp@careertrack.sn")
                .orElseGet(() -> employeeRepository.save(Employee.builder()
                        .employeeNumber("EMP-LEAVE-01")
                        .firstName("Fatou")
                        .lastName("Diallo")
                        .nationalId("CNI-EMP-LEAVE")
                        .email("emp@careertrack.sn")
                        .birthDate(LocalDate.of(1996, 7, 22))
                        .hireDate(LocalDate.of(2023, 2, 1))
                        .department(department)
                        .position(position)
                        .manager(manager)
                        .user(userEmp)
                        .status(EmployeeStatus.ACTIVE)
                        .build()));

        // 6. Type de congé et solde
        leaveType = leaveTypeRepository.findByCode("CONGE_PAYE")
                .orElseGet(() -> leaveTypeRepository.save(LeaveType.builder()
                        .code("CONGE_PAYE")
                        .name("Congé Payé Annuel")
                        .maxDaysPerYear(24)
                        .paid(true)
                        .build()));

        leaveBalanceRepository.save(LeaveBalance.builder()
                .employee(employee)
                .leaveType(leaveType)
                .periodYear(2026)
                .allocatedDays(new BigDecimal("24.0"))
                .usedDays(BigDecimal.ZERO)
                .remainingDays(new BigDecimal("24.0"))
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/leaves/calculate-days calcule correctement les jours ouvrables")
    void testCalculateWorkingDays() throws Exception {
        // Lundi 1er juin 2026 au dimanche 7 juin 2026 : 5 jours ouvrés
        mockMvc.perform(get("/api/v1/leaves/calculate-days")
                        .header(HttpHeaders.AUTHORIZATION, empToken)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workingDays").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/leaves crée une demande avec succès si le solde est suffisant")
    void testCreateLeaveRequest_Success() throws Exception {
        LeaveRequestCreateDto dto = LeaveRequestCreateDto.builder()
                .employeeId(employee.getId())
                .leaveTypeId(leaveType.getId())
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .requestedDays(new BigDecimal("5.0"))
                .reason("Vacances d'été")
                .build();

        mockMvc.perform(post("/api/v1/leaves")
                        .header(HttpHeaders.AUTHORIZATION, empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.requestedDays").value(5.0));
    }

    @Test
    @DisplayName("POST /api/v1/leaves échoue si les jours demandés dépassent le solde restant")
    void testCreateLeaveRequest_InsufficientBalance() throws Exception {
        LeaveRequestCreateDto dto = LeaveRequestCreateDto.builder()
                .employeeId(employee.getId())
                .leaveTypeId(leaveType.getId())
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 7, 20))
                .requestedDays(new BigDecimal("35.0"))
                .reason("Long repos")
                .build();

        mockMvc.perform(post("/api/v1/leaves")
                        .header(HttpHeaders.AUTHORIZATION, empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Solde de congés insuffisant")));
    }

    @Test
    @DisplayName("PUT /api/v1/leaves/{id}/validate valide la demande, déduit le solde et notifie le collaborateur")
    void testValidateLeave_Approval() throws Exception {
        LeaveRequest request = leaveRequestRepository.save(LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .requestedDays(new BigDecimal("5.0"))
                .status(LeaveStatus.SUBMITTED)
                .build());

        LeaveApprovalDto approvalDto = LeaveApprovalDto.builder()
                .status(LeaveStatus.APPROVED_N1)
                .managerComment("Bonnes vacances !")
                .build();

        mockMvc.perform(put("/api/v1/leaves/" + request.getId() + "/validate")
                        .header(HttpHeaders.AUTHORIZATION, mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED_N1"));

        // Vérifier déduction du solde
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(
                employee.getId(), leaveType.getId(), 2026
        ).orElseThrow();
        assertThat(balance.getUsedDays()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("19.0"));

        // Vérifier notification créée
        var notifs = notificationRepository.findAll();
        assertThat(notifs).isNotEmpty();
        assertThat(notifs.get(0).getTitle()).contains("validée");
    }

    @Test
    @DisplayName("PATCH /api/v1/leaves/{id}/cancel annule le congé et restitue les jours au solde")
    void testCancelLeave_RestoresBalance() throws Exception {
        // Congé déjà approuvé
        LeaveRequest request = leaveRequestRepository.save(LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .requestedDays(new BigDecimal("5.0"))
                .status(LeaveStatus.APPROVED_N1)
                .build());

        // Mettre à jour le solde comme consommé
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(
                employee.getId(), leaveType.getId(), 2026
        ).orElseThrow();
        balance.setUsedDays(new BigDecimal("5.0"));
        balance.setRemainingDays(new BigDecimal("19.0"));
        leaveBalanceRepository.save(balance);

        mockMvc.perform(patch("/api/v1/leaves/" + request.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Vérifier restitution
        LeaveBalance restored = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndPeriodYear(
                employee.getId(), leaveType.getId(), 2026
        ).orElseThrow();
        assertThat(restored.getUsedDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(restored.getRemainingDays()).isEqualByComparingTo(new BigDecimal("24.0"));
    }

    @Test
    @DisplayName("GET /api/v1/leaves/conflicts détecte les conflits si 2 personnes ou plus sont absentes")
    void testDetectTeamConflicts() throws Exception {
        // Créer un second employé dans le même département
        Employee colleague = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-COLLEAGUE")
                .firstName("Ousmane")
                .lastName("Ba")
                .nationalId("CNI-COLLEAGUE")
                .email("colleague@careertrack.sn")
                .birthDate(LocalDate.of(1990, 1, 1))
                .hireDate(LocalDate.of(2021, 1, 1))
                .department(department)
                .status(EmployeeStatus.ACTIVE)
                .build());

        leaveRequestRepository.save(LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 10))
                .requestedDays(new BigDecimal("7.0"))
                .status(LeaveStatus.APPROVED_N1)
                .build());

        leaveRequestRepository.save(LeaveRequest.builder()
                .employee(colleague)
                .leaveType(leaveType)
                .startDate(LocalDate.of(2026, 8, 5))
                .endDate(LocalDate.of(2026, 8, 15))
                .requestedDays(new BigDecimal("8.0"))
                .status(LeaveStatus.SUBMITTED)
                .build());

        mockMvc.perform(get("/api/v1/leaves/conflicts")
                        .header(HttpHeaders.AUTHORIZATION, mgrToken)
                        .param("departmentId", department.getId().toString())
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasConflict").value(true))
                .andExpect(jsonPath("$.data.overlappingRequestsCount").value(2));
    }
}
