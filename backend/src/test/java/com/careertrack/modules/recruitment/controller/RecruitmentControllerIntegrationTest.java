package com.careertrack.modules.recruitment.controller;

import com.careertrack.common.security.JwtTokenProvider;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.Role;
import com.careertrack.modules.auth.entity.RoleType;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.RoleRepository;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.employee.entity.ContractType;
import com.careertrack.modules.notification.repository.NotificationRepository;
import com.careertrack.modules.recruitment.dto.JobOfferCreateDto;
import com.careertrack.modules.recruitment.entity.*;
import com.careertrack.modules.recruitment.repository.CandidateRepository;
import com.careertrack.modules.recruitment.repository.JobApplicationRepository;
import com.careertrack.modules.recruitment.repository.JobOfferRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecruitmentControllerIntegrationTest {

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
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String drhToken;
    private User drhUser;
    private JobOffer jobOffer;
    private Candidate candidate;
    private JobApplication application;

    @BeforeEach
    void setUp() {
        cleanUp();

        Role drhRole = roleRepository.findByName(RoleType.ROLE_DRH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).build()));

        drhUser = userRepository.save(User.builder()
                .username("drh.recruitment.test")
                .email("drh.recruitment@careertrack.sn")
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

        jobOffer = jobOfferRepository.save(JobOffer.builder()
                .code("OFF-TEST-" + UUID.randomUUID().toString().substring(0, 5))
                .title("Développeur Fullstack React Spring")
                .contractType(ContractType.CDI)
                .description("Poste CDI pour renforcer l'équipe CareerTrack")
                .status(JobOfferStatus.OPEN)
                .openPositions(2)
                .closingDate(LocalDate.now().plusMonths(1))
                .build());

        candidate = candidateRepository.save(Candidate.builder()
                .firstName("Moussa")
                .lastName("Faye")
                .email("moussa.faye." + UUID.randomUUID().toString().substring(0, 5) + "@test.sn")
                .phone("+221 77 000 11 22")
                .build());

        application = jobApplicationRepository.save(JobApplication.builder()
                .jobOffer(jobOffer)
                .candidate(candidate)
                .status(ApplicationStatus.RECU)
                .currentStep("INITIAL_REVIEW")
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jobApplicationRepository.deleteAll();
        candidateRepository.deleteAll();
        jobOfferRepository.deleteAll();
        notificationRepository.deleteAll();
        if (drhUser != null) {
            userRepository.deleteById(drhUser.getId());
            drhUser = null;
        }
    }

    @Test
    @DisplayName("GET /api/v1/recruitment/applications - Récupérer le pipeline Kanban")
    void shouldGetAllApplicationsForKanban() throws Exception {
        mockMvc.perform(get("/api/v1/recruitment/applications")
                        .header(HttpHeaders.AUTHORIZATION, drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].candidateFullName").value("Moussa Faye"))
                .andExpect(jsonPath("$.data[0].status").value("RECU"));
    }

    @Test
    @DisplayName("PATCH /api/v1/recruitment/applications/{id}/status - Déplacer vers PRESELECTIONNE")
    void shouldUpdateApplicationStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/recruitment/applications/{id}/status", application.getId())
                        .param("status", "PRESELECTIONNE")
                        .header(HttpHeaders.AUTHORIZATION, drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PRESELECTIONNE"));
    }

    @Test
    @DisplayName("POST /api/v1/recruitment/offers - Publier une nouvelle offre d'emploi")
    void shouldCreateJobOffer() throws Exception {
        JobOfferCreateDto createDto = JobOfferCreateDto.builder()
                .title("Ingénieur DevOps")
                .contractType(ContractType.CDI)
                .description("Gestion de cluster Kubernetes et CI/CD GitHub Actions")
                .openPositions(1)
                .closingDate(LocalDate.now().plusMonths(2))
                .build();

        mockMvc.perform(post("/api/v1/recruitment/offers")
                        .header(HttpHeaders.AUTHORIZATION, drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Ingénieur DevOps"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /api/v1/recruitment/offers - Lister les offres publiées")
    void shouldGetPublishedOffers() throws Exception {
        mockMvc.perform(get("/api/v1/recruitment/offers")
                        .header(HttpHeaders.AUTHORIZATION, drhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
