# Règles de Calcul de la Paie, Barèmes et Paramètres Légaux — CareerTrack

> **AVERTISSEMENT LÉGAL OBLIGATOIRE**  
> Tous les taux, plafonds, seuils fiscaux et barèmes présentés dans ce document et implémentés dans l'application portent la mention **« VALEUR À CONFIRMER »** et la source **« source à vérifier »**.  
> Le système ne prétend pas être certifié conforme à la législation sénégalaise en vigueur. Ces paramètres sont fournis à titre de modélisation technique et **doivent faire l'objet d'une validation formelle par le service juridique et la direction financière** avant tout usage réel ou contractuel.

---

## 1. Principes Temporels et Dates d'Effet

### 1.1. Date de Détermination des Paramètres d'une Période
Pour tout bulletin de paie généré pour une période donnée `(année, mois)`, **la date d'effet déterminante est le dernier jour civil du mois considéré** (ex. `2026-09-30` pour le bulletin de septembre 2026, ou `2024-02-29` pour une année bissextile).

Le système sélectionne pour chaque paramètre la dernière valeur valide dont la colonne `valid_from <= end_of_month`.

```sql
SELECT * FROM payroll_parameters
WHERE param_key = :key AND valid_from <= :endOfMonth
ORDER BY valid_from DESC
LIMIT 1;
```

Cela garantit qu'un changement de taux intervenant en cours d'année (par exemple au 1er juillet) s'applique fidèlement aux périodes postérieures sans rétroactivité non souhaitée sur les mois antérieurs.

---

## 2. Cotisations Sociales et Patronales

### 2.1. IPRES — Régime Général (RG)
- **Assiette** : Salaire brut plafonné à **432 000 FCFA / mois** (`PAYROLL_IPRES_RG_CEILING`).
- **Taux Salarié** : **5,6 %** (`PAYROLL_IPRES_SALARIAL_RATE`).
- **Taux Patronal** : **8,4 %** (`PAYROLL_IPRES_PATRONAL_RATE`).
- **Statut** : VALEUR À CONFIRMER (source à vérifier).
- **Note** : Le plafond de 432 000 FCFA ne correspond pas à 3 × plafond CSS (63 000 FCFA × 3 = 189 000 FCFA), mais au plafond autonome fixé par les arrêtés ministériels IPRES.

### 2.2. IPRES — Régime Complémentaire Cadres (RCC)
- **Assiette** : Tranche de salaire comprise entre **432 000 FCFA** (`PAYROLL_IPRES_RG_CEILING`) et **1 296 000 FCFA** (`PAYROLL_IPRES_RC_CEILING`, soit 3 × 432 000 FCFA).
- **Taux Salarié** : **2,4 %** (`PAYROLL_IPRES_RC_SALARIAL_RATE`).
- **Taux Patronal** : **3,6 %** (`PAYROLL_IPRES_RC_PATRONAL_RATE`).
- **Statut** : VALEUR À CONFIRMER (source à vérifier).

#### Question Ouverte — Éligibilité à l'IPRES Complémentaire
> **QUESTION OUVERTE DOCUMENTÉE :**  
> Les sources documentaires et pratiques RH au Sénégal divergent sur le champ d'application de l'IPRES Régime Complémentaire :  
> 1. S'applique-t-elle **strictement aux salariés ayant le statut contractuel de Cadre** (`is_cadre = true`), quelle que soit la tranche de rémunération ?  
> 2. Ou s'applique-t-elle à **tout salarié percevant un salaire excédant 432 000 FCFA**, quel que soit son statut catégoriel ?  
>  
> **Solution implémentée :** Le comportement est piloté par le paramètre dynamique `PAYROLL_IPRES_RC_CADRES_ONLY` (défaut `true`) :  
> - Si `true` : seuls les employés avec `is_cadre = true` cotisent à l'IPRES RCC sur la tranche 432 000 – 1 296 000 FCFA.  
> - Si `false` : tout employé cotise dès lors que son brut dépasse 432 000 FCFA.

### 2.3. Caisse de Sécurité Sociale (CSS)
- **Assiette** : Salaire brut plafonné à **63 000 FCFA / mois** (`PAYROLL_CSS_CEILING`).
- **Prestations Familiales (PF)** : **7,0 %** employeur (`PAYROLL_CSS_PRESTATIONS_FAMILIALES_RATE`). Part salariale : **0 %**.
- **Accidents du Travail et Maladies Professionnelles (AT/MP)** : **1,0 %** employeur (`PAYROLL_CSS_ACCIDENTS_TRAVAIL_RATE`). Part salariale : **0 %**.
- **Statut** : VALEUR À CONFIRMER (source à vérifier).

