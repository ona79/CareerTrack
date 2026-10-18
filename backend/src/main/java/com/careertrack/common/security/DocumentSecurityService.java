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

import com.careertrack.modules.document.repository.EmployeeDocumentRepository;
import com.careertrack.modules.employee.entity.DocumentType;
import com.careertrack.modules.employee.entity.EmployeeDocument;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("documentSecurity")
@RequiredArgsConstructor
public class DocumentSecurityService {

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;

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

    /**
     * Envoi d'un document collaborateur (CNI, DIPLOME, AUTRE) :
     * - DRH : Oui pour n'importe quel employé et tout type de document
     * - DIRECTION : Non
     * - MANAGER : Non (sauf si pour lui-même en tant qu'employé hors contrat)
     * - EMPLOYE : Uniquement pour lui-même ET PAS de type CONTRAT (réservé DRH)
     */
    public boolean canUploadDocument(UUID employeeId, DocumentType type) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        // Un collaborateur (ou manager pour lui-même) ne peut pas téléverser de contrat
        if (type == DocumentType.CONTRAT) {
            return false;
        }

        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        return currentEmpOpt.isPresent() && currentEmpOpt.get().getId().equals(employeeId);
    }

    /**
     * Liste des documents d'un collaborateur (métadonnées) :
     * - DRH : Oui (tous les collaborateurs)
     * - DIRECTION : Non (aucun document nominatif)
     * - MANAGER : Non (aucun accès aux pièces d'identité ni aux diplômes de ses subordonnés)
     * - EMPLOYE : Uniquement ses propres documents
     */
    public boolean canListEmployeeDocuments(UUID employeeId) {
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
     * Téléchargement d'un document :
     * - DRH : Oui (tout document)
     * - DIRECTION : Non
     * - MANAGER : Non (aucun accès aux pièces d'identité / diplômes des subordonnés)
     * - EMPLOYE : Uniquement ses propres documents
     */
    public boolean canDownloadDocument(UUID documentId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        Optional<EmployeeDocument> docOpt = employeeDocumentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new ResourceNotFoundException("Document introuvable avec l'identifiant : " + documentId);
        }

        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        return currentEmpOpt.isPresent() && docOpt.get().getEmployee().getId().equals(currentEmpOpt.get().getId());
    }

    /**
     * Suppression d'un document :
     * - DRH : Oui (tout document)
     * - DIRECTION : Non
     * - MANAGER : Non
     * - EMPLOYE : Uniquement ses propres documents qu'il a lui-même déposés (uploaded_by)
     */
    public boolean canDeleteDocument(UUID documentId) {
        if (hasRole("DRH")) {
            return true;
        }
        if (hasRole("DIRECTION")) {
            return false;
        }

        Optional<EmployeeDocument> docOpt = employeeDocumentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new ResourceNotFoundException("Document introuvable avec l'identifiant : " + documentId);
        }

        EmployeeDocument doc = docOpt.get();
        Optional<Employee> currentEmpOpt = getCurrentEmployee();
        if (currentEmpOpt.isEmpty() || !doc.getEmployee().getId().equals(currentEmpOpt.get().getId())) {
            return false;
        }

        // Vérification fine : le document doit avoir été téléversé par l'utilisateur connecté
        UserPrincipal currentUser = getCurrentUser();
        return currentUser != null && doc.getUploadedBy() != null && doc.getUploadedBy().getId().equals(currentUser.getId());
    }
}

