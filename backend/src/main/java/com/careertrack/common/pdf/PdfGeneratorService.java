package com.careertrack.common.pdf;

import com.careertrack.modules.employee.entity.Contract;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.entity.PayslipLine;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class PdfGeneratorService {

    @Value("${careertrack.company-name:Sénégal Digital Solutions PME}")
    private String companyName = "Sénégal Digital Solutions PME";

    private static final DeviceRgb PRIMARY_BLUE = new DeviceRgb(37, 99, 235);   // #2563EB
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(15, 23, 42);       // #0F172A
    private static final DeviceRgb TEXT_MUTED = new DeviceRgb(100, 116, 139);   // #64748B
    private static final DeviceRgb BG_LIGHT = new DeviceRgb(248, 250, 252);     // #F8FAFC

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(Locale.FRENCH);

    /**
     * Génère une Attestation de Travail officielle conforme à la pratique RH au Sénégal.
     */
    public byte[] generateWorkCertificate(Employee employee, Contract contract) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 50, 40, 50);

            // 1. En-tête de l'entreprise
            document.add(new Paragraph(companyName.toUpperCase())
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(PRIMARY_BLUE));

            document.add(new Paragraph("Société à Responsabilité Limitée (SARL) - Capital : 10 000 000 FCFA\n" +
                    "Siège social : Avenue Cheikh Anta Diop, Dakar, Sénégal\n" +
                    "NINEA : 006548972 2V2 - R.C.C.M : SN.DKR.2018.B.1425\n" +
                    "Téléphone : +221 33 824 00 00 | E-mail : rh@careertrack.sn")
                    .setFontSize(8)
                    .setFontColor(TEXT_MUTED)
                    .setMarginBottom(30));

            // Ligne de séparation
            document.add(new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth()
                    .setBorder(new SolidBorder(PRIMARY_BLUE, 1))
                    .setMarginBottom(35));

            // 2. Titre du document
            document.add(new Paragraph("ATTESTATION DE TRAVAIL")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(TEXT_DARK)
                    .setMarginBottom(35));

            // 3. Corps de l'attestation
            String contractTypeLabel = (contract != null && contract.getContractType() != null)
                    ? contract.getContractType().name() : "CDI";
            String positionTitle = (employee.getPosition() != null)
                    ? employee.getPosition().getTitle() : "Collaborateur";
            String departmentName = (employee.getDepartment() != null)
                    ? employee.getDepartment().getName() : "Non spécifié";

            String formattedHireDate = employee.getHireDate() != null
                    ? employee.getHireDate().format(DATE_FORMATTER) : "N/A";

            String textBody = "Je soussigné(e), Directeur des Ressources Humaines de la société " + companyName + ",\n\n" +
                    "Certifie par la présente que :\n\n" +
                    "Monsieur / Madame : " + employee.getFullName() + "\n" +
                    "Numéro d'immatriculation RH : " + employee.getEmployeeNumber() + "\n" +
                    "Numéro de CNI / Passeport : " + employee.getNationalId() + "\n\n" +
                    "Est employé(e) au sein de notre entreprise depuis le " + formattedHireDate + " en qualité de " +
                    positionTitle + " au sein du département " + departmentName + ",\n" +
                    "dans le cadre d'un contrat de travail à " + contractTypeLabel + ".\n\n" +
                    "La présente attestation lui est délivrée sur sa demande pour servir et valoir ce que de droit.";

            document.add(new Paragraph(textBody)
                    .setFontSize(11)
                    .setFontColor(TEXT_DARK)
                    .setMultipliedLeading(1.5f)
                    .setMarginBottom(50));

            // 4. Date et Signature
            String formattedToday = LocalDate.now().format(DATE_FORMATTER);
            document.add(new Paragraph("Fait à Dakar, le " + formattedToday)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontColor(TEXT_DARK)
                    .setMarginBottom(40));

            Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();

            Cell employeeCell = new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("Le Collaborateur\n(Lu et approuvé)")
                            .setFontSize(10).setItalic().setFontColor(TEXT_MUTED));

            Cell drhCell = new Cell().setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("Pour la Direction Générale\nLa Direction des Ressources Humaines\n[Cachet et Signature]")
                            .setFontSize(10).setBold().setFontColor(TEXT_DARK));

            signatureTable.addCell(employeeCell);
            signatureTable.addCell(drhCell);
            document.add(signatureTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Erreur lors de la génération de l'attestation de travail: {}", ex.getMessage());
            throw new RuntimeException("Échec de génération du document PDF : " + ex.getMessage(), ex);
        }
    }

    /**
     * Génère un Bulletin de Paie conforme aux cotisations sénégalaises (IPRES, CSS, IPM).
     */
    public byte[] generatePayslipPdf(Payslip payslip) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(30, 40, 30, 40);

            // En-tête Bulletin de Paie
            document.add(new Paragraph(companyName.toUpperCase())
                    .setFontSize(12).setBold().setFontColor(PRIMARY_BLUE));
            document.add(new Paragraph("BULLETIN DE PAIE - PÉRIODE : " + payslip.getPeriodMonth() + "/" + payslip.getPeriodYear())
                    .setFontSize(14).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            // Infos Salarié & Entreprise
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginBottom(15);
            infoTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("Salarié : " + payslip.getEmployee().getFullName() + "\n" +
                            "Matricule : " + payslip.getEmployee().getEmployeeNumber() + "\n" +
                            "Poste : " + (payslip.getEmployee().getPosition() != null ? payslip.getEmployee().getPosition().getTitle() : "N/A"))
                            .setFontSize(9)));

            infoTable.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("Date de paiement : " + payslip.getPaymentDate().format(DATE_FORMATTER) + "\n" +
                            "Devise : FCFA\n" +
                            "Organismes : IPRES (5.6%) / CSS (7.0%)")
                            .setFontSize(9)));
            document.add(infoTable);

            // Tableau des Rubriques de paie
            Table linesTable = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20})).useAllAvailableWidth().setMarginBottom(20);
            linesTable.addHeaderCell(new Cell().setBackgroundColor(BG_LIGHT).add(new Paragraph("Rubrique").setBold().setFontSize(9)));
            linesTable.addHeaderCell(new Cell().setBackgroundColor(BG_LIGHT).add(new Paragraph("Base (FCFA)").setBold().setFontSize(9)));
            linesTable.addHeaderCell(new Cell().setBackgroundColor(BG_LIGHT).add(new Paragraph("Taux").setBold().setFontSize(9)));
            linesTable.addHeaderCell(new Cell().setBackgroundColor(BG_LIGHT).add(new Paragraph("Montant (FCFA)").setBold().setFontSize(9)));

            for (PayslipLine line : payslip.getLines()) {
                linesTable.addCell(new Cell().add(new Paragraph(line.getLabel()).setFontSize(9)));
                linesTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMATTER.format(line.getBaseAmount())).setFontSize(9)));
                linesTable.addCell(new Cell().add(new Paragraph(String.format("%.2f %%", line.getRate().doubleValue() * 100)).setFontSize(9)));
                linesTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMATTER.format(line.getAmount())).setFontSize(9)));
            }
            document.add(linesTable);

            // Totaux
            Table totalTable = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
            totalTable.addCell(new Cell().add(new Paragraph("SALAIRE BRUT :").setBold().setFontSize(10)));
            totalTable.addCell(new Cell().add(new Paragraph(CURRENCY_FORMATTER.format(payslip.getGrossSalary()) + " FCFA").setBold().setFontSize(10)));

            totalTable.addCell(new Cell().add(new Paragraph("TOTAL RETENUES :").setFontColor(ColorConstants.RED).setFontSize(10)));
            totalTable.addCell(new Cell().add(new Paragraph("- " + CURRENCY_FORMATTER.format(payslip.getTotalDeductions()) + " FCFA").setFontColor(ColorConstants.RED).setFontSize(10)));

            totalTable.addCell(new Cell().setBackgroundColor(BG_LIGHT).add(new Paragraph("NET À PAYER :").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE)));
            totalTable.addCell(new Cell().setBackgroundColor(BG_LIGHT).add(new Paragraph(CURRENCY_FORMATTER.format(payslip.getNetSalary()) + " FCFA").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE)));

            document.add(totalTable);
            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Erreur lors de la génération du bulletin de paie: {}", ex.getMessage());
            throw new RuntimeException("Échec de génération du bulletin de paie : " + ex.getMessage(), ex);
        }
    }

    /**
     * Génère un modèle de Contrat de Travail (démonstration).
     */
    public byte[] generateEmploymentContract(Employee employee, Contract contract) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 50, 40, 50);

            // 1. En-tête
            document.add(new Paragraph(companyName.toUpperCase())
                    .setFontSize(14).setBold().setFontColor(PRIMARY_BLUE));
            document.add(new Paragraph("Société à Responsabilité Limitée (SARL) - Capital : 10 000 000 FCFA\n" +
                    "Siège social : Avenue Cheikh Anta Diop, Dakar, Sénégal\n" +
                    "NINEA : 006548972 2V2 - R.C.C.M : SN.DKR.2018.B.1425")
                    .setFontSize(8).setFontColor(TEXT_MUTED).setMarginBottom(20));

            // Titre
            String typeTitle = (contract != null && contract.getContractType() != null)
                    ? contract.getContractType().name() : "CDI";
            document.add(new Paragraph("CONTRAT DE TRAVAIL (" + typeTitle + ")")
                    .setFontSize(16).setBold().setTextAlignment(TextAlignment.CENTER).setFontColor(TEXT_DARK).setMarginBottom(20));

            // Parties
            String posTitle = (employee.getPosition() != null) ? employee.getPosition().getTitle() : "Collaborateur";
            String deptTitle = (employee.getDepartment() != null) ? employee.getDepartment().getName() : "Non spécifié";
            String salaryStr = (contract != null && contract.getGrossSalary() != null)
                    ? CURRENCY_FORMATTER.format(contract.getGrossSalary()) + " FCFA" : "À convenir";
            String startDateStr = (contract != null && contract.getStartDate() != null)
                    ? contract.getStartDate().format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);

            String body = "ENTRE LES SOUSSIGNÉS :\n\n" +
                    "1. La société " + companyName + ", représentée par sa Direction des Ressources Humaines,\n" +
                    "Ci-après dénommée « L'Employeur »,\n\nD'UNE PART,\n\n" +
                    "ET :\n\n" +
                    "2. M./Mme " + employee.getFullName() + ", titulaire de la CNI n° " + employee.getNationalId() + ",\n" +
                    "Demeurant à : " + (employee.getAddress() != null ? employee.getAddress() : "Dakar, Sénégal") + ",\n" +
                    "Ci-après dénommé(e) « Le Salarié »,\n\nD'AUTRE PART.\n\n" +
                    "IL A ÉTÉ CONVENU CE QUI SUIT :\n\n" +
                    "Article 1 - Engagement et Fonction : Le Salarié est engagé en qualité de " + posTitle + " au sein du département " + deptTitle + ".\n\n" +
                    "Article 2 - Prise d'effet et Durée : Le présent contrat prend effet le " + startDateStr + " sous le régime " + typeTitle + ".\n\n" +
                    "Article 3 - Rémunération : Le Salarié percevra un salaire brut mensuel de " + salaryStr + ", soumis aux cotisations obligatoires (IPRES, CSS, IPM).\n\n" +
                    "Article 4 - Congés payés : Le Salarié acquiert 2 jours ouvrables de congé par mois de service effectif, conformément aux règles de l'entreprise.";

            document.add(new Paragraph(body).setFontSize(10).setFontColor(TEXT_DARK).setMultipliedLeading(1.3f).setMarginBottom(30));

            // Signatures
            String todayStr = LocalDate.now().format(DATE_FORMATTER);
            document.add(new Paragraph("Fait en deux originaux à Dakar, le " + todayStr)
                    .setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(20));

            Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            sigTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("Le Salarié\n(Précédé de la mention « Lu et approuvé »)").setFontSize(9).setItalic().setFontColor(TEXT_MUTED)));
            sigTable.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).add(new Paragraph("Pour l'Employeur\nLa Direction Générale\n[Cachet et Signature]").setFontSize(9).setBold().setFontColor(TEXT_DARK)));
            document.add(sigTable);

            // Avertissement légal
            document.add(new Paragraph("Modèle de démonstration, à faire valider par un juriste ou le service RH avant tout usage réel.")
                    .setFontSize(8)
                    .setItalic()
                    .setFontColor(TEXT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Erreur lors de la génération du contrat de travail: {}", ex.getMessage());
            throw new RuntimeException("Échec de génération du contrat : " + ex.getMessage(), ex);
        }
    }
}
