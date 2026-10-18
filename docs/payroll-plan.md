# Plan d'implémentation – Étape 4 du Lot 1 : Paie Réelle

> **Document de référence faisant foi** pour l'implémentation de la paie réelle dans CareerTrack (branche `feature/CT-01-payroll`).  
> **Règle absolue** : Les barèmes et taux légaux ne sont pas certifiés. Tous les paramètres portent le statut **`VALEUR À CONFIRMER`** et la mention **`source à vérifier`**. L'application ne prétend à aucun moment être en conformité légale certifiée.

---

## 1. Répertoire des Valeurs Légales et Paramètres de Paie

Tous les paramètres de paie et barèmes sont stockés en base de données avec date d'effet (`valid_from`). La date retenue pour déterminer les paramètres applicables à une période mensuelle donnée est le **dernier jour du mois de la période** (ex: `2026-09-30` pour la période `2026-09`).

| Paramètre | Clé / Rubrique | Base / Assiette | Part Salariale | Part Patronale | Statut | Source documentaire |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **IPRES Régime Général** | `IPRES_RG` | Salaire brut plafonné à **432 000 FCFA** | **5,6 %** | **8,4 %** | VALEUR À CONFIRMER | Pratique courante IPRES / Arrêté d'application (source à vérifier ; note : le libellé « 3x plafond CSS » est erroné car 3 × 63 000 = 189 000) |
| **IPRES Régime Complémentaire Cadres** | `IPRES_RC` | Tranche entre **432 000 FCFA** et **1 296 000 FCFA** | **2,4 %** | **3,6 %** | VALEUR À CONFIRMER | Convention collective interprofessionnelle / Régime de retraite des cadres IPRES (source à vérifier). Condition paramétrable `PAYROLL_IPRES_RC_CADRES_ONLY` (défaut : `true`). |
| **CSS Prestations Familiales** | `CSS_PF` | Salaire brut plafonné à **63 000 FCFA** | **0,0 %** | **7,0 %** | VALEUR À CONFIRMER | Code de la Sécurité Sociale sénégalais (source à vérifier) |
| **CSS Accidents du Travail / MP** | `CSS_AT` | Salaire brut plafonné à **63 000 FCFA** | **0,0 %** | **1,0 %** | VALEUR À CONFIRMER | Code de la Sécurité Sociale sénégalais (source à vérifier ; taux standard PME tertiaire) |
| **CFCE** | `CFCE` | Salaire brut total non plafonné | **0,0 %** | **3,0 %** | VALEUR À CONFIRMER | Code Général des Impôts (source à vérifier) |
| **IPM (Santé)** | `IPM` | Salaire brut plafonné à **250 000 FCFA** | **3,0 %** *(fourchette 2% à 7,5%)* | **3,0 %** *(fourchette 2% à 7,5%)* | VALEUR À CONFIRMER | Réglementation IPM / Accord d'entreprise (source à vérifier). Déductibilité salariale IR : `PAYROLL_IPM_DEDUCTIBLE_FROM_IR` (défaut : `true`). |
| **Frais Professionnels** | `FRAIS_PRO` | 30 % du brut imposable (Brut - cotisations déductibles) | Abattement 30 % | N/A | VALEUR À CONFIRMER | CGI (source à vérifier). Plafond mensuel : **75 000 FCFA** (soit 900 000 FCFA par an). |
| **Barème Annuel de l'IR** | `TAX_IR` | Tranches annuelles sur quotient familial (Revenu Annuel Imposable / Parts) | Barème progressif par tranches (0%, 20%, 30%, 35%, 37%, 40%) | N/A | VALEUR À CONFIRMER | CGI sénégalais (source à vérifier). Taux maximal vérifié : 40% (paramétrable jusqu'à 43% selon LF 2026). Calcul annuel exact / 12, arrondi HALF_UP au franc. |
| **Parts Fiscales (Quotient)** | `TAX_PARTS` | Situation matrimoniale et enfants à charge | Célibataire: 1, Marié: 1.5, +0.5 / enfant (max 5) | N/A | VALEUR À CONFIRMER | CGI (source à vérifier). Règle paramétrable, calculée par le moteur (non stockée sur `employees`). |
| **TRIMF** | `TRIMF` | Tranches de revenu brut annuel | Barème par tranches annuelles réparties en douzièmes | N/A | VALEUR À CONFIRMER | CGI (source à vérifier). Sources divergentes (900 à 36 000 FCFA/an ou plus). Testé isolément avec sa propre table. |

---

## 2. Cas Témoins Mathématiques Indépendants

