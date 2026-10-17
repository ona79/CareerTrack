-- ==============================================================================
-- Migration V3 (Profil DEV uniquement) : Comptes de test et jeu de données de démo
-- Mot de passe par défaut pour tous les comptes : password123
-- BCrypt : $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- Ce fichier est chargé uniquement en profil DEV (non déployé en production).
-- ==============================================================================

-- 1. Insertion des 4 Utilisateurs de test
INSERT INTO users (id, username, email, password, enabled, created_by, last_modified_by) VALUES
    ('22222222-2222-2222-2222-222222222201', 'drh.ndiaye', 'drh@careertrack.sn', '$2a$10$xLseePHNhuMtlGSwtCpG8eAw8eS70OSYgMWuCn4VytDiqpyoMAEc2', true, 'SYSTEM', 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222202', 'mgr.sow', 'manager@careertrack.sn', '$2a$10$xLseePHNhuMtlGSwtCpG8eAw8eS70OSYgMWuCn4VytDiqpyoMAEc2', true, 'SYSTEM', 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222203', 'emp.diallo', 'employe@careertrack.sn', '$2a$10$xLseePHNhuMtlGSwtCpG8eAw8eS70OSYgMWuCn4VytDiqpyoMAEc2', true, 'SYSTEM', 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222204', 'dir.fall', 'direction@careertrack.sn', '$2a$10$xLseePHNhuMtlGSwtCpG8eAw8eS70OSYgMWuCn4VytDiqpyoMAEc2', true, 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 2. Association Utilisateurs <-> Rôles
INSERT INTO user_roles (user_id, role_id) VALUES
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101'), -- DRH
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111102'), -- MANAGER
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111103'), -- EMPLOYE
    ('22222222-2222-2222-2222-222222222204', '11111111-1111-1111-1111-111111111104')  -- DIRECTION
ON CONFLICT DO NOTHING;

-- 3. Départements initiaux
INSERT INTO departments (id, code, name, description, created_by, last_modified_by) VALUES
    ('33333333-3333-3333-3333-333333333301', 'DIR-GEN', 'Direction Générale', 'Pilotage stratégique et gouvernance', 'SYSTEM', 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333302', 'DEP-RH', 'Ressources Humaines', 'Gestion du capital humain, paie et talents', 'SYSTEM', 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333303', 'DEP-TECH', 'Ingénierie & Systèmes', 'Développement logiciel, data et infrastructure', 'SYSTEM', 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333304', 'DEP-FIN', 'Finance & Comptabilité', 'Comptabilité générale, trésorerie et fiscalité', 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 4. Postes initiaux avec grilles salariales en FCFA
INSERT INTO positions (id, code, title, department_id, min_salary, max_salary, description, created_by, last_modified_by) VALUES
    ('44444444-4444-4444-4444-444444444401', 'POS-DG', 'Directeur Général', '33333333-3333-3333-3333-333333333301', 2500000, 4500000, 'Direction et représentation de l''entreprise', 'SYSTEM', 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444402', 'POS-DRH', 'Directeur des Ressources Humaines', '33333333-3333-3333-3333-333333333302', 1500000, 2800000, 'Pilotage de la politique RH et des relations sociales', 'SYSTEM', 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444403', 'POS-EM-TECH', 'Engineering Manager', '33333333-3333-3333-3333-333333333303', 1400000, 2500000, 'Management de l''équipe technique et delivery', 'SYSTEM', 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444404', 'POS-DEV-SR', 'Développeur Fullstack Senior', '33333333-3333-3333-3333-333333333303', 800000, 1600000, 'Conception et développement d''applications critiques', 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 5. Employés de démonstration
INSERT INTO employees (id, user_id, employee_number, first_name, last_name, national_id, email, phone, address, birth_date, hire_date, department_id, position_id, manager_id, status, created_by, last_modified_by) VALUES
    ('55555555-5555-5555-5555-555555555501', '22222222-2222-2222-2222-222222222204', 'EMP-001', 'Cheikh', 'Fall', '1 980 1978 00123', 'direction@careertrack.sn', '+221 77 123 45 67', 'Almadies, Dakar', '1978-05-14', '2018-01-01', '33333333-3333-3333-3333-333333333301', '44444444-4444-4444-4444-444444444401', NULL, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('55555555-5555-5555-5555-555555555502', '22222222-2222-2222-2222-222222222201', 'EMP-002', 'Aminata', 'Ndiaye', '2 985 1985 00456', 'drh@careertrack.sn', '+221 77 234 56 78', 'Mermoz, Dakar', '1985-09-22', '2019-03-15', '33333333-3333-3333-3333-333333333302', '44444444-4444-4444-4444-444444444402', '55555555-5555-5555-5555-555555555501', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('55555555-5555-5555-5555-555555555503', '22222222-2222-2222-2222-222222222202', 'EMP-003', 'Mamadou', 'Sow', '1 988 1988 00789', 'manager@careertrack.sn', '+221 77 345 67 89', 'Sacré-Cœur 3, Dakar', '1988-11-30', '2020-02-01', '33333333-3333-3333-3333-333333333303', '44444444-4444-4444-4444-444444444403', '55555555-5555-5555-5555-555555555501', 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('55555555-5555-5555-5555-555555555504', '22222222-2222-2222-2222-222222222203', 'EMP-004', 'Ousmane', 'Diallo', '1 996 1996 00321', 'employe@careertrack.sn', '+221 77 456 78 90', 'Liberté 6, Dakar', '1996-07-18', '2022-06-01', '33333333-3333-3333-3333-333333333303', '44444444-4444-4444-4444-444444444404', '55555555-5555-5555-5555-555555555503', 'ACTIVE', 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 6. Contrats initiaux
INSERT INTO contracts (id, employee_id, contract_type, start_date, end_date, gross_salary, is_active, created_by, last_modified_by) VALUES
    ('66666666-6666-6666-6666-666666666601', '55555555-5555-5555-5555-555555555501', 'CDI', '2018-01-01', NULL, 3500000, true, 'SYSTEM', 'SYSTEM'),
    ('66666666-6666-6666-6666-666666666602', '55555555-5555-5555-5555-555555555502', 'CDI', '2019-03-15', NULL, 2200000, true, 'SYSTEM', 'SYSTEM'),
    ('66666666-6666-6666-6666-666666666603', '55555555-5555-5555-5555-555555555503', 'CDI', '2020-02-01', NULL, 1800000, true, 'SYSTEM', 'SYSTEM'),
    ('66666666-6666-6666-6666-666666666604', '55555555-5555-5555-5555-555555555504', 'CDI', '2022-06-01', NULL, 950000, true, 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- 7. Exemple de Bulletin de Paie pour test de génération PDF
INSERT INTO payslips (id, employee_id, period_month, period_year, gross_salary, net_salary, total_deductions, payment_date, status, created_by, last_modified_by) VALUES
    ('99999999-9999-9999-9999-999999999901', '55555555-5555-5555-5555-555555555504', 9, 2026, 950000, 786800, 163200, '2026-09-30', 'GENERATED', 'SYSTEM', 'SYSTEM')
ON CONFLICT DO NOTHING;

INSERT INTO payslip_lines (id, payslip_id, line_type, label, base_amount, rate, amount) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '99999999-9999-9999-9999-999999999901', 'BASE_SALARY', 'Salaire de Base Mensuel', 950000, 1.0000, 950000),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '99999999-9999-9999-9999-999999999901', 'IPRES_EMPLOYEE', 'Retraite IPRES Salariale (5.6%)', 950000, 0.0560, 53200),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '99999999-9999-9999-9999-999999999901', 'IPM', 'Cotisation Mutuelle Santé (IPM)', 950000, 0.0150, 14250),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '99999999-9999-9999-9999-999999999901', 'TAX_IR', 'Impôt sur le Revenu (IR)', 950000, 0.1008, 95750)
ON CONFLICT (id) DO NOTHING;
