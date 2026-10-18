package com.careertrack.modules.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_trimf_brackets")
@Getter
@Setter
public class PayrollTrimfBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bracket_order", nullable = false)
    private int bracketOrder;

    @Column(name = "min_annual_gross", precision = 14, scale = 2, nullable = false)
    private BigDecimal minAnnualGross;

    @Column(name = "max_annual_gross", precision = 14, scale = 2)
    private BigDecimal maxAnnualGross;

    @Column(name = "annual_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal annualAmount;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "VALEUR_A_CONFIRMER";

    @Column(name = "source", length = 500)
    private String source;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PayrollTrimfBracket() {}

    public PayrollTrimfBracket(UUID id, int bracketOrder, BigDecimal minAnnualGross, BigDecimal maxAnnualGross, BigDecimal annualAmount, String status, String source, LocalDate validFrom, Instant createdAt) {
        this.id = id;
        this.bracketOrder = bracketOrder;
        this.minAnnualGross = minAnnualGross;
        this.maxAnnualGross = maxAnnualGross;
        this.annualAmount = annualAmount;
        this.status = status != null ? status : "VALEUR_A_CONFIRMER";
        this.source = source;
        this.validFrom = validFrom;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private int bracketOrder;
        private BigDecimal minAnnualGross;
        private BigDecimal maxAnnualGross;
        private BigDecimal annualAmount;
        private String status = "VALEUR_A_CONFIRMER";
        private String source;
        private LocalDate validFrom;
        private Instant createdAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder bracketOrder(int bracketOrder) { this.bracketOrder = bracketOrder; return this; }
        public Builder minAnnualGross(BigDecimal minAnnualGross) { this.minAnnualGross = minAnnualGross; return this; }
        public Builder maxAnnualGross(BigDecimal maxAnnualGross) { this.maxAnnualGross = maxAnnualGross; return this; }
        public Builder annualAmount(BigDecimal annualAmount) { this.annualAmount = annualAmount; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder validFrom(LocalDate validFrom) { this.validFrom = validFrom; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public PayrollTrimfBracket build() {
            return new PayrollTrimfBracket(id, bracketOrder, minAnnualGross, maxAnnualGross, annualAmount, status, source, validFrom, createdAt);
        }
    }
}
