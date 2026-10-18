package com.careertrack.common.pdf;

import com.careertrack.common.pdf.template.AnnualEvaluationPdfData;
import com.careertrack.common.pdf.template.TrainingCertificatePdfData;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Constructeur fluide et gabarit de base pour documents PDF CareerTrack.
 * Permet de créer un document PDF complet et stylisé en quelques lignes de code.
 */
@Slf4j
public class PdfDocumentBuilder {

    private final ByteArrayOutputStream baos;
    private final PdfWriter writer;
    private final PdfDocument pdfDoc;
    private final Document document;
    private final PdfFont fontRegular;
    private final PdfFont fontBold;
    private final PdfFont fontItalic;

    public PdfDocumentBuilder(PageSize pageSize, String title, String author, String subject, String companyName, String footerNote) {
        try {
            this.baos = new ByteArrayOutputStream();
            this.writer = new PdfWriter(baos);
            this.pdfDoc = new PdfDocument(writer);

            // Métadonnées PDF
            PdfDocumentInfo info = pdfDoc.getDocumentInfo();
            info.setTitle(title != null ? title : "Document CareerTrack");
            info.setAuthor(author != null ? author : "CareerTrack HR");
            info.setSubject(subject != null ? subject : "Système de Gestion RH");
            info.setCreator("CareerTrack PME Sénégal");

            // Événement d'en-tête et pied de page
            pdfDoc.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE,
                    new PdfHeaderFooterEventHandler(companyName, footerNote));

            this.document = new Document(pdfDoc, pageSize != null ? pageSize : PageSize.A4);
            this.document.setMargins(40, 40, 45, 40);

            // Polices avec encodage StandardFonts (Helvetica gère parfaitement les caractères ISO français)
            this.fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            this.fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            this.fontItalic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
            this.document.setFont(fontRegular);
        } catch (IOException e) {
            log.error("Erreur lors de l'initialisation du PdfDocumentBuilder: {}", e.getMessage());
            throw new RuntimeException("Impossible d'initialiser le document PDF", e);
        }
    }

    public static PdfDocumentBuilder standardA4(String title, String subject, String companyName, String footerNote) {
        return new PdfDocumentBuilder(PageSize.A4, title, "CareerTrack", subject, companyName, footerNote);
    }

    public static PdfDocumentBuilder landscapeA4(String title, String subject, String companyName, String footerNote) {
        return new PdfDocumentBuilder(PageSize.A4.rotate(), title, "CareerTrack", subject, companyName, footerNote);
    }

    /**
     * Ajoute l'en-tête d'entreprise (avec coordonnées lues depuis company_settings).
     * Les lignes absentes disparaissent sans laisser de vide ou de texte factice.
     */
    public PdfDocumentBuilder addCompanyHeader(String companyName, String city, String ninea, String rccm) {
        if (companyName != null && !companyName.isBlank()) {
            document.add(new Paragraph(companyName.toUpperCase())
                    .setFont(fontBold)
                    .setFontSize(13)
                    .setFontColor(PdfTheme.PRIMARY_GREEN)
                    .setMarginBottom(2));
        }

        StringBuilder legalDetails = new StringBuilder();
        if (city != null && !city.isBlank()) {
            legalDetails.append("Siège : ").append(city).append(", Sénégal\n");
        }
        if (ninea != null && !ninea.isBlank()) {
            legalDetails.append("NINEA : ").append(ninea);
        }
        if (rccm != null && !rccm.isBlank()) {
            if (!legalDetails.isEmpty() && legalDetails.charAt(legalDetails.length() - 1) != '\n') {
                legalDetails.append(" — ");
            }
            legalDetails.append("R.C.C.M : ").append(rccm);
        }

        if (!legalDetails.isEmpty()) {
            document.add(new Paragraph(legalDetails.toString().trim())
                    .setFontSize(8)
                    .setFontColor(PdfTheme.TEXT_SECONDARY)
                    .setMarginBottom(12));
        }

        // Ligne de séparation verte
        document.add(new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(PdfTheme.PRIMARY_GREEN, 1.5f))
                .setMarginBottom(20));

        return this;
    }

    /**
     * Titre principal du document.
     */
    public PdfDocumentBuilder addDocumentTitle(String title) {
        document.add(new Paragraph(title)
                .setFont(fontBold)
                .setFontSize(16)
                .setFontColor(PdfTheme.TEXT_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));
        return this;
    }

    /**
     * Paragraphe normal.
     */
    public PdfDocumentBuilder addParagraph(String text, float fontSize, boolean bold, TextAlignment alignment, float marginBottom) {
        Paragraph p = new Paragraph(text)
                .setFont(bold ? fontBold : fontRegular)
                .setFontSize(fontSize)
                .setFontColor(PdfTheme.TEXT_DARK)
                .setTextAlignment(alignment)
                .setMarginBottom(marginBottom)
                .setMultipliedLeading(1.4f);
        document.add(p);
        return this;
    }

    /**
     * Bloc d'avis ou de démonstration encadré.
     */
    public PdfDocumentBuilder addNoticeBox(String notice) {
        if (notice == null || notice.isBlank()) return this;
        Table noticeTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth()
                .setMarginTop(15)
                .setMarginBottom(15);
        Cell cell = new Cell()
                .setBackgroundColor(PdfTheme.BG_LIGHT)
                .setBorder(new SolidBorder(PdfTheme.ACCENT_GOLD, 1f))
                .setPadding(8)
                .add(new Paragraph(notice)
                        .setFont(fontItalic)
                        .setFontSize(8)
                        .setFontColor(PdfTheme.TEXT_SECONDARY)
                        .setTextAlignment(TextAlignment.CENTER));
        noticeTable.addCell(cell);
        document.add(noticeTable);
        return this;
    }

    /**
     * Bloc de signature bilatéral "Fait à [Ville], le jj/mm/aaaa".
     */
    public PdfDocumentBuilder addSignatureBlock(String city, LocalDate date, String leftTitle, String leftRole, String rightTitle, String rightRole) {
        String effectiveCity = (city != null && !city.isBlank()) ? city : "Dakar";
        String dateFormatted = PdfFormatUtils.formatDate(date != null ? date : LocalDate.now());

        document.add(new Paragraph("Fait à " + effectiveCity + ", le " + dateFormatted)
                .setFont(fontRegular)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(PdfTheme.TEXT_DARK)
                .setMarginBottom(15));

        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(10);

        Cell leftCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph((leftTitle != null ? leftTitle : "") + "\n" + (leftRole != null ? "(" + leftRole + ")" : ""))
                        .setFont(fontItalic)
                        .setFontSize(9)
                        .setFontColor(PdfTheme.TEXT_SECONDARY));

        Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph((rightTitle != null ? rightTitle : "") + "\n" + (rightRole != null ? rightRole : "") + "\n[Cachet et Signature]")
                        .setFont(fontBold)
                        .setFontSize(9)
                        .setFontColor(PdfTheme.TEXT_DARK));

        sigTable.addCell(leftCell);
        sigTable.addCell(rightCell);
        document.add(sigTable);

        return this;
    }

    /**
     * Moteur de tableau générique pour export de liste (Lot 2).
     * Exclut par principe toute colonne sensible (salaire, CNI, date de naissance).
     */
    public PdfDocumentBuilder addGenericTable(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty()) return this;

        float[] pointColumnWidths = new float[headers.size()];
        for (int i = 0; i < headers.size(); i++) pointColumnWidths[i] = 100f / headers.size();

        Table table = new Table(UnitValue.createPercentArray(pointColumnWidths))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        // En-têtes
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(PdfTheme.PRIMARY_GREEN)
                    .setBorder(new SolidBorder(PdfTheme.BORDER_LIGHT, 0.5f))
                    .setPadding(6)
                    .add(new Paragraph(header != null ? header : "")
                            .setFont(fontBold)
                            .setFontSize(8)
                            .setFontColor(ColorConstants.WHITE)));
        }

        // Données
        if (rows != null) {
            boolean alternate = false;
            for (List<String> row : rows) {
                alternate = !alternate;
                for (String cellValue : row) {
                    table.addCell(new Cell()
                            .setBackgroundColor(alternate ? PdfTheme.BG_LIGHT : ColorConstants.WHITE)
                            .setBorder(new SolidBorder(PdfTheme.BORDER_LIGHT, 0.5f))
                            .setPadding(5)
                            .add(new Paragraph(cellValue != null ? cellValue : "")
                                    .setFont(fontRegular)
                                    .setFontSize(8)
                                    .setFontColor(PdfTheme.TEXT_DARK)));
                }
            }
        }

        document.add(table);
        return this;
    }

    public Document getDocument() {
        return document;
    }

    public PdfFont getFontRegular() {
        return fontRegular;
    }

    public PdfFont getFontBold() {
        return fontBold;
    }

    public PdfFont getFontItalic() {
        return fontItalic;
    }

    /**
     * Finalise et renvoie les octets du fichier PDF.
     */
    public byte[] build() {
        document.close();
        return baos.toByteArray();
    }
}
