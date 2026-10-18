package com.careertrack.modules.document.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.document.repository.EmployeeDocumentContentRepository;
import com.careertrack.modules.employee.entity.EmployeeDocumentContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatabaseDocumentStorageService implements DocumentStorageService {

    private final EmployeeDocumentContentRepository contentRepository;

    @Override
    @Transactional
    public void store(UUID documentId, byte[] data) {
        EmployeeDocumentContent content = EmployeeDocumentContent.builder()
                .documentId(documentId)
                .data(data)
                .build();
        contentRepository.save(content);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] load(UUID documentId) {
        return contentRepository.findById(documentId)
                .map(EmployeeDocumentContent::getData)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu binaire introuvable pour le document : " + documentId));
    }

    @Override
    @Transactional
    public void delete(UUID documentId) {
        if (contentRepository.existsById(documentId)) {
            contentRepository.deleteById(documentId);
        }
    }
}
