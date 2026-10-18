package com.careertrack.common.security;

import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("employeeSecurity")
@RequiredArgsConstructor
public class EmployeeSecurityService {

    private final EmployeeRepository employeeRepository;

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private UserPrincipal getCurrentUser() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private boolean hasRole(String role) {
        Authentication auth = getAuthentication();
        if (auth == null) return false;
        String fullRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(fullRole));
    }

    private Optional<Employee> getCurrentEmployee() {
        UserPrincipal user = getCurrentUser();
        if (user == null) {
            return Optional.empty();
        }
        return employeeRepository.findByUserId(user.getId());
    }

    /**
     * Accès au dossier collaborateur nominatif (GET /api/v1/employees/{id}) :
     * - DRH : Oui (tous les employés)
     * - DIRECTION : Non (aucun dossier individuel nominatif complet)
     * - MANAGER : Oui si subordonné direct, ou lui-même
     * - EMPLOYE : Uniquement son propre dossier
     */
    public boolean canAccessEmployee(UUID employeeId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        if (currentEmpOpt.isEmpty()) {
            return false;
        }
        Employee currentEmp = currentEmpOpt.get();

        // Soi-même
        if (currentEmp.getId().equals(employeeId)) {
            return true;
        }

        // Subordonné direct d'un Manager
        if (hasRole("MANAGER")) {
            Optional<Employee> targetEmpOpt = employeeRepository.findById(employeeId);
            if (targetEmpOpt.isPresent()) {
                Employee targetEmp = targetEmpOpt.get();
                return targetEmp.getManager() != null && currentEmp.getId().equals(targetEmp.getManager().getId());
            }
        }

        return false;
    }

    /**
     * Peut voir les données sensibles (CNI, date de naissance, téléphone, adresse).
     * DRH : oui. EMPLOYE : uniquement lui-même. Autres (MANAGER, DIRECTION) : non.
     */
    public boolean canSeeSensitiveData(UUID employeeId) {
        if (hasRole("DRH")) return true;
        return getCurrentEmployee()
                .map(e -> e.getId().equals(employeeId))
                .orElse(false);
    }
}