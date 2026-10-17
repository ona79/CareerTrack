package com.careertrack.modules.employee.repository;

import com.careertrack.modules.employee.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    Optional<Position> findByCode(String code);
    List<Position> findByDepartmentId(UUID departmentId);
}
