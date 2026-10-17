-- ==============================================================================
-- Migration V1 : Schéma complet de la base de données CareerTrack
-- Base PostgreSQL conforme au modèle relationnel et au Code du Travail sénégalais
-- ==============================================================================

-- Activation de l'extension pgcrypto pour la génération automatique de UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------------------------
-- 1. MODULE AUTHENTIFICATION & UTILISATEURS
-- ------------------------------------------------------------------------------
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 2. MODULE ORGANISATION (DÉPARTEMENTS & POSTES)
-- ------------------------------------------------------------------------------
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    manager_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE TABLE positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    min_salary NUMERIC(14, 2) NOT NULL DEFAULT 0,
    max_salary NUMERIC(14, 2) NOT NULL DEFAULT 0,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

-- ------------------------------------------------------------------------------
-- 3. MODULE EMPLOYÉS & DOSSIER COLLABORATEUR
-- ------------------------------------------------------------------------------
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    employee_number VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    national_id VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    address VARCHAR(255),
    birth_date DATE NOT NULL,
    hire_date DATE NOT NULL,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    position_id UUID REFERENCES positions(id) ON DELETE SET NULL,
    manager_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

-- Ajout de la clé étrangère manager_id sur departments vers employees
ALTER TABLE departments
    ADD CONSTRAINT fk_department_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL;

CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    contract_type VARCHAR(30) NOT NULL, -- CDI, CDD, STAGE, PRESTATION
    start_date DATE NOT NULL,
    end_date DATE,
    gross_salary NUMERIC(14, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    document_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE TABLE salary_histories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    previous_salary NUMERIC(14, 2) NOT NULL,
    new_salary NUMERIC(14, 2) NOT NULL,
    change_reason VARCHAR(255) NOT NULL,
    effective_date DATE NOT NULL,
    approved_by UUID REFERENCES employees(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- CNI, DIPLOME, CONTRAT, ATTESTATION, FICHE_PAIE
    title VARCHAR(200) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 4. MODULE PAIE & BULLETINS DE SALAIRE (PME SÉNÉGALAISE : IPRES, CSS, IPM)
-- ------------------------------------------------------------------------------
CREATE TABLE payslips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    period_month INT NOT NULL,
    period_year INT NOT NULL,
    gross_salary NUMERIC(14, 2) NOT NULL,
    net_salary NUMERIC(14, 2) NOT NULL,
    total_deductions NUMERIC(14, 2) NOT NULL,
    payment_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'GENERATED', -- DRAFT, GENERATED, PAID
    pdf_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    CONSTRAINT uk_payslip_employee_period UNIQUE (employee_id, period_month, period_year)
);

CREATE TABLE payslip_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payslip_id UUID NOT NULL REFERENCES payslips(id) ON DELETE CASCADE,
    line_type VARCHAR(50) NOT NULL, -- BASE_SALARY, BONUS, IPRES_EMPLOYEE, CSS_EMPLOYER, IPM, TAX_IR
    label VARCHAR(200) NOT NULL,
    base_amount NUMERIC(14, 2) NOT NULL,
    rate NUMERIC(6, 4) NOT NULL DEFAULT 0,
    amount NUMERIC(14, 2) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 5. MODULE CONGÉS & ABSENCES
-- ------------------------------------------------------------------------------
CREATE TABLE leave_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    max_days_per_year INT,
    is_paid BOOLEAN NOT NULL DEFAULT TRUE,
    requires_justification BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE leave_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_types(id) ON DELETE CASCADE,
    period_year INT NOT NULL,
    allocated_days NUMERIC(5, 1) NOT NULL DEFAULT 0,
    used_days NUMERIC(5, 1) NOT NULL DEFAULT 0,
    remaining_days NUMERIC(5, 1) NOT NULL DEFAULT 0,
    CONSTRAINT uk_leave_balance_employee_type_year UNIQUE (employee_id, leave_type_id, period_year)
);

