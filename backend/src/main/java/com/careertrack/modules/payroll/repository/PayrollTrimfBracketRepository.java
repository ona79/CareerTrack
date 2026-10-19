package com.careertrack.modules.payroll.repository;

import com.careertrack.modules.payroll.entity.PayrollTrimfBracket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollTrimfBracketRepository extends JpaRepository<PayrollTrimfBracket, UUID> {

    @Query("SELECT b FROM PayrollTrimfBracket b WHERE b.validFrom <= :date ORDER BY b.bracketOrder ASC")
    List<PayrollTrimfBracket> findActiveBracketsAt(@Param("date") LocalDate date);

    List<PayrollTrimfBracket> findAllByOrderByBracketOrderAsc();
}
