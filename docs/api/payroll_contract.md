# Contrat d'API — Module de Paie & Masse Salariale

## 1. Endpoints de Paie

### 1.1. Génération de la Paie Mensuelle
- **URL** : `POST /api/v1/payroll/generate`
- **Rôles autorisés** : `ROLE_DRH`, `ROLE_ADMIN`
- **Paramètres de requête (Query Params)** :
  - `year` (int, obligatoire) : ex. `2026`
  - `month` (int, obligatoire) : ex. `9`
- **Comportement** :
  - Idempotent : calcule ou recalcule les bulletins de tous les employés ayant un contrat actif au dernier jour du mois.
  - Les bulletins au statut `GENERATED` ou `CANCELLED` sont recalculés et écrasés.
  - Les bulletins au statut `VALIDATED` ou `PAID` sont strictement préservés.
- **Réponses** :
  - `200 OK` : Liste des bulletins générés `List<PayslipResponseDto>`.
  - `400 Bad Request` : Mois ou année invalide.
  - `403 Forbidden` : Utilisateur non DRH/ADMIN.

---

### 1.2. Consultation Nominative des Bulletins (Entreprise)
- **URL** : `GET /api/v1/payroll/payslips`
- **Rôles autorisés** : `ROLE_DRH`, `ROLE_ADMIN`
- **Paramètres de requête** :
  - `period` (string, optionnel) : Format `YYYY-MM` (ex. `2026-09`)
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
  - Impossible d'accéder aux bulletins d'un tiers.
- **Réponses** :
  - `200 OK` : `List<PayslipResponseDto>`.

---

### 1.4. Mise à Jour du Statut d'un Bulletin
- **URL** : `PATCH /api/v1/payroll/payslips/{id}/status`
- **Rôles autorisés** : `ROLE_DRH`, `ROLE_ADMIN`
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

### 1.5. Téléchargement du Bulletin PDF
- **URL** : `GET /api/v1/payroll/payslips/{id}/pdf` (ou alias `GET /api/v1/documents/payslip/{id}`)
- **Rôles autorisés** : Propriétaire du bulletin ou `ROLE_DRH` / `ROLE_ADMIN`
- **Réponses** :
  - `200 OK` : Flux binaire `application/pdf` avec en-tête `Content-Disposition: inline; filename="..."`.
  - `403 Forbidden` : Salarié tentant de télécharger le bulletin d'un tiers.
  - `404 Not Found` : Bulletin introuvable.

---

### 1.6. Paramètres Légaux & Barèmes
- **URL** : `GET /api/v1/payroll/parameters`
- **Rôles autorisés** : `ROLE_DRH`, `ROLE_ADMIN`
- **Réponses** :
  - `200 OK` : `List<PayrollParameterResponseDto>` avec `paramKey`, `paramValue`, `category`, `status` (`VALEUR À CONFIRMER`), `source` (`source à vérifier`), `validFrom`.

---

### 1.7. Statistiques de Masse Salariale
- **URL** : `GET /api/v1/payroll/stats`
- **Rôles autorisés** : `ROLE_DRH`, `ROLE_ADMIN`, `ROLE_DIRECTION`
- **Paramètres de requête** :
  - `year` (int, obligatoire) : ex. `2026`
  - `month` (int, obligatoire) : ex. `9`
- **Structure de la réponse** :
```json
{
  "periodYear": 2026,
  "periodMonth": 9,
  "totalEmployeesPaid": 12,
  "totalGrossSalary": 11400000,
  "totalNetSalary": 7817424,
  "totalEmployerContributions": 1151712,
  "totalPayrollCost": 12551712,
  "anonymized": false,
  "departmentBreakdowns": [
    {
      "departmentName": "Ingénierie & Systèmes",
      "employeeCount": 5,
      "totalGrossSalary": 4750000,
      "totalNetSalary": 3257260,
      "totalEmployerContributions": 479880
    },
    {
      "departmentName": "Autres",
      "employeeCount": 3,
      "totalGrossSalary": 2850000,
      "totalNetSalary": 1954356,
      "totalEmployerContributions": 287928
    }
  ]
}
```

#### Règles de Confidentialité Appliquées
1. **Regroupement des petits départements** : Tout département avec $< 3$ salariés payés est fusionné dans `"Autres"`.
2. **Masquage de "Autres"** : Si la ligne `"Autres"` issue de ce regroupement compte elle-même $< 3$ salariés, elle est omise.
3. **Masquage global** : Si l'effectif total payé de l'entreprise est $< 3$ salariés, le champ `anonymized` vaut `true` et `departmentBreakdowns` est une liste vide (`[]`). Seuls les totaux globaux sont renvoyés.
