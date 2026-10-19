-- ==============================================================================
-- Migration V12 : Refonte du moteur de paie réelle et données fiscales des employés
-- Statut légal : VALEUR À CONFIRMER (source à vérifier sur chaque paramètre)
-- ==============================================================================

-- 1. Enrichissement de la table employees (champs nécessaires au calcul fiscal/social)
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS is_cadre BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS marital_status VARCHAR(30) NOT NULL DEFAULT 'CELIBATAIRE',
    ADD COLUMN IF NOT EXISTS dependents_count INT NOT NULL DEFAULT 0;

-- 2. Dédoublonnage préventif des bulletins existants et garantie de la contrainte unique
DELETE FROM payslips a USING payslips b
WHERE a.created_at < b.created_at
  AND a.employee_id = b.employee_id
  AND a.period_year = b.period_year
  AND a.period_month = b.period_month;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_payslips_employee_period'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_payslip_employee_period'
    ) THEN
        ALTER TABLE payslips ADD CONSTRAINT uq_payslips_employee_period UNIQUE (employee_id, period_year, period_month);
    END IF;
END $$;

-- 3. Enrichissement de la table payslips
ALTER TABLE payslips
    ADD COLUMN IF NOT EXISTS taxable_gross NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_parts NUMERIC(4, 2) NOT NULL DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS employer_contributions NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;

-- 4. Enrichissement de la table payslip_lines
ALTER TABLE payslip_lines
    ADD COLUMN IF NOT EXISTS category VARCHAR(30) NOT NULL DEFAULT 'DEDUCTION_SALARIALE';

-- Rétro-compatibilité des lignes existantes
UPDATE payslip_lines SET category = 'GAIN_BRUT' WHERE line_type IN ('BASE_SALARY', 'BONUS');
UPDATE payslip_lines SET category = 'COTISATION_PATRONALE' WHERE line_type LIKE '%EMPLOYER%' OR line_type IN ('CSS_PF', 'CSS_AT', 'CFCE');
UPDATE payslip_lines SET category = 'DEDUCTION_SALARIALE' WHERE line_type IN ('IPRES_EMPLOYEE', 'IPRES_RC_EMPLOYEE', 'IPM', 'TAX_IR', 'TRIMF');

-- 5. Nettoyage des anciennes clés de paie dans company_settings (source unique dans payroll_parameters)
DELETE FROM company_settings WHERE setting_key IN ('IPRES_EMPLOYEE_RATE', 'IPRES_EMPLOYER_RATE', 'CSS_EMPLOYER_RATE');

-- 6. Création de la table des paramètres de paie (payroll_parameters)
CREATE TABLE IF NOT EXISTS payroll_parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    param_key VARCHAR(100) NOT NULL,
    param_value VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VALEUR_A_CONFIRMER',
    source VARCHAR(500),
    valid_from DATE NOT NULL DEFAULT '2020-01-01',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    CONSTRAINT uq_payroll_parameter_key_valid_from UNIQUE (param_key, valid_from)
);

CREATE INDEX IF NOT EXISTS idx_payroll_parameters_key_valid ON payroll_parameters(param_key, valid_from);

-- 7. Création de la table du barème progressif de l'Impôt sur le Revenu (payroll_tax_brackets)
CREATE TABLE IF NOT EXISTS payroll_tax_brackets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bracket_order INT NOT NULL,
    min_annual_income NUMERIC(14, 2) NOT NULL,
    max_annual_income NUMERIC(14, 2),
    rate NUMERIC(6, 4) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VALEUR_A_CONFIRMER',
    source VARCHAR(500),
    valid_from DATE NOT NULL DEFAULT '2020-01-01',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payroll_tax_bracket_order_valid_from UNIQUE (bracket_order, valid_from)
);

CREATE INDEX IF NOT EXISTS idx_payroll_tax_brackets_valid ON payroll_tax_brackets(valid_from, bracket_order);

-- 8. Création de la table du barème de la TRIMF (payroll_trimf_brackets)
CREATE TABLE IF NOT EXISTS payroll_trimf_brackets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bracket_order INT NOT NULL,
    min_annual_gross NUMERIC(14, 2) NOT NULL,
    max_annual_gross NUMERIC(14, 2),
    annual_amount NUMERIC(14, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VALEUR_A_CONFIRMER',
    source VARCHAR(500),
    valid_from DATE NOT NULL DEFAULT '2020-01-01',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payroll_trimf_bracket_order_valid_from UNIQUE (bracket_order, valid_from)
);

