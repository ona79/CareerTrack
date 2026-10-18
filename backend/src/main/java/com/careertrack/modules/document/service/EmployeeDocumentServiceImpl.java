package com.careertrack.modules.document.service;

import com.careertrack.common.exception.BadRequestException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.common.security.UserPrincipal;
import com.careertrack.modules.auth.entity.User;
import com.careertrack.modules.auth.repository.UserRepository;
import com.careertrack.modules.document.dto.DocumentResponseDto;
import com.careertrack.modules.document.repository.EmployeeDocumentRepository;
import com.careertrack.modules.document.util.FileNameSanitizer;
import com.careertrack.modules.document.util.FileSecurityValidator;
import com.careertrack.modules.employee.entity.DocumentType;
import com.careertrack.modules.employee.entity.Employee;
import com.careertrack.modules.employee.entity.EmployeeDocument;
import com.careertrack.modules.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 Mo

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final DocumentStorageService documentStorageService;

    private User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "ANONYME";
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    @Override
    @Transactional
    public DocumentResponseDto uploadDocument(MultipartFile file, UUID employeeId, DocumentType type, String title) {
        if (employeeId == null) {
            throw new BadRequestException("L'identifiant de l'employé est obligatoire.");
        }
        if (type == null) {
            throw new BadRequestException("Le type de document est obligatoire.");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Le libellé du document est obligatoire.");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier téléversé ne peut pas être vide.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Le fichier dépasse la taille maximale autorisée de 5 Mo.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable avec l'identifiant : " + employeeId));

        String sanitizedFilename = FileNameSanitizer.sanitize(file.getOriginalFilename());
        FileSecurityValidator.ValidatedFileInfo fileInfo = FileSecurityValidator.validate(file, sanitizedFilename);

        User currentUser = getCurrentUserEntity();

        EmployeeDocument document = EmployeeDocument.builder()
                .employee(employee)
                .documentType(type)
                .title(title.trim())
                .fileName(sanitizedFilename)
                .fileSize(file.getSize())
                .mimeType(fileInfo.mimeType())
                .uploadedBy(currentUser)
                .build();

        EmployeeDocument savedDoc = employeeDocumentRepository.save(document);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Erreur lors de la lecture des octets du fichier téléversé.");
        }

        documentStorageService.store(savedDoc.getId(), bytes);

        log.info("Document téléversé avec succès : id={}, type={}, employeeId={}, taille={} octets par l'utilisateur '{}' [userId={}]",
                savedDoc.getId(), savedDoc.getDocumentType(), employee.getId(), savedDoc.getFileSize(), getCurrentUsername(), getCurrentUserId());

        return mapToDto(savedDoc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentsByEmployee(UUID employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employé introuvable avec l'identifiant : " + employeeId);
        }

        List<EmployeeDocument> documents = employeeDocumentRepository.findByEmployeeIdOrderByUploadedAtDesc(employeeId);
        return documents.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocument(UUID documentId) {
        EmployeeDocument document = getDocumentMetadata(documentId);
        byte[] data = documentStorageService.load(documentId);

        log.info("Document téléchargé : id={}, type={}, nom='{}' par l'utilisateur '{}' [userId={}]",
                document.getId(), document.getDocumentType(), document.getFileName(), getCurrentUsername(), getCurrentUserId());

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocument getDocumentMetadata(UUID documentId) {
        return employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable avec l'identifiant : " + documentId));
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        EmployeeDocument document = getDocumentMetadata(documentId);

        documentStorageService.delete(documentId);
        employeeDocumentRepository.delete(document);

        log.info("Document supprimé : id={}, type={}, nom='{}' par l'utilisateur '{}' [userId={}]",
                document.getId(), document.getDocumentType(), document.getFileName(), getCurrentUsername(), getCurrentUserId());
    }

    private DocumentResponseDto mapToDto(EmployeeDocument doc) {
        return DocumentResponseDto.builder()
                .id(doc.getId())
                .employeeId(doc.getEmployee() != null ? doc.getEmployee().getId() : null)
                .documentType(doc.getDocumentType())
                .title(doc.getTitle())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .uploadedAt(doc.getUploadedAt())
                .uploadedBy(doc.getUploadedBy() != null ? doc.getUploadedBy().getId() : null)
                .downloadUrl("/api/v1/documents/" + doc.getId() + "/download")
                .build();
    }
}
