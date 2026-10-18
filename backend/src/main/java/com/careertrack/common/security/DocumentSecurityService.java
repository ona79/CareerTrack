package com.careertrack.common.security;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.payroll.entity.Payslip;
import com.careertrack.modules.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("documentSecurity")
@RequiredArgsConstructor
public class DocumentSecurityService {

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;

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
     * Accès aux attestations de travail :
     * - DRH : Oui (tous)
     * - DIRECTION : Non (aucun document individuel)
     * - MANAGER : Uniquement ses subordonnés directs
     * - EMPLOYE : Uniquement lui-même
     */
    public boolean canAccessWorkCertificate(UUID employeeId) {
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
     * Accès aux fiches de paie :
     * - DRH : Oui (toutes)
     * - DIRECTION : Non
     * - MANAGER : Non (jamais aux bulletins des salariés)
     * - EMPLOYE : Uniquement ses propres fiches de paie
     */
    public boolean canAccessPayslip(UUID payslipId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION") || hasRole("MANAGER")) {
            // Note : si le manager télécharge sa propre fiche de paie en tant qu'employé
            Optional<Payslip> payslipOpt = payslipRepository.findById(payslipId);
            if (payslipOpt.isEmpty()) {
                throw new ResourceNotFoundException("Bulletin de paie introuvable");
            }
            Optional<Employee> currentEmpOpt = getCurrentEmployee();
            return currentEmpOpt.isPresent() && payslipOpt.get().getEmployee().getId().equals(currentEmpOpt.get().getId());
        }

        Optional<Payslip> payslipOpt = payslipRepository.findById(payslipId);
        if (payslipOpt.isEmpty()) {
            throw new ResourceNotFoundException("Bulletin de paie introuvable");
        }
        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        return currentEmpOpt.isPresent() && payslipOpt.get().getEmployee().getId().equals(currentEmpOpt.get().getId());
    }

    /**
     * Accès aux contrats de travail :
     * - DRH : Oui
     * - DIRECTION : Non
     * - MANAGER : Non
     * - EMPLOYE : Uniquement son propre contrat
     */
    public boolean canAccessContract(UUID employeeId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        return currentEmpOpt.isPresent() && currentEmpOpt.get().getId().equals(employeeId);
    }
}
