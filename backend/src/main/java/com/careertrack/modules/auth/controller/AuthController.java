package com.careertrack.modules.auth.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.auth.dto.JwtResponse;
import com.careertrack.modules.auth.dto.LoginRequest;
import com.careertrack.modules.auth.dto.RefreshTokenRequest;
import com.careertrack.modules.auth.dto.UserDto;
import com.careertrack.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints d'authentification JWT, gestion de session et profil")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur", description = "Authentifie l'utilisateur et retourne un Access Token (15m) et un Refresh Token (7j)")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", jwtResponse));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token d'accès", description = "Génère un nouvel Access Token à partir d'un Refresh Token valide")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        JwtResponse jwtResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token rafraîchi avec succès", jwtResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Invalide le Refresh Token de session")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecté", description = "Retourne les informations et rôles de l'utilisateur authentifié")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }
}
