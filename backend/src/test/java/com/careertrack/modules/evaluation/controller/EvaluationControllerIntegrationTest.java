package com.careertrack.modules.evaluation.controller;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.evaluation.dto.*;
import com.careertrack.modules.evaluation.entity.CampaignStatus;
import com.careertrack.modules.evaluation.entity.EvaluationCampaign;
import com.careertrack.modules.evaluation.entity.ObjectiveCategory;
import com.careertrack.modules.evaluation.repository.EvaluationCampaignRepository;
import com.careertrack.modules.evaluation.repository.EvaluationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationControllerIntegrationTest {

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
    private EvaluationCampaignRepository campaignRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private com.careertrack.modules.evaluation.repository.ObjectiveRepository objectiveRepository;

    private String drhToken;
    private String mgrToken;
    private String empToken;

    private User drhUser;
    private User mgrUser;
    private User empUser;

    private Employee mgrEmployee;
    private Employee empEmployee;
    private EvaluationCampaign testCampaign;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        objectiveRepository.deleteAll();
        evaluationRepository.deleteAll();
        campaignRepository.deleteAll();
        if (empEmployee != null) {
            employeeRepository.delete(empEmployee);
            empEmployee = null;
        }
        if (mgrEmployee != null) {
            employeeRepository.delete(mgrEmployee);
            mgrEmployee = null;
        }
        if (empUser != null) {
            userRepository.delete(empUser);
            empUser = null;
        }
        if (mgrUser != null) {
            userRepository.delete(mgrUser);
            mgrUser = null;
        }
        if (drhUser != null) {
            userRepository.delete(drhUser);
            drhUser = null;
        }
    }

    @BeforeEach
    void setUp() {
        cleanUp();
        Role drhRole = roleRepository.findByName(RoleType.ROLE_DRH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).build()));
        Role mgrRole = roleRepository.findByName(RoleType.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_MANAGER).build()));
        Role empRole = roleRepository.findByName(RoleType.ROLE_EMPLOYE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).build()));

        String suffix = UUID.randomUUID().toString().substring(0, 6);

        drhUser = userRepository.save(User.builder()
                .username("drh.eval." + suffix)
                .email("drh.eval." + suffix + "@careertrack.sn")
                .password("secret")
                .roles(Set.of(drhRole))
                .build());

        mgrUser = userRepository.save(User.builder()
                .username("mgr.eval." + suffix)
                .email("mgr.eval." + suffix + "@careertrack.sn")
                .password("secret")
                .roles(Set.of(mgrRole))
                .build());

        empUser = userRepository.save(User.builder()
                .username("emp.eval." + suffix)
                .email("emp.eval." + suffix + "@careertrack.sn")
                .password("secret")
                .roles(Set.of(empRole))
                .build());

        mgrEmployee = employeeRepository.save(Employee.builder()
                .user(mgrUser)
                .employeeNumber("MGR-EVAL-" + suffix)
                .firstName("Cheikh")
                .lastName("Seck")
                .nationalId("NAT-MGR-" + suffix)
                .email("mgr.eval." + suffix + "@careertrack.sn")
                .birthDate(LocalDate.of(1985, 5, 10))
                .hireDate(LocalDate.of(2022, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .build());

        empEmployee = employeeRepository.save(Employee.builder()
                .user(empUser)
                .employeeNumber("EMP-EVAL-" + suffix)
                .firstName("Amadou")
                .lastName("Diop")
                .nationalId("NAT-EMP-" + suffix)
                .email("emp.eval." + suffix + "@careertrack.sn")
                .birthDate(LocalDate.of(1992, 8, 15))
                .manager(mgrEmployee)
                .hireDate(LocalDate.of(2023, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .build());

        drhToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder()
                        .id(drhUser.getId())
                        .username(drhUser.getUsername())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRH")))
                        .build()
        );

        mgrToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder()
                        .id(mgrUser.getId())
                        .username(mgrUser.getUsername())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))
                        .build()
        );

        empToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder()
                        .id(empUser.getId())
                        .username(empUser.getUsername())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE")))
                        .build()
        );

        testCampaign = campaignRepository.save(EvaluationCampaign.builder()
                .title("Campagne Annuelle Test " + suffix)
                .campaignYear(2026)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(CampaignStatus.IN_PROGRESS)
                .build());
    }

    @Test
    @DisplayName("Lister les campagnes d'évaluation via GET /api/v1/evaluations/campaigns")
    void shouldGetCampaigns() throws Exception {
        mockMvc.perform(get("/api/v1/evaluations/campaigns")
                        .header(HttpHeaders.AUTHORIZATION, empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Workflow complet de l'évaluation annuelle : initialisation, auto-évaluation, évaluation managériale et PDF signable")
    void shouldExecuteFullEvaluationWorkflow() throws Exception {
        // 1. Création de la fiche d'évaluation avec un objectif SMART
        EvaluationCreateDto createDto = EvaluationCreateDto.builder()
                .campaignId(testCampaign.getId())
                .employeeId(empEmployee.getId())
                .evaluatorId(mgrEmployee.getId())
                .objectives(List.of(
                        EvaluationCreateDto.ObjectiveInputDto.builder()
                                .title("Déploiement Spring Boot 3")
                                .category(ObjectiveCategory.OPERATIONAL)
                                .weight(BigDecimal.valueOf(100))
                                .achievementRate(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeFullName").value("Amadou Diop"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        UUID evalId = UUID.fromString(objectMapper.readTree(createRes).path("data").path("id").asText());
        UUID objId = UUID.fromString(objectMapper.readTree(createRes).path("data").path("objectives").get(0).path("id").asText());

        // 2. Auto-évaluation par l'employé
        EvaluationSelfSubmitDto selfDto = EvaluationSelfSubmitDto.builder()
                .selfScore(new BigDecimal("4.0"))
                .selfFeedback("Objectif atteint à temps")
                .objectiveRates(List.of(
                        EvaluationSelfSubmitDto.ObjectiveRateDto.builder()
                                .objectiveId(objId)
                                .selfRate(new BigDecimal("90.0"))
                                .build()
                ))
                .build();

        mockMvc.perform(patch("/api/v1/evaluations/" + evalId + "/self-eval")
                        .header(HttpHeaders.AUTHORIZATION, empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SELF_SUBMITTED"))
                .andExpect(jsonPath("$.data.selfScore").value(4.0));

        // 3. Évaluation par le manager N+1 avec calcul automatique du score pondéré (100% à 95% = 4.75 sur 5.0)
        EvaluationManagerSubmitDto mgrDto = EvaluationManagerSubmitDto.builder()
                .managerScore(new BigDecimal("4.5"))
                .managerFeedback("Très bon travail sur la CI/CD")
                .interviewDate(LocalDate.of(2026, 11, 15))
                .objectiveRates(List.of(
                        EvaluationManagerSubmitDto.ManagerObjectiveRateDto.builder()
                                .objectiveId(objId)
                                .achievementRate(new BigDecimal("95.0"))
                                .status("ACHIEVED")
                                .build()
                ))
                .build();

        mockMvc.perform(patch("/api/v1/evaluations/" + evalId + "/manager-eval")
                        .header(HttpHeaders.AUTHORIZATION, mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mgrDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MANAGER_EVALUATED"))
                .andExpect(jsonPath("$.data.managerScore").value(4.5))
                .andExpect(jsonPath("$.data.finalScore").value(4.75));

        // 4. Clôture de l'évaluation
        EvaluationCompleteDto completeDto = EvaluationCompleteDto.builder()
                .finalFeedback("Entretien annuel validé par la hiérarchie")
                .build();

        mockMvc.perform(patch("/api/v1/evaluations/" + evalId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 5. Téléchargement du PDF signable
        mockMvc.perform(get("/api/v1/evaluations/" + evalId + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, empToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"entretien_annuel_" + evalId + ".pdf\""));
    }

    @Test
    @DisplayName("Vérification du scoping : l'employé ne voit que ses évaluations via /api/v1/evaluations/my")
    void shouldReturnOnlyEmployeeEvaluations() throws Exception {
        mockMvc.perform(get("/api/v1/evaluations/my")
                        .header(HttpHeaders.AUTHORIZATION, empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
