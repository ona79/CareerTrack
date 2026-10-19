package com.careertrack.modules.employee.controller;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.dto.EmployeeDeactivateDto;
import com.careertrack.modules.employee.dto.EmployeeSelfUpdateDto;
import com.careertrack.modules.employee.dto.EmployeeUpdateDto;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.employee.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerIntegrationTest {

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
    private ContractRepository contractRepository;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @Autowired
    private SalaryHistoryRepository salaryHistoryRepository;

    @Autowired
    private com.careertrack.modules.payroll.repository.PayslipRepository payslipRepository;

    @Autowired
    private com.careertrack.modules.leave.repository.LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private com.careertrack.modules.leave.repository.LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private com.careertrack.modules.evaluation.repository.EvaluationRepository evaluationRepository;

    @Autowired
    private com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository evaluationCampaignRepository;

    @Autowired
    private com.careertrack.modules.document.repository.EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private com.careertrack.modules.document.repository.EmployeeDocumentContentRepository employeeDocumentContentRepository;

    private String drhToken;
    private String mgrToken;
    private String empToken;

    private Department deptTech;
    private Department deptFinance;
    private Position posLead;
    private Position posDev;

    private Employee drhEmp;
    private Employee mgrEmp;
    private Employee empSub;
    private Employee otherEmp;

    @BeforeEach
    void setUp() {
        employeeDocumentContentRepository.deleteAll();
        employeeDocumentRepository.deleteAll();
        evaluationRepository.deleteAll();
        evaluationCampaignRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();
        payslipRepository.deleteAll();
        positionHistoryRepository.deleteAll();
        salaryHistoryRepository.deleteAll();
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

        // 2. Utilisateurs
        User userDrh = userRepository.save(User.builder().username("drh.user").email("drh@careertrack.sn").password("hash").enabled(true).roles(Set.of(roleDrh)).build());
        User userMgr = userRepository.save(User.builder().username("mgr.user").email("mgr@careertrack.sn").password("hash").enabled(true).roles(Set.of(roleMgr)).build());
        User userEmp = userRepository.save(User.builder().username("emp.user").email("emp@careertrack.sn").password("hash").enabled(true).roles(Set.of(roleEmp)).build());
        User userOther = userRepository.save(User.builder().username("other.user").email("other@careertrack.sn").password("hash").enabled(true).roles(Set.of(roleEmp)).build());

        // 3. Départements et Postes
        deptTech = departmentRepository.save(Department.builder().name("Technologie").code("TECH").build());
        deptFinance = departmentRepository.save(Department.builder().name("Finance").code("FIN").build());

        posLead = positionRepository.save(Position.builder().title("Tech Lead").code("TECH-LEAD").department(deptTech).build());
        posDev = positionRepository.save(Position.builder().title("Développeur").code("DEV-JR").department(deptTech).build());

        // 4. Salariés
        drhEmp = employeeRepository.save(Employee.builder().user(userDrh).employeeNumber("EMP-001").firstName("Aminata").lastName("Diallo").nationalId("NAT-001").email("drh@careertrack.sn").phone("770000001").address("Dakar").birthDate(LocalDate.of(1985, 1, 1)).hireDate(LocalDate.of(2018, 1, 1)).department(deptTech).position(posLead).status(EmployeeStatus.ACTIVE).build());
        mgrEmp = employeeRepository.save(Employee.builder().user(userMgr).employeeNumber("EMP-002").firstName("Mamadou").lastName("Sow").nationalId("NAT-002").email("mgr@careertrack.sn").phone("770000002").address("Dakar").birthDate(LocalDate.of(1988, 2, 2)).hireDate(LocalDate.of(2019, 2, 1)).department(deptTech).position(posLead).status(EmployeeStatus.ACTIVE).build());
        empSub = employeeRepository.save(Employee.builder().user(userEmp).employeeNumber("EMP-003").firstName("Ousmane").lastName("Ba").nationalId("NAT-003").email("emp@careertrack.sn").phone("770000003").address("Thies").birthDate(LocalDate.of(1995, 3, 3)).hireDate(LocalDate.of(2022, 3, 1)).department(deptTech).position(posDev).manager(mgrEmp).status(EmployeeStatus.ACTIVE).build());
        otherEmp = employeeRepository.save(Employee.builder().user(userOther).employeeNumber("EMP-004").firstName("Khadija").lastName("Fall").nationalId("NAT-004").email("other@careertrack.sn").phone("770000004").address("Dakar").birthDate(LocalDate.of(1996, 4, 4)).hireDate(LocalDate.of(2023, 4, 1)).department(deptFinance).position(posDev).status(EmployeeStatus.ACTIVE).build());

        contractRepository.save(Contract.builder().employee(empSub).contractType(ContractType.CDI).startDate(LocalDate.of(2022, 3, 1)).grossSalary(new BigDecimal("600000")).active(true).build());

        // 5. Tokens JWT
        drhToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userDrh.getId()).username(userDrh.getUsername()).email(userDrh.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH"))).build());
        mgrToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userMgr.getId()).username(userMgr.getUsername()).email(userMgr.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).build());
        empToken = jwtTokenProvider.generateAccessTokenFromPrincipal(UserPrincipal.builder().id(userEmp.getId()).username(userEmp.getUsername()).email(userEmp.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE"))).build());
    }

    @Test
    @DisplayName("GET /api/v1/employees — DTO réduit : aucun champ sensible (nationalId, birthDate, phone, email, salary)")
    void listShouldNeverContainSensitiveFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode content = root.path("data").path("content");

        for (JsonNode item : content) {
            // Champs autorisés dans le DTO réduit
            assertThat(item.has("id")).isTrue();
            assertThat(item.has("employeeNumber")).isTrue();
            assertThat(item.has("fullName")).isTrue();
            assertThat(item.has("positionTitle")).isTrue();
            assertThat(item.has("departmentName")).isTrue();
            assertThat(item.has("status")).isTrue();

            // Champs sensibles STRICTEMENT INTERDITS dans la liste
            assertThat(item.has("nationalId")).as("nationalId ne doit pas être présent").isFalse();
            assertThat(item.has("birthDate")).as("birthDate ne doit pas être présent").isFalse();
            assertThat(item.has("address")).as("address ne doit pas être présent").isFalse();
            assertThat(item.has("phone")).as("phone ne doit pas être présent").isFalse();
            assertThat(item.has("email")).as("email ne doit pas être présent").isFalse();
            assertThat(item.has("salary")).as("salary ne doit pas être présent").isFalse();
            assertThat(item.has("grossSalary")).as("grossSalary ne doit pas être présent").isFalse();
        }
    }

    @Test
    @DisplayName("GET /api/v1/employees — Un Manager ne voit que ses subordonnés directs et lui-même")
    void managerShouldOnlySeeSubordinatesAndSelfInList() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mgrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2))) // mgrEmp + empSub
                .andExpect(jsonPath("$.data.content[*].employeeNumber", containsInAnyOrder("EMP-002", "EMP-003")));
    }

    @Test
    @DisplayName("GET /api/v1/employees — Tri non autorisé renvoie 400 Bad Request")
    void invalidSortShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/employees?sortBy=unknownField")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Champ de tri non autorisé")));
    }

    @Test
    @DisplayName("GET /api/v1/employees — Recherche par nom et filtre par département")
    void searchAndDepartmentFilter() throws Exception {
        mockMvc.perform(get("/api/v1/employees?search=Ousmane&departmentId=" + deptTech.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].employeeNumber", is("EMP-003")));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} — Données masquées pour un Manager sur son subordonné")
    void managerDetailOnSubordinateShouldMaskSensitiveBlocks() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + empSub.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mgrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identity.fullName", is("Ousmane Ba")))
                .andExpect(jsonPath("$.data.personalData").doesNotExist()) // null
                .andExpect(jsonPath("$.data.contract.grossSalary").doesNotExist()) // null
                .andExpect(jsonPath("$.data.salaryHistory").doesNotExist()) // null
                .andExpect(jsonPath("$.data.documents").doesNotExist()) // null
                .andExpect(jsonPath("$.data.positionHistory").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/employees/{id}/contact — Employé modifie ses propres coordonnées")
    void employeeCanUpdateOwnContact() throws Exception {
        EmployeeSelfUpdateDto updateDto = EmployeeSelfUpdateDto.builder()
                .phone("778889900")
                .address("Nouvelle Adresse Dakar")
                .build();

        mockMvc.perform(patch("/api/v1/employees/" + empSub.getId() + "/contact")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personalData.phone", is("778889900")))
                .andExpect(jsonPath("$.data.personalData.address", is("Nouvelle Adresse Dakar")));
    }

    @Test
    @DisplayName("PATCH /api/v1/employees/{id}/deactivate — Refus 409 Conflict si subordonnés actifs")
    void deactivateShouldFailWith409IfManagerHasActiveSubordinates() throws Exception {
        EmployeeDeactivateDto dto = EmployeeDeactivateDto.builder()
                .reason("Départ à la retraite")
                .build();

        mockMvc.perform(patch("/api/v1/employees/" + mgrEmp.getId() + "/deactivate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("collaborateur(s) actif(s) sous sa responsabilité")));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} — Refus 400 si cycle hiérarchique")
    void updateManagerShouldPreventHierarchyCycle() throws Exception {
        // mgrEmp a empSub comme subordonné. Tenter d'assigner empSub comme manager de mgrEmp doit échouer
        EmployeeUpdateDto dto = EmployeeUpdateDto.builder()
                .firstName("Mamadou")
                .lastName("Sow")
                .nationalId("NAT-002")
                .email("mgr@careertrack.sn")
                .birthDate(LocalDate.of(1988, 2, 2))
                .hireDate(LocalDate.of(2019, 2, 1))
                .managerId(empSub.getId()) // Crée un cycle A -> B -> A
                .build();

        mockMvc.perform(put("/api/v1/employees/" + mgrEmp.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cycle hiérarchique")));
    }

    @Test
    @DisplayName("Scénario Organigramme : mise à jour du manager reflétée immédiatement sur /orgchart/tree")
    void orgChartShouldReflectManagerUpdateImmediately() throws Exception {
        // 1. Initialement, otherEmp n'a pas de manager (racine)
        mockMvc.perform(get("/api/v1/orgchart/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].employeeNumber", hasItem("EMP-004")));

        // 2. DRH assigne mgrEmp comme manager de otherEmp
        EmployeeUpdateDto dto = EmployeeUpdateDto.builder()
                .firstName(otherEmp.getFirstName())
                .lastName(otherEmp.getLastName())
                .nationalId(otherEmp.getNationalId())
                .email(otherEmp.getEmail())
                .birthDate(otherEmp.getBirthDate())
                .hireDate(otherEmp.getHireDate())
                .departmentId(deptFinance.getId())
                .positionId(posDev.getId())
                .managerId(mgrEmp.getId())
                .build();

        mockMvc.perform(put("/api/v1/employees/" + otherEmp.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        // 3. Appel immédiat à /orgchart/tree : otherEmp apparaît désormais sous mgrEmp
        MvcResult treeResult = mockMvc.perform(get("/api/v1/orgchart/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andReturn();

        String treeJson = treeResult.getResponse().getContentAsString();
        JsonNode treeRoot = objectMapper.readTree(treeJson).path("data");

        // Recherche du nœud mgrEmp dans l'arbre
        boolean foundOtherUnderMgr = false;
        for (JsonNode rootNode : treeRoot) {
            if (rootNode.path("employeeNumber").asText().equals("EMP-002")) {
                JsonNode subs = rootNode.path("subordinates");
                for (JsonNode sub : subs) {
                    if (sub.path("employeeNumber").asText().equals("EMP-004")) {
                        foundOtherUnderMgr = true;
                    }
                }
            }
        }
        assertThat(foundOtherUnderMgr).as("otherEmp doit apparaître comme subordonné de mgrEmp dans l'organigramme").isTrue();
    }
}
