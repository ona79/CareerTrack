-- ------------------------------------------------------------------------------
-- MIGRATION V14 : HISTORIQUE DES AVANCEMENTS ET CHANGEMENTS DE POSTES / DEPARTEMENTS
-- ------------------------------------------------------------------------------

CREATE TABLE position_histories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    previous_position_id UUID REFERENCES positions(id) ON DELETE SET NULL,
    new_position_id UUID REFERENCES positions(id) ON DELETE SET NULL,
    previous_department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    new_department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    effective_date DATE NOT NULL,
    change_reason VARCHAR(255) NOT NULL,
    changed_by UUID REFERENCES employees(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_position_histories_employee ON position_histories(employee_id);
