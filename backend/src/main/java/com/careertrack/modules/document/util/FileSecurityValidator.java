package com.careertrack.modules.document.util;

import com.careertrack.common.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

public final class FileSecurityValidator {

    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}; // .PNG....
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // JPEG SOI

    public record ValidatedFileInfo(String mimeType, String normalizedExtension) {}

    private FileSecurityValidator() {
    }

    /**
     * Valide le fichier par son contenu binaire (magic numbers) et vérifie la cohérence avec l'extension.
     * Renvoie le mimeType officiel déterminé par la signature.
     */
    public static ValidatedFileInfo validate(MultipartFile file, String filename) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier téléversé ne peut pas être vide.");
        }

        byte[] header = new byte[8];
        int bytesRead;
        try (InputStream is = file.getInputStream()) {
            bytesRead = is.read(header);
        } catch (IOException e) {
            throw new BadRequestException("Impossible de lire le contenu du fichier téléversé.");
        }

        if (bytesRead < 4) {
            throw new BadRequestException("Le fichier téléversé est corrompu ou incomplet.");
        }

        String detectedMime = null;
        String detectedFormat = null;

        if (startsWith(header, PDF_MAGIC)) {
            detectedMime = "application/pdf";
            detectedFormat = "PDF";
        } else if (startsWith(header, PNG_MAGIC)) {
            detectedMime = "image/png";
            detectedFormat = "PNG";
        } else if (startsWith(header, JPEG_MAGIC)) {
            detectedMime = "image/jpeg";
            detectedFormat = "JPEG";
        }

        if (detectedMime == null) {
            throw new BadRequestException("Format de fichier non supporté. Seuls les formats PDF, JPEG et PNG sont acceptés.");
        }

        // Vérification de cohérence entre la signature et l'extension du nom de fichier
        String lowerFilename = (filename != null ? filename : "").toLowerCase(Locale.ROOT);
        boolean extensionValid = false;

        if ("PDF".equals(detectedFormat)) {
            extensionValid = lowerFilename.endsWith(".pdf");
        } else if ("PNG".equals(detectedFormat)) {
            extensionValid = lowerFilename.endsWith(".png");
        } else if ("JPEG".equals(detectedFormat)) {
            extensionValid = lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg");
        }

        if (!extensionValid) {
            throw new BadRequestException("L'extension du fichier ne correspond pas à son format réel (" + detectedFormat + " détecté).");
        }

        return new ValidatedFileInfo(detectedMime, detectedFormat.toLowerCase(Locale.ROOT));
    }

    private static boolean startsWith(byte[] source, byte[] match) {
        if (source.length < match.length) return false;
        for (int i = 0; i < match.length; i++) {
            if (source[i] != match[i]) return false;
        }
        return true;
    }
}
