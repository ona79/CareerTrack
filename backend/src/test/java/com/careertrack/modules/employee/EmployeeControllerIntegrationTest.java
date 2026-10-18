package com.careertrack.modules.employee;

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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;

    private String drhToken;
    private String managerToken;
    private String employee1Token;
    private String employee2Token;
    private String directionToken;

    private Employee managerEmployee;
    private Employee employee1;
    private Employee employee2;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        positionRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role roleDrh = roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).description("DRH").build());
        Role roleEmp = roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).description("Employe").build());
        Role roleDir = roleRepository.save(Role.builder().name(RoleType.ROLE_DIRECTION).description("Direction").build());
        Role roleMgr = roleRepository.save(Role.builder().name(RoleType.ROLE_MANAGER).description("Manager").build());

        User userDrh = userRepository.save(User.builder().username("test.drh").email("drh@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleDrh)).build());
        User userMgr = userRepository.save(User.builder().username("test.mgr").email("mgr@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleMgr)).build());
        User userEmp1 = userRepository.save(User.builder().username("test.emp1").email("emp1@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleEmp)).build());
        User userEmp2 = userRepository.save(User.builder().username("test.emp2").email("emp2@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleEmp)).build());
        User userDir = userRepository.save(User.builder().username("test.dir").email("dir@test.sn")
                .password("$2a$10$xyz").enabled(true).roles(Set.of(roleDir)).build());

        Department dept = departmentRepository.save(Department.builder()
                .name("Ingénierie").code("DEP-IT-" + UUID.randomUUID().toString().substring(0, 5)).build());
        Position pos = positionRepository.save(Position.builder()
                .title("Ingénieur Logiciel").code("POS-ING-" + UUID.randomUUID().toString().substring(0, 5))
                .department(dept).build());

        managerEmployee = employeeRepository.save(Employee.builder()
                .user(userMgr).employeeNumber("EMP-MGR-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Amadou").lastName("Diallo").nationalId("NAT-MGR-" + UUID.randomUUID())
                .email("mgr@careertrack.sn").phone("+221770001122")
                .birthDate(LocalDate.of(1985, 3, 12)).hireDate(LocalDate.of(2020, 6, 1))
                .department(dept).position(pos).status(EmployeeStatus.ACTIVE).build());

        employee1 = employeeRepository.save(Employee.builder()
                .user(userEmp1).employeeNumber("EMP-1-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Moussa").lastName("Sarr").nationalId("NAT-1-" + UUID.randomUUID())
                .email("emp1@careertrack.sn").phone("+221771112233")
                .birthDate(LocalDate.of(1990, 5, 10)).hireDate(LocalDate.of(2023, 1, 15))
                .department(dept).position(pos).manager(managerEmployee).status(EmployeeStatus.ACTIVE).build());

        employee2 = employeeRepository.save(Employee.builder()
                .user(userEmp2).employeeNumber("EMP-2-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Fatou").lastName("Diop").nationalId("NAT-2-" + UUID.randomUUID())
                .email("emp2@careertrack.sn").phone("+221772223344")
                .birthDate(LocalDate.of(1995, 8, 20)).hireDate(LocalDate.of(2024, 2, 1))
                .department(dept).position(pos).manager(null).status(EmployeeStatus.ACTIVE).build());

        drhToken = token(userDrh, "ROLE_DRH");
        managerToken = token(userMgr, "ROLE_MANAGER");
        employee1Token = token(userEmp1, "ROLE_EMPLOYE");
        employee2Token = token(userEmp2, "ROLE_EMPLOYE");
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

    // ─────────────────────────────────────────────────────────────
    // Règle 1 : EMPLOYE sur le dossier d'un autre → 403
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("EMPLOYE ne peut PAS accéder au dossier d'un autre employé (403)")
    void employee_cannot_access_other_employee() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + employee2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────
    // Règle 2 : EMPLOYE sur son propre dossier → 200 avec données complètes
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("EMPLOYE accède à son propre dossier (200, données sensibles visibles)")
    void employee_can_access_own_record() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/employees/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("nationalId").asText()).isNotBlank();
        assertThat(data.get("birthDate").asText()).isNotBlank();
        assertThat(data.get("phone").asText()).isNotBlank();
    }

    // ─────────────────────────────────────────────────────────────
    // Règle 3 : MANAGER sur un subordonné → 200 mais données sensibles masquées
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("MANAGER voit son subordonné mais sans CNI/naissance/téléphone/adresse")
    void manager_sees_subordinate_without_sensitive_data() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/employees/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("nationalId").isNull()).isTrue();
        assertThat(data.get("birthDate").isNull()).isTrue();
        assertThat(data.get("phone").isNull()).isTrue();
        assertThat(data.get("address").isNull()).isTrue();
        assertThat(data.get("firstName").asText()).isEqualTo("Moussa");
    }

    // ─────────────────────────────────────────────────────────────
    // Bonus : DRH voit tout, données sensibles incluses
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("DRH voit n'importe quel dossier avec données sensibles complètes")
    void drh_sees_everything() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/employees/" + employee2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("nationalId").asText()).isNotBlank();
    }

    // ─────────────────────────────────────────────────────────────
    // Bonus : DIRECTION ne peut PAS accéder aux dossiers individuels
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("DIRECTION n'a pas accès aux dossiers nominatifs (403)")
    void direction_cannot_access_employee_record() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La liste ne contient aucune donnée sensible")
    void list_contains_no_sensitive_fields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("content");
        assertThat(content.size()).isEqualTo(3);
        for (JsonNode item : content) {
            assertThat(item.has("fullName")).isTrue();
            assertThat(item.has("nationalId")).isFalse();
            assertThat(item.has("phone")).isFalse();
            assertThat(item.has("address")).isFalse();
            assertThat(item.has("birthDate")).isFalse();
            assertThat(item.has("email")).isFalse();
        }
    }

    @Test
    @DisplayName("Un tri sur un champ interdit est refusé (400)")
    void list_rejects_forbidden_sort_field() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .param("sortBy", "nationalId")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MANAGER ne voit que son équipe dans la liste")
    void manager_list_is_limited_to_his_team() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("content");
        List<String> names = new java.util.ArrayList<>();
        content.forEach(item -> names.add(item.get("fullName").asText()));

        assertThat(names).containsExactlyInAnyOrder("Amadou Diallo", "Moussa Sarr");
        assertThat(names).doesNotContain("Fatou Diop");
    }

    @Test
    @DisplayName("La recherche par nom ne renvoie que les employés correspondants")
    void list_search_filters_by_name() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/employees")
                        .param("search", "sarr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("content");
        assertThat(content.size()).isEqualTo(1);
        assertThat(content.get(0).get("fullName").asText()).isEqualTo("Moussa Sarr");
    }

    @Test
    @DisplayName("DELETE désactive l'employé sans supprimer son dossier")
    void delete_deactivates_employee() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/employees/" + employee2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isOk());

        Employee reloaded = employeeRepository.findById(employee2.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
    }
}