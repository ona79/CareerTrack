package com.careertrack.modules.employee.dto;

import java.util.UUID;

public record ReferenceItemDto(UUID id, String label, UUID parentId) {
    public String getName() {
        return label;
    }

    public String getTitle() {
        return label;
    }
}