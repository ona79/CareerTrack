package com.careertrack.modules.recruitment.entity;

/**
 * Statuts du pipeline Kanban de recrutement.
 * Flux : RECU -> PRESELECTIONNE -> ENTRETIEN -> OFFRE -> EMBAUCHE / REFUSE
 */
public enum ApplicationStatus {
    RECU,
    PRESELECTIONNE,
    ENTRETIEN,
    OFFRE,
    EMBAUCHE,
    REFUSE
}
