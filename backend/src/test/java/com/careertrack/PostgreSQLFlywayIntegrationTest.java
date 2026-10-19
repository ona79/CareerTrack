package com.careertrack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgreSQLFlywayIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/dev-migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Valide le schéma Flyway et la conformité Hibernate ddl-auto=validate sur PostgreSQL")
    void shouldApplyMigrationsAndValidateEntities() {
        // 1. Vérification que Flyway a exécuté les migrations
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(migrationCount).isGreaterThanOrEqualTo(2);

        // 2. Vérification de la création des tables clés
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class
        );

        assertThat(tables).contains(
                "users", "roles", "user_roles", "refresh_tokens",
                "departments", "positions", "employees", "contracts", "salary_histories", "employee_documents", "employee_document_contents",
                "payslips", "payslip_lines",
                "leave_types", "leave_balances", "leave_requests",
                "evaluation_campaigns", "evaluations", "objectives",
                "trainings", "training_sessions", "training_enrollments",
                "promotion_proposals", "salary_simulations",
                "job_offers", "candidates", "job_applications", "application_interviews",
                "notifications", "company_settings",
                "payroll_parameters", "payroll_tax_brackets", "payroll_trimf_brackets"
        );

        // 3. Vérification des colonnes ajoutées par V12 sur employees
        List<String> employeeColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'employees'",
                String.class
        );
        assertThat(employeeColumns).contains("is_cadre", "marital_status", "dependents_count");

        // 4. Vérification des colonnes ajoutées par V12 sur payslips
        List<String> payslipColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'payslips'",
                String.class
        );
        assertThat(payslipColumns).contains("taxable_gross", "tax_parts", "employer_contributions", "cancelled_by", "cancelled_at");

        // 5. Vérification de la colonne category sur payslip_lines
        List<String> lineColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'payslip_lines'",
                String.class
        );
        assertThat(lineColumns).contains("category");

        // 6. Vérification des données de référence (V2) et seed dev (V3, V13)
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        assertThat(roleCount).isEqualTo(4);

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(userCount).isEqualTo(4);

        Integer paramCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payroll_parameters", Integer.class);
        assertThat(paramCount).isGreaterThanOrEqualTo(20);

        Integer taxBracketCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payroll_tax_brackets", Integer.class);
        assertThat(taxBracketCount).isEqualTo(6);

        Integer trimfBracketCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payroll_trimf_brackets", Integer.class);
        assertThat(trimfBracketCount).isEqualTo(5);

        // Vérification du statut 'VALEUR_A_CONFIRMER' sur tous les paramètres initiaux
        Integer unconfirmedParams = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payroll_parameters WHERE status = 'VALEUR_A_CONFIRMER'", Integer.class);
        assertThat(unconfirmedParams).isEqualTo(paramCount);
    }
}
