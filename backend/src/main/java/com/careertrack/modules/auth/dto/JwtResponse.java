package com.careertrack.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Réponse d'authentification réussie avec Access et Refresh tokens")
public class JwtResponse {

    @Schema(description = "Token d'accès JWT")
    private String accessToken;

    @Schema(description = "Token de rafraîchissement")
    private String refreshToken;

    @Schema(description = "Type de token", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Durée de validité du token d'accès en millisecondes")
    private long expiresIn;

    @Schema(description = "Identifiant unique de l'utilisateur")
    private UUID userId;

    @Schema(description = "Nom d'utilisateur")
    private String username;

    @Schema(description = "Adresse e-mail")
    private String email;

    @Schema(description = "Rôles attribués à l'utilisateur")
    private List<String> roles;

    @Schema(description = "Identifiant de l'employé associé si existant")
    private UUID employeeId;
}
