package com.careertrack.modules.document;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import com.careertrack.modules.payroll.entity.PayslipStatus;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ContractRepository contractRepository;

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private com.careertrack.modules.evaluation.repository.EvaluationRepository evaluationRepository;

    @Autowired
    private com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository evaluationCampaignRepository;

    @Autowired
    private com.careertrack.modules.leave.repository.LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private com.careertrack.modules.leave.repository.LeaveBalanceRepository leaveBalanceRepository;

    private String drhToken;
    private String employee1Token;
    private String employee2Token;
    private String directionToken;
    private String managerToken;

    private Employee employee1;
    private Employee employee2;
    private Employee managerEmployee;
    private Payslip employee1Payslip;

    @BeforeEach
    void setUp() {
        evaluationRepository.deleteAll();
        evaluationCampaignRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        payslipRepository.deleteAll();
        contractRepository.deleteAll();
        employeeRepository.deleteAll();
        positionRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Rôles
        Role roleDrh = roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).description("DRH").build());
        Role roleEmp = roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).description("Employe").build());
        Role roleDir = roleRepository.save(Role.builder().name(RoleType.ROLE_DIRECTION).description("Direction").build());
        Role roleMgr = roleRepository.save(Role.builder().name(RoleType.ROLE_MANAGER).description("Manager").build());

        // 2. Utilisateurs
        User userDrh = userRepository.save(User.builder()
                .username("test.drh")
                .email("drh@test.sn")
                .password("$2a$10$xyz")
                .enabled(true)
                .roles(Set.of(roleDrh))
                .build());

        User userMgr = userRepository.save(User.builder()
                .username("test.mgr")
                .email("mgr@test.sn")
                .password("$2a$10$xyz")
                .enabled(true)
                .roles(Set.of(roleMgr))
                .build());

        User userEmp1 = userRepository.save(User.builder()
                .username("test.emp1")
                .email("emp1@test.sn")
                .password("$2a$10$xyz")
                .enabled(true)
                .roles(Set.of(roleEmp))
                .build());

        User userEmp2 = userRepository.save(User.builder()
                .username("test.emp2")
                .email("emp2@test.sn")
                .password("$2a$10$xyz")
                .enabled(true)
                .roles(Set.of(roleEmp))
                .build());

        User userDir = userRepository.save(User.builder()
                .username("test.dir")
                .email("dir@test.sn")
                .password("$2a$10$xyz")
                .enabled(true)
                .roles(Set.of(roleDir))
                .build());

        // 3. Département & Poste
        Department dept = departmentRepository.save(Department.builder()
                .name("Ingénierie")
                .code("DEP-IT-" + UUID.randomUUID().toString().substring(0, 5))
                .build());

        Position pos = positionRepository.save(Position.builder()
                .title("Ingénieur Logiciel")
                .code("POS-ING-" + UUID.randomUUID().toString().substring(0, 5))
                .department(dept)
                .build());

        // 4. Salariés
        managerEmployee = employeeRepository.save(Employee.builder()
                .user(userMgr)
                .employeeNumber("EMP-MGR-001")
                .firstName("Amadou")
                .lastName("Diallo")
                .nationalId("1 985 1985 99999")
                .email("amadou.diallo@careertrack.sn")
                .phone("+221770001122")
                .birthDate(LocalDate.of(1985, 3, 12))
                .hireDate(LocalDate.of(2020, 6, 1))
                .department(dept)
                .position(pos)
                .status(EmployeeStatus.ACTIVE)
                .build());

        employee1 = employeeRepository.save(Employee.builder()
                .user(userEmp1)
                .employeeNumber("EMP-IT-001")
                .firstName("Moussa")
                .lastName("Sarr")
                .nationalId("1 990 1990 12345")
                .email("moussa.sarr@careertrack.sn")
                .phone("+221771112233")
                .birthDate(LocalDate.of(1990, 5, 10))
                .hireDate(LocalDate.of(2023, 1, 15))
                .department(dept)
                .position(pos)
                .manager(managerEmployee)
                .status(EmployeeStatus.ACTIVE)
                .build());

        employee2 = employeeRepository.save(Employee.builder()
                .user(userEmp2)
                .employeeNumber("EMP-IT-002")
                .firstName("Fatou")
                .lastName("Diop")
                .nationalId("2 995 1995 54321")
                .email("fatou.diop@careertrack.sn")
                .phone("+221772223344")
                .birthDate(LocalDate.of(1995, 8, 20))
                .hireDate(LocalDate.of(2024, 2, 1))
                .department(dept)
                .position(pos)
                .manager(null)
                .status(EmployeeStatus.ACTIVE)
                .build());

        // 5. Contrats
        contractRepository.save(Contract.builder()
                .employee(employee1)
                .contractType(ContractType.CDI)
                .startDate(LocalDate.of(2023, 1, 15))
                .grossSalary(new BigDecimal("850000"))
                .active(true)
                .build());

        contractRepository.save(Contract.builder()
                .employee(employee2)
                .contractType(ContractType.CDI)
                .startDate(LocalDate.of(2024, 2, 1))
                .grossSalary(new BigDecimal("750000"))
                .active(true)
                .build());

        // 6. Bulletin de paie pour employee1
        employee1Payslip = Payslip.builder()
                .employee(employee1)
                .periodMonth(9)
                .periodYear(2026)
                .grossSalary(new BigDecimal("850000"))
                .netSalary(new BigDecimal("704000"))
                .totalDeductions(new BigDecimal("146000"))
                .paymentDate(LocalDate.of(2026, 9, 30))
                .status(PayslipStatus.GENERATED)
                .build();

        PayslipLine line1 = PayslipLine.builder().payslip(employee1Payslip).label("Salaire de base").baseAmount(new BigDecimal("850000")).rate(BigDecimal.ONE).amount(new BigDecimal("850000")).lineType(PayslipLineType.BASE_SALARY).build();
        PayslipLine line2 = PayslipLine.builder().payslip(employee1Payslip).label("IPRES 5.6%").baseAmount(new BigDecimal("850000")).rate(new BigDecimal("0.056")).amount(new BigDecimal("47600")).lineType(PayslipLineType.IPRES_EMPLOYEE).build();
        employee1Payslip.setLines(new java.util.ArrayList<>(List.of(line1, line2)));
        employee1Payslip = payslipRepository.save(employee1Payslip);

        // 7. Tokens JWT
        drhToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder()
                .id(userDrh.getId()).username(userDrh.getUsername()).email(userDrh.getEmail())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH"))).build());

        managerToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder()
                .id(userMgr.getId()).username(userMgr.getUsername()).email(userMgr.getEmail())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).build());

        employee1Token = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder()
                .id(userEmp1.getId()).username(userEmp1.getUsername()).email(userEmp1.getEmail())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE"))).build());

        employee2Token = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder()
                .id(userEmp2.getId()).username(userEmp2.getUsername()).email(userEmp2.getEmail())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE"))).build());

        directionToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder()
                .id(userDir.getId()).username(userDir.getUsername()).email(userDir.getEmail())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_DIRECTION"))).build());
    }

    @Test
    @DisplayName("DRH peut télécharger l'attestation de travail d'un employé en PDF (200 OK + %PDF-)")
    void drhShouldDownloadWorkCertificatePdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/documents/attestation/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();
        assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Un employé peut télécharger sa propre attestation de travail en PDF (200 OK)")
    void employeeShouldDownloadOwnWorkCertificate() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/documents/attestation/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Un employé NE PEUT PAS télécharger l'attestation d'un autre (403 Forbidden)")
    void employeeShouldNotDownloadOtherEmployeeCertificate() throws Exception {
        mockMvc.perform(get("/api/v1/documents/attestation/" + employee2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La Direction ne peut pas télécharger de document individuel (403 Forbidden)")
    void directionShouldNotDownloadIndividualDocument() throws Exception {
        mockMvc.perform(get("/api/v1/documents/attestation/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRH peut télécharger le bulletin de paie en PDF (200 OK + %PDF-)")
    void drhShouldDownloadPayslipPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/documents/payslip/" + employee1Payslip.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("bulletin-paie")))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Un employé peut télécharger son propre bulletin de paie (200 OK)")
    void employeeShouldDownloadOwnPayslip() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/documents/payslip/" + employee1Payslip.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Un employé NE PEUT PAS télécharger le bulletin de paie d'un autre (403 Forbidden)")
    void employeeShouldNotDownloadOtherEmployeePayslip() throws Exception {
        mockMvc.perform(get("/api/v1/documents/payslip/" + employee1Payslip.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRH peut télécharger le contrat de travail en PDF (200 OK + %PDF-)")
    void drhShouldDownloadContractPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/documents/contract/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Cas limite : Identifiant non UUID renvoie 400 Bad Request (jamais 500)")
    void nonUuidIdentifierShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/documents/attestation/invalid-uuid-format")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cas limite : Document inexistant renvoie 404 Not Found (jamais 500)")
    void nonExistingDocumentShouldReturn404() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/documents/attestation/" + randomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Manager peut télécharger l'attestation de travail d'un subordonné direct (200 OK + %PDF-)")
    void managerShouldDownloadDirectSubordinateCertificate() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/documents/attestation/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();
        assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Manager NE PEUT PAS télécharger l'attestation d'un employé hors de son équipe (403 Forbidden)")
    void managerShouldNotDownloadNonSubordinateCertificate() throws Exception {
        mockMvc.perform(get("/api/v1/documents/attestation/" + employee2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager NE PEUT PAS télécharger la fiche de paie d'un subordonné (403 Forbidden)")
    void managerShouldNotDownloadSubordinatePayslip() throws Exception {
        mockMvc.perform(get("/api/v1/documents/payslip/" + employee1Payslip.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager NE PEUT PAS télécharger le contrat de travail d'un subordonné (403 Forbidden)")
    void managerShouldNotDownloadSubordinateContract() throws Exception {
        mockMvc.perform(get("/api/v1/documents/contract/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un employé NE PEUT PAS télécharger le contrat de travail d'un autre (403 Forbidden)")
    void employeeShouldNotDownloadOtherEmployeeContract() throws Exception {
        mockMvc.perform(get("/api/v1/documents/contract/" + employee2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La Direction ne peut pas télécharger de fiche de paie (403 Forbidden)")
    void directionShouldNotDownloadPayslip() throws Exception {
        mockMvc.perform(get("/api/v1/documents/payslip/" + employee1Payslip.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La Direction ne peut pas télécharger de contrat de travail (403 Forbidden)")
    void directionShouldNotDownloadContract() throws Exception {
        mockMvc.perform(get("/api/v1/documents/contract/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager NE PEUT PAS télécharger la fiche de paie d'un employé hors de son équipe (403 Forbidden)")
    void managerShouldNotDownloadNonSubordinatePayslip() throws Exception {
        mockMvc.perform(get("/api/v1/documents/payslip/" + employee1Payslip.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee2Token))
                .andExpect(status().isForbidden());
    }
}
