package com.careertrack.modules.orgchart;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.document.repository.EmployeeDocumentRepository;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.ContractRepository;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.employee.repository.SalaryHistoryRepository;
import com.careertrack.modules.leave.repository.LeaveBalanceRepository;
import com.careertrack.modules.payroll.repository.PayslipRepository; // <-- NOUVEL IMPORT
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrgChartControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;

    // Repositories ajoutés pour le nettoyage des tables enfants
    @Autowired private ContractRepository contractRepository;
    @Autowired private SalaryHistoryRepository salaryHistoryRepository;
    @Autowired private EmployeeDocumentRepository employeeDocumentRepository;
    @Autowired private PayslipRepository payslipRepository; // <-- NOUVELLE DÉCLARATION
    @Autowired private LeaveBalanceRepository leaveBalanceRepository;

    private String drhToken;
    private String employeeToken;
    private String directionToken;

    private Employee boss;
    private Employee worker;
    private Employee loner;

    @BeforeEach
    void setUp() {
        // Ordre important : on supprime d'abord les tables "enfants" (dépendantes), puis "parents"
        leaveBalanceRepository.deleteAll();
        payslipRepository.deleteAll(); // <-- EN PREMIER
        employeeDocumentRepository.deleteAll();
        contractRepository.deleteAll();
        salaryHistoryRepository.deleteAll();

        employeeRepository.deleteAll();
        positionRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role roleDrh = roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).description("DRH").build());
        Role roleEmp = roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).description("Employe").build());
        Role roleDir = roleRepository.save(Role.builder().name(RoleType.ROLE_DIRECTION).description("Direction").build());

        User userDrh = userRepository.save(User.builder().username("org.drh").email("org.drh@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleDrh)).build());
        User userDir = userRepository.save(User.builder().username("org.dir").email("org.dir@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleDir)).build());
        User userBoss = userRepository.save(User.builder().username("org.boss").email("org.boss@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleEmp)).build());
        User userWorker = userRepository.save(User.builder().username("org.worker").email("org.worker@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleEmp)).build());
        User userLoner = userRepository.save(User.builder().username("org.loner").email("org.loner@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleEmp)).build());

        Department dept = departmentRepository.save(Department.builder()
                .name("Ingenierie").code("DEP-ORG-" + UUID.randomUUID().toString().substring(0, 5)).build());
        Position pos = positionRepository.save(Position.builder()
                .title("Ingenieur").code("POS-ORG-" + UUID.randomUUID().toString().substring(0, 5))
                .department(dept).build());

        boss = employeeRepository.save(Employee.builder()
                .user(userBoss).employeeNumber("ORG-B-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Amadou").lastName("Diallo").nationalId("NAT-B-" + UUID.randomUUID())
                .email("boss@careertrack.sn").phone("+221770001122")
                .birthDate(LocalDate.of(1985, 3, 12)).hireDate(LocalDate.of(2020, 6, 1))
                .department(dept).position(pos).status(EmployeeStatus.ACTIVE).build());

        worker = employeeRepository.save(Employee.builder()
                .user(userWorker).employeeNumber("ORG-W-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Moussa").lastName("Sarr").nationalId("NAT-W-" + UUID.randomUUID())
                .email("worker@careertrack.sn").phone("+221771112233")
                .birthDate(LocalDate.of(1990, 5, 10)).hireDate(LocalDate.of(2023, 1, 15))
                .department(dept).position(pos).manager(boss).status(EmployeeStatus.ACTIVE).build());

        loner = employeeRepository.save(Employee.builder()
                .user(userLoner).employeeNumber("ORG-L-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Fatou").lastName("Diop").nationalId("NAT-L-" + UUID.randomUUID())
                .email("loner@careertrack.sn").phone("+221772223344")
                .birthDate(LocalDate.of(1995, 8, 20)).hireDate(LocalDate.of(2024, 2, 1))
                .department(dept).position(pos).status(EmployeeStatus.ACTIVE).build());

        drhToken = token(userDrh, "ROLE_DRH");
        employeeToken = token(userWorker, "ROLE_EMPLOYE");
        directionToken = token(userDir, "ROLE_DIRECTION");
    }

    private String token(User user, String role) {
        return jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .authorities(List.of(new SimpleGrantedAuthority(role)))
                        .build());
    }

    private JsonNode getTree(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgchart/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private void collectNames(JsonNode nodes, List<String> names) {
        for (JsonNode node : nodes) {
            names.add(node.get("name").asText());
            collectNames(node.get("subordinates"), names);
        }
    }

    @Test
    @DisplayName("L'arbre reflète la hiérarchie et ne contient aucune donnée de contact")
    void tree_contains_hierarchy_without_contact_data() throws Exception {
        JsonNode roots = getTree(employeeToken);

        List<String> rootNames = new ArrayList<>();
        roots.forEach(r -> rootNames.add(r.get("name").asText()));
        assertThat(rootNames).containsExactlyInAnyOrder("Amadou Diallo", "Fatou Diop");

        JsonNode bossNode = null;
        for (JsonNode r : roots) {
            if (r.get("name").asText().equals("Amadou Diallo")) {
                bossNode = r;
            }
        }
        assertThat(bossNode).isNotNull();
        assertThat(bossNode.get("subordinates").size()).isEqualTo(1);
        assertThat(bossNode.get("subordinates").get(0).get("name").asText()).isEqualTo("Moussa Sarr");
        assertThat(bossNode.path("email").isNull() || bossNode.path("email").isMissingNode()).isTrue();
        assertThat(bossNode.path("phone").isNull() || bossNode.path("phone").isMissingNode()).isTrue();
    }

    @Test
    @DisplayName("Un employé désactivé n'apparaît plus dans l'organigramme")
    void tree_excludes_terminated_employees() throws Exception {
        loner.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(loner);

        List<String> names = new ArrayList<>();
        collectNames(getTree(drhToken), names);

        assertThat(names).doesNotContain("Fatou Diop");
        assertThat(names).contains("Amadou Diallo", "Moussa Sarr");
    }

    @Test
    @DisplayName("Une boucle dans la hiérarchie ne fait pas planter l'organigramme")
    void tree_survives_a_manager_cycle() throws Exception {
        boss.setManager(worker);
        employeeRepository.save(boss);
        try {
            List<String> names = new ArrayList<>();
            collectNames(getTree(drhToken), names);
            assertThat(names).contains("Fatou Diop");
        } finally {
            boss.setManager(null);
            employeeRepository.save(boss);
        }
    }

    @Test
    @DisplayName("La Direction peut lire l'organigramme")
    void direction_can_read_tree() throws Exception {
        assertThat(getTree(directionToken).size()).isGreaterThan(0);
    }
}