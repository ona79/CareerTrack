-- ==============================================================================
-- Migration V2 : Données de référence de base (Production & Développement)
-- Contient UNIQUEMENT les rôles, types de congés légaux et paramètres initiaux.
-- Ne contient AUCUN compte utilisateur ni mot de passe de test.
-- ==============================================================================

-- 1. Rôles applicatifs obligatoires
INSERT INTO roles (id, name, description) VALUES
    ('11111111-1111-1111-1111-111111111101', 'ROLE_DRH', 'Direction des Ressources Humaines - Accès complet'),
    ('11111111-1111-1111-1111-111111111102', 'ROLE_MANAGER', 'Manager d''équipe - Validation N+1 et évaluations'),
    ('11111111-1111-1111-1111-111111111103', 'ROLE_EMPLOYE', 'Collaborateur - Espace personnel et demandes'),
    ('11111111-1111-1111-1111-111111111104', 'ROLE_DIRECTION', 'Direction Générale - Consultation et tableaux de bord');

-- 2. Types de congés légaux (Code du Travail du Sénégal)
INSERT INTO leave_types (id, code, name, max_days_per_year, is_paid, requires_justification) VALUES
    ('77777777-7777-7777-7777-777777777701', 'CONGE_PAYE', 'Congé Payé Annuel', 24, true, false),
    ('77777777-7777-7777-7777-777777777702', 'CONGE_MATERNITE', 'Congé de Maternité (14 semaines)', 98, true, true),
    ('77777777-7777-7777-7777-777777777703', 'CONGE_MALADIE', 'Congé Maladie', 30, true, true),
    ('77777777-7777-7777-7777-777777777704', 'EVENEMENT_FAMILIAL', 'Permissions Exceptionnelles', 10, true, true),
    ('77777777-7777-7777-7777-777777777705', 'CONGE_SANS_SOLDE', 'Congé sans solde', 90, false, false);

-- 3. Paramètres réglementaires et d'entreprise par défaut
INSERT INTO company_settings (id, setting_key, setting_value, description) VALUES
    ('88888888-8888-8888-8888-888888888801', 'LEAVE_DAYS_PER_MONTH', '2.0', 'Nombre de jours ouvrables de congé acquis par mois de travail effectif'),
    ('88888888-8888-8888-8888-888888888802', 'MAX_CDD_MONTHS', '24', 'Durée maximale légale cumulée pour les contrats à durée déterminée (CDD)'),
    ('88888888-8888-8888-8888-888888888803', 'CURRENCY_CODE', 'XOF', 'Code devise standard (Franc CFA BCEAO)'),
    ('88888888-8888-8888-8888-888888888804', 'CURRENCY_SYMBOL', 'FCFA', 'Symbole d''affichage de la monnaie'),
    ('88888888-8888-8888-8888-888888888805', 'IPRES_EMPLOYEE_RATE', '0.056', 'Taux de cotisation retraite IPRES salariale'),
    ('88888888-8888-8888-8888-888888888806', 'IPRES_EMPLOYER_RATE', '0.084', 'Taux de cotisation retraite IPRES patronale'),
    ('88888888-8888-8888-8888-888888888807', 'CSS_EMPLOYER_RATE', '0.070', 'Taux de cotisation accident du travail / prestations familiales CSS');
