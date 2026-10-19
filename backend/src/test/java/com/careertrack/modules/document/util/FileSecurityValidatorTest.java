package com.careertrack.modules.document.util;

import com.careertrack.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSecurityValidatorTest {

    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 sample content".getBytes();
    private static final byte[] VALID_PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    private static final byte[] VALID_JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @Test
    @DisplayName("Valide avec succès un PDF authentique")
    void shouldValidateAuthenticPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "mon_contrat.pdf", "application/pdf", VALID_PDF_BYTES);
        FileSecurityValidator.ValidatedFileInfo info = FileSecurityValidator.validate(file, "mon_contrat.pdf");

        assertThat(info.mimeType()).isEqualTo("application/pdf");
        assertThat(info.normalizedExtension()).isEqualTo("pdf");
    }

    @Test
    @DisplayName("Valide avec succès une image PNG authentique")
    void shouldValidateAuthenticPng() {
        MockMultipartFile file = new MockMultipartFile("file", "cni.png", "image/png", VALID_PNG_BYTES);
        FileSecurityValidator.ValidatedFileInfo info = FileSecurityValidator.validate(file, "cni.png");

        assertThat(info.mimeType()).isEqualTo("image/png");
        assertThat(info.normalizedExtension()).isEqualTo("png");
    }

    @Test
    @DisplayName("Valide avec succès une image JPEG authentique")
    void shouldValidateAuthenticJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "diplome.jpg", "image/jpeg", VALID_JPEG_BYTES);
        FileSecurityValidator.ValidatedFileInfo info = FileSecurityValidator.validate(file, "diplome.jpg");

        assertThat(info.mimeType()).isEqualTo("image/jpeg");
        assertThat(info.normalizedExtension()).isEqualTo("jpeg");
    }

    @Test
    @DisplayName("Rejette un fichier vide (400)")
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> FileSecurityValidator.validate(file, "vide.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Le fichier téléversé ne peut pas être vide");
    }

    @Test
    @DisplayName("Rejette un faux PDF qui est en réalité un fichier texte (400)")
    void shouldRejectFakePdfTextContent() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", "Hello World! Ceci n'est pas un pdf".getBytes());
        assertThatThrownBy(() -> FileSecurityValidator.validate(file, "fake.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Format de fichier non supporté");
    }

    @Test
    @DisplayName("Rejette un PNG renommé en .pdf (incohérence signature/extension) (400)")
    void shouldRejectPngRenamedToPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "malicieux.pdf", "application/pdf", VALID_PNG_BYTES);
        assertThatThrownBy(() -> FileSecurityValidator.validate(file, "malicieux.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ne correspond pas à son format réel (PNG détecté)");
    }

    @Test
    @DisplayName("Rejette un PDF renommé en .png (400)")
    void shouldRejectPdfRenamedToPng() {
        MockMultipartFile file = new MockMultipartFile("file", "fichier.png", "image/png", VALID_PDF_BYTES);
        assertThatThrownBy(() -> FileSecurityValidator.validate(file, "fichier.png"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ne correspond pas à son format réel (PDF détecté)");
    }
}
