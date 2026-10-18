package com.careertrack.modules.employee.entity;

import com.careertrack.common.entity.AuditableEntity;
import com.careertrack.modules.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "employee_number", length = 50, nullable = false, unique = true)
    private String employeeNumber;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "national_id", length = 50, nullable = false, unique = true)
    private String nationalId;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Employee> subordinates = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "is_cadre", nullable = false)
    @Builder.Default
    private boolean isCadre = false;

    @Column(name = "marital_status", length = 30, nullable = false)
    @Builder.Default
    private String maritalStatus = "CELIBATAIRE";

    @Column(name = "dependents_count", nullable = false)
    @Builder.Default
    private int dependentsCount = 0;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
