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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Autowired
    private com.careertrack.modules.document.repository.EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private com.careertrack.modules.document.repository.EmployeeDocumentContentRepository employeeDocumentContentRepository;

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
        employeeDocumentContentRepository.deleteAll();
        employeeDocumentRepository.deleteAll();
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

    // =========================================================================
    // TESTS DU LOT 1 - ÉTAPE 2 : DOCUMENTS (UPLOAD, STOCKAGE, TÉLÉCHARGEMENT)
    // =========================================================================

    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 test document content for testing".getBytes();
    private static final byte[] VALID_PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};

    @Test
    @DisplayName("DRH peut téléverser un document pour un employé (201 Created + métadonnées sans octets)")
    void drhShouldUploadDocumentSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cni_officielle.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Carte Nationale d'Identité")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Carte Nationale d'Identité"))
                .andExpect(jsonPath("$.data.documentType").value("CNI"))
                .andExpect(jsonPath("$.data.fileName").value("cni_officielle.pdf"))
                .andExpect(jsonPath("$.data.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.data.downloadUrl").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Prouver que la réponse ne contient pas le contenu binaire
        assertThat(responseBody).doesNotContain("\"fileData\"");
        assertThat(responseBody).doesNotContain("\"data\":[");
    }

    @Test
    @DisplayName("Rejette le téléversement si le champ file est absent (400 Bad Request)")
    void shouldRejectUploadWithMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents")
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Titre sans fichier")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rejette le téléversement d'un fichier trop gros (> 5 Mo) (413 Payload Too Large ou 400)")
    void shouldRejectUploadWithFileSizeOver5Mb() throws Exception {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1024]; // > 5 Mo
        System.arraycopy(VALID_PDF_BYTES, 0, largeBytes, 0, VALID_PDF_BYTES.length);
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "grand_fichier.pdf", "application/pdf", largeBytes);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(largeFile)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "AUTRE")
                        .param("title", "Gros document")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(413, 400)));
    }

    @Test
    @DisplayName("Rejette un faux PDF qui est du texte brut (400 Bad Request)")
    void shouldRejectUploadWithFakePdf() throws Exception {
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file", "faux.pdf", "application/pdf", "Ceci est du texte pur et non un PDF".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(fakeFile)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Faux PDF")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Format de fichier non supporté")));
    }

    @Test
    @DisplayName("Rejette un PNG renommé en .pdf (incohérence signature/extension) (400 Bad Request)")
    void shouldRejectUploadWithPngRenamedToPdf() throws Exception {
        MockMultipartFile evilFile = new MockMultipartFile(
                "file", "image_renommee.pdf", "application/pdf", VALID_PNG_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(evilFile)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Image PNG déguisée en PDF")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ne correspond pas à son format réel (PNG détecté)")));
    }

    @Test
    @DisplayName("Employé peut téléverser un DIPLOME ou une CNI pour lui-même (201 Created)")
    void employeeShouldUploadForHimself() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "diplome_master.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "DIPLOME")
                        .param("title", "Diplôme de Master 2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.documentType").value("DIPLOME"));
    }

    @Test
    @DisplayName("Employé NE PEUT PAS téléverser de type CONTRAT (réservé à la DRH) (403 Forbidden)")
    void employeeShouldNotUploadContract() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "mon_contrat.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CONTRAT")
                        .param("title", "Tentative de contrat par employé")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employé NE PEUT PAS téléverser un document pour un autre employé (403 Forbidden)")
    void employeeShouldNotUploadForOtherEmployee() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cni.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee2.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Tentative cross-employee")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employé peut lister ses propres documents sans contenu binaire (200 OK)")
    void employeeShouldListHisOwnDocumentsWithoutBinaryContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cni.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Ma CNI")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/documents/employee/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Ma CNI"))
                .andExpect(jsonPath("$.data[0].downloadUrl").exists())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        // Preuve : aucun octet ni champ binaire dans la liste
        assertThat(json).doesNotContain("\"fileData\"");
        assertThat(json).contains("\"title\":\"Ma CNI\"");
        assertThat(json).contains("\"downloadUrl\"");
    }

    @Test
    @DisplayName("Employé NE PEUT PAS lister les documents d'un autre employé (403 Forbidden)")
    void employeeShouldNotListOtherEmployeeDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents/employee/" + employee2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager NE PEUT PAS lister les documents de ses subordonnés (403 Forbidden)")
    void managerShouldNotListEmployeeDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents/employee/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Direction NE PEUT PAS lister les documents individuels (403 Forbidden)")
    void directionShouldNotListEmployeeDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents/employee/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directionToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employé peut télécharger son propre document avec en-têtes sécurisés (200 OK + nosniff)")
    void employeeShouldDownloadHisOwnDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "Diplôme_Licence.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "DIPLOME")
                        .param("title", "Diplôme Licence Pro")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isCreated())
                .andReturn();

        String downloadUrl = com.jayway.jsonpath.JsonPath.read(uploadRes.getResponse().getContentAsString(), "$.data.downloadUrl");

        mockMvc.perform(get(downloadUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("filename*="),
                        org.hamcrest.Matchers.containsString("Dipl%C3%B4me_Licence.pdf")
                )))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Employé NE PEUT PAS télécharger le document d'un autre (403 Forbidden)")
    void employeeShouldNotDownloadOtherEmployeeDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cni_emp1.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "CNI confidentielle")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isCreated())
                .andReturn();

        String downloadUrl = com.jayway.jsonpath.JsonPath.read(uploadRes.getResponse().getContentAsString(), "$.data.downloadUrl");

        mockMvc.perform(get(downloadUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Téléchargement d'un document inexistant retourne 404 Not Found")
    void shouldReturn404WhenDownloadingNonExistentDocument() throws Exception {
        mockMvc.perform(get("/api/v1/documents/" + UUID.randomUUID() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Identifiant invalide (non-UUID) retourne 400 Bad Request")
    void shouldReturn400WhenInvalidUuidPassed() throws Exception {
        mockMvc.perform(get("/api/v1/documents/not-a-valid-uuid/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Employé peut supprimer un document qu'il a lui-même téléversé (204 No Content)")
    void employeeShouldDeleteHisOwnUploadedDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "mon_attestation.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "AUTRE")
                        .param("title", "Autre pièce personnelle")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = com.jayway.jsonpath.JsonPath.read(uploadRes.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isNoContent());

        // Le téléchargement doit désormais renvoyer 404
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Employé NE PEUT PAS supprimer un document déposé par la DRH (403 Forbidden)")
    void employeeShouldNotDeleteDocumentUploadedByDrh() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat_drh.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CONTRAT")
                        .param("title", "Contrat de travail déposé par la DRH")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = com.jayway.jsonpath.JsonPath.read(uploadRes.getResponse().getContentAsString(), "$.data.id");

        // L'employé essaie de supprimer le document déposé par la DRH -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Téléversement pour un employé inexistant retourne 404 Not Found")
    void shouldRejectUploadForNonExistentEmployee() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cni.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", UUID.randomUUID().toString())
                        .param("type", "CNI")
                        .param("title", "CNI employé inconnu")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Vérification stricte : la liste des documents ne contient AUCUN champ binaire ou data dans DocumentResponseDto")
    void testEmployeeDocumentsList_ContainsNoBinaryOrDataFieldInDocumentDto() throws Exception {
        // 1. Vérification par réflexion sur la classe DocumentResponseDto
        java.lang.reflect.Field[] fields = com.careertrack.modules.document.dto.DocumentResponseDto.class.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            assertThat(field.getName())
                    .as("Le DTO ne doit déclarer aucun champ nommé 'data', 'bytes', ou 'content'")
                    .isNotIn("data", "bytes", "content", "fileData", "rawBytes");
            assertThat(field.getType())
                    .as("Le DTO ne doit contenir aucun type tableau d'octets")
                    .isNotEqualTo(byte[].class);
        }

        // 2. Téléverser un document pour l'employé 1
        MockMultipartFile file = new MockMultipartFile(
                "file", "cni_sans_binaire.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Pièce d'identité sans binaire")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isCreated());

        // 3. Appel de l'endpoint GET /documents/employee/{id}
        MvcResult listResult = mockMvc.perform(get("/api/v1/documents/employee/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employee1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String responseBody = listResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
        com.fasterxml.jackson.databind.JsonNode dataArray = root.get("data");

        assertThat(dataArray.isArray()).isTrue();
        assertThat(dataArray.size()).isGreaterThanOrEqualTo(1);

        for (com.fasterxml.jackson.databind.JsonNode docNode : dataArray) {
            // Champs attendus
            assertThat(docNode.has("id")).isTrue();
            assertThat(docNode.has("employeeId")).isTrue();
            assertThat(docNode.has("documentType")).isTrue();
            assertThat(docNode.has("title")).isTrue();
            assertThat(docNode.has("fileName")).isTrue();
            assertThat(docNode.has("fileSize")).isTrue();
            assertThat(docNode.has("mimeType")).isTrue();
            assertThat(docNode.has("uploadedAt")).isTrue();
            assertThat(docNode.has("downloadUrl")).isTrue();

            // Absence totale de données binaires ou champ data interne
            assertThat(docNode.has("data")).as("L'objet document dans la liste ne doit pas avoir de champ data").isFalse();
            assertThat(docNode.has("bytes")).as("L'objet document ne doit pas avoir de champ bytes").isFalse();
            assertThat(docNode.has("content")).as("L'objet document ne doit pas avoir de champ content").isFalse();
            assertThat(docNode.has("fileData")).as("L'objet document ne doit pas avoir de champ fileData").isFalse();
        }
    }

    @Test
    @DisplayName("Manager : peut lister ses propres documents (200 OK) mais 403 sur subordonnés")
    void manager_canListOwnDocuments_andForbiddenOnSubordinate() throws Exception {
        // 1. Manager liste ses propres documents -> 200 OK
        mockMvc.perform(get("/api/v1/documents/employee/" + managerEmployee.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Manager tente de lister les documents de son subordonné (employee1) -> 403 Forbidden
        mockMvc.perform(get("/api/v1/documents/employee/" + employee1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager : peut téléverser CNI/DIPLOME/AUTRE pour lui-même (201 Created)")
    void manager_canUploadOwnDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "diplome_manager.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", managerEmployee.getId().toString())
                        .param("type", "DIPLOME")
                        .param("title", "Diplôme d'ingénieur Manager")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeId").value(managerEmployee.getId().toString()))
                .andExpect(jsonPath("$.data.documentType").value("DIPLOME"));
    }

    @Test
    @DisplayName("Manager : NE PEUT PAS téléverser pour son subordonné (403 Forbidden)")
    void manager_cannotUploadSubordinateDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cni_subordonne.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Tentative d'envoi pour subordonné")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager : NE PEUT PAS téléverser de contrat pour lui-même (403 Forbidden)")
    void manager_cannotUploadContractForSelf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat_manager.pdf", "application/pdf", VALID_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("employeeId", managerEmployee.getId().toString())
                        .param("type", "CONTRAT")
                        .param("title", "Tentative dépôt contrat par manager")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager : peut télécharger son propre document (200 OK) mais 403 sur subordonné")
    void manager_canDownloadOwnDocument_andForbiddenOnSubordinate() throws Exception {
        // 1. Manager dépose un document
        MockMultipartFile fileMgr = new MockMultipartFile(
                "file", "cni_manager.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(fileMgr)
                        .param("employeeId", managerEmployee.getId().toString())
                        .param("type", "CNI")
                        .param("title", "CNI Manager Sow")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isCreated())
                .andReturn();

        String mgrDocId = com.jayway.jsonpath.JsonPath.read(uploadRes.getResponse().getContentAsString(), "$.data.id");

        // 2. Manager télécharge son propre document -> 200 OK
        mockMvc.perform(get("/api/v1/documents/" + mgrDocId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk());

        // 3. DRH dépose un document pour le subordonné (employee1)
        MockMultipartFile fileSub = new MockMultipartFile(
                "file", "diplome_subordonne.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadSubRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(fileSub)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "DIPLOME")
                        .param("title", "Diplôme sous-fifre")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isCreated())
                .andReturn();

        String subDocId = com.jayway.jsonpath.JsonPath.read(uploadSubRes.getResponse().getContentAsString(), "$.data.id");

        // 4. Manager essaie de télécharger le document de son subordonné -> 403 Forbidden
        mockMvc.perform(get("/api/v1/documents/" + subDocId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager : peut supprimer son propre document déposé (204 No Content) mais 403 sur subordonné")
    void manager_canDeleteOwnDocument_andForbiddenOnSubordinate() throws Exception {
        // 1. Manager dépose un document
        MockMultipartFile fileMgr = new MockMultipartFile(
                "file", "cni_manager_del.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(fileMgr)
                        .param("employeeId", managerEmployee.getId().toString())
                        .param("type", "AUTRE")
                        .param("title", "Document temporaire manager")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isCreated())
                .andReturn();

        String mgrDocId = com.jayway.jsonpath.JsonPath.read(uploadRes.getResponse().getContentAsString(), "$.data.id");

        // 2. DRH dépose un document pour subordonné
        MockMultipartFile fileSub = new MockMultipartFile(
                "file", "cni_sub.pdf", "application/pdf", VALID_PDF_BYTES);

        MvcResult uploadSubRes = mockMvc.perform(multipart("/api/v1/documents")
                        .file(fileSub)
                        .param("employeeId", employee1.getId().toString())
                        .param("type", "CNI")
                        .param("title", "Pièce subordonné")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + drhToken))
                .andExpect(status().isCreated())
                .andReturn();

        String subDocId = com.jayway.jsonpath.JsonPath.read(uploadSubRes.getResponse().getContentAsString(), "$.data.id");

        // 3. Manager essaie de supprimer le document du subordonné -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/documents/" + subDocId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // 4. Manager supprime son propre document déposé -> 204 No Content
        mockMvc.perform(delete("/api/v1/documents/" + mgrDocId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isNoContent());
    }
}

