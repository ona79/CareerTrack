package com.careertrack.modules.promotion.controller;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.promotion.dto.PromotionProposalDto;
import com.careertrack.modules.promotion.dto.SalarySimulationRequestDto;
import com.careertrack.modules.promotion.entity.PromotionProposal;
import com.careertrack.modules.promotion.entity.PromotionStatus;
import com.careertrack.modules.promotion.repository.PromotionProposalRepository;
import com.careertrack.modules.promotion.repository.SalarySimulationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionControllerIntegrationTest {

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
    private EmployeeRepository employeeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PromotionProposalRepository promotionProposalRepository;

    @Autowired
    private SalarySimulationRepository salarySimulationRepository;

    @Autowired
    private com.careertrack.modules.employee.repository.SalaryHistoryRepository salaryHistoryRepository;

    @Autowired
    private com.careertrack.modules.employee.repository.PositionHistoryRepository positionHistoryRepository;

    private String drhToken;
    private User drhUser;
    private Employee employee;
    private Position position;
    private PromotionProposal proposal;

    @BeforeEach
    void setUp() {
        cleanUp();

        String suffix = UUID.randomUUID().toString().substring(0, 6);

        Role drhRole = roleRepository.findByName(RoleType.ROLE_DRH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).build()));

        drhUser = userRepository.save(User.builder()
                .username("drh.promo." + suffix)
                .email("drh.promo." + suffix + "@careertrack.sn")
                .password("hash")
                .roles(Set.of(drhRole))
                .build());

        UserPrincipal principal = UserPrincipal.builder()
                .id(drhUser.getId())
                .username(drhUser.getUsername())
                .email(drhUser.getEmail())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH")))
                .build();
        drhToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(principal);

        position = positionRepository.save(Position.builder()
                .code("POS-PROMO-" + suffix)
                .title("Architecte Principal")
                .minSalary(new BigDecimal("800000"))
                .build());

        employee = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-PR-" + suffix)
                .firstName("Assane")
                .lastName("Ndiaye")
                .nationalId("SN-PR-" + suffix)
                .birthDate(LocalDate.of(1992, 5, 12))
                .email("assane.ndiaye." + suffix + "@careertrack.sn")
                .status(EmployeeStatus.ACTIVE)
                .hireDate(LocalDate.of(2022, 1, 10))
                .position(position)
                .build());

        proposal = promotionProposalRepository.save(PromotionProposal.builder()
                .employee(employee)
                .targetPosition(position)
                .currentSalary(new BigDecimal("900000"))
                .proposedSalary(new BigDecimal("1150000"))
                .justification("Performances remarquables")
                .status(PromotionStatus.SUBMITTED)
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        promotionProposalRepository.deleteAll();
        salarySimulationRepository.deleteAll();
        salaryHistoryRepository.deleteAll();
        positionHistoryRepository.deleteAll();
        if (employee != null) {
            employeeRepository.deleteById(employee.getId());
            employee = null;
        }
        if (position != null) {
            positionRepository.deleteById(position.getId());
            position = null;
        }
        if (drhUser != null) {
            userRepository.deleteById(drhUser.getId());
            drhUser = null;
        }
    }

    @Test
    @DisplayName("GET /api/v1/promotions - Lister les propositions de promotion")
    void shouldGetAllProposals() throws Exception {
        mockMvc.perform(get("/api/v1/promotions")
                        .header(HttpHeaders.AUTHORIZATION, drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employeeFullName").value("Assane Ndiaye"))
                .andExpect(jsonPath("$.data[0].currentSalary").value(900000.0));
    }

    @Test
    @DisplayName("PATCH /api/v1/promotions/{id}/status - Valider une promotion")
    void shouldApprovePromotionProposal() throws Exception {
        mockMvc.perform(patch("/api/v1/promotions/{id}/status", proposal.getId())
                        .param("status", "APPROVED")
                        .param("comment", "Félicitations pour la promotion")
                        .header(HttpHeaders.AUTHORIZATION, drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/v1/promotions/simulations - Calculer et sauvegarder une simulation de masse salariale")
    void shouldSimulateSalaryImpact() throws Exception {
        SalarySimulationRequestDto requestDto = SalarySimulationRequestDto.builder()
                .title("Revalorisation Générale Annuelle")
                .averageIncreasePercentage(new BigDecimal("6.5"))
                .build();

        mockMvc.perform(post("/api/v1/promotions/simulations")
                        .header(HttpHeaders.AUTHORIZATION, drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Revalorisation Générale Annuelle"))
                .andExpect(jsonPath("$.data.averageIncreasePercentage").value(6.5))
                .andExpect(jsonPath("$.data.currentMonthlyPayroll").isNumber())
                .andExpect(jsonPath("$.data.projectedMonthlyPayroll").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/promotions/simulations - Lister les simulations enregistrées")
    void shouldGetSalarySimulationsList() throws Exception {
        mockMvc.perform(get("/api/v1/promotions/simulations")
                        .header(HttpHeaders.AUTHORIZATION, drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
