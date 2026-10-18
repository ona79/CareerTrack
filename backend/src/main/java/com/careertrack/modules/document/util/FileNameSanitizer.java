package com.careertrack.modules.document.util;

import java.text.Normalizer;

public final class FileNameSanitizer {

    private static final int MAX_BASE_NAME_LENGTH = 100;

    private FileNameSanitizer() {
    }

    /**
     * Nettoie et sécurise un nom de fichier :
     * - Supprime les chemins d'accès (path traversal : ../../etc/)
     * - Supprime les caractères de contrôle (\r, \n, \0, etc.)
     * - Supprime les caractères spéciaux interdits (<, >, :, ", /, \, |, ?, *)
     * - Préserve les lettres accentuées françaises et les espaces
     * - Tronque le nom s'il est excessivement long tout en préservant l'extension
     */
    public static String sanitize(String rawFilename) {
        if (rawFilename == null || rawFilename.isBlank()) {
            return "document.bin";
        }

        // 1. Extraire uniquement le nom de base sans chemin
        String name = rawFilename.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        // 2. Supprimer les retours chariots et caractères de contrôle
        name = name.replaceAll("[\\r\\n\\t\\u0000\\f]", "");

        // 3. Supprimer les séquences de path traversal résiduelles
        name = name.replaceAll("\\.\\.+", ".");

        // 4. Supprimer les caractères interdits pour les systèmes de fichiers et les en-têtes HTTP
        name = name.replaceAll("[\\\\/:*?\"<>|;]", "");

        // 5. Nettoyer les espaces de début et fin
        name = name.trim();

        if (name.isEmpty() || name.equals(".")) {
            return "document.bin";
        }

        // 6. Gérer l'extension et la longueur maximale
        int dotIndex = name.lastIndexOf('.');
        String baseName;
        String extension = "";
        if (dotIndex > 0) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        } else {
            baseName = name;
        }

        if (baseName.length() > MAX_BASE_NAME_LENGTH) {
            baseName = baseName.substring(0, MAX_BASE_NAME_LENGTH);
        }

        return baseName + extension;
    }
}
