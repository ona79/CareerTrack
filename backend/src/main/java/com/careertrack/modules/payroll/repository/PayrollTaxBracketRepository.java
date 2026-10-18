package com.careertrack.modules.payroll.repository;

import com.careertrack.modules.payroll.entity.PayrollTaxBracket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollTaxBracketRepository extends JpaRepository<PayrollTaxBracket, UUID> {

    @Query("SELECT b FROM PayrollTaxBracket b WHERE b.validFrom <= :date ORDER BY b.bracketOrder ASC")
    List<PayrollTaxBracket> findActiveBracketsAt(@Param("date") LocalDate date);

    List<PayrollTaxBracket> findAllByOrderByBracketOrderAsc();
}
