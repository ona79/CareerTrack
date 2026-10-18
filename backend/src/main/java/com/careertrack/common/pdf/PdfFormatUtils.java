package com.careertrack.common.pdf;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilitaires de formatage stricts pour les documents PDF sénégalais.
 * Utilise l'espace insécable standard U+00A0 pour séparer les milliers afin
 * d'éviter l'espace fine U+202F non supportée par certaines polices ISO/CP1252.
 */
public final class PdfFormatUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private PdfFormatUtils() {
        // Empêcher l'instanciation
    }

    /**
     * Formate un montant en FCFA avec séparateur de milliers U+00A0.
     * Exemple : 450000 -> "450 000 FCFA"
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 FCFA";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator('\u00A0'); // Espace insécable standard U+00A0
        symbols.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount) + " FCFA";
    }

    /**
     * Formate une date en jj/mm/aaaa.
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "N/A";
        }
        return date.format(DATE_FORMATTER);
    }
}
