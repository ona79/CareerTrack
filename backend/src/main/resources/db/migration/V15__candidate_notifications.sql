-- Migration V15: Support notifications pour les candidats sans compte utilisateur et métadonnées
ALTER TABLE notifications ALTER COLUMN recipient_id DROP NOT NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS recipient_email VARCHAR(150);
