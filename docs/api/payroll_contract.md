# Contrat d'API — Module de Paie & Masse Salariale

## 1. Endpoints de Paie

### 1.1. Génération de la Paie Mensuelle
- **URL** : `POST /api/v1/payroll/generate`
- **Rôles autorisés** : `ROLE_DRH`
- **Paramètres de requête (Query Params)** :
  - Soit `period` (string, ex: `"2026-09"`).
  - Soit `year` (int, ex: `2026`) et `month` (int, ex: `9`).
- **Comportement** :
  - Idempotent : calcule ou recalcule les bulletins de tous les employés ayant un contrat actif au dernier jour du mois.
  - Les bulletins au statut `GENERATED` ou `CANCELLED` sont recalculés et écrasés.
  - Les bulletins au statut `VALIDATED` ou `PAID` sont strictement préservés.
- **Réponses** :
  - `200 OK` : Résultat de génération `PayrollGenerationResultDto`.
  - `400 Bad Request` : Période, mois ou année invalide.
  - `403 Forbidden` : Utilisateur non DRH.

---

### 1.2. Consultation Nominative des Bulletins (Entreprise)
- **URL** : `GET /api/v1/payroll/payslips`
- **Rôles autorisés** : `ROLE_DRH`
- **Paramètres de requête** :
  - `period` (string, obligatoire) : Format `YYYY-MM` (ex. `2026-09`)
- **Comportement** :
  - Renvoie la liste complète des bulletins avec totaux et lignes détaillées pour la DRH.
  - `ROLE_DIRECTION`, `ROLE_MANAGER`, `ROLE_EMPLOYE` reçoivent un `403 Forbidden`.
- **Réponses** :
  - `200 OK` : `List<PayslipResponseDto>`.
  - `403 Forbidden` : Accès refusé.

---

### 1.3. Consultation Personnelle (« Mes Bulletins »)
- **URL** : `GET /api/v1/payroll/my-payslips`
- **Rôles autorisés** : Authentifié (`ROLE_EMPLOYE`, `ROLE_MANAGER`, `ROLE_DRH`, `ROLE_DIRECTION`)
- **Comportement** :
  - Renvoie uniquement les bulletins appartenant au compte employé lié à l'utilisateur connecté.
  - Aucun paramètre requis (déduit du token JWT de session).
- **Réponses** :
  - `200 OK` : `List<PayslipResponseDto>`.

---

### 1.4. Consultation des Bulletins d'un Employé Spécifique
- **URL** : `GET /api/v1/payroll/payslips/employee/{employeeId}`
- **Rôles autorisés** : Le collaborateur lui-même ou `ROLE_DRH`
- **Comportement** :
  - Renvoie la liste des bulletins du collaborateur cible.
  - `403 Forbidden` pour un manager ou un autre collaborateur.
- **Réponses** :
  - `200 OK` : `List<PayslipResponseDto>`.
  - `403 Forbidden` : Non autorisé.

---

### 1.5. Détail d'un Bulletin Spécifique
- **URL** : `GET /api/v1/payroll/payslips/{id}`
- **Rôles autorisés** : Le titulaire du bulletin ou `ROLE_DRH`
- **Réponses** :
  - `200 OK` : `PayslipResponseDto` avec toutes les lignes de calcul détaillées.
  - `403 Forbidden` : Non autorisé.
  - `404 Not Found` : Bulletin introuvable.

---

### 1.6. Téléchargement du Bulletin PDF
- **URL** : `GET /api/v1/payroll/payslips/{id}/pdf` (et alias `GET /api/v1/documents/payslip/{id}`)
- **Rôles autorisés** : Le titulaire du bulletin ou `ROLE_DRH`
- **Réponses** :
  - `200 OK` : Flux binaire `application/pdf` avec en-tête `Content-Disposition: attachment; filename="..."`.
  - `403 Forbidden` : Salarié tentant de télécharger le bulletin d'un tiers.
  - `404 Not Found` : Bulletin introuvable.

---

### 1.7. Mise à Jour du Statut d'un Bulletin
- **URL** : `PATCH /api/v1/payroll/payslips/{id}/status`
- **Rôles autorisés** : `ROLE_DRH`
- **Paramètres de requête** :
  - `status` (string, obligatoire) : `VALIDATED`, `PAID`, `CANCELLED`
- **Règles de transition** :
  - `GENERATED` $\to$ `VALIDATED`
  - `VALIDATED` $\to$ `PAID`
  - `VALIDATED` $\to$ `CANCELLED` (enregistre `cancelled_by` et `cancelled_at`)
  - `PAID` $\to$ Aucune transition autorisée (`400 Bad Request`, statut verrouillé)
- **Réponses** :
  - `200 OK` : `PayslipResponseDto` mis à jour.
  - `400 Bad Request` : Transition invalide ou bulletin `PAID`.
  - `404 Not Found` : Bulletin introuvable.

---

### 1.8. Paramètres Légaux & Barèmes
- **URL** : `GET /api/v1/payroll/parameters`
- **Rôles autorisés** : `ROLE_DRH`, `ROLE_DIRECTION`
- **Comportement** :
  - Renvoie la liste de tous les paramètres légaux, tranches fiscales et cotisations configurés en base avec leur statut et source.
- **Réponses** :
  - `200 OK` : `List<PayrollParameterResponseDto>` avec `key`, `value`, `category`, `status` (`VALEUR À CONFIRMER`), `source` (`source à vérifier`), `validFrom`.

---

### 1.9. Statistiques de Masse Salariale
- **URL** : `GET /api/v1/payroll/stats`
- **Rôles autorisés** : `ROLE_DRH`, `ROLE_DIRECTION`
- **Paramètres de requête** :
  - Soit `period` (string, ex: `"2026-09"`).
  - Soit `year` (int, ex: `2026`) et `month` (int, ex: `9`).
- **Structure de la réponse** :
```json
{
  "period": "2026-09",
  "periodYear": 2026,
  "periodMonth": 9,
  "totalEmployeesPaid": 12,
  "totalGrossSalary": 11400000.00,
  "totalNetSalary": 7817424.00,
  "totalDeductions": 1500000.00,
  "totalEmployerContributions": 1151712.00,
  "totalEmployerCost": 12551712.00,
  "totalPayrollCost": 12551712.00,
  "anonymized": false,
  "departmentBreakdown": [
    {
      "departmentName": "Ingénierie & Systèmes",
      "employeeCount": 5,
      "grossSalary": 4750000.00,
      "totalGrossSalary": 4750000.00,
      "netSalary": 3257260.00,
      "totalNetSalary": 3257260.00,
      "employerCost": 5229880.00,
      "totalEmployerContributions": 479880.00
    },
    {
      "departmentName": "Autres",
      "employeeCount": 3,
      "grossSalary": 2850000.00,
      "totalGrossSalary": 2850000.00,
      "netSalary": 1954356.00,
      "totalNetSalary": 1954356.00,
      "employerCost": 3137928.00,
      "totalEmployerContributions": 287928.00
    }
  ]
}
```

#### Règles de Confidentialité Appliquées
1. **Regroupement des petits départements** : Tout département avec $< 3$ salariés payés est fusionné dans `"Autres"`.
2. **Masquage de "Autres"** : Si la ligne `"Autres"` issue de ce regroupement compte elle-même $< 3$ salariés, elle est omise.
3. **Masquage global** : Si l'effectif total payé de l'entreprise est $< 3$ salariés, le champ `anonymized` vaut `true` et `departmentBreakdown` est une liste vide (`[]`). Seuls les totaux globaux sont renvoyés.
