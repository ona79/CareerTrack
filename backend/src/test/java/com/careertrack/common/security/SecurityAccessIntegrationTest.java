package com.careertrack.common.security;

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
import com.careertrack.modules.evaluation.entity.Evaluation;
import com.careertrack.modules.evaluation.entity.EvaluationCampaign;
import com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository;
import com.careertrack.modules.evaluation.repository.EvaluationRepository;
import com.careertrack.modules.leave.entity.LeaveBalance;
import com.careertrack.modules.leave.entity.LeaveType;
import com.careertrack.modules.leave.repository.LeaveBalanceRepository;
import com.careertrack.modules.leave.repository.LeaveRequestRepository;
import com.careertrack.modules.leave.repository.LeaveTypeRepository;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessIntegrationTest {

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
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EvaluationCampaignRepository campaignRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private com.careertrack.modules.document.repository.EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private com.careertrack.modules.document.repository.EmployeeDocumentContentRepository employeeDocumentContentRepository;

    private String drhToken;
    private String mgrToken;
    private String empToken;
    private String otherEmpToken;
    private String dirToken;

    private Employee drhEmployee;
    private Employee mgrEmployee;
    private Employee empEmployee;
    private Employee otherEmployee;
    private Payslip empPayslip;
    private Evaluation empEvaluation;

    @BeforeEach
    void setUp() {
        employeeDocumentContentRepository.deleteAll();
        employeeDocumentRepository.deleteAll();
        evaluationRepository.deleteAll();
        campaignRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        leaveTypeRepository.deleteAll();
        payslipRepository.deleteAll();
        contractRepository.deleteAll();
        employeeRepository.deleteAll();
        positionRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Rôles
        Role roleDrh = roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).description("DRH").build());
        Role roleMgr = roleRepository.save(Role.builder().name(RoleType.ROLE_MANAGER).description("Manager").build());
        Role roleEmp = roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).description("Employé").build());
        Role roleDir = roleRepository.save(Role.builder().name(RoleType.ROLE_DIRECTION).description("Direction").build());

        // 2. Vrais Utilisateurs du seed
        User userDrh = userRepository.save(User.builder().username("drh.ndiaye").email("drh@careertrack.sn").password("$2a$10$xyz").enabled(true).roles(Set.of(roleDrh)).build());
        User userMgr = userRepository.save(User.builder().username("mgr.sow").email("manager@careertrack.sn").password("$2a$10$xyz").enabled(true).roles(Set.of(roleMgr)).build());
        User userEmp = userRepository.save(User.builder().username("emp.diallo").email("employe@careertrack.sn").password("$2a$10$xyz").enabled(true).roles(Set.of(roleEmp)).build());
        User userOther = userRepository.save(User.builder().username("emp.autre").email("autre@careertrack.sn").password("$2a$10$xyz").enabled(true).roles(Set.of(roleEmp)).build());
        User userDir = userRepository.save(User.builder().username("dir.fall").email("direction@careertrack.sn").password("$2a$10$xyz").enabled(true).roles(Set.of(roleDir)).build());

        // 3. Département & Poste
        Department dept = departmentRepository.save(Department.builder().name("Ingénierie & Systèmes").code("DEP-TECH").build());
        Position pos = positionRepository.save(Position.builder().title("Développeur Fullstack Senior").code("POS-DEV-SR").department(dept).build());

        // 4. Salariés avec hiérarchie réelle
        drhEmployee = employeeRepository.save(Employee.builder().user(userDrh).employeeNumber("EMP-002").firstName("Aminata").lastName("Ndiaye").nationalId("2 985 1985 00456").email("drh@careertrack.sn").phone("+221 77 234 56 78").birthDate(LocalDate.of(1985, 9, 22)).hireDate(LocalDate.of(2019, 3, 15)).department(dept).position(pos).status(EmployeeStatus.ACTIVE).build());
        mgrEmployee = employeeRepository.save(Employee.builder().user(userMgr).employeeNumber("EMP-003").firstName("Mamadou").lastName("Sow").nationalId("1 988 1988 00789").email("manager@careertrack.sn").phone("+221 77 345 67 89").birthDate(LocalDate.of(1988, 11, 30)).hireDate(LocalDate.of(2020, 2, 1)).department(dept).position(pos).status(EmployeeStatus.ACTIVE).build());
        empEmployee = employeeRepository.save(Employee.builder().user(userEmp).employeeNumber("EMP-004").firstName("Ousmane").lastName("Diallo").nationalId("1 996 1996 00321").email("employe@careertrack.sn").phone("+221 77 456 78 90").birthDate(LocalDate.of(1996, 7, 18)).hireDate(LocalDate.of(2022, 6, 1)).department(dept).position(pos).manager(mgrEmployee).status(EmployeeStatus.ACTIVE).build());
        otherEmployee = employeeRepository.save(Employee.builder().user(userOther).employeeNumber("EMP-005").firstName("Fatou").lastName("Ba").nationalId("2 997 1997 00999").email("autre@careertrack.sn").phone("+221 77 999 88 77").birthDate(LocalDate.of(1997, 1, 10)).hireDate(LocalDate.of(2023, 1, 1)).department(dept).position(pos).status(EmployeeStatus.ACTIVE).build());

        // 5. Bulletin de paie
        empPayslip = payslipRepository.save(Payslip.builder().employee(empEmployee).periodMonth(9).periodYear(2026).grossSalary(new BigDecimal("950000")).netSalary(new BigDecimal("786800")).totalDeductions(new BigDecimal("163200")).paymentDate(LocalDate.of(2026, 9, 30)).status(PayslipStatus.GENERATED).build());

        // 6. Évaluation
        EvaluationCampaign camp = campaignRepository.save(EvaluationCampaign.builder().title("Campagne 2026").campaignYear(2026).startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 12, 31)).build());
        empEvaluation = evaluationRepository.save(Evaluation.builder().campaign(camp).employee(empEmployee).evaluator(mgrEmployee).interviewDate(LocalDate.of(2026, 6, 15)).finalScore(new BigDecimal("4.5")).build());

        // 7. Tokens JWT générés depuis UserPrincipal
        drhToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userDrh.getId()).username(userDrh.getUsername()).email(userDrh.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH"))).build());
        mgrToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userMgr.getId()).username(userMgr.getUsername()).email(userMgr.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).build());
        empToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userEmp.getId()).username(userEmp.getUsername()).email(userEmp.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE"))).build());
        otherEmpToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userOther.getId()).username(userOther.getUsername()).email(userOther.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE"))).build());
        dirToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userDir.getId()).username(userDir.getUsername()).email(userDir.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_DIRECTION"))).build());
    }

    @Test
    @DisplayName("Requête sans jeton renvoie 401 Unauthorized")
    void requestWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + empEmployee.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Employé consultant son propre dossier -> 200 OK")
    void employeeShouldAccessOwnFile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + empEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + empToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Employé consultant le dossier d'un autre employé -> 403 Forbidden")
    void employeeShouldNotAccessOtherEmployeeFile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + otherEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + empToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager consultant le dossier d'un subordonné direct -> 200 OK")
    void managerShouldAccessDirectSubordinateFile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + empEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mgrToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Manager consultant le dossier d'un employé hors équipe -> 403 Forbidden")
    void managerShouldNotAccessNonSubordinateFile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + otherEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mgrToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Direction consultant un dossier individuel d'employé -> 403 Forbidden")
    void directionShouldNotAccessIndividualEmployeeFile() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + empEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dirToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Direction consultant la liste nominative de paie -> 403 Forbidden")
    void directionShouldNotAccessNominativePayrollList() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/payslips?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dirToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRH consultant la liste nominative de paie -> 200 OK")
    void drhShouldAccessNominativePayrollList() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/payslips?period=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Employé consultant les bulletins d'un autre collaborateur -> 403 Forbidden")
    void employeeShouldNotAccessOtherEmployeePayslips() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/payslips/employee/" + otherEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + empToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employé consultant un bulletin de paie d'un tiers -> 403 Forbidden")
    void employeeShouldNotAccessOtherEmployeePayslipById() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/payslips/" + empPayslip.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherEmpToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employé consultant les congés d'un autre salarié -> 403 Forbidden")
    void employeeShouldNotAccessOtherEmployeeLeaves() throws Exception {
        mockMvc.perform(get("/api/v1/leaves/employee/" + otherEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + empToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employé consultant les soldes de congés d'un autre salarié -> 403 Forbidden")
    void employeeShouldNotAccessOtherEmployeeBalances() throws Exception {
        mockMvc.perform(get("/api/v1/leaves/balances/" + otherEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + empToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employé consultant l'évaluation d'un autre collaborateur -> 403 Forbidden")
    void employeeShouldNotAccessOtherEmployeeEvaluation() throws Exception {
        mockMvc.perform(get("/api/v1/evaluations/" + empEvaluation.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherEmpToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Direction validant une promotion (PATCH /status) -> 403 Forbidden")
    void directionShouldNotPatchPromotionStatus() throws Exception {
        UUID randomPromoId = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/promotions/" + randomPromoId + "/status?status=APPROVED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dirToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Notifications /me pour l'utilisateur connecté -> 200 OK")
    void userShouldAccessOwnNotificationsViaMe() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + empToken))
                .andExpect(status().isOk());
    }
}
