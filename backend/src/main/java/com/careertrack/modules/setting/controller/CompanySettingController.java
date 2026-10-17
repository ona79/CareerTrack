package com.careertrack.modules.setting.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.setting.dto.CompanySettingDto;
import com.careertrack.modules.setting.service.CompanySettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Paramètres de l'Entreprise", description = "Règles réglementaires, taux de cotisations et plafonds légaux")
public class CompanySettingController {

    private final CompanySettingService settingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")
    @Operation(summary = "Lister les paramètres réglementaires de l'entreprise", description = "Accessible à la DRH et Direction")
    public ResponseEntity<ApiResponse<List<CompanySettingDto>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.success(settingService.getAllSettings()));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Mettre à jour la valeur d'un paramètre d'entreprise", description = "Réservé à la DRH")
    public ResponseEntity<ApiResponse<CompanySettingDto>> updateSetting(
            @PathVariable String key,
            @RequestParam String value) {
        return ResponseEntity.ok(ApiResponse.success("Paramètre mis à jour", settingService.updateSetting(key, value)));
    }
}
