package com.careertrack.modules.document.repository;

import com.careertrack.modules.employee.entity.EmployeeDocumentContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmployeeDocumentContentRepository extends JpaRepository<EmployeeDocumentContent, UUID> {
}
