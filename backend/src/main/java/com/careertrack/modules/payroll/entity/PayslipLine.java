package com.careertrack.modules.payroll.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payslip_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayslipLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id", nullable = false)
    private Payslip payslip;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", length = 50, nullable = false)
    private PayslipLineType lineType;

    @Column(name = "label", length = 200, nullable = false)
    private String label;

    @Column(name = "base_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal baseAmount;

    @Column(name = "rate", precision = 6, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    @Builder.Default
    private PayslipLineCategory category = PayslipLineCategory.DEDUCTION_SALARIALE;
}
