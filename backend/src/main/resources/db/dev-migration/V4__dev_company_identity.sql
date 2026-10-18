-- ==============================================================================
-- Migration V4 (Profils DEV et DEMO uniquement) : identité d'entreprise FICTIVE
-- utilisée par l'en-tête des PDF (common/pdf).
-- Ces valeurs ne sont PAS des données de référence : elles ne sont jamais chargées
-- en production (db/dev-migration n'est pas dans les locations Flyway du profil prod).
-- En production, la DRH renseigne COMPANY_NAME, COMPANY_CITY, COMPANY_NINEA et
-- COMPANY_RCCM via les paramètres ; une valeur absente fait disparaître sa ligne.
-- ==============================================================================

INSERT INTO company_settings (id, setting_key, setting_value, description) VALUES
    ('88888888-8888-8888-8888-888888888901', 'COMPANY_NAME', 'Teranga Tech PME SARL (démo fictive)', 'Raison sociale affichée sur les documents (fictive, dev/démo)'),
    ('88888888-8888-8888-8888-888888888902', 'COMPANY_CITY', 'Dakar', 'Ville du siège, utilisée pour « Fait à <ville> » (dev/démo)'),
    ('88888888-8888-8888-8888-888888888903', 'COMPANY_NINEA', 'DEMO-000000000', 'NINEA fictif (dev/démo uniquement)'),
    ('88888888-8888-8888-8888-888888888904', 'COMPANY_RCCM', 'SN.DKR.DEMO.0000', 'RCCM fictif (dev/démo uniquement)')
ON CONFLICT DO NOTHING;
