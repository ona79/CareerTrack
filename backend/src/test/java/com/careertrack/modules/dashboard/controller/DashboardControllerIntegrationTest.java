package com.careertrack.modules.dashboard.controller;

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
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.EmployeeRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIntegrationTest {

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
    private EmployeeRepository employeeRepository;

    private String drhToken;
    private String mgrToken;
    private String empToken;

    @BeforeEach
    void setUp() {
        Role roleEmp = roleRepository.findByName(RoleType.ROLE_EMPLOYE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).description("Employé").build()));
        Role roleMgr = roleRepository.findByName(RoleType.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_MANAGER).description("Manager").build()));
        Role roleDrh = roleRepository.findByName(RoleType.ROLE_DRH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).description("DRH").build()));

        User userDrh = userRepository.findByUsername("dash.drh")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("dash.drh")
                        .email("dash.drh@careertrack.sn")
                        .password("hash")
                        .roles(Set.of(roleDrh))
                        .enabled(true)
                        .build()));

        User userMgr = userRepository.findByUsername("dash.mgr")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("dash.mgr")
                        .email("dash.mgr@careertrack.sn")
                        .password("hash")
                        .roles(Set.of(roleMgr))
                        .enabled(true)
                        .build()));

        User userEmp = userRepository.findByUsername("dash.emp")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("dash.emp")
                        .email("dash.emp@careertrack.sn")
                        .password("hash")
                        .roles(Set.of(roleEmp))
                        .enabled(true)
                        .build()));

        drhToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder().id(userDrh.getId()).username(userDrh.getUsername())
                        .email(userDrh.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH"))).build());

        mgrToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder().id(userMgr.getId()).username(userMgr.getUsername())
                        .email(userMgr.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))).build());

        empToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder().id(userEmp.getId()).username(userEmp.getUsername())
                        .email(userEmp.getEmail()).authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE"))).build());

        Department dept = departmentRepository.findByCode("DASH_DEPT")
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code("DASH_DEPT")
                        .name("Département Dashboard Test")
                        .description("Test")
                        .build()));

        employeeRepository.findByEmail("dash.mgr@careertrack.sn")
                .orElseGet(() -> employeeRepository.save(Employee.builder()
                        .employeeNumber("DASH-MGR")
                        .firstName("Manager")
                        .lastName("Dash")
                        .nationalId("CNI-DASH-MGR")
                        .email("dash.mgr@careertrack.sn")
                        .birthDate(LocalDate.of(1982, 4, 10))
                        .hireDate(LocalDate.of(2018, 1, 1))
                        .department(dept)
                        .user(userMgr)
                        .status(EmployeeStatus.ACTIVE)
                        .build()));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/stats accessible à DRH avec retour des KPIs consolidés")
    void testGetStats_DrhAccess() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, drhToken)
                        .param("periodYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.periodYear").value(2026))
                .andExpect(jsonPath("$.data.headcount").exists())
                .andExpect(jsonPath("$.data.agePyramid").isArray())
                .andExpect(jsonPath("$.data.turnover").exists())
                .andExpect(jsonPath("$.data.payroll").exists())
                .andExpect(jsonPath("$.data.training").exists())
                .andExpect(jsonPath("$.data.leaves").exists());
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/stats accessible à MANAGER")
    void testGetStats_ManagerAccess() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, mgrToken)
                        .param("periodYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.headcount").exists());
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/stats interdit aux collaborateurs standards (403 Forbidden)")
    void testGetStats_EmployeeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, empToken))
                .andExpect(status().isForbidden());
    }
}
