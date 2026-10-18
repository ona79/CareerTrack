package com.careertrack.modules.document.dto;

import com.careertrack.modules.employee.entity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Métadonnées d'un document collaborateur (sans le contenu binaire)")
public class DocumentResponseDto {

    @Schema(description = "Identifiant unique du document")
    private UUID id;

    @Schema(description = "Identifiant de l'employé associé")
    private UUID employeeId;

    @Schema(description = "Type de document officiel")
    private DocumentType documentType;

    @Schema(description = "Libellé / Titre du document")
    private String title;

    @Schema(description = "Nom assaini du fichier")
    private String fileName;

    @Schema(description = "Taille du fichier en octets")
    private Long fileSize;

    @Schema(description = "Type MIME déterminé par la signature")
    private String mimeType;

    @Schema(description = "Date et heure de téléversement")
    private Instant uploadedAt;

    @Schema(description = "Identifiant de l'utilisateur ayant déposé le fichier")
    private UUID uploadedBy;

    @Schema(description = "URL relative de téléchargement direct")
    private String downloadUrl;
}