CREATE TABLE leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_types(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    requested_days NUMERIC(5, 1) NOT NULL,
    reason TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, APPROVED_N1, APPROVED_DRH, REJECTED, CANCELLED
    manager_comment TEXT,
    approved_by UUID REFERENCES employees(id) ON DELETE SET NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

-- ------------------------------------------------------------------------------
-- 6. MODULE ÉVALUATIONS DE PERFORMANCE & OBJECTIFS SMART
-- ------------------------------------------------------------------------------
CREATE TABLE evaluation_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(150) NOT NULL,
    campaign_year INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, CLOSED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE TABLE evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES evaluation_campaigns(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    evaluator_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SELF_SUBMITTED, MANAGER_EVALUATED, COMPLETED
    self_score NUMERIC(5, 2) DEFAULT 0,
    manager_score NUMERIC(5, 2) DEFAULT 0,
    final_score NUMERIC(5, 2) DEFAULT 0,
    feedback TEXT,
    summary_pdf_url VARCHAR(500),
    interview_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    CONSTRAINT uk_evaluation_campaign_employee UNIQUE (campaign_id, employee_id)
);

CREATE TABLE objectives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id UUID NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL, -- OPERATIONAL, STRATEGIC, BEHAVIORAL
    weight NUMERIC(5, 2) NOT NULL DEFAULT 1.0,
    target_date DATE,
    achievement_rate NUMERIC(5, 2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS'
);

-- ------------------------------------------------------------------------------
-- 7. MODULE FORMATIONS PROFESSIONNELLES
-- ------------------------------------------------------------------------------
CREATE TABLE trainings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    duration_hours INT NOT NULL DEFAULT 0,
    provider VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE TABLE training_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_id UUID NOT NULL REFERENCES trainings(id) ON DELETE CASCADE,
    session_code VARCHAR(50) NOT NULL UNIQUE,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    location VARCHAR(200),
    max_participants INT NOT NULL DEFAULT 15,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED' -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
);

CREATE TABLE training_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED, CONFIRMED, ATTENDED, COMPLETED, DROPPED
    score NUMERIC(5, 2),
    certificate_url VARCHAR(500),
    feedback TEXT,
    CONSTRAINT uk_session_employee UNIQUE (session_id, employee_id)
);

-- ------------------------------------------------------------------------------
-- 8. MODULE PROMOTIONS & SIMULATIONS SALARIALES
-- ------------------------------------------------------------------------------
CREATE TABLE promotion_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    proposed_by UUID REFERENCES employees(id) ON DELETE SET NULL,
    target_position_id UUID REFERENCES positions(id) ON DELETE SET NULL,
    current_salary NUMERIC(14, 2) NOT NULL,
    proposed_salary NUMERIC(14, 2) NOT NULL,
    justification TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, APPROVED, REJECTED
    validated_by UUID REFERENCES employees(id) ON DELETE SET NULL,
    validated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE TABLE salary_simulations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(150) NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    average_increase_percentage NUMERIC(5, 2) NOT NULL,
    projected_payroll_impact NUMERIC(14, 2) NOT NULL,
    simulated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 9. MODULE RECRUTEMENT & PIPELINE KANBAN
-- ------------------------------------------------------------------------------
CREATE TABLE job_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    position_id UUID REFERENCES positions(id) ON DELETE SET NULL,
    contract_type VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- DRAFT, OPEN, CLOSED, ARCHIVED
    open_positions INT NOT NULL DEFAULT 1,
    closing_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE TABLE candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    cv_url VARCHAR(500),
    linkedin_url VARCHAR(255),
    source VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_offer_id UUID NOT NULL REFERENCES job_offers(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    application_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'RECU', -- RECU, PRESELECTIONNE, ENTRETIEN, OFFRE, EMBAUCHE, REFUSE
    current_step VARCHAR(50) DEFAULT 'INITIAL_REVIEW',
    rating INT DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    CONSTRAINT uk_job_candidate UNIQUE (job_offer_id, candidate_id)
);

CREATE TABLE application_interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    interviewer_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    interview_date TIMESTAMP WITH TIME ZONE NOT NULL,
    interview_type VARCHAR(50) NOT NULL, -- RH, TECHNIQUE, DIRECTION
    notes TEXT,
    result VARCHAR(30) -- FAVORABLE, DEFAVORABLE, EN_ATTENTE
);

-- ------------------------------------------------------------------------------
-- 10. MODULE NOTIFICATIONS (IN-APP & EMAIL)
-- ------------------------------------------------------------------------------
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP', -- IN_APP, EMAIL, BOTH
    notification_type VARCHAR(50) NOT NULL,        -- LEAVE_STATUS, EVALUATION, RECRUITMENT, TRAINING
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,
    metadata TEXT
);

-- ------------------------------------------------------------------------------
-- 11. PARAMÈTRES RÉGLEMENTAIRES & D'ENTREPRISE CONFIGURABLES
-- ------------------------------------------------------------------------------
CREATE TABLE company_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- INDEX STRATÉGIQUES POUR LES PERFORMANCES
-- ------------------------------------------------------------------------------
CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_contracts_employee ON contracts(employee_id);
CREATE INDEX idx_payslips_employee ON payslips(employee_id);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_evaluations_employee ON evaluations(employee_id);
CREATE INDEX idx_job_applications_status ON job_applications(status);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, is_read);
