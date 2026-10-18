-- ==============================================================================
-- Migration V11 : Stockage et enrichissement des métadonnées des documents collaborateurs
-- ==============================================================================

-- 1. Ajout des colonnes de métadonnées et adaptation de la table employee_documents
ALTER TABLE employee_documents
    ADD COLUMN IF NOT EXISTS file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE employee_documents
    ALTER COLUMN file_url DROP NOT NULL;

-- 2. Création de la table dédiée au contenu binaire (isolation stricte pour les listes)
CREATE TABLE IF NOT EXISTS employee_document_contents (
    document_id UUID PRIMARY KEY REFERENCES employee_documents(id) ON DELETE CASCADE,
    data BYTEA NOT NULL
);
