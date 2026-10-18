package com.careertrack.common.pdf;

import com.itextpdf.kernel.colors.DeviceRgb;

/**
 * Charte visuelle officielle CareerTrack pour la génération des documents PDF.
 * Palette institutionnelle harmonisée avec l'interface :
 * - Vert principal : #1A6B3C
 * - Or accent : #F5A623
 * - Gris texte sombre : #0F172A
 * - Gris secondaire : #64748B
 * - Fond de tableau / alternance : #F8FAFC
 * - Bordure douce : #E2E8F0
 */
public final class PdfTheme {

    private PdfTheme() {
    }

    public static final DeviceRgb PRIMARY_GREEN = new DeviceRgb(26, 107, 60);    // #1A6B3C
    public static final DeviceRgb ACCENT_GOLD    = new DeviceRgb(245, 166, 35);   // #F5A623
    public static final DeviceRgb TEXT_DARK      = new DeviceRgb(15, 23, 42);     // #0F172A
    public static final DeviceRgb TEXT_SECONDARY = new DeviceRgb(100, 116, 139);  // #64748B
    public static final DeviceRgb BG_LIGHT       = new DeviceRgb(248, 250, 252);  // #F8FAFC
    public static final DeviceRgb BORDER_LIGHT   = new DeviceRgb(226, 232, 240);  // #E2E8F0
}
