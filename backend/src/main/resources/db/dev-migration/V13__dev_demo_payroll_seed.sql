-- ==============================================================================
-- Migration V13 (Profil DEV & DEMO uniquement) : Configuration cas témoin paie
-- ==============================================================================

-- 1. Configuration de l'employé démo EMP-004 (Ousmane Diallo) comme cadre célibataire sans enfant
UPDATE employees
SET is_cadre = true,
    marital_status = 'CELIBATAIRE',
    dependents_count = 0
WHERE id = '55555555-5555-5555-5555-555555555504' OR employee_number = 'EMP-004';

-- 2. Mise à jour du bulletin de démo 09/2026 avec les valeurs réelles calculées
UPDATE payslips
SET gross_salary = 950000,
    taxable_gross = 905876,
    tax_parts = 1.0,
    total_deductions = 298548,
    net_salary = 651452,
    employer_contributions = 95976,
    status = 'GENERATED',
    payment_date = '2026-09-30'
WHERE id = '99999999-9999-9999-9999-999999999901';

-- 3. Remplacement des lignes de paie par les rubriques complètes salariées et patronales
DELETE FROM payslip_lines WHERE payslip_id = '99999999-9999-9999-9999-999999999901';

INSERT INTO payslip_lines (id, payslip_id, line_type, label, base_amount, rate, amount, category) VALUES
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'BASE_SALARY', 'Salaire de Base Mensuel', 950000, 1.0000, 950000, 'GAIN_BRUT'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'IPRES_RG_EMPLOYEE', 'Retraite IPRES Régime Général (5,6%)', 432000, 0.0560, 24192, 'DEDUCTION_SALARIALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'IPRES_RC_EMPLOYEE', 'Retraite IPRES Cadres (2,4%)', 518000, 0.0240, 12432, 'DEDUCTION_SALARIALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'IPM', 'Cotisation Mutuelle Santé IPM (3%)', 250000, 0.0300, 7500, 'DEDUCTION_SALARIALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'TAX_IR', 'Impôt sur le Revenu (IR)', 830876, 0.3062, 254424, 'DEDUCTION_SALARIALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'IPRES_RG_EMPLOYER', 'Retraite IPRES Régime Général Patronale (8,4%)', 432000, 0.0840, 36288, 'COTISATION_PATRONALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'IPRES_RC_EMPLOYER', 'Retraite IPRES Cadres Patronale (3,6%)', 518000, 0.0360, 18648, 'COTISATION_PATRONALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'CSS_PF', 'CSS Prestations Familiales (7%)', 63000, 0.0700, 4410, 'COTISATION_PATRONALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'CSS_AT', 'CSS Accidents du Travail (1%)', 63000, 0.0100, 630, 'COTISATION_PATRONALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'CFCE', 'Contribution Forfaitaire Employeur CFCE (3%)', 950000, 0.0300, 28500, 'COTISATION_PATRONALE'),
    (gen_random_uuid(), '99999999-9999-9999-9999-999999999901', 'IPM_EMPLOYER', 'Cotisation Mutuelle Santé IPM Patronale (3%)', 250000, 0.0300, 7500, 'COTISATION_PATRONALE');
