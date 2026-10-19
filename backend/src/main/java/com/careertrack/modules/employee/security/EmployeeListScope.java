package com.careertrack.modules.employee.security;

import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmployeeListScope {

    private final EmployeeRepository employeeRepository;

    /**
     * Périmètre de la liste des employés.
     * Retourne null pour le DRH et la Direction (aucune restriction),
     * sinon l'identifiant du manager dont on affiche l'équipe.
     */
    public UUID managerScopeOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Non authentifié");
        }
        if (hasRole(auth, "ROLE_DRH") || hasRole(auth, "ROLE_DIRECTION")) {
            return null;
        }
        if (hasRole(auth, "ROLE_MANAGER") && auth.getPrincipal() instanceof UserPrincipal user) {
            return employeeRepository.findByUserId(user.getId())
                    .map(e -> e.getId())
                    .orElseThrow(() -> new AccessDeniedException("Aucun dossier employé lié à ce compte"));
        }
        throw new AccessDeniedException("Accès à la liste refusé");
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}