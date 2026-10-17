package com.careertrack.modules.employee.repository;

import com.careertrack.modules.employee.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    List<Contract> findByEmployeeId(UUID employeeId);
    Optional<Contract> findByEmployeeIdAndActiveTrue(UUID employeeId);
}
