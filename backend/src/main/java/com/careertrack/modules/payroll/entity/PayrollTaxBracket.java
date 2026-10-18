package com.careertrack.modules.payroll.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_tax_brackets")
@Getter
@Setter
public class PayrollTaxBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bracket_order", nullable = false)
    private int bracketOrder;

    @Column(name = "min_annual_income", precision = 14, scale = 2, nullable = false)
    private BigDecimal minAnnualIncome;

    @Column(name = "max_annual_income", precision = 14, scale = 2)
    private BigDecimal maxAnnualIncome;

    @Column(name = "rate", precision = 6, scale = 4, nullable = false)
    private BigDecimal rate;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "VALEUR_A_CONFIRMER";

    @Column(name = "source", length = 500)
    private String source;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PayrollTaxBracket() {}

    public PayrollTaxBracket(UUID id, int bracketOrder, BigDecimal minAnnualIncome, BigDecimal maxAnnualIncome, BigDecimal rate, String status, String source, LocalDate validFrom, Instant createdAt) {
        this.id = id;
        this.bracketOrder = bracketOrder;
        this.minAnnualIncome = minAnnualIncome;
        this.maxAnnualIncome = maxAnnualIncome;
        this.rate = rate;
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
        private BigDecimal minAnnualIncome;
        private BigDecimal maxAnnualIncome;
        private BigDecimal rate;
        private String status = "VALEUR_A_CONFIRMER";
        private String source;
        private LocalDate validFrom;
        private Instant createdAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder bracketOrder(int bracketOrder) { this.bracketOrder = bracketOrder; return this; }
        public Builder minAnnualIncome(BigDecimal minAnnualIncome) { this.minAnnualIncome = minAnnualIncome; return this; }
        public Builder maxAnnualIncome(BigDecimal maxAnnualIncome) { this.maxAnnualIncome = maxAnnualIncome; return this; }
        public Builder rate(BigDecimal rate) { this.rate = rate; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder validFrom(LocalDate validFrom) { this.validFrom = validFrom; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public PayrollTaxBracket build() {
            return new PayrollTaxBracket(id, bracketOrder, minAnnualIncome, maxAnnualIncome, rate, status, source, validFrom, createdAt);
        }
    }
}