Pour valider le moteur de paie sans biais d'implémentation, les cas témoins ont été calculés à la main / tableur :

### A. Cas Témoin Salarié Cadre (Brut 950 000 FCFA, Célibataire, 1 part, hors TRIMF)
- **Salaire brut contractuel** : 950 000 FCFA
- **IPRES Régime Général Salarié (5,6 % plafonné à 432 000)** :
  $$432\,000 \times 5{,}6\% = 24\,192\text{ FCFA}$$
- **IPRES Complémentaire Cadres Salarié (2,4 % sur [432 000 - 950 000])** :
  $$(950\,000 - 432\,000) \times 2{,}4\% = 518\,000 \times 2{,}4\% = 12\,432\text{ FCFA}$$
- **IPM Salariale (3,0 % plafonné à 250 000)** :
  $$250\,000 \times 3{,}0\% = 7\,500\text{ FCFA}$$
- **Total cotisations salariales sociales déductibles** :
  $$24\,192 + 12\,432 + 7\,500 = 44\,124\text{ FCFA}$$
- **Brut imposable mensuel** :
  $$950\,000 - 44\,124 = 905\,876\text{ FCFA}$$
- **Déduction Frais Professionnels (30 % plafonné à 75 000 FCFA/mois)** :
  $$905\,876 \times 30\% = 271\,762{,}8 \implies \text{Plafond atteint : } 75\,000\text{ FCFA}$$
- **Net imposable mensuel** :
  $$905\,876 - 75\,000 = 830\,876\text{ FCFA}$$
- **Revenu annuel imposable** :
  $$830\,876 \times 12 = 9\,970\,512\text{ FCFA}$$
- **Quotient familial (1 part)** :
  $$Q = 9\,970\,512\text{ FCFA}$$
- **Calcul de l'IR annuel par tranches** :
  - Tranche 1 (0 à 630 000) à 0 % = 0 FCFA
  - Tranche 2 (630 000 à 1 500 000 = 870 000) à 20 % = 174 000 FCFA
  - Tranche 3 (1 500 000 à 4 000 000 = 2 500 000) à 30 % = 750 000 FCFA
  - Tranche 4 (4 000 000 à 8 000 000 = 4 000 000) à 35 % = 1 400 000 FCFA
  - Tranche 5 (8 000 000 à 9 970 512 = 1 970 512) à 37 % = 729 089,44 FCFA
  - Total IR annuel = $0 + 174\,000 + 750\,000 + 1\,400\,000 + 729\,089{,}44 = 3\,053\,089{,}44\text{ FCFA}$
- **IR mensuel (divisé par 12)** :
  $$\frac{3\,053\,089{,}44}{12} = 254\,424{,}12 \implies \mathbf{254\,424\text{ FCFA}}\text{ (arrondi HALF\_UP)}$$
- **Total retenues salariales (hors TRIMF)** :
  $$24\,192 + 12\,432 + 7\,500 + 254\,424 = 298\,548\text{ FCFA}$$
- **Net à payer (hors TRIMF)** :
  $$950\,000 - 298\,548 = \mathbf{651\,452\text{ FCFA}}$$

### B. Cas Témoin Charges Patronales (Même Salaire 950 000 FCFA Cadre)
- **IPRES Général Patronal (8,4 % plafonné à 432 000)** :
  $$432\,000 \times 8{,}4\% = \mathbf{36\,288\text{ FCFA}}$$
- **IPRES Complémentaire Patronal (3,6 % sur [432 000 - 950 000])** :
  $$518\,000 \times 3{,}6\% = \mathbf{18\,648\text{ FCFA}}$$
- **CSS Prestations Familiales (7,0 % plafonné à 63 000)** :
  $$63\,000 \times 7{,}0\% = \mathbf{4\,410\text{ FCFA}}$$
- **CSS Accidents du Travail (1,0 % plafonné à 63 000)** :
  $$63\,000 \times 1{,}0\% = \mathbf{630\text{ FCFA}}$$
- **CFCE (3,0 % sur brut total 950 000)** :
  $$950\,000 \times 3{,}0\% = \mathbf{28\,500\text{ FCFA}}$$
- **IPM Patronale (3,0 % plafonné à 250 000)** :
  $$250\,000 \times 3{,}0\% = \mathbf{7\,500\text{ FCFA}}$$
- **Total Charges Patronales** :
  $$36\,288 + 18\,648 + 4\,410 + 630 + 28\,500 + 7\,500 = \mathbf{95\,976\text{ FCFA}}$$
- **Coût total employeur** :
  $$950\,000 + 95\,976 = \mathbf{1\,045\,976\text{ FCFA}}$$

