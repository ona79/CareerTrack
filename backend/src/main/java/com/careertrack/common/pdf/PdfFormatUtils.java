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

    /** Espace insécable standard (présent dans Latin-1 / WinAnsi, rendu par Helvetica). */
    public static final char NBSP = '\u00A0';

    /**
     * Formate un montant en FCFA : séparateur de milliers U+00A0 ET espace insécable
     * U+00A0 avant "FCFA", afin que le montant ne soit jamais coupé en fin de ligne.
     * Exemple : 450000 -> "450 000 FCFA"
     */
    public static String formatCurrency(BigDecimal amount) {
        return formatAmount(amount) + NBSP + "FCFA";
    }

    /**
     * Montant sans unité (colonnes de tableau déjà libellées en FCFA).
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator(NBSP);
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(amount);
    }

    /**
     * Nombre décimal à la française, sans zéros inutiles : 2.0 -> "2", 2.5 -> "2,5", 4.6 -> "4,6".
     */
    public static String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator(NBSP);
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symbols).format(value);
    }

    /**
     * Taux exprimé en fraction (0.056) formaté en pourcentage à la française : "5,60 %".
     */
    public static String formatRate(BigDecimal rate) {
        if (rate == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(NBSP);
        return new DecimalFormat("#,##0.00", symbols).format(rate.multiply(BigDecimal.valueOf(100))) + NBSP + "%";
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
