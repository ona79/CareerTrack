package com.careertrack.modules.leave.entity;

public enum LeaveStatus {
    SUBMITTED,
    APPROVED_N1,
    APPROVED_DRH,
    REJECTED,
    CANCELLED,

    // Aliases français demandés dans le cahier des charges
    EN_ATTENTE,
    VALIDE,
    REFUSE,
    ANNULE;

    public boolean isApproved() {
        return this == APPROVED_N1 || this == APPROVED_DRH || this == VALIDE;
    }

    public boolean isPending() {
        return this == SUBMITTED || this == EN_ATTENTE;
    }

    public boolean isRejected() {
        return this == REJECTED || this == REFUSE;
    }

    public boolean isCancelled() {
        return this == CANCELLED || this == ANNULE;
    }
}