### 2.4. Contribution Forfaitaire à la Charge de l'Employeur (CFCE)
- **Assiette** : Salaire brut total **sans plafond**.
- **Taux Patronal** : **3,0 %** (`PAYROLL_CFCE_RATE`). Part salariale : **0 %**.
- **Statut** : VALEUR À CONFIRMER (source à vérifier).

### 2.5. Institution de Prévoyance Maladie (IPM)
- **Assiette** : Salaire brut plafonné à **250 000 FCFA / mois** (`PAYROLL_IPM_CEILING`).
- **Taux Salarié** : **3,0 %** (`PAYROLL_IPM_SALARIAL_RATE`).
- **Taux Patronal** : **3,0 %** (`PAYROLL_IPM_PATRONAL_RATE`).
- **Déductibilité fiscale** : Pilotée par `PAYROLL_IPM_DEDUCTIBLE_FROM_IR` (défaut `true` : la cotisation IPM salariale est déduite de l'assiette du brut imposable à l'IR).
- **Statut** : VALEUR À CONFIRMER (source à vérifier).

---

## 3. Détermination de la Base Imposable à l'Impôt sur le Revenu (IR)

### 3.1. Brut Imposable
$$\text{Brut Imposable} = \text{Brut Total} - (\text{Cotisation IPRES RG Salarié} + \text{Cotisation IPRES RCC Salarié} + \text{Cotisation IPM Salarié})$$

### 3.2. Déduction Forfaitaire pour Frais Professionnels
- **Taux** : **30 %** du brut imposable (`PAYROLL_PROFESSIONAL_EXPENSES_RATE`).
- **Plafond Mensuel** : **75 000 FCFA / mois** (`PAYROLL_PROFESSIONAL_EXPENSES_MONTHLY_CEILING`), soit **900 000 FCFA / an**.
- **Statut** : VALEUR À CONFIRMER (source à vérifier).

### 3.3. Net Imposable
$$\text{Net Imposable Mensuel} = \text{Brut Imposable} - \text{Frais Professionnels Mensuels}$$
$$\text{Revenu Annuel Imposable} = \text{Net Imposable Mensuel} \times 12$$

---

## 4. Barème Progressif Annuel de l'IR et Quotient Familial

### 4.1. Quotient Familial (Nombre de Parts)
Le nombre de parts fiscales est calculé à la volée par le moteur de paie (non stocké sur la table employé) selon la situation familiale déclarée :
- Célibataire, veuf(ve), divorcé(e) sans enfant : **1,0 part**
- Marié(e) sans enfant : **1,5 part**
- Majoration par enfant à charge : **+0,5 part** par enfant
- **Plafond légal** : Maximum **5,0 parts**

### 4.2. Barème Annuel Progressif Exact
Le calcul s'effectue sur le barème annuel légal strict, **sans seuils mensuels arrondis** :

| Tranche de Revenu Annuel par Part (FCFA) | Taux Applicable |
| :--- | :---: |
| De 0 à 630 000 FCFA | **0 %** |
| De 630 001 à 1 500 000 FCFA | **20 %** |
| De 1 500 001 à 4 000 000 FCFA | **30 %** |
| De 4 000 001 à 8 000 000 FCFA | **35 %** |
| De 8 000 001 à 13 500 000 FCFA | **37 %** |
| Au-delà de 13 500 000 FCFA | **40 %** |

### 4.3. Algorithme de Calcul de l'IR
1. $\text{Revenu par Part} = \frac{\text{Revenu Annuel Imposable}}{\text{Nombre de Parts}}$
2. Calcul de l'impôt par tranche sur le Revenu par Part.
3. $\text{Impôt Annuel Global} = \text{Impôt par Part} \times \text{Nombre de Parts}$
4. $\text{Impôt Mensuel Retenu} = \text{Round}\left(\frac{\text{Impôt Annuel Global}}{12}\right)$ avec arrondi standard `HALF_UP` au franc supérieur.

---

## 5. Taxe Représentative de l'Impôt du Minimum Fiscal (TRIMF)

- La TRIMF est régie par une table de tranches annuelles réparties en douzièmes mensuels (`payroll_trimf_brackets`).
- **Statut** : VALEUR À CONFIRMER. Les sources documentaires divergent profondément sur les tranches et montants (allant de 900 FCFA à 36 000 FCFA annuels, ou 48 000 FCFA au-delà de 30 millions, ou forfaits mensuels).
- **Règle de test** : Aucun test de net à payer ne dépend de la TRIMF ; la TRIMF est testée isolément avec ses propres jeux de données dédiés.

---

## 6. Cas Témoins Validés (Benchmark de Référence)