---

## 3. Architecture et Découpage des Étapes d'Implémentation

### Étape (a) : Migrations Flyway et Entités JPA
1. **Migration `V12__payroll_and_employee_tax_fields.sql`** :
   - Mise à jour de `employees` :
     - `is_cadre` BOOLEAN NOT NULL DEFAULT FALSE
     - `marital_status` VARCHAR(30) NOT NULL DEFAULT 'CELIBATAIRE'
     - `dependents_count` INT NOT NULL DEFAULT 0
   - Nettoyage préventif des doublons éventuels sur `payslips(employee_id, period_year, period_month)` avant contrainte.
   - Ajout de la contrainte unique `uq_payslips_employee_period UNIQUE (employee_id, period_year, period_month)`.
   - Mise à jour de `payslips` :
     - `taxable_gross` NUMERIC(15,2) DEFAULT 0
     - `tax_parts` NUMERIC(4,2) DEFAULT 1.0
     - `employer_contributions` NUMERIC(15,2) DEFAULT 0
     - `cancelled_by` VARCHAR(100) NULL
     - `cancelled_at` TIMESTAMP WITH TIME ZONE NULL
     - Statuts supportés : `GENERATED`, `VALIDATED`, `PAID`, `CANCELLED`
   - Mise à jour de `payslip_lines` :
     - `category` VARCHAR(30) NOT NULL DEFAULT 'DEDUCTION_SALARIALE' (`GAIN_BRUT`, `DEDUCTION_SALARIALE`, `COTISATION_PATRONALE`)
   - Création des tables :
     - `payroll_parameters` (`key`, `value`, `description`, `category`, `status`, `source`, `valid_from`)
     - `payroll_tax_brackets` (`id`, `bracket_order`, `min_annual_income`, `max_annual_income`, `rate`, `valid_from`)
     - `payroll_trimf_brackets` (`id`, `bracket_order`, `min_annual_gross`, `max_annual_gross`, `annual_amount`, `valid_from`)
   - Insertion des paramètres initiaux avec statut `'VALEUR_A_CONFIRMER'`.
2. **Migration DEV `V13__dev_demo_payroll_seed.sql` (dans `db/dev-migration`)** :
   - Configuration de `EMP-004` (Ousmane Diallo) : `is_cadre = true`, `marital_status = 'CELIBATAIRE'`, `dependents_count = 0`.
   - Régénération du bulletin de test `99999999-9999-9999-9999-999999999901` (09/2026) avec les montants exacts du cas témoin (IR 254 424 FCFA, charges patronales 95 976 FCFA, etc.).
3. **Entités JPA et Repositories** :
   - Enrichissement de `Employee`, `Payslip`, `PayslipLine`.
   - Création de `PayrollParameter`, `PayrollTaxBracket`, `PayrollTrimfBracket`.

### Étape (b) : Moteur de Calcul `PayrollCalculator` et Tests Unitaires Purs
1. Composant autonome `PayrollCalculator` :
   - Aucun appel BD, logique pure en `BigDecimal` avec arrondi `RoundingMode.HALF_UP` à 0 décimale pour les montants finaux.
   - Calcul des parts fiscales selon situation (`CELIBATAIRE`=1.0, `MARIE`=1.5, +0.5/enfant, plafonné à 5.0).
   - Calcul IPRES RG et RC (si `is_cadre` ou selon paramètre `PAYROLL_IPRES_RC_CADRES_ONLY`).
   - Calcul CSS PF, CSS AT, CFCE, IPM (part salariale et patronale).
   - Calcul déduction frais professionnels (30 % plafonné à 75 000 FCFA/mois).
   - Calcul IR annuel par tranche / 12 * parts.
   - Calcul TRIMF via table de tranches annuelles divisée par 12.
2. Suite exhaustive de tests unitaires `PayrollCalculatorTest` :
   - Salarié non-cadre sous 432 000 FCFA.
   - Salarié cadre à 950 000 FCFA (vérification exacte du cas témoin salarié et du cas témoin patronal).
   - Test des limites exactes des tranches IR (seuils 630 000, 1 500 000, 4 000 000, 8 000 000, 13 500 000).
   - Test des variations de quotient familial (1 part, 1.5 parts, 2 parts, 5 parts).
   - Test isolé de la TRIMF sur sa table dédiée.
   - Test de cohérence : somme des lignes = totaux du bulletin.

### Étape (c) : Service de Paie, Endpoints REST, Statuts et Statistiques
1. **Suppression des anciens `@Value` et paramètres codés en dur** :
   - Tous les taux et plafonds proviennent de `PayrollParameterRepository`, `PayrollTaxBracketRepository`, `PayrollTrimfBracketRepository` en filtrant par `valid_from <= date_fin_mois`.
2. **Génération de la paie (`POST /api/v1/payroll/generate?period=YYYY-MM`)** :
   - Réservé au rôle `DRH`.
   - Filtre strictement les employés `ACTIVE` disposant d'un contrat actif (`is_active = true`).
   - Idempotence : si un bulletin existe déjà pour le mois :
     - Statut `PAID` ou `VALIDATED` : conservé intact, ignoré.
     - Statut `GENERATED` ou `CANCELLED` : recalculé et mis à jour.
   - Retourne un résumé d'exécution : `{ period, totalProcessed, totalGenerated, totalSkipped, totalAmount }`.
3. **Gestion des Statuts et Annulation (`PATCH /api/v1/payroll/{id}/status`)** :
   - Transitions autorisées :
     - `GENERATED` -> `VALIDATED` (DRH)
     - `VALIDATED` -> `PAID` (DRH)
     - `VALIDATED` -> `CANCELLED` (DRH, avec horodatage `cancelled_at` et utilisateur `cancelled_by`).
   - Un bulletin `PAID` est strictement immuable (interdiction d'annulation ou modification).
4. **Endpoint Statistiques et Masse Salariale (`GET /api/v1/payroll/stats?period=YYYY-MM`)** :
   - Accessible à `DRH` et `DIRECTION`.
   - Règle de confidentialité stricte :
     - Regroupement des départements comptant moins de 3 employés payés dans `"Autres"`.
     - Si `"Autres"` compte elle-même moins de 3 employés payés, la catégorie est masquée / fusionnée.
     - Si l'effectif total payé de l'entreprise est inférieur à 3, aucune répartition par département n'est renvoyée (uniquement effectif global et totaux de masse salariale).
     - Zéro PII exposé.
5. **Accès et Sécurité des Bulletins** :
   - `EMPLOYE` et `MANAGER` ne peuvent consulter que leurs propres bulletins (`GET /api/v1/payroll/my-payslips` ou par leur `employeeId`).
   - `DIRECTION` n'a pas accès aux bulletins individuels (403 Forbidden), uniquement aux agrégats statistiques.

### Étape (d) : Génération du PDF du Bulletin
1. Mise à jour de `PdfGeneratorService` :
   - Ajout de la mention obligatoire : **« Modèle de démonstration, taux à valider »**.
   - Présentation claire des rubriques sous forme de tableau détaillé :
     - Rubrique
     - Base de calcul (FCFA)
     - Taux (%)
     - Retenue Salariale (FCFA)
     - Cotisation Patronale (FCFA)
   - Totaux bien distincts :
     - Total Brut
     - Total Retenues Salariales
     - **Net à Payer (FCFA)**
     - Total Cotisations Patronales
     - Coût Total Employeur
   - Décimales à la française (virgule, espaces insécables avant FCFA).

### Étape (e) : Interface Frontend `/payroll`
1. Connexion exclusive aux endpoints API réels (suppression des mocks).
2. Bandeau d'avertissement permanent pour le DRH : **« Taux et barèmes à valider avant toute utilisation réelle »**.
3. Tiroir / modale d'affichage des paramètres légaux en lecture seule (Clé, Valeur, Catégorie, Statut, Source).
4. Sélecteur de période mensuelle (YYYY-MM).
5. Bouton DRH « Générer la paie » avec dialogue de confirmation et affichage du résultat d'exécution.
6. Actions de cycle de vie des bulletins selon statut : Valider, Payer, Annuler (si validé).
7. Filtrage des vues selon les rôles :
   - `EMPLOYE` / `MANAGER` : vue restreinte à ses propres bulletins.
   - `DIRECTION` : redirection ou message informatif / vue restreinte aux statistiques de masse salariale.
8. Audit de tous les boutons de l'écran pour éliminer tout bouton inopérant.

---

## 4. Documentation et Contrats
- `docs/payroll-plan.md` : le présent plan d'implémentation.
- `docs/payroll-rules.md` : spécification technique exhaustive, limites du moteur (pas de prorata d'entrée/sortie, pas de primes variables ni d'heures supplémentaires, brut = salaire contractuel) et question ouverte sur l'IPRES complémentaire cadre.
- `docs/api/payroll_stats_contract.md` : contrat de l'API de statistiques.
- Liste explicite dans la PR des modifications partagées sur la table `employees` (Lot 2).
