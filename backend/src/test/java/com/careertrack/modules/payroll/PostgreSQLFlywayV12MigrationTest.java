package com.careertrack.modules.payroll;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSQLFlywayV12MigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    @DisplayName("Prouve que la migration V12 s'applique avec succès sur une base contenant déjà des bulletins et des doublons")
    void shouldApplyV12MigrationOnDatabaseWithExistingPayslipsAndDuplicates() {
        // 1. Initialisation de la source de données JDBC directe vers le container
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(postgres.getDriverClassName());
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 2. Exécution de Flyway jusqu'à V11 (schéma initial + storage documents)
        Flyway flywayV11 = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("11")
                .load();
        flywayV11.migrate();

        // 3. Insertion d'un employé de test pour V11
        UUID employeeId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO employees (id, employee_number, first_name, last_name, national_id, email, birth_date, hire_date, status)
            VALUES (?, 'EMP-TEST-MIG', 'Moussa', 'Ndiaye', '1999999999', 'moussa.test@careertrack.sn', '1990-01-01', '2022-01-01', 'ACTIVE')
        """, employeeId);

        // Dans V1, la contrainte uk_payslip_employee_period existe sur (employee_id, period_month, period_year).
        // Supprimons-la temporairement pour simuler une base ayant accumulé des doublons ou ayant eu la contrainte altérée
        jdbcTemplate.execute("ALTER TABLE payslips DROP CONSTRAINT IF EXISTS uk_payslip_employee_period");

        // Insérons 2 bulletins pour le même employé, même mois (09), même année (2026) avec dates de création différentes
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO payslips (id, employee_id, period_month, period_year, gross_salary, net_salary, total_deductions, payment_date, status, created_at)
            VALUES (?, ?, 9, 2026, 950000, 750000, 200000, '2026-09-30', 'GENERATED', '2026-09-25 10:00:00+00')
        """, p1, employeeId);

        jdbcTemplate.update("""
            INSERT INTO payslips (id, employee_id, period_month, period_year, gross_salary, net_salary, total_deductions, payment_date, status, created_at)
            VALUES (?, ?, 9, 2026, 950000, 760000, 190000, '2026-09-30', 'GENERATED', '2026-09-26 12:00:00+00')
        """, p2, employeeId);

        Integer duplicateCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payslips WHERE employee_id = ? AND period_month = 9 AND period_year = 2026",
                Integer.class, employeeId);
        assertThat(duplicateCountBefore).isEqualTo(2);

        // 4. Migration Flyway vers V12 (et au-delà)
        Flyway flywayV12 = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

        // Prouve que la migration V12 s'exécute sans erreur malgré les doublons
        assertThatCode(flywayV12::migrate).doesNotThrowAnyException();

        // 5. Vérification que le dédoublonnage a conservé le bulletin le plus récent (p2)
        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payslips WHERE employee_id = ? AND period_month = 9 AND period_year = 2026",
                Integer.class, employeeId);
        assertThat(countAfter).isEqualTo(1);

        UUID remainingId = jdbcTemplate.queryForObject(
                "SELECT id FROM payslips WHERE employee_id = ? AND period_month = 9 AND period_year = 2026",
                UUID.class, employeeId);
        assertThat(remainingId).isEqualTo(p2);

        // 6. Vérification que les nouvelles colonnes V12 existent bien sur payslips et employees
        Boolean isCadre = jdbcTemplate.queryForObject(
                "SELECT is_cadre FROM employees WHERE id = ?", Boolean.class, employeeId);
        assertThat(isCadre).isFalse();

        // 7. Vérification que la contrainte unique empêche désormais l'insertion d'un nouveau doublon
        assertThatCode(() -> {
            jdbcTemplate.update("""
                INSERT INTO payslips (id, employee_id, period_month, period_year, gross_salary, net_salary, total_deductions, payment_date, status)
                VALUES (gen_random_uuid(), ?, 9, 2026, 950000, 750000, 200000, '2026-09-30', 'GENERATED')
            """, employeeId);
        }).hasMessageContaining("violates unique constraint");
    }
}