### 6.1. Cas Témoin Salarié (Brut = 950 000 FCFA, Cadre, Célibataire 1 part)
- **Salaire Brut** : 950 000 FCFA
- **IPRES RG Salarié (5,6 % sur 432 000)** : 24 192 FCFA
- **IPRES RCC Salarié (2,4 % sur [950 000 - 432 000] = 518 000)** : 12 432 FCFA
- **IPM Salarié (3 % sur 250 000)** : 7 500 FCFA
- **Brut Imposable** : $950\,000 - (24\,192 + 12\,432 + 7\,500) = \mathbf{905\,876\text{ FCFA}}$
- **Frais Professionnels** : $905\,876 \times 30\% = 271\,762,80$ plafonnés à **75 000 FCFA**
- **Net Imposable Mensuel** : $905\,876 - 75\,000 = \mathbf{830\,876\text{ FCFA}}$
- **Revenu Annuel Imposable (1 part)** : $830\,876 \times 12 = \mathbf{9\,970\,512\text{ FCFA}}$
- **Calcul IR par tranches annuelles** :
  - 0 à 630 000 à 0% = 0 FCFA
  - 630 001 à 1 500 000 à 20% = 174 000 FCFA
  - 1 500 001 à 4 000 000 à 30% = 750 000 FCFA
  - 4 000 001 à 8 000 000 à 35% = 1 400 000 FCFA
  - 8 000 001 à 9 970 512 à 37% = 729 089,44 FCFA
  - Total Annuel IR = 3 053 089,44 FCFA
- **IR Retenu Mensuel** : $3\,053\,089,44 / 12 = \mathbf{254\,424\text{ FCFA}}$ (arrondi HALF_UP)
- **Total Déductions Salariales** (hors TRIMF) : $24\,192 + 12\,432 + 7\,500 + 254\,424 = \mathbf{298\,548\text{ FCFA}}$
- **Net à Payer Salarié** (hors TRIMF) : $950\,000 - 298\,548 = \mathbf{651\,452\text{ FCFA}}$

### 6.2. Cas Témoin Patronal Indépendant (Même Salaire 950 000 FCFA, Cadre)
- **IPRES RG Employeur (8,4 % sur 432 000)** : 36 288 FCFA
- **IPRES RCC Employeur (3,6 % sur 518 000)** : 18 648 FCFA
- **CSS Prestations Familiales (7 % sur 63 000)** : 4 410 FCFA
- **CSS Accidents du Travail (1 % sur 63 000)** : 630 FCFA
- **CFCE Employeur (3 % sur brut total 950 000)** : 28 500 FCFA
- **IPM Employeur (3 % sur 250 000)** : 7 500 FCFA
- **Total Cotisations Patronales** : $\mathbf{95\,976\text{ FCFA}}$
- **Coût Global de la Rémunération** : $950\,000 + 95\,976 = \mathbf{1\,045\,976\text{ FCFA}}$

---

## 7. Cycle de Vie des Statuts et Journalisation des Corrections

Les bulletins de paie suivent la machine à états stricte suivante :

```
[ Contrat Actif ] ──(Génération)──> [ GÉNÉRÉ ] ──(Validation DRH)──> [ VALIDÉ ] ──(Paiement)──> [ PAYÉ ] (Verrouillé)
                                                                          │
                                                                   (Annulation DRH)
                                                                          ▼
                                                                     [ ANNULÉ ] (Régénérable)
```

1. **`GÉNÉRÉ` (`GENERATED`)** : Bulletin calculé par le moteur. Modifiable et recalculable lors d'une nouvelle génération de la même période.
2. **`VALIDÉ` (`VALIDATED`)** : Bulletin vérifié par la DRH. Prêt pour ordonnancement du paiement bancaire.
3. **`PAYÉ` (`PAID`)** : Salaire décaissé. **Strictement intouchable et immuable**. Aucune suppression ni modification autorisée.
4. **`ANNULÉ` (`CANCELLED`)** : Statut d'exception activé par la DRH sur un bulletin `VALIDÉ` avant paiement (par exemple si une prime ou un congé n'avait pas été pris en compte).
   - Les champs `cancelled_by` (identifiant DRH) et `cancelled_at` (horodatage UTC) sont obligatoirement renseignés.
   - Le bulletin passe au statut `CANCELLED` et peut être écrasé/régénéré lors du prochain calcul de paie.

---

## 8. Règles de Confidentialité des Statistiques de Masse Salariale

Conformément aux exigences de protection des données personnelles et de secret des rémunérations :
1. **Règle du seuil de département** : Tout département comptant **moins de 3 salariés payés** sur la période a ses données agrégées dans une ligne commune intitulée `"Autres"`.
2. **Règle du seuil "Autres"** : Si la ligne `"Autres"` elle-même compte moins de 3 salariés, elle est totalement masquée.
3. **Règle du seuil d'effectif global** : Si l'effectif total payé de l'entreprise sur la période est **inférieur à 3 salariés**, aucune ventilation par département n'est renvoyée (le tableau est omis). Seuls l'effectif global et les totaux globaux de masse salariale sont communiqués.
