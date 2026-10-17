package com.careertrack.modules.payroll.entity;

import com.careertrack.common.entity.AuditableEntity;
import com.careertrack.modules.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "gross_salary", precision = 14, scale = 2, nullable = false)
    private BigDecimal grossSalary;

    @Column(name = "net_salary", precision = 14, scale = 2, nullable = false)
    private BigDecimal netSalary;

    @Column(name = "total_deductions", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalDeductions;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private PayslipStatus status = PayslipStatus.GENERATED;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @OneToMany(mappedBy = "payslip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayslipLine> lines = new ArrayList<>();
}
