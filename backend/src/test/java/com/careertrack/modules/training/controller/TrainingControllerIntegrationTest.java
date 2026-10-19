package com.careertrack.modules.training.controller;

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
import com.careertrack.modules.training.dto.*;
import com.careertrack.modules.training.entity.EnrollmentStatus;
import com.careertrack.modules.training.entity.Training;
import com.careertrack.modules.training.repository.TrainingEnrollmentRepository;
import com.careertrack.modules.training.repository.TrainingRepository;
import com.careertrack.modules.training.repository.TrainingSessionRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainingControllerIntegrationTest {

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
    private TrainingRepository trainingRepository;

    @Autowired
    private TrainingSessionRepository sessionRepository;

    @Autowired
    private TrainingEnrollmentRepository enrollmentRepository;

    private String drhToken;
    private String empToken;

    private User drhUser;
    private User empUser;
    private Employee empEmployee;
    private Training testTraining;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        enrollmentRepository.deleteAll();
        sessionRepository.deleteAll();
        trainingRepository.deleteAll();
        if (empEmployee != null) {
            employeeRepository.delete(empEmployee);
            empEmployee = null;
        }
        if (drhUser != null) {
            userRepository.delete(drhUser);
            drhUser = null;
        }
        if (empUser != null) {
            userRepository.delete(empUser);
            empUser = null;
        }
    }

    @BeforeEach
    void setUp() {
        cleanUp();
        Role drhRole = roleRepository.findByName(RoleType.ROLE_DRH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_DRH).build()));
        Role empRole = roleRepository.findByName(RoleType.ROLE_EMPLOYE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYE).build()));

        String suffix = UUID.randomUUID().toString().substring(0, 6);

        drhUser = userRepository.save(User.builder()
                .username("drh.train." + suffix)
                .email("drh.train." + suffix + "@careertrack.sn")
                .password("secret")
                .roles(Set.of(drhRole))
                .build());

        empUser = userRepository.save(User.builder()
                .username("emp.train." + suffix)
                .email("emp.train." + suffix + "@careertrack.sn")
                .password("secret")
                .roles(Set.of(empRole))
                .build());

        empEmployee = employeeRepository.save(Employee.builder()
                .user(empUser)
                .employeeNumber("EMP-TR-" + suffix)
                .firstName("Fatou")
                .lastName("Sall")
                .nationalId("NAT-TR-" + suffix)
                .email("emp.train." + suffix + "@careertrack.sn")
                .birthDate(LocalDate.of(1994, 6, 20))
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

        empToken = "Bearer " + jwtTokenProvider.generateAccessTokenFromPrincipal(
                UserPrincipal.builder()
                        .id(empUser.getId())
                        .username(empUser.getUsername())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE")))
                        .build()
        );

        testTraining = trainingRepository.save(Training.builder()
                .code("MGT-TEST-" + suffix)
                .title("Management Agile des Équipes")
                .durationHours(25)
                .provider("ISM Dakar")
                .category("Management")
                .description("Pilotage par les objectifs et rituels agiles")
                .build());
    }

    @Test
    @DisplayName("Consulter le catalogue de formation via GET /api/v1/trainings")
    void shouldGetTrainingsCatalogue() throws Exception {
        mockMvc.perform(get("/api/v1/trainings")
                        .header(HttpHeaders.AUTHORIZATION, empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Workflow complet de formation : planification de session, inscription, complétion et téléchargement d'attestation PDF")
    void shouldExecuteTrainingWorkflow() throws Exception {
        // 1. Planification d'une session par la DRH
        Instant start = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant end = start.plus(3, ChronoUnit.DAYS);

        TrainingSessionCreateDto sessionDto = TrainingSessionCreateDto.builder()
                .trainingId(testTraining.getId())
                .sessionCode("SESS-MGT-" + UUID.randomUUID().toString().substring(0, 4))
                .startDate(start)
                .endDate(end)
                .location("Dakar Plateau")
                .maxParticipants(10)
                .build();

        String sessionRes = mockMvc.perform(post("/api/v1/trainings/sessions")
                        .header(HttpHeaders.AUTHORIZATION, drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        UUID sessionId = UUID.fromString(objectMapper.readTree(sessionRes).path("data").path("id").asText());

        // 2. Inscription de l'employé connecté
        EnrollmentRequestDto enrollDto = EnrollmentRequestDto.builder()
                .sessionId(sessionId)
                .build();

        String enrollRes = mockMvc.perform(post("/api/v1/trainings/sessions/enroll")
                        .header(HttpHeaders.AUTHORIZATION, empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        UUID enrollmentId = UUID.fromString(objectMapper.readTree(enrollRes).path("data").path("id").asText());

        // 3. Complétion de la formation avec note par la DRH
        EnrollmentStatusUpdateDto updateDto = EnrollmentStatusUpdateDto.builder()
                .status(EnrollmentStatus.COMPLETED)
                .score(new BigDecimal("94.0"))
                .feedback("Assiduité exemplaire et mise en pratique réussie")
                .build();

        mockMvc.perform(patch("/api/v1/trainings/enrollments/" + enrollmentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, drhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.certificateUrl").isNotEmpty());

        // 4. Téléchargement de l'attestation officielle en PDF
        mockMvc.perform(get("/api/v1/trainings/enrollments/" + enrollmentId + "/certificate")
                        .header(HttpHeaders.AUTHORIZATION, empToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attestation_formation_" + enrollmentId + ".pdf\""));
    }
}
