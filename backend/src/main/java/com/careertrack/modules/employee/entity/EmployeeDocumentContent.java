package com.careertrack.modules.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "employee_document_contents")
public class EmployeeDocumentContent {

    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "data", nullable = false)
    private byte[] data;

    public EmployeeDocumentContent() {
    }

    public EmployeeDocumentContent(UUID documentId, byte[] data) {
        this.documentId = documentId;
        this.data = data;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID documentId;
        private byte[] data;

        public Builder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder data(byte[] data) {
            this.data = data;
            return this;
        }

        public EmployeeDocumentContent build() {
            return new EmployeeDocumentContent(documentId, data);
        }
    }
}
