package com.careertrack.modules.employee.controller;

import com.careertrack.common.payload.ApiResponse;
import com.careertrack.modules.employee.dto.SalaryHistoryDto;
import com.careertrack.modules.employee.service.EmployeeHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Historique employé", description = "Historique des avancements et des salaires")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeHistoryController {

    private final EmployeeHistoryService employeeHistoryService;

    @GetMapping("/{id}/salary-history")
    @PreAuthorize("hasRole('DRH')")
    @Operation(summary = "Historique des salaires", description = "Évolutions de salaire d'un employé, de la plus récente à la plus ancienne (DRH uniquement)")
    public ResponseEntity<ApiResponse<List<SalaryHistoryDto>>> getSalaryHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeHistoryService.getSalaryHistory(id)));
    }
}