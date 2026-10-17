# Réglementation de la Paie Sénégalaise – Guide Technique CareerTrack

Ce document détaille les règles légales, les taux et les évolutions à intégrer pour le calcul de paie des PME au Sénégal.

---

## 1. Cotisations Sociales

### A. Retraite (IPRES - Institution de Prévoyance Retraite du Sénégal)
- **Régime Général (Salariés non-cadres & base cadre)** :
  - Part Salariale : **5,6 %** (retenue à la source).
  - Part Patronale : **8,4 %** (à la charge de l'employeur).
  - **Plafond mensuel légal d'assiette** : **432 000 FCFA** (au-delà, la tranche n'est plus soumise au régime général).
- **Régime Complémentaire Cadres (RCC)** :
  - Applicable aux collaborateurs ayant le statut cadre.
  - Part Salariale : **2,4 %** additionnels.
  - Part Patronale : **3,6 %** additionnels.
  - **Plafond mensuel légal d'assiette** : **1 296 000 FCFA** (3 fois le plafond général).

### B. Caisse de Sécurité Sociale (CSS)
- **100 % à la charge de l'EMPLOYEUR** (0 % de retenue salariale, strictement vérifié selon le Code de la Sécurité Sociale sénégalais).
- Prestations Familiales : **7,0 %**.
- Accidents du Travail et Maladies Professionnelles (AT/MP) : **1,0 % à 5,0 %** selon la catégorie de risque (taux standard PME tertiaire : 1% à 3%).
- **Plafond mensuel légal d'assiette** : **63 000 FCFA** par salarié.

---

## 2. Fiscalité Salariale (Retenues Fiscales à la Source)

### A. Impôt sur le Revenu (IR) – À intégrer
Dans la version initiale, un taux effectif paramétrable moyen (ex: 12 %) est fourni à titre indicatif pour les simulations préliminaires.
**Évolution requise pour la production** :
Remplacer le taux fixe par le **barème progressif officiel du Code Général des Impôts (CGI sénégalais, Art. 115 bis et suivants)** :
1. Calcul du Revenu Brut Imposable (Brut - cotisations IPRES déductibles).
2. Abattement forfaitaire pour frais professionnels (30 %, plafonné légalement).
3. Application du barème progressif par tranches annuelles :
   - Tranche 0 à 630 000 FCFA : **0 %**
   - Tranche 630 001 à 1 500 000 FCFA : **20 %**
   - Tranche 1 500 001 à 4 000 000 FCFA : **30 %**
   - Tranche 4 000 001 à 8 000 000 FCFA : **35 %**
   - Tranche au-delà de 8 000 000 FCFA : **40 %**
4. Réduction pour charges de famille (Quotient familial : de 1 part à 5 parts fiscales selon la situation matrimoniale et le nombre d'enfants à charge).

### B. Taxe Représentative de l'Impôt du Minimum Fiscal (TRIMF)
- Impôt forfaitaire annuel prélevé mensuellement à la source :
  - Entre 900 FCFA/mois et 3 000 FCFA/mois selon la tranche de rémunération brute annuelle.
- Actuellement paramétré à 900 FCFA/mois dans `careertrack.labor-rules.trimf-monthly-amount`.

### C. Contribution Forfaitaire à la Charge des Employeurs (CFCE)
- Part patronale : **3 %** de la masse salariale brute (hors exonérations spécifiques conventionnelles).
