package com.careertrack.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Demande de connexion avec nom d'utilisateur et mot de passe")
public class LoginRequest {

    @NotBlank(message = "Le nom d'utilisateur ou email est obligatoire")
    @Schema(description = "Nom d'utilisateur ou adresse e-mail", example = "drh.ndiaye")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Schema(description = "Mot de passe de l'utilisateur", example = "password123")
    private String password;
}
