package com.careertrack.modules.leave.entity;

import com.careertrack.modules.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "leave_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "allocated_days", precision = 5, scale = 1, nullable = false)
    @Builder.Default
    private BigDecimal allocatedDays = BigDecimal.ZERO;

    @Column(name = "used_days", precision = 5, scale = 1, nullable = false)
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(name = "remaining_days", precision = 5, scale = 1, nullable = false)
    @Builder.Default
    private BigDecimal remainingDays = BigDecimal.ZERO;
}
