package com.careertrack.modules.payroll;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.ContractType;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.document.repository.EmployeeDocumentContentRepository;
import com.careertrack.modules.document.repository.EmployeeDocumentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.payroll.repository.PayslipLineRepository;
import com.careertrack.modules.payroll.repository.PayslipRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PayrollControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private PayslipLineRepository payslipLineRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private EmployeeDocumentContentRepository employeeDocumentContentRepository;

    @Autowired
    private com.careertrack.modules.evaluation.repository.EvaluationRepository evaluationRepository;

    @Autowired
    private com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository evaluationCampaignRepository;

    @Autowired
    private com.careertrack.modules.leave.repository.LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private com.careertrack.modules.leave.repository.LeaveBalanceRepository leaveBalanceRepository;

    private User drhUser;
    private User managerUser;
    private User employeeUser;
    private User directionUser;

    private Employee drhEmployee;
    private Employee managerEmployee;
    private Employee regularEmployee;

    private String drhToken;
    private String managerToken;
    private String employeeToken;
    private String directionToken;

    @BeforeEach
    void setUp() {
        employeeDocumentContentRepository.deleteAll();
        employeeDocumentRepository.deleteAll();
        evaluationRepository.deleteAll();
        evaluationCampaignRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        payslipLineRepository.deleteAll();
        payslipRepository.deleteAll();
        contractRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        Role drhRole = roleRepository.findByName(RoleType.ROLE_DRH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).description("DRH").build()));
        Role managerRole = roleRepository.findByName(RoleType.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_MANAGER).description("Manager").build()));
        Role empRole = roleRepository.findByName(RoleType.ROLE_EMPLOYE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).description("Employé").build()));
        Role dirRole = roleRepository.findByName(RoleType.ROLE_DIRECTION)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DIRECTION).description("Direction").build()));

        drhUser = userRepository.save(User.builder().username("drh.test").email("drh.test@careertrack.sn").password("hash").enabled(true).roles(Set.of(drhRole)).build());
        managerUser = userRepository.save(User.builder().username("mgr.test").email("mgr.test@careertrack.sn").password("hash").enabled(true).roles(Set.of(managerRole)).build());
        employeeUser = userRepository.save(User.builder().username("emp.test").email("emp.test@careertrack.sn").password("hash").enabled(true).roles(Set.of(empRole)).build());
        directionUser = userRepository.save(User.builder().username("dir.test").email("dir.test@careertrack.sn").password("hash").enabled(true).roles(Set.of(dirRole)).build());

        drhEmployee = employeeRepository.save(Employee.builder()
                .user(drhUser).employeeNumber("EMP-DRH").firstName("Aminata").lastName("Ndiaye")
                .nationalId("NAT-DRH").email("drh.test@careertrack.sn").birthDate(LocalDate.of(1985, 1, 1))
                .hireDate(LocalDate.of(2020, 1, 1)).status(EmployeeStatus.ACTIVE).isCadre(true)
                .build());

        managerEmployee = employeeRepository.save(Employee.builder()
                .user(managerUser).employeeNumber("EMP-MGR").firstName("Mamadou").lastName("Sow")
                .nationalId("NAT-MGR").email("mgr.test@careertrack.sn").birthDate(LocalDate.of(1988, 1, 1))
                .hireDate(LocalDate.of(2021, 1, 1)).status(EmployeeStatus.ACTIVE).isCadre(true)
                .build());

        regularEmployee = employeeRepository.save(Employee.builder()
                .user(employeeUser).employeeNumber("EMP-REG").firstName("Ousmane").lastName("Diallo")
                .nationalId("NAT-REG").email("emp.test@careertrack.sn").birthDate(LocalDate.of(1995, 1, 1))
                .hireDate(LocalDate.of(2022, 1, 1)).status(EmployeeStatus.ACTIVE).isCadre(false)
                .build());

        contractRepository.save(Contract.builder().employee(drhEmployee).contractType(ContractType.CDI).startDate(LocalDate.of(2020, 1, 1)).grossSalary(new BigDecimal("1500000")).active(true).build());
        contractRepository.save(Contract.builder().employee(managerEmployee).contractType(ContractType.CDI).startDate(LocalDate.of(2021, 1, 1)).grossSalary(new BigDecimal("950000")).active(true).build());
        contractRepository.save(Contract.builder().employee(regularEmployee).contractType(ContractType.CDI).startDate(LocalDate.of(2022, 1, 1)).grossSalary(new BigDecimal("300000")).active(true).build());

        drhToken = createToken(drhUser, "ROLE_DRH");
        managerToken = createToken(managerUser, "ROLE_MANAGER");
        employeeToken = createToken(employeeUser, "ROLE_EMPLOYE");
        directionToken = createToken(directionUser, "ROLE_DIRECTION");
    }

    private String createToken(User user, String role) {
        return jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/payroll/parameters : DRH et DIRECTION autorisés, EMPLOYE interdit (403)")
    void shouldControlAccessToPayrollParameters() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/parameters")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/payroll/parameters")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/payroll/parameters")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/payroll/generate : DRH génère la paie avec succès ; EMPLOYE et DIRECTION ont 403")
    void shouldGeneratePayrollAsDrhOnly() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/generate?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payroll/generate?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payroll/generate?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalGenerated").value(3))
                .andExpect(jsonPath("$.data.period").value("2026-09"));

        assertThat(payslipRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("GET /api/v1/payroll/payslips/employee/{id} : EMPLOYE et MANAGER accèdent à leurs propres bulletins ; 403 pour autrui ou DIRECTION")
    void shouldControlAccessToPersonalPayslips() throws Exception {
        // DRH génère la paie d'abord
        mockMvc.perform(post("/api/v1/payroll/generate?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk());

        // Employé accède à ses propres bulletins -> 200
        mockMvc.perform(get("/api/v1/payroll/payslips/employee/" + regularEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].employeeNumber").value("EMP-REG"));

        // Employé tente d'accéder aux bulletins de son manager -> 403
        mockMvc.perform(get("/api/v1/payroll/payslips/employee/" + managerEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // Manager accède à ses PROPRES bulletins -> 200
        mockMvc.perform(get("/api/v1/payroll/payslips/employee/" + managerEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].employeeNumber").value("EMP-MGR"));

        // Manager tente d'accéder aux bulletins d'un collaborateur -> 403
        mockMvc.perform(get("/api/v1/payroll/payslips/employee/" + regularEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // DIRECTION tente d'accéder aux bulletins nominatifs -> 403
        mockMvc.perform(get("/api/v1/payroll/payslips/employee/" + regularEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cycle de vie du statut : GENERATED -> VALIDATED -> CANCELLED / PAID")
    void shouldUpdatePayslipStatus() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/generate?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk());

        Payslip p = payslipRepository.findAll().get(0);
        assertThat(p.getStatus()).isEqualTo(PayslipStatus.GENERATED);

        // DRH valide
        mockMvc.perform(patch("/api/v1/payroll/payslips/" + p.getId() + "/status?status=VALIDATED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VALIDATED"));

        // DRH annule le bulletin validé
        mockMvc.perform(patch("/api/v1/payroll/payslips/" + p.getId() + "/status?status=CANCELLED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledBy").value("drh.test"));
    }

    @Test
    @DisplayName("GET /api/v1/payroll/stats : Accessible à DRH et DIRECTION, interdit à EMPLOYE")
    void shouldControlAccessToPayrollStats() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/generate?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk());

        // DRH accède aux stats
        mockMvc.perform(get("/api/v1/payroll/stats?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEmployeesPaid").value(3));

        // DIRECTION accède aux stats
        mockMvc.perform(get("/api/v1/payroll/stats?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEmployeesPaid").value(3));

        // EMPLOYE a 403
        mockMvc.perform(get("/api/v1/payroll/stats?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }
}
