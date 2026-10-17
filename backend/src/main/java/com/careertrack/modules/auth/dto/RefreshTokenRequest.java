package com.careertrack.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Demande de rafraîchissement du token d'accès")
public class RefreshTokenRequest {

    @NotBlank(message = "Le refresh token est obligatoire")
    @Schema(description = "Token de rafraîchissement (Refresh Token)")
    private String refreshToken;
}
