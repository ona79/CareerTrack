package com.careertrack.common.pdf;

import com.careertrack.common.pdf.template.AnnualEvaluationPdfData;
import com.careertrack.common.pdf.template.TrainingCertificatePdfData;
import com.careertrack.modules.employee.entity.*;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.setting.entity.CompanySetting;
import com.careertrack.modules.setting.repository.CompanySettingRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PdfGenerationTest {

    private CompanySettingRepository companySettingRepository;
    private PdfGeneratorService pdfGeneratorService;

    @BeforeEach
    void setUp() {
        companySettingRepository = Mockito.mock(CompanySettingRepository.class);
        when(companySettingRepository.findBySettingKey("COMPANY_NAME"))
                .thenReturn(Optional.of(CompanySetting.builder().settingKey("COMPANY_NAME").settingValue("Teranga Tech PME SARL").build()));
        when(companySettingRepository.findBySettingKey("COMPANY_CITY"))
                .thenReturn(Optional.of(CompanySetting.builder().settingKey("COMPANY_CITY").settingValue("Dakar").build()));
        when(companySettingRepository.findBySettingKey("COMPANY_NINEA"))
                .thenReturn(Optional.of(CompanySetting.builder().settingKey("COMPANY_NINEA").settingValue("DEMO-000000000").build()));
        when(companySettingRepository.findBySettingKey("COMPANY_RCCM"))
                .thenReturn(Optional.of(CompanySetting.builder().settingKey("COMPANY_RCCM").settingValue("SN.DKR.DEMO.0000").build()));
        when(companySettingRepository.findBySettingKey("LEAVE_DAYS_PER_MONTH"))
                .thenReturn(Optional.of(CompanySetting.builder().settingKey("LEAVE_DAYS_PER_MONTH").settingValue("2.0").build()));

        pdfGeneratorService = new PdfGeneratorService(companySettingRepository);
    }

    private String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    @Test
    @DisplayName("Génère une attestation de travail valide au format PDF avec iText 7 et accents français")
    void shouldGenerateWorkCertificatePdf() throws IOException {
        Department dept = Department.builder()
                .name("Ingénierie & Systèmes")
                .code("DEP-TECH")
                .build();

        Position pos = Position.builder()
                .title("Développeur Fullstack Senior")
                .code("POS-DEV")
                .department(dept)
                .build();

        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .hireDate(LocalDate.of(2022, 6, 1))
                .department(dept)
                .position(pos)
                .build();

        Contract contract = Contract.builder()
                .contractType(ContractType.CDI)
                .grossSalary(new BigDecimal("950000"))
                .startDate(LocalDate.of(2022, 6, 1))
                .build();

        byte[] pdfBytes = pdfGeneratorService.generateWorkCertificate(emp, contract);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        String text = extractText(pdfBytes);
        assertThat(text).contains("TERANGA TECH PME SARL");
        assertThat(text).contains("Nous soussignés, la Direction des Ressources Humaines de la société");
        assertThat(text).contains("certifions que :");
        assertThat(text).contains("Ousmane Diallo");
        assertThat(text).contains("EMP-004");
        assertThat(text).contains("depuis le 01/06/2022");
        assertThat(text).contains("Développeur Fullstack");
        assertThat(text).contains("Fait à Dakar");
        assertThat(text).contains("Page 1 / 1");
        // CNI absente par défaut, plus de bloc "Le Collaborateur"
        assertThat(text).doesNotContain("1 996 1996 00321");
        assertThat(text).doesNotContain("CNI");
        assertThat(text).doesNotContain("Le Collaborateur");
        assertThat(text).doesNotContain("Lu et approuvé");

        // Option explicite : CNI affichée
        String withId = extractText(pdfGeneratorService.generateWorkCertificate(emp, contract, true));
        assertThat(withId).contains("1 996 1996 00321");
    }

    @Test
    @DisplayName("Attestation d'un employé sorti : 'a été employé(e) du <début> au <fin>'")
    void shouldGenerateWorkCertificateForFormerEmployee() throws IOException {
        Employee emp = Employee.builder()
                .employeeNumber("EMP-010")
                .firstName("Awa")
                .lastName("Sarr")
                .hireDate(LocalDate.of(2022, 6, 1))
                .status(EmployeeStatus.TERMINATED)
                .build();
        Contract cdd = Contract.builder()
                .contractType(ContractType.CDD)
                .startDate(LocalDate.of(2022, 6, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .build();

        String text = extractText(pdfGeneratorService.generateWorkCertificate(emp, cdd));
        assertThat(text).contains("a été employé(e) au sein de notre entreprise du 01/06/2022 au 31/12/2025");
        assertThat(text).doesNotContain("depuis le");

        // Statut encore ACTIF mais date de fin de contrat dépassée : même formulation
        Employee stillActive = Employee.builder().employeeNumber("EMP-011").firstName("Ibou").lastName("Faye")
                .hireDate(LocalDate.of(2023, 1, 2)).build();
        Contract expired = Contract.builder().contractType(ContractType.CDD)
                .startDate(LocalDate.of(2023, 1, 2)).endDate(LocalDate.now().minusDays(1)).build();
        assertThat(extractText(pdfGeneratorService.generateWorkCertificate(stillActive, expired)))
                .contains("a été employé(e) au sein de notre entreprise du 02/01/2023 au");

        // Licencié sans date de fin connue : pas de date inventée
        Employee noEnd = Employee.builder().employeeNumber("EMP-012").firstName("Fatou").lastName("Kane")
                .hireDate(LocalDate.of(2021, 3, 1)).status(EmployeeStatus.TERMINATED).build();
        assertThat(extractText(pdfGeneratorService.generateWorkCertificate(noEnd, null)))
                .contains("a été employé(e) au sein de notre entreprise à compter du 01/03/2021");
    }

    @Test
    @DisplayName("Génère un bulletin de paie valide avec cotisations sociales sénégalaises en FCFA")
    void shouldGeneratePayslipPdf() throws IOException {
        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .hireDate(LocalDate.of(2022, 6, 1))
                .build();

        Payslip payslip = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(emp)
                .periodMonth(9)
                .periodYear(2026)
                .paymentDate(LocalDate.of(2026, 9, 30))
                .status(PayslipStatus.GENERATED)
                .grossSalary(new BigDecimal("950000"))
                .netSalary(new BigDecimal("786800"))
                .totalDeductions(new BigDecimal("163200"))
                .lines(List.of(
                        PayslipLine.builder().label("Salaire de base").baseAmount(new BigDecimal("950000")).rate(BigDecimal.ONE).amount(new BigDecimal("950000")).lineType(PayslipLineType.BASE_SALARY).build(),
                        PayslipLine.builder().label("IPRES Salariale").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.056")).amount(new BigDecimal("53200")).lineType(PayslipLineType.IPRES_EMPLOYEE).build(),
                        PayslipLine.builder().label("Impôt sur le Revenu (IR)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.1008")).amount(new BigDecimal("95750")).lineType(PayslipLineType.TAX_IR).build()
                ))
                .build();

        byte[] pdfBytes = pdfGeneratorService.generatePayslipPdf(payslip);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        String text = extractText(pdfBytes);
        assertThat(text).contains("BULLETIN DE PAIE — PÉRIODE : 09/2026");
        assertThat(text).contains("Ousmane Diallo");
        assertThat(text).contains("EMP-004");
        // Montants exacts : séparateur U+00A0 et espace insécable avant FCFA
        assertThat(text).contains("950\u00A0000\u00A0FCFA");
        assertThat(text).contains("786\u00A0800\u00A0FCFA");
        assertThat(text).contains("163\u00A0200\u00A0FCFA");
        // Taux à la française, pas de taux sur le salaire de base
        assertThat(text).contains("5,60\u00A0%");
        assertThat(text).contains("10,08\u00A0%");
        assertThat(text).doesNotContain("100,00");
        assertThat(text).doesNotContain("5.60");
        // Statut traduit
        assertThat(text).contains("Statut : Généré");
        assertThat(text).doesNotContain("GENERATED");
        // Lignes enregistrées et colonnes salariales / patronales étape 4
        assertThat(text).doesNotContain("IPM");
        assertThat(text).contains("Part Patronale");
        assertThat(text).contains("TOTAL COTISATIONS PATRONALES");
        assertThat(text).contains("COÛT GLOBAL EMPLOYEUR");
        assertThat(text).contains("Modèle de démonstration, taux à valider");
        assertThat(text).contains("Page 1 / 1");
    }

    @Test
    @DisplayName("Génère un contrat de travail (modèle de démonstration) au format PDF")
    void shouldGenerateContractPdf() throws IOException {
        Department dept = Department.builder()
                .name("Ingénierie & Systèmes")
                .code("DEP-TECH")
                .build();

        Position pos = Position.builder()
                .title("Développeur Fullstack Senior")
                .code("POS-DEV-SR")
                .department(dept)
                .build();

        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .address("Liberté 6, Dakar")
                .hireDate(LocalDate.of(2022, 6, 1))
                .department(dept)
                .position(pos)
                .build();

        Contract contract = Contract.builder()
                .contractType(ContractType.CDI)
                .grossSalary(new BigDecimal("950000"))
                .startDate(LocalDate.of(2022, 6, 1))
                .build();

        byte[] pdfBytes = pdfGeneratorService.generateEmploymentContract(emp, contract);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        String text = extractText(pdfBytes);
        assertThat(text).contains("CONTRAT DE TRAVAIL (CDI)");
        assertThat(text).contains("Ousmane Diallo");
        assertThat(text).contains("1 996 1996 00321");
        // Montant jamais coupé : aucune fin de ligne entre le nombre et FCFA
        assertThat(text).contains("950\u00A0000\u00A0FCFA");
        assertThat(text).contains("soumis aux cotisations sociales et fiscales en vigueur");
        assertThat(text).doesNotContain("IPRES");
        assertThat(text).doesNotContain("CSS");
        assertThat(text).contains("2 jours ouvrables");
        assertThat(text).doesNotContain("2.0");
        assertThat(text).contains("selon le paramétrage");
        assertThat(text).contains("en vigueur dans l'entreprise");
        assertThat(text).contains("Modèle de démonstration, à faire valider par un juriste");
    }

    @Test
    @DisplayName("Paramètres d'entreprise absents : aucune valeur inventée (ville, NINEA, RCCM, congés)")
    void shouldNotInventMissingCompanySettings() throws IOException {
        CompanySettingRepository emptyRepo = Mockito.mock(CompanySettingRepository.class);
        when(emptyRepo.findBySettingKey(Mockito.anyString())).thenReturn(Optional.empty());
        PdfGeneratorService service = new PdfGeneratorService(emptyRepo);

        Employee emp = Employee.builder().employeeNumber("EMP-020").firstName("Modou").lastName("Ndiaye")
                .hireDate(LocalDate.of(2024, 2, 1)).build();
        Contract c = Contract.builder().contractType(ContractType.CDI).startDate(LocalDate.of(2024, 2, 1))
                .grossSalary(new BigDecimal("450000")).build();

        String cert = extractText(service.generateWorkCertificate(emp, c));
        assertThat(cert).contains("Teranga Tech PME SARL");
        assertThat(cert).doesNotContain("Siège");
        assertThat(cert).doesNotContain("NINEA");
        assertThat(cert).doesNotContain("R.C.C.M");
        assertThat(cert).doesNotContain("Fait à");
        assertThat(cert).contains("Fait le ");

        String contract = extractText(service.generateEmploymentContract(emp, c));
        assertThat(contract).contains("450\u00A0000\u00A0FCFA");
        assertThat(contract).doesNotContain("Article 4");
    }

    @Test
    @DisplayName("En profil prod : lève une BadRequestException claire si COMPANY_NAME n'est pas configuré")
    void shouldThrowBadRequestInProdIfCompanyNameMissing() {
        CompanySettingRepository emptyRepo = Mockito.mock(CompanySettingRepository.class);
        when(emptyRepo.findBySettingKey(Mockito.anyString())).thenReturn(Optional.empty());
        org.springframework.core.env.Environment prodEnv = Mockito.mock(org.springframework.core.env.Environment.class);
        when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"prod"});

        PdfGeneratorService prodService = new PdfGeneratorService(emptyRepo, prodEnv);

        Employee emp = Employee.builder().employeeNumber("EMP-020").firstName("Modou").lastName("Ndiaye")
                .hireDate(LocalDate.of(2024, 2, 1)).build();
        Contract c = Contract.builder().contractType(ContractType.CDI).startDate(LocalDate.of(2024, 2, 1)).build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> prodService.generateWorkCertificate(emp, c))
                .isInstanceOf(com.careertrack.common.exception.BadRequestException.class)
                .hasMessageContaining("Les informations de l'entreprise (raison sociale) doivent être renseignées dans les paramètres");
    }

    @Test
    @DisplayName("En profil prod : génère avec la raison sociale configurée et sans nom/ville inventés si le reste est vide")
    void shouldGenerateInProdWithConfiguredNameAndNoInventedFields() throws IOException {
        CompanySettingRepository repo = Mockito.mock(CompanySettingRepository.class);
        when(repo.findBySettingKey("COMPANY_NAME"))
                .thenReturn(Optional.of(CompanySetting.builder().settingKey("COMPANY_NAME").settingValue("Cabinet Sénégal Conseil SARL").build()));
        when(repo.findBySettingKey(Mockito.argThat(k -> !"COMPANY_NAME".equals(k)))).thenReturn(Optional.empty());

        org.springframework.core.env.Environment prodEnv = Mockito.mock(org.springframework.core.env.Environment.class);
        when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"prod"});

        PdfGeneratorService prodService = new PdfGeneratorService(repo, prodEnv);

        Employee emp = Employee.builder().employeeNumber("EMP-021").firstName("Fatou").lastName("Diop")
                .hireDate(LocalDate.of(2024, 3, 1)).build();
        Contract c = Contract.builder().contractType(ContractType.CDI).startDate(LocalDate.of(2024, 3, 1))
                .grossSalary(new BigDecimal("500000")).build();

        String cert = extractText(prodService.generateWorkCertificate(emp, c));
        assertThat(cert).contains("CABINET SÉNÉGAL CONSEIL SARL");
        assertThat(cert).doesNotContain("Teranga");
        assertThat(cert).doesNotContain("Siège");
        assertThat(cert).doesNotContain("NINEA");
        assertThat(cert).doesNotContain("R.C.C.M");
        assertThat(cert).doesNotContain("Fait à");
        assertThat(cert).contains("Fait le ");
    }

    @Test
    @DisplayName("Génère un PDF valide avec des champs nuls (robustesse)")
    void shouldHandleNullFieldsGracefully() throws IOException {
        // Employé sans département, sans poste, sans adresse
        Employee empBare = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-999")
                .firstName("Moussa")
                .lastName("Ba")
                .build();

        // Contrat sans salaire et sans date
        Contract contractBare = Contract.builder().build();

        byte[] certPdf = pdfGeneratorService.generateWorkCertificate(empBare, contractBare);
        assertThat(certPdf).isNotEmpty();
        assertThat(extractText(certPdf)).contains("Moussa Ba");

        byte[] contractPdf = pdfGeneratorService.generateEmploymentContract(empBare, contractBare);
        assertThat(contractPdf).isNotEmpty();
        assertThat(extractText(contractPdf)).contains("Moussa Ba");

        // Payslip sans lignes et montants nuls
        Payslip payslipBare = Payslip.builder()
                .employee(empBare)
                .periodMonth(1)
                .periodYear(2026)
                .build();

        byte[] payslipPdf = pdfGeneratorService.generatePayslipPdf(payslipBare);
        assertThat(payslipPdf).isNotEmpty();
        assertThat(extractText(payslipPdf)).contains("Moussa Ba");
    }

    @Test
    @DisplayName("Génère le gabarit d'évaluation annuelle (Lot 4) avec grille SMART et signatures")
    void shouldGenerateAnnualEvaluationTemplate() throws IOException {
        AnnualEvaluationPdfData data = new AnnualEvaluationPdfData(
                "Campagne Annuelle 2026",
                "Ousmane Diallo",
                "EMP-004",
                "Développeur Fullstack Senior",
                "Ingénierie & Systèmes",
                "Mamadou Sow (Engineering Manager)",
                LocalDate.of(2026, 6, 15),
                new BigDecimal("4.6"),
                "Excellente progression technique sur le socle Spring Boot 3 et la sécurité.",
                List.of(
                        new AnnualEvaluationPdfData.ObjectiveItem("Livraison API Documents", "Attestation, Paie, Contrat en PDF", 40, 100, "100% conforme cahier des charges"),
                        new AnnualEvaluationPdfData.ObjectiveItem("Qualité logicielle & Tests", "Tests unitaires et intégration", 30, 95, "Couverture > 60%"),
                        new AnnualEvaluationPdfData.ObjectiveItem("Mentorat juniors", "Ateliers hebdomadaires", 30, 90, "2 sessions / mois")
                )
        );

        byte[] pdfBytes = pdfGeneratorService.generateAnnualEvaluationPdf(data);
        assertThat(pdfBytes).isNotEmpty();

        String text = extractText(pdfBytes);
        assertThat(text).contains("SYNTHÈSE D'ENTRETIEN ANNUEL D'ÉVALUATION");
        assertThat(text).contains("Ousmane Diallo");
        assertThat(text).contains("Mamadou Sow");
        assertThat(text).contains("4,6 / 5");
        assertThat(text).contains("Livraison API Documents");
        assertThat(text).contains("Le Salarié Évalué");
    }

    @Test
    @DisplayName("Génère le gabarit d'attestation de formation (Lot 4)")
    void shouldGenerateTrainingCertificateTemplate() throws IOException {
        TrainingCertificatePdfData data = new TrainingCertificatePdfData(
                "CERT-2026-089",
                "Aminata Ndiaye",
                "EMP-002",
                "DRH",
                "Gestion Avancée de la Paie Sénégalaise & Déclarations IPRES/CSS",
                "Cabinet RH Dakar Conseil",
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 6),
                35,
                "Maîtrise du calcul brut/net, barèmes IR et déclarations sociales en ligne."
        );

        byte[] pdfBytes = pdfGeneratorService.generateTrainingCertificatePdf(data);
        assertThat(pdfBytes).isNotEmpty();

        String text = extractText(pdfBytes);
        assertThat(text).contains("ATTESTATION DE SUIVI DE FORMATION");
        assertThat(text).contains("Aminata Ndiaye");
        assertThat(text).contains("CERT-2026-089");
        assertThat(text).contains("35 heures de formation");
        assertThat(text).contains("Cabinet RH Dakar Conseil");
    }

    @Test
    @DisplayName("Génère un export tabulaire générique sans colonnes sensibles (Lot 2 - Registre)")
    void shouldGenerateTabularReportTemplate() throws IOException {
        List<String> headers = List.of("Matricule", "Nom Complet", "Département", "Poste", "Statut", "Date Embauche");
        List<List<String>> rows = List.of(
                List.of("EMP-001", "Cheikh Fall", "Direction Générale", "Directeur Général", "ACTIF", "01/01/2018"),
                List.of("EMP-002", "Aminata Ndiaye", "Ressources Humaines", "Directeur des RH", "ACTIF", "15/03/2019"),
                List.of("EMP-003", "Mamadou Sow", "Ingénierie & Systèmes", "Engineering Manager", "ACTIF", "01/02/2020"),
                List.of("EMP-004", "Ousmane Diallo", "Ingénierie & Systèmes", "Développeur Fullstack Senior", "ACTIF", "01/06/2022")
        );

        byte[] pdfBytes = pdfGeneratorService.generateTabularReportPdf(
                "REGISTRE UNIQUE DU PERSONNEL",
                "Situation officielle des effectifs au 18 octobre 2026",
                headers,
                rows
        );

        assertThat(pdfBytes).isNotEmpty();
        String text = extractText(pdfBytes);
        assertThat(text).contains("REGISTRE UNIQUE DU PERSONNEL");
        assertThat(text).contains("Ousmane Diallo");
        assertThat(text).contains("Engineering Manager");
        // Absence absolue de données sensibles
        assertThat(text).doesNotContain("950 000");
        assertThat(text).doesNotContain("1 996 1996 00321");
    }

    @Test
    @DisplayName("Gère le multi-pages et les retours à la ligne sans débordement sur texte long")
    void shouldHandleMultiPageLongDocument() throws IOException {
        List<String> headers = List.of("N°", "Intitulé", "Catégorie", "Commentaire");
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            rows.add(List.of(
                    String.valueOf(i),
                    "Article de référence " + i,
                    "Catégorie Standard",
                    "Description étendue avec du texte volumineux pour tester la pagination automatique et le retour à la ligne sur plusieurs pages A4."
            ));
        }

        byte[] pdfBytes = pdfGeneratorService.generateTabularReportPdf("RAPPORT MULTI-PAGES", "Test de pagination", headers, rows);
        assertThat(pdfBytes).isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int total = doc.getNumberOfPages();
            assertThat(total).isGreaterThan(1);

            // Chaque page porte "Page i / total"
            for (int i = 1; i <= total; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                assertThat(stripper.getText(doc)).contains("Page " + i + " / " + total);
            }
        }
    }

    @Test
    @DisplayName("Génère les 3 PDF échantillons dans target/pdf-samples/ pour preuve visuelle")
    void generateSamplePdfsForVisualProof() throws IOException {
        java.nio.file.Path samplesDir = java.nio.file.Paths.get("target", "pdf-samples");
        java.nio.file.Files.createDirectories(samplesDir);

        Department dept = Department.builder().name("Ingénierie & Systèmes").code("DEP-TECH").build();
        Position pos = Position.builder().title("Développeur Fullstack Senior").code("POS-DEV").department(dept).build();
        Employee emp = Employee.builder()
                .id(UUID.randomUUID())
                .employeeNumber("EMP-004")
                .firstName("Ousmane")
                .lastName("Diallo")
                .nationalId("1 996 1996 00321")
                .address("Liberté 6, Dakar")
                .hireDate(LocalDate.of(2022, 6, 1))
                .department(dept)
                .position(pos)
                .build();

        Contract contract = Contract.builder()
                .contractType(ContractType.CDI)
                .grossSalary(new BigDecimal("950000"))
                .startDate(LocalDate.of(2022, 6, 1))
                .build();

        Payslip payslip = Payslip.builder()
                .id(UUID.randomUUID())
                .employee(emp)
                .periodMonth(9)
                .periodYear(2026)
                .paymentDate(LocalDate.of(2026, 9, 30))
                .status(PayslipStatus.GENERATED)
                .grossSalary(new BigDecimal("950000"))
                .netSalary(new BigDecimal("786800"))
                .totalDeductions(new BigDecimal("163200"))
                .lines(List.of(
                        PayslipLine.builder().label("Salaire de base").baseAmount(new BigDecimal("950000")).rate(BigDecimal.ONE).amount(new BigDecimal("950000")).lineType(PayslipLineType.BASE_SALARY).build(),
                        PayslipLine.builder().label("IPRES Salariale").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.056")).amount(new BigDecimal("53200")).lineType(PayslipLineType.IPRES_EMPLOYEE).build(),
                        PayslipLine.builder().label("Cotisation Mutuelle Santé (IPM)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.015")).amount(new BigDecimal("14250")).lineType(PayslipLineType.IPM).build(),
                        PayslipLine.builder().label("Impôt sur le Revenu (IR)").baseAmount(new BigDecimal("950000")).rate(new BigDecimal("0.1008")).amount(new BigDecimal("95750")).lineType(PayslipLineType.TAX_IR).build()
                ))
                .build();

        byte[] certPdf = pdfGeneratorService.generateWorkCertificate(emp, contract);
        byte[] contractPdf = pdfGeneratorService.generateEmploymentContract(emp, contract);
        byte[] payslipPdf = pdfGeneratorService.generatePayslipPdf(payslip);

        java.nio.file.Files.write(samplesDir.resolve("attestation_travail_sample.pdf"), certPdf);
        java.nio.file.Files.write(samplesDir.resolve("contrat_travail_sample.pdf"), contractPdf);
        java.nio.file.Files.write(samplesDir.resolve("fiche_paie_sample.pdf"), payslipPdf);
    }
}
