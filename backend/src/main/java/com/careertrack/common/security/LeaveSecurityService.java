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
@Service("leaveSecurity")
@RequiredArgsConstructor
public class LeaveSecurityService {

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
     * Accès aux demandes d'un employé (GET /api/v1/leaves/employee/{employeeId}) :
     * - DRH : Oui
     * - DIRECTION : Non
     * - MANAGER : Subordonné direct ou lui-même
     * - EMPLOYE : Uniquement lui-même
     */
    public boolean canAccessEmployeeLeaves(UUID employeeId) {
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

        if (currentEmp.getId().equals(employeeId)) {
            return true;
        }

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
     * Accès aux soldes de congés d'un employé (GET /api/v1/leaves/balances/{employeeId}) :
     * - DRH : Oui
     * - DIRECTION : Non
     * - MANAGER : Subordonné direct ou lui-même
     * - EMPLOYE : Uniquement lui-même
     */
    public boolean canAccessEmployeeBalances(UUID employeeId) {
        return canAccessEmployeeLeaves(employeeId);
    }
}
