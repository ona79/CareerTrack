package com.careertrack.common.security;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import com.careertrack.modules.evaluation.entity.Evaluation;
import com.careertrack.modules.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("evaluationSecurity")
@RequiredArgsConstructor
public class EvaluationSecurityService {

    private final EmployeeRepository employeeRepository;
    private final EvaluationRepository evaluationRepository;

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
     * Accès au détail d'une fiche d'évaluation individuelle (GET /api/v1/evaluations/{id}) :
     * - DRH : Oui
     * - DIRECTION : Non (pas de notation individuelle)
     * - MANAGER : Si c'est l'évaluateur N+1 ou lui-même
     * - EMPLOYE : Uniquement sa propre évaluation
     */
    public boolean canAccessEvaluation(UUID evaluationId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiche d'évaluation introuvable : " + evaluationId));

        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        if (currentEmpOpt.isEmpty()) {
            return false;
        }
        Employee currentEmp = currentEmpOpt.get();

        // Soi-même (évalué)
        if (eval.getEmployee() != null && eval.getEmployee().getId().equals(currentEmp.getId())) {
            return true;
        }

        // Évaluateur (Manager)
        if (hasRole("MANAGER")) {
            if (eval.getEvaluator() != null && eval.getEvaluator().getId().equals(currentEmp.getId())) {
                return true;
            }
            if (eval.getEmployee() != null && eval.getEmployee().getManager() != null
                    && eval.getEmployee().getManager().getId().equals(currentEmp.getId())) {
                return true;
            }
        }

        return false;
    }
}
