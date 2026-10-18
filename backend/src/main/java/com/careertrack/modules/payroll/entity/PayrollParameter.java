package com.careertrack.modules.payroll.entity;

import com.careertrack.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_parameters")
@Getter
@Setter
public class PayrollParameter extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "param_key", length = 100, nullable = false)
    private String key;

    @Column(name = "param_value", length = 255, nullable = false)
    private String value;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "VALEUR_A_CONFIRMER";

    @Column(name = "source", length = 500)
    private String source;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    public PayrollParameter() {}

    public PayrollParameter(UUID id, String key, String value, String description, String category, String status, String source, LocalDate validFrom) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.description = description;
        this.category = category;
        this.status = status != null ? status : "VALEUR_A_CONFIRMER";
        this.source = source;
        this.validFrom = validFrom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String key;
        private String value;
        private String description;
        private String category;
        private String status = "VALEUR_A_CONFIRMER";
        private String source;
        private LocalDate validFrom;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder key(String key) { this.key = key; return this; }
        public Builder value(String value) { this.value = value; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder validFrom(LocalDate validFrom) { this.validFrom = validFrom; return this; }

        public PayrollParameter build() {
            return new PayrollParameter(id, key, value, description, category, status, source, validFrom);
        }
    }
}
