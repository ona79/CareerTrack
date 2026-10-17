package com.careertrack.modules.payroll.entity;

public enum PayslipLineType {
    BASE_SALARY,       // Salaire de base
    OVERTIME,          // Heures supplémentaires
    BONUS,             // Primes et indemnités (transport, logement)
    IPRES_EMPLOYEE,    // Retraite régime général IPRES part salariale
    IPRES_EMPLOYER,    // Retraite régime général IPRES part patronale
    CSS_EMPLOYER,      // Caisse de Sécurité Sociale part patronale
    IPM,               // Mutuelle de santé (Institut de Prévoyance Maladie)
    TAX_IR,            // Retenue fiscale Impôt sur le Revenu (IR)
    OTHER_DEDUCTION    // Autres retenues / avances
}
