package com.careertrack.common.pdf;

import com.careertrack.common.pdf.template.AnnualEvaluationPdfData;
import com.careertrack.common.pdf.template.TrainingCertificatePdfData;
import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeStatus;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.careertrack.modules.payroll.entity.PayslipLineType;
import com.careertrack.modules.payroll.entity.PayslipStatus;
import com.careertrack.modules.setting.repository.CompanySettingRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service centralisé de génération de documents PDF officiels pour CareerTrack.
 * Respecte strictement la charte visuelle, l'encodage français, les réglages
 * d'entreprise dynamiques et la tolérance aux valeurs nulles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final CompanySettingRepository companySettingRepository;

    public PdfGeneratorService() {
        this.companySettingRepository = null;
    }

    @Value("${careertrack.company-name:Entreprise de démonstration (fictive)}")
    private String defaultCompanyName = "Entreprise de démonstration (fictive)";

    private String getSetting(String key, String fallback) {
        if (companySettingRepository == null) return fallback;
        try {
            return companySettingRepository.findBySettingKey(key)
                    .map(s -> s.getSettingValue() != null && !s.getSettingValue().isBlank() ? s.getSettingValue() : fallback)
                    .orElse(fallback);
        } catch (Exception e) {
            log.warn("Impossible de lire le paramètre {}: {}", key, e.getMessage());
            return fallback;
        }
    }

    private String getCompanyName() {
        // Repli clairement fictif, réservé au dev / à la démo (COMPANY_NAME absent).
        return getSetting("COMPANY_NAME", defaultCompanyName);
    }

    private String getCompanyCity() {
        // Pas de ville inventée : absente => "Fait le ..." et pas de ligne "Siège".
        return getSetting("COMPANY_CITY", null);
    }

    private String getCompanyNinea() {
        return getSetting("COMPANY_NINEA", null);
    }

    private String getCompanyRccm() {
        return getSetting("COMPANY_RCCM", null);
    }

    /**
     * 1. Attestation de travail officielle (sans numéro de CNI par défaut).
     */
    public byte[] generateWorkCertificate(Employee employee, Contract contract) {
        return generateWorkCertificate(employee, contract, false);
    }

    /**
     * Attestation de travail avec option d'affichage du numéro de CNI (désactivée par défaut :
     * le PDF ne contient que les données utiles au document).
     */
    public byte[] generateWorkCertificate(Employee employee, Contract contract, boolean includeNationalId) {
        String compName = getCompanyName();
        String city = getCompanyCity();
        String ninea = getCompanyNinea();
        String rccm = getCompanyRccm();

        PdfDocumentBuilder builder = PdfDocumentBuilder.standardA4(
                "Attestation de Travail - " + (employee != null ? employee.getEmployeeNumber() : ""),
                "Attestation d'emploi",
                compName,
                null
        );

        builder.addCompanyHeader(compName, city, ninea, rccm);
        builder.addDocumentTitle("ATTESTATION DE TRAVAIL");

        String empName = (employee != null && employee.getFullName() != null) ? employee.getFullName() : "N/A";
        String empNumber = (employee != null && employee.getEmployeeNumber() != null) ? employee.getEmployeeNumber() : "N/A";
        String position = (employee != null && employee.getPosition() != null && employee.getPosition().getTitle() != null)
                ? employee.getPosition().getTitle() : "Collaborateur";
        String department = (employee != null && employee.getDepartment() != null && employee.getDepartment().getName() != null)
                ? employee.getDepartment().getName() : null;

        LocalDate start = (employee != null && employee.getHireDate() != null) ? employee.getHireDate()
                : (contract != null ? contract.getStartDate() : null);
        LocalDate end = contract != null ? contract.getEndDate() : null;
        boolean terminated = employee != null && employee.getStatus() == EmployeeStatus.TERMINATED;
        boolean ended = terminated || (end != null && end.isBefore(LocalDate.now()));

        StringBuilder text = new StringBuilder();
        text.append("Nous soussignés, la Direction des Ressources Humaines de la société ")
                .append(compName).append(", certifions que :\n\n")
                .append("Monsieur / Madame : ").append(empName).append("\n")
                .append("Matricule : ").append(empNumber).append("\n");
        if (includeNationalId && employee != null && employee.getNationalId() != null) {
            text.append("Numéro de CNI / Passeport : ").append(employee.getNationalId()).append("\n");
        }
        text.append("\n");

        String startStr = PdfFormatUtils.formatDate(start);
        if (ended) {
            if (end != null) {
                text.append("a été employé(e) au sein de notre entreprise du ").append(startStr)
                        .append(" au ").append(PdfFormatUtils.formatDate(end));
            } else {
                text.append("a été employé(e) au sein de notre entreprise à compter du ").append(startStr);
            }
        } else {
            text.append("est employé(e) au sein de notre entreprise depuis le ").append(startStr);
        }
        text.append(" en qualité de ").append(position);
        if (department != null) {
            text.append(" au sein du département ").append(department);
        }
        String contractPhrase = contractPhrase(contract);
        if (contractPhrase != null) {
            text.append(",\n").append(contractPhrase);
        }
        text.append(".\n\nLa présente attestation est délivrée à l'intéressé(e) pour servir et valoir ce que de droit.");

        builder.addParagraph(text.toString(), 11, false, TextAlignment.LEFT, 40);

        builder.addSignatureBlock(
                city,
                LocalDate.now(),
                null,
                null,
                "Pour la Direction Générale",
                "La Direction des Ressources Humaines"
        );

        return builder.build();
    }

    private static String contractPhrase(Contract contract) {
        if (contract == null || contract.getContractType() == null) return null;
        return switch (contract.getContractType()) {
            case CDI -> "dans le cadre d'un contrat de travail à durée indéterminée (CDI)";
            case CDD -> "dans le cadre d'un contrat de travail à durée déterminée (CDD)";
            case STAGE -> "dans le cadre d'une convention de stage";
            case PRESTATION -> "dans le cadre d'un contrat de prestation";
        };
    }

    /**
     * 2. Contrat de travail (Modèle démonstration paramétré).
     */
    public byte[] generateEmploymentContract(Employee employee, Contract contract) {
        String compName = getCompanyName();
        String city = getCompanyCity();
        String ninea = getCompanyNinea();
        String rccm = getCompanyRccm();
        String leaveDays = formatSettingNumber(getSetting("LEAVE_DAYS_PER_MONTH", null));

        String contractType = (contract != null && contract.getContractType() != null)
                ? contract.getContractType().name() : "CDI";

        PdfDocumentBuilder builder = PdfDocumentBuilder.standardA4(
                "Contrat de Travail - " + (employee != null ? employee.getEmployeeNumber() : ""),
                "Contrat de travail " + contractType,
                compName,
                "Modèle de démonstration, à faire valider par un juriste ou le service RH avant tout usage réel"
        );

        builder.addCompanyHeader(compName, city, ninea, rccm);
        builder.addDocumentTitle("CONTRAT DE TRAVAIL (" + contractType + ")");

        String empName = (employee != null && employee.getFullName() != null) ? employee.getFullName() : "N/A";
        String empNationalId = (employee != null && employee.getNationalId() != null) ? employee.getNationalId() : "N/A";
        String empAddress = (employee != null && employee.getAddress() != null) ? employee.getAddress() : "Non renseignée";
        String position = (employee != null && employee.getPosition() != null && employee.getPosition().getTitle() != null)
                ? employee.getPosition().getTitle() : "Collaborateur";
        String department = (employee != null && employee.getDepartment() != null && employee.getDepartment().getName() != null)
                ? employee.getDepartment().getName() : null;

        String grossSalaryStr = (contract != null && contract.getGrossSalary() != null)
                ? PdfFormatUtils.formatCurrency(contract.getGrossSalary()) : "à convenir";
        String startDateStr = (contract != null && contract.getStartDate() != null)
                ? PdfFormatUtils.formatDate(contract.getStartDate()) : PdfFormatUtils.formatDate(LocalDate.now());
        String durationStr = "CDD".equals(contractType)
                ? "durée déterminée (CDD)" + (contract != null && contract.getEndDate() != null
                        ? ", jusqu'au " + PdfFormatUtils.formatDate(contract.getEndDate()) : "")
                : "durée indéterminée (CDI)";

        String body = "ENTRE LES SOUSSIGNÉS :\n\n" +
                "1. La société " + compName + ",\n" +
                "Ci-après dénommée « L'Employeur »,\n\nD'UNE PART,\n\n" +
                "ET :\n\n" +
                "2. M./Mme " + empName + ", titulaire de la CNI/Passeport n° " + empNationalId + ",\n" +
                "Demeurant à : " + empAddress + ",\n" +
                "Ci-après dénommé(e) « Le Salarié »,\n\nD'AUTRE PART.\n\n" +
                "IL A ÉTÉ CONVENU CE QUI SUIT :\n\n" +
                "Article 1 - Engagement et Fonction : Le Salarié est engagé en qualité de " + position
                + (department != null ? " au sein du département " + department : "") + ".\n\n" +
                "Article 2 - Prise d'effet et Durée : Le présent contrat prend effet le " + startDateStr + " sous le régime du contrat à " + durationStr + ".\n\n" +
                "Article 3 - Rémunération : En contrepartie de ses prestations, le Salarié percevra un salaire brut mensuel de " + grossSalaryStr + ", soumis aux cotisations sociales et fiscales en vigueur." +
                (leaveDays != null
                        ? "\n\nArticle 4 - Congés payés : Le Salarié acquiert " + leaveDays + " jours ouvrables de congé par mois de travail effectif, selon le paramétrage en vigueur dans l'entreprise."
                        : "");

        builder.addParagraph(body, 9.5f, false, TextAlignment.LEFT, 20);

        builder.addSignatureBlock(
                city,
                contract != null && contract.getStartDate() != null ? contract.getStartDate() : LocalDate.now(),
                "Le Salarié",
                "Précédé de la mention « Lu et approuvé »",
                "Pour l'Employeur",
                "La Direction Générale"
        );

        builder.addNoticeBox("Modèle de démonstration, à faire valider par un juriste ou le service RH avant tout usage réel.");

        return builder.build();
    }

    /**
     * 3. Bulletin de paie (Tableau structuré sans recalcul, fidèle aux lignes enregistrées).
     */
    public byte[] generatePayslipPdf(Payslip payslip) {
        String compName = getCompanyName();
        String city = getCompanyCity();
        String ninea = getCompanyNinea();
        String rccm = getCompanyRccm();

        int month = payslip != null ? payslip.getPeriodMonth() : 1;
        int year = payslip != null ? payslip.getPeriodYear() : LocalDate.now().getYear();
        String periodFormatted = String.format("%02d/%d", month, year);

        PdfDocumentBuilder builder = PdfDocumentBuilder.standardA4(
                "Bulletin de Paie - " + periodFormatted + " - " + (payslip != null && payslip.getEmployee() != null ? payslip.getEmployee().getEmployeeNumber() : ""),
                "Bulletin de paie mensuel",
                compName,
                "Modèle de démonstration, taux à valider"
        );

        builder.addCompanyHeader(compName, city, ninea, rccm);
        builder.addDocumentTitle("BULLETIN DE PAIE — PÉRIODE : " + periodFormatted);

        // Informations Salarié & Paiement
        Employee emp = payslip != null ? payslip.getEmployee() : null;
        String empName = emp != null ? emp.getFullName() : "N/A";
        String empNumber = emp != null ? emp.getEmployeeNumber() : "N/A";
        String empPosition = (emp != null && emp.getPosition() != null) ? emp.getPosition().getTitle() : "N/A";
        String empDept = (emp != null && emp.getDepartment() != null) ? emp.getDepartment().getName() : "N/A";
        String paymentDateStr = (payslip != null && payslip.getPaymentDate() != null)
                ? PdfFormatUtils.formatDate(payslip.getPaymentDate()) : "N/A";

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        Cell leftInfo = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .add(new Paragraph("Salarié : " + empName + "\n" +
                        "Matricule RH : " + empNumber + "\n" +
                        "Département : " + empDept + "\n" +
                        "Fonction : " + empPosition)
                        .setFont(builder.getFontRegular())
                        .setFontSize(9)
                        .setFontColor(PdfTheme.TEXT_DARK));

        String statusLabel = payslip != null ? payslipStatusLabel(payslip.getStatus()) : null;
        Cell rightInfo = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Date de règlement : " + paymentDateStr + "\n" +
                        "Devise : FCFA" +
                        (statusLabel != null ? "\nStatut : " + statusLabel : ""))
                        .setFont(builder.getFontRegular())
                        .setFontSize(9)
                        .setFontColor(PdfTheme.TEXT_DARK));

        infoTable.addCell(leftInfo);
        infoTable.addCell(rightInfo);
        builder.getDocument().add(infoTable);

        // Tableau des Rubriques de paie réelles
        Table linesTable = new Table(UnitValue.createPercentArray(new float[]{42, 20, 18, 20}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        linesTable.addHeaderCell(createHeaderCell("Rubrique de paie", builder));
        linesTable.addHeaderCell(createHeaderCell("Base (FCFA)", builder));
        linesTable.addHeaderCell(createHeaderCell("Taux appliqué", builder));
        linesTable.addHeaderCell(createHeaderCell("Montant (FCFA)", builder));

        List<PayslipLine> lines = (payslip != null && payslip.getLines() != null) ? payslip.getLines() : List.of();
        boolean alternate = false;

        for (PayslipLine line : lines) {
            alternate = !alternate;
            String label = line.getLabel() != null ? line.getLabel() : "Rubrique";
            String baseStr = line.getBaseAmount() != null ? PdfFormatUtils.formatAmount(line.getBaseAmount()) : "-";
            // Pas de taux pour le salaire de base (taux 100 % sans signification).
            boolean isBaseSalary = line.getLineType() == PayslipLineType.BASE_SALARY;
            String rateStr = !isBaseSalary && line.getRate() != null && line.getRate().compareTo(BigDecimal.ZERO) > 0
                    ? PdfFormatUtils.formatRate(line.getRate()) : "-";
            String amountStr = line.getAmount() != null ? PdfFormatUtils.formatAmount(line.getAmount()) : "0";

            linesTable.addCell(createDataCell(label, alternate, TextAlignment.LEFT, builder));
            linesTable.addCell(createDataCell(baseStr, alternate, TextAlignment.RIGHT, builder));
            linesTable.addCell(createDataCell(rateStr, alternate, TextAlignment.CENTER, builder));
            linesTable.addCell(createDataCell(amountStr, alternate, TextAlignment.RIGHT, builder));
        }

        builder.getDocument().add(linesTable);

        // Tableau des Totaux
        BigDecimal gross = payslip != null && payslip.getGrossSalary() != null ? payslip.getGrossSalary() : BigDecimal.ZERO;
        BigDecimal deductions = payslip != null && payslip.getTotalDeductions() != null ? payslip.getTotalDeductions() : BigDecimal.ZERO;
        BigDecimal net = payslip != null && payslip.getNetSalary() != null ? payslip.getNetSalary() : BigDecimal.ZERO;

        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        totalTable.addCell(createTotalCell("SALAIRE BRUT TOTAL :", false, builder));
        totalTable.addCell(createTotalAmountCell(PdfFormatUtils.formatCurrency(gross), false, builder));

        totalTable.addCell(createTotalCell("TOTAL RETENUES SALARIALES :", false, builder));
        totalTable.addCell(createTotalAmountCell("-" + PdfFormatUtils.NBSP + PdfFormatUtils.formatCurrency(deductions), false, builder));

        Cell netLabelCell = new Cell()
                .setBackgroundColor(PdfTheme.PRIMARY_GREEN)
                .setBorder(new SolidBorder(PdfTheme.PRIMARY_GREEN, 1))
                .setPadding(8)
                .add(new Paragraph("NET À PAYER :")
                        .setFont(builder.getFontBold())
                        .setFontSize(12)
                        .setFontColor(ColorConstants.WHITE));

        Cell netAmountCell = new Cell()
                .setBackgroundColor(PdfTheme.PRIMARY_GREEN)
                .setBorder(new SolidBorder(PdfTheme.PRIMARY_GREEN, 1))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(8)
                .add(new Paragraph(PdfFormatUtils.formatCurrency(net))
                        .setFont(builder.getFontBold())
                        .setFontSize(12)
                        .setFontColor(ColorConstants.WHITE));

        totalTable.addCell(netLabelCell);
        totalTable.addCell(netAmountCell);
        builder.getDocument().add(totalTable);

        builder.addNoticeBox("Modèle de démonstration, taux à valider");

        return builder.build();
    }

    private static String payslipStatusLabel(PayslipStatus status) {
        if (status == null) return null;
        return switch (status) {
            case DRAFT -> "Brouillon";
            case GENERATED -> "Généré";
            case PAID -> "Payé";
        };
    }

    /** "2.0" -> "2", "2.5" -> "2,5" ; valeur absente ou invalide -> null (clause omise). */
    private static String formatSettingNumber(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return PdfFormatUtils.formatDecimal(new BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Cell createHeaderCell(String text, PdfDocumentBuilder builder) {
        return new Cell()
                .setBackgroundColor(PdfTheme.PRIMARY_GREEN)
                .setBorder(new SolidBorder(PdfTheme.BORDER_LIGHT, 0.5f))
                .setPadding(6)
                .add(new Paragraph(text)
                        .setFont(builder.getFontBold())
                        .setFontSize(9)
                        .setFontColor(ColorConstants.WHITE));
    }

    private Cell createDataCell(String text, boolean alternate, TextAlignment alignment, PdfDocumentBuilder builder) {
        return new Cell()
                .setBackgroundColor(alternate ? PdfTheme.BG_LIGHT : ColorConstants.WHITE)
                .setBorder(new SolidBorder(PdfTheme.BORDER_LIGHT, 0.5f))
                .setTextAlignment(alignment)
                .setPadding(5)
                .add(new Paragraph(text)
                        .setFont(builder.getFontRegular())
                        .setFontSize(8.5f)
                        .setFontColor(PdfTheme.TEXT_DARK));
    }

    private Cell createTotalCell(String label, boolean isNet, PdfDocumentBuilder builder) {
        return new Cell()
                .setBackgroundColor(PdfTheme.BG_LIGHT)
                .setBorder(new SolidBorder(PdfTheme.BORDER_LIGHT, 0.5f))
                .setPadding(6)
                .add(new Paragraph(label)
                        .setFont(builder.getFontBold())
                        .setFontSize(9.5f)
                        .setFontColor(PdfTheme.TEXT_DARK));
    }

    private Cell createTotalAmountCell(String amount, boolean isNet, PdfDocumentBuilder builder) {
        return new Cell()
                .setBackgroundColor(PdfTheme.BG_LIGHT)
                .setBorder(new SolidBorder(PdfTheme.BORDER_LIGHT, 0.5f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(6)
                .add(new Paragraph(amount)
                        .setFont(builder.getFontBold())
                        .setFontSize(9.5f)
                        .setFontColor(PdfTheme.TEXT_DARK));
    }

    /**
     * 4. Gabarit pour le Lot 4 : Bilan d'Entretien Annuel d'Évaluation.
     * Prêt à l'emploi et extensible pour l'équipe du lot 4.
     */
    public byte[] generateAnnualEvaluationPdf(AnnualEvaluationPdfData data) {
        String compName = getCompanyName();
        String city = getCompanyCity();
        String ninea = getCompanyNinea();
        String rccm = getCompanyRccm();

        PdfDocumentBuilder builder = PdfDocumentBuilder.standardA4(
                "Bilan d'Évaluation Annuelle - " + (data != null ? data.employeeName() : ""),
                "Entretien annuel d'évaluation N+1",
                compName,
                "Gabarit de performance Lot 4"
        );

        builder.addCompanyHeader(compName, city, ninea, rccm);
        builder.addDocumentTitle("SYNTHÈSE D'ENTRETIEN ANNUEL D'ÉVALUATION");

        if (data != null) {
            String campTitle = data.campaignTitle() != null ? data.campaignTitle() : "Campagne Annuelle";
            String emp = data.employeeName() != null ? data.employeeName() : "N/A";
            String mat = data.employeeNumber() != null ? data.employeeNumber() : "N/A";
            String pos = data.positionTitle() != null ? data.positionTitle() : "N/A";
            String dept = data.departmentName() != null ? data.departmentName() : "N/A";
            String eval = data.evaluatorName() != null ? data.evaluatorName() : "N/A";
            String dateStr = data.interviewDate() != null ? PdfFormatUtils.formatDate(data.interviewDate()) : "N/A";
            String scoreStr = data.finalScore() != null ? PdfFormatUtils.formatDecimal(data.finalScore()) + " / 5" : "Non noté";

            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);

            summaryTable.addCell(new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).add(new Paragraph(
                    "Campagne : " + campTitle + "\n" +
                    "Collaborateur : " + emp + " (" + mat + ")\n" +
                    "Fonction : " + pos + " | Dép. : " + dept)
                    .setFont(builder.getFontRegular()).setFontSize(9).setFontColor(PdfTheme.TEXT_DARK)));

            summaryTable.addCell(new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).add(new Paragraph(
                    "Évaluateur N+1 : " + eval + "\n" +
                    "Date d'entretien : " + dateStr + "\n" +
                    "Score global : " + scoreStr)
                    .setFont(builder.getFontBold()).setFontSize(9).setFontColor(PdfTheme.PRIMARY_GREEN)));

            builder.getDocument().add(summaryTable);

            // Grille des objectifs SMART
            if (data.objectives() != null && !data.objectives().isEmpty()) {
                builder.addParagraph("Objectifs SMART Évalués :", 11, true, TextAlignment.LEFT, 8);

                Table objTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 15, 15}))
                        .useAllAvailableWidth()
                        .setMarginBottom(15);
                objTable.addHeaderCell(createHeaderCell("Objectif SMART", builder));
                objTable.addHeaderCell(createHeaderCell("Indicateur KPI", builder));
                objTable.addHeaderCell(createHeaderCell("Poids", builder));
                objTable.addHeaderCell(createHeaderCell("Atteinte", builder));

                boolean alt = false;
                for (AnnualEvaluationPdfData.ObjectiveItem item : data.objectives()) {
                    alt = !alt;
                    objTable.addCell(createDataCell(item.title() + "\n" + (item.description() != null ? item.description() : ""), alt, TextAlignment.LEFT, builder));
                    objTable.addCell(createDataCell(item.kpi() != null ? item.kpi() : "-", alt, TextAlignment.LEFT, builder));
                    objTable.addCell(createDataCell(item.weight() + " %", alt, TextAlignment.CENTER, builder));
                    objTable.addCell(createDataCell(item.achievementRate() + " %", alt, TextAlignment.CENTER, builder));
                }
                builder.getDocument().add(objTable);
            }

            if (data.generalComments() != null && !data.generalComments().isBlank()) {
                builder.addParagraph("Commentaires & Perspectives d'Évolution :", 10, true, TextAlignment.LEFT, 4);
                builder.addParagraph(data.generalComments(), 9, false, TextAlignment.LEFT, 15);
            }

            builder.addSignatureBlock(
                    city,
                    data.interviewDate() != null ? data.interviewDate() : LocalDate.now(),
                    "Le Salarié Évalué",
                    "Visa et commentaires",
                    "L'Évaluateur N+1",
                    "Validation hiérarchique"
            );
        }

        return builder.build();
    }

    /**
     * 5. Gabarit pour le Lot 4 : Attestation de Fin de Formation.
     */
    public byte[] generateTrainingCertificatePdf(TrainingCertificatePdfData data) {
        String compName = getCompanyName();
        String city = getCompanyCity();
        String ninea = getCompanyNinea();
        String rccm = getCompanyRccm();

        PdfDocumentBuilder builder = PdfDocumentBuilder.standardA4(
                "Attestation de Formation - " + (data != null ? data.employeeName() : ""),
                "Attestation de compétences",
                compName,
                "Gabarit de formation Lot 4"
        );

        builder.addCompanyHeader(compName, city, ninea, rccm);
        builder.addDocumentTitle("ATTESTATION DE SUIVI DE FORMATION");

        if (data != null) {
            String emp = data.employeeName() != null ? data.employeeName() : "N/A";
            String certNum = data.certificateNumber() != null ? data.certificateNumber() : "CERT-DEMO";
            String training = data.trainingTitle() != null ? data.trainingTitle() : "Formation Professionnelle";
            String org = data.organizationName() != null ? data.organizationName() : compName;
            String compDate = data.completionDate() != null ? PdfFormatUtils.formatDate(data.completionDate()) : "N/A";
            int hours = data.durationHours() > 0 ? data.durationHours() : 35;

            String body = "La Direction de " + compName + " atteste par la présente que :\n\n" +
                    "Monsieur / Madame : " + emp + "\n" +
                    "Immatriculation : " + (data.employeeNumber() != null ? data.employeeNumber() : "N/A") + "\n\n" +
                    "A suivi avec succès l'action de développement des compétences suivante :\n\n" +
                    "Intitulé de la session : « " + training + " »\n" +
                    "Organisme formateur : " + org + "\n" +
                    "Volume horaire accompli : " + hours + " heures de formation\n" +
                    "Date de validation des acquis : " + compDate + "\n" +
                    "Référence du certificat : " + certNum + "\n\n" +
                    "Compétences clés validées :\n" +
                    (data.skillsAcquired() != null ? data.skillsAcquired() : "Mise en pratique opérationnelle selon le plan de développement RH.") + "\n\n" +
                    "En foi de quoi, la présente attestation lui est remise en reconnaissance de son parcours.";

            builder.addParagraph(body, 10, false, TextAlignment.LEFT, 30);

            builder.addSignatureBlock(
                    city,
                    data.completionDate() != null ? data.completionDate() : LocalDate.now(),
                    "Le Participant",
                    "Émargement",
                    "Le Responsable Formation",
                    "Direction des Ressources Humaines"
            );
        }

        return builder.build();
    }

    /**
     * 6. Gabarit générique tabulaire pour le Lot 2 (Export de listes / Registre).
     * Par principe de sécurité RBAC et de confidentialité, les colonnes sensibles
     * (CNI, salaires, date de naissance) ne sont pas incluses par défaut.
     */
    public byte[] generateTabularReportPdf(String title, String subtitle, List<String> headers, List<List<String>> rows) {
        String compName = getCompanyName();
        String city = getCompanyCity();
        String ninea = getCompanyNinea();
        String rccm = getCompanyRccm();

        PdfDocumentBuilder builder = PdfDocumentBuilder.landscapeA4(
                title != null ? title : "Rapport Tabulaire",
                subtitle != null ? subtitle : "Export de données RH",
                compName,
                "Export généré par CareerTrack"
        );

        builder.addCompanyHeader(compName, city, ninea, rccm);
        builder.addDocumentTitle(title != null ? title : "RAPPORT TABULAIRE");

        if (subtitle != null && !subtitle.isBlank()) {
            builder.addParagraph(subtitle, 10, true, TextAlignment.CENTER, 15);
        }

        builder.addGenericTable(headers, rows);

        return builder.build();
    }
}
