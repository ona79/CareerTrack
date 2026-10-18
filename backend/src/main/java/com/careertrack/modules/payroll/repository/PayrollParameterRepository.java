package com.careertrack.modules.payroll.repository;

import com.careertrack.modules.payroll.entity.PayrollParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollParameterRepository extends JpaRepository<PayrollParameter, UUID> {

    @Query("SELECT p FROM PayrollParameter p WHERE p.validFrom <= :date ORDER BY p.validFrom DESC")
    List<PayrollParameter> findAllValidAt(@Param("date") LocalDate date);

    @Query("SELECT p FROM PayrollParameter p WHERE p.key = :key AND p.validFrom <= :date ORDER BY p.validFrom DESC")
    List<PayrollParameter> findByKeyValidAt(@Param("key") String key, @Param("date") LocalDate date);

    List<PayrollParameter> findAllByOrderByKeyAsc();
}
