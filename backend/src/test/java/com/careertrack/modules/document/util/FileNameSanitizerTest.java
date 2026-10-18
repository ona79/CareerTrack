package com.careertrack.modules.document.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileNameSanitizerTest {

    @Test
    @DisplayName("Neutralise le path traversal (../../etc/passwd.pdf -> passwd.pdf)")
    void shouldSanitizePathTraversal() {
        String sanitized = FileNameSanitizer.sanitize("../../etc/passwd.pdf");
        assertThat(sanitized).isEqualTo("passwd.pdf");

        String windowsTraversal = FileNameSanitizer.sanitize("..\\..\\windows\\system32\\cmd.exe.pdf");
        assertThat(windowsTraversal).isEqualTo("cmd.exe.pdf");
    }

    @Test
    @DisplayName("Préserve les accents français (Diplôme Licence.pdf)")
    void shouldPreserveAccents() {
        String sanitized = FileNameSanitizer.sanitize("Diplôme Licence.pdf");
        assertThat(sanitized).isEqualTo("Diplôme Licence.pdf");
    }

    @Test
    @DisplayName("Supprime les retours chariots et caractères de contrôle")
    void shouldRemoveCarriageReturnsAndControlChars() {
        String sanitized = FileNameSanitizer.sanitize("mon\r\nnom\tde\0fichier.pdf");
        assertThat(sanitized).isEqualTo("monnomdefichier.pdf");
    }

    @Test
    @DisplayName("Tronque un nom excessivement long tout en conservant l'extension")
    void shouldTruncateVeryLongFilename() {
        String veryLong = "A".repeat(200) + ".pdf";
        String sanitized = FileNameSanitizer.sanitize(veryLong);
        assertThat(sanitized).hasSize(104); // 100 char base + .pdf
        assertThat(sanitized).endsWith(".pdf");
    }

    @Test
    @DisplayName("Gère les noms vides ou nuls avec un nom par défaut")
    void shouldHandleNullOrEmptyFilename() {
        assertThat(FileNameSanitizer.sanitize(null)).isEqualTo("document.bin");
        assertThat(FileNameSanitizer.sanitize("   ")).isEqualTo("document.bin");
        assertThat(FileNameSanitizer.sanitize("/")).isEqualTo("document.bin");
    }
}
