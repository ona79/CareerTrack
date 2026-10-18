package com.careertrack.common.pdf;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gestionnaire d'événements iText 7 pour l'en-tête et le pied de page paginé (Page X / Y).
 */
public class PdfHeaderFooterEventHandler implements IEventHandler {

    private final String companyName;
    private final String customFooterNote;
    private static final DateTimeFormatter FOOTER_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    public PdfHeaderFooterEventHandler(String companyName, String customFooterNote) {
        this.companyName = companyName != null && !companyName.isBlank() ? companyName : null;
        this.customFooterNote = customFooterNote != null && !customFooterNote.isBlank() ? customFooterNote : null;
    }

    public PdfHeaderFooterEventHandler(String companyName) {
        this(companyName, null);
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        int pageNumber = pdfDoc.getPageNumber(page);

        Rectangle pageSize = page.getPageSize();
        PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
        Canvas canvas = new Canvas(pdfCanvas, pageSize);

        float marginHorizontal = 40f;
        float headerY = pageSize.getTop() - 25f;
        float footerY = pageSize.getBottom() + 20f;
        float width = pageSize.getWidth() - (2 * marginHorizontal);

        // 1. En-tête discret si multi-pages (à partir de la page 2)
        if (pageNumber > 1 && companyName != null) {
            Paragraph header = new Paragraph(companyName.toUpperCase())
                    .setFontSize(7)
                    .setFontColor(PdfTheme.TEXT_SECONDARY);
            canvas.showTextAligned(header, marginHorizontal, headerY, TextAlignment.LEFT);

            // Ligne séparatrice en-tête
            pdfCanvas.saveState()
                    .setStrokeColor(PdfTheme.BORDER_LIGHT)
                    .setLineWidth(0.5f)
                    .moveTo(marginHorizontal, headerY - 5)
                    .lineTo(pageSize.getWidth() - marginHorizontal, headerY - 5)
                    .stroke()
                    .restoreState();
        }

        // 2. Ligne séparatrice pied de page
        pdfCanvas.saveState()
                .setStrokeColor(PdfTheme.BORDER_LIGHT)
                .setLineWidth(0.5f)
                .moveTo(marginHorizontal, footerY + 15)
                .lineTo(pageSize.getWidth() - marginHorizontal, footerY + 15)
                .stroke()
                .restoreState();

        // 3. Pied de page gauche : date ou note légale
        String dateStr = "Document généré le " + LocalDate.now().format(FOOTER_DATE_FORMAT);
        if (customFooterNote != null) {
            dateStr = customFooterNote + " — " + dateStr;
        }
        Paragraph footerLeft = new Paragraph(dateStr)
                .setFontSize(7)
                .setItalic()
                .setFontColor(PdfTheme.TEXT_SECONDARY);
        canvas.showTextAligned(footerLeft, marginHorizontal, footerY, TextAlignment.LEFT);

        // 4. Pied de page droit : Page X
        Paragraph footerRight = new Paragraph("Page " + pageNumber)
                .setFontSize(7)
                .setFontColor(PdfTheme.TEXT_SECONDARY);
        canvas.showTextAligned(footerRight, pageSize.getWidth() - marginHorizontal, footerY, TextAlignment.RIGHT);

        canvas.close();
    }
}