CREATE INDEX IF NOT EXISTS idx_payroll_trimf_brackets_valid ON payroll_trimf_brackets(valid_from, bracket_order);

-- 9. Données initiales des paramètres de paie (Toutes marquées VALEUR_A_CONFIRMER)
INSERT INTO payroll_parameters (param_key, param_value, description, category, status, source, valid_from) VALUES
    ('IPRES_RG_EMPLOYEE_RATE', '0.056', 'Taux salarial IPRES régime général', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPRES_RG_EMPLOYER_RATE', '0.084', 'Taux patronal IPRES régime général', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPRES_RG_CEILING', '432000', 'Plafond mensuel assiette IPRES régime général (FCFA)', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPRES_RC_EMPLOYEE_RATE', '0.024', 'Taux salarial IPRES régime complémentaire cadres', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPRES_RC_EMPLOYER_RATE', '0.036', 'Taux patronal IPRES régime complémentaire cadres', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPRES_RC_CEILING', '1296000', 'Plafond mensuel assiette IPRES complémentaire cadres (FCFA)', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('PAYROLL_IPRES_RC_CADRES_ONLY', 'true', 'Application de l''IPRES complémentaire réservée aux cadres', 'REGLE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('CSS_PF_EMPLOYER_RATE', '0.070', 'Taux patronal CSS Prestations Familiales', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('CSS_AT_EMPLOYER_RATE', '0.010', 'Taux patronal CSS Accidents du Travail / MP', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('CSS_CEILING', '63000', 'Plafond mensuel assiette cotisations CSS (FCFA)', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('CFCE_EMPLOYER_RATE', '0.030', 'Taux patronal CFCE sur brut total', 'FISCALITE_PATRONALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPM_EMPLOYEE_RATE', '0.030', 'Taux salarial IPM Mutuelle de santé', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPM_EMPLOYER_RATE', '0.030', 'Taux patronal IPM Mutuelle de santé', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('IPM_CEILING', '250000', 'Plafond mensuel assiette IPM (FCFA)', 'COTISATION_SOCIALE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('PAYROLL_IPM_DEDUCTIBLE_FROM_IR', 'true', 'Déductibilité de la cotisation IPM salariale du brut imposable', 'REGLE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('FRAIS_PRO_RATE', '0.30', 'Taux de déduction forfaitaire pour frais professionnels', 'FISCALITE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('FRAIS_PRO_MONTHLY_CEILING', '75000', 'Plafond mensuel de déduction des frais professionnels (FCFA)', 'FISCALITE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('TAX_PARTS_SINGLE', '1.0', 'Parts fiscales de base pour célibataire/divorcé/veuf', 'FISCALITE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('TAX_PARTS_MARRIED', '1.5', 'Parts fiscales de base pour marié', 'FISCALITE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('TAX_PARTS_PER_DEPENDENT', '0.5', 'Majoration de part fiscale par enfant à charge', 'FISCALITE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    ('TAX_PARTS_MAX', '5.0', 'Nombre maximal de parts fiscales', 'FISCALITE', 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01')
ON CONFLICT (param_key, valid_from) DO NOTHING;

-- 10. Tranches annuelles de l'Impôt sur le Revenu (IR)
INSERT INTO payroll_tax_brackets (bracket_order, min_annual_income, max_annual_income, rate, status, source, valid_from) VALUES
    (1, 0, 630000, 0.0000, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (2, 630000, 1500000, 0.2000, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (3, 1500000, 4000000, 0.3000, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (4, 4000000, 8000000, 0.3500, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (5, 8000000, 13500000, 0.3700, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (6, 13500000, NULL, 0.4000, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01')
ON CONFLICT (bracket_order, valid_from) DO NOTHING;

-- 11. Tranches annuelles de la TRIMF
INSERT INTO payroll_trimf_brackets (bracket_order, min_annual_gross, max_annual_gross, annual_amount, status, source, valid_from) VALUES
    (1, 0, 600000, 900, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (2, 600000, 1200000, 3600, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (3, 1200000, 2400000, 4800, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (4, 2400000, 3600000, 12000, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01'),
    (5, 3600000, NULL, 36000, 'VALEUR_A_CONFIRMER', 'source à vérifier', '2020-01-01')
ON CONFLICT (bracket_order, valid_from) DO NOTHING;
