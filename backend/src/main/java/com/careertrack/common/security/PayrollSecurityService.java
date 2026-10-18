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
@Service("payrollSecurity")
@RequiredArgsConstructor
public class PayrollSecurityService {

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
     * Accès à l'historique des bulletins d'un collaborateur (GET /api/v1/payroll/payslips/employee/{employeeId}) :
     * - DRH : Oui
     * - DIRECTION : Non (pas de données nominatives de salaire)
     * - MANAGER : Non (les managers ne voient pas les bulletins des membres de leur équipe)
     * - EMPLOYE : Uniquement lui-même
     */
    public boolean canAccessEmployeePayslips(UUID employeeId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        return currentEmpOpt.isPresent() && currentEmpOpt.get().getId().equals(employeeId);
    }

    /**
     * Accès au détail d'un bulletin de paie (GET /api/v1/payroll/payslips/{id}) :
     * - DRH : Oui
     * - DIRECTION : Non
     * - MANAGER : Non (sauf si c'est son propre bulletin)
     * - EMPLOYE : Uniquement son propre bulletin
     */
    public boolean canAccessPayslip(UUID payslipId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        Optional<Payslip> payslipOpt = payslipRepository.findById(payslipId);
        if (payslipOpt.isEmpty()) {
            throw new ResourceNotFoundException("Bulletin de paie introuvable : " + payslipId);
        }

        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        return currentEmpOpt.isPresent() && payslipOpt.get().getEmployee().getId().equals(currentEmpOpt.get().getId());
    }
}
