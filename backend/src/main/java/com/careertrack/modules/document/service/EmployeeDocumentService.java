package com.careertrack.modules.document.service;

import com.careertrack.modules.document.dto.DocumentResponseDto;
import com.careertrack.modules.employee.entity.DocumentType;
import com.careertrack.modules.employee.entity.EmployeeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface EmployeeDocumentService {

    DocumentResponseDto uploadDocument(MultipartFile file, UUID employeeId, DocumentType type, String title);

    List<DocumentResponseDto> getDocumentsByEmployee(UUID employeeId);

    byte[] downloadDocument(UUID documentId);

    EmployeeDocument getDocumentMetadata(UUID documentId);

    void deleteDocument(UUID documentId);
}
