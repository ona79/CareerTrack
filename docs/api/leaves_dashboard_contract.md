# Contrat d'Interface & Spécification API : Lot 3 (Congés, Absences & Dashboard RH)

**Version :** 1.0.0  
**Date :** Octobre 2026  
**Branche :** `feature/CT-03-leaves-dashboard`  
**Conformité légale :** Code du Travail du Sénégal (Loi n° 97-17, Art. L148 & s.), Règlementation IPRES & CSS.

---

## 1. Endpoints Congés & Absences (`/api/v1/leaves`)

### 1.1. Soumission d'une demande de congé
- **URL :** `POST /api/v1/leaves`
- **Autorisation :** `AUTHENTICATED` (un employé pose pour lui-même, ou DRH/ADMIN)
- **Corps de la requête (`LeaveRequestDto`) :**
  ```json
  {
    "employeeId": "11111111-1111-1111-1111-111111111101",
    "leaveTypeId": "77777777-7777-7777-7777-777777777701",
    "startDate": "2026-11-02",
    "endDate": "2026-11-06",
    "reason": "Congés annuels de repos"
  }
  ```
- **Règles métier :**
  - Calcul automatique des **jours ouvrables** (du lundi au vendredi, exclusion des samedis et dimanches).
  - Vérification du solde : si `leaveType.deductibleFromBalance == true`, refus immédiat `400 Bad Request` si `requestedDays > remainingDays`.
  - Initialisation dynamique du solde si inexistant : calculé au prorata des mois d'ancienneté depuis `hireDate` avec taux paramétrable lu dans `company_settings` (`LEAVE_DAYS_PER_MONTH`, défaut 2.0j/mois).
  - Statut initial : `SUBMITTED` (`EN_ATTENTE`).
- **Réponse :** `201 Created` avec `ApiResponse<LeaveRequestResponseDto>`.

---

### 1.2. Décision Managériale N+1
- **URL :** `PATCH /api/v1/leaves/{id}/approve`
- **Autorisation :** `hasAnyRole('MANAGER', 'DRH', 'ADMIN')`
- **Corps de la requête (`LeaveApprovalDto`) :**
  ```json
  {
    "action": "APPROVE",
    "comment": "Demande validée"
  }
  ```
- **Comportement :**
  - Si `action == "APPROVE"` :
    - Statut passe à `APPROVED_N1` (ou `APPROVED_DRH`).
    - Mise à jour atomique du compteur : `usedDays += requestedDays`, `remainingDays -= requestedDays`.
  - Si `action == "REJECT"` :
    - Statut passe à `REJECTED`. Le solde n'est pas débité.
  - Notification automatique in-app envoyée au collaborateur avec type `LEAVE_STATUS` et message explicite.

---

### 1.3. Annulation de demande
- **URL :** `PATCH /api/v1/leaves/{id}/cancel`
- **Autorisation :** `hasAnyRole('DRH', 'ADMIN')` OU employé demandeur propriétaire
- **Corps de la requête :** Optionnel (`comment`).
- **Comportement :**
  - Si la demande était déjà validée (`APPROVED_N1` ou `APPROVED_DRH`), les jours consommés sont automatiquement restitués : `usedDays -= requestedDays`, `remainingDays += requestedDays`.
  - Statut passe à `CANCELLED`.

---

### 1.4. Calcul prévisionnel des jours ouvrables
- **URL :** `GET /api/v1/leaves/calculate-days?startDate=2026-11-02&endDate=2026-11-06`
- **Autorisation :** `AUTHENTICATED`
- **Réponse (`200 OK`) :**
  ```json
  {
    "success": true,
    "data": {
      "startDate": "2026-11-02",
      "endDate": "2026-11-06",
      "workingDays": 5
    }
  }
  ```

---

### 1.5. Détection des conflits d'équipe
- **URL :** `GET /api/v1/leaves/conflicts?departmentId={deptId}&startDate={start}&endDate={end}`
- **Autorisation :** `AUTHENTICATED`
- **Règle :** Un conflit est détecté si **$\ge 2$ collaborateurs** du même département ont déjà une demande validée ou en attente chevauchant la période.
- **Réponse (`200 OK`) :**
  ```json
  {
    "success": true,
    "data": {
      "departmentId": "22222222-2222-2222-2222-222222222201",
      "startDate": "2026-11-02",
      "endDate": "2026-11-06",
      "hasConflict": true,
      "threshold": 2,
      "overlappingRequestsCount": 2,
      "overlappingRequests": [ ... ]
    }
  }
  ```

---

### 1.6. Calendrier d'équipe
- **URL :** `GET /api/v1/leaves/calendar?departmentId={deptId}&startDate={start}&endDate={end}`
- **Autorisation :** `AUTHENTICATED`
- **Réponse :** Liste des absences de l'équipe sur la plage, incluant le flag `conflicting: boolean`.

---

## 2. Endpoints Dashboard & Statistiques RH (`/api/v1/dashboard`)

### 2.1. Indicateurs Globaux et Analytiques
- **URL :** `GET /api/v1/dashboard/stats`
- **Paramètres optionnels :**
  - `departmentId` (UUID) : Filtre sur un département spécifique
  - `positionId` (UUID) : Filtre sur un poste spécifique
  - `periodYear` (Integer) : Année calendaire (défaut : année en cours)
- **Autorisation :** `hasAnyRole('MANAGER', 'DRH', 'DIRECTION')`
- **Périmètre (Scoping) :**
  - `DRH` et `DIRECTION` : vision consolidée sur toute l'entreprise ou filtrée.
  - `MANAGER` : restreint automatiquement et obligatoirement à son équipe directe (`managerEmployeeId = currentManagerId`).

### 2.2. Structure du Payload de Réponse (`DashboardStatsDto`)
```json
{
  "success": true,
  "data": {
    "periodYear": 2026,
    "headcount": {
      "totalEmployees": 125,
      "activeEmployees": 118,
      "onLeaveEmployees": 4,
      "terminatedEmployees": 3,
      "contractBreakdown": [
        { "contractType": "CDI", "count": 85, "percentage": 72.0 },
        { "contractType": "CDD", "count": 25, "percentage": 21.2 },
        { "contractType": "STAGE", "count": 8, "percentage": 6.8 }
      ],
      "departmentBreakdown": [
        { "departmentId": "...", "departmentName": "Ingénierie & Tech", "employeeCount": 42 }
      ]
    },
    "agePyramid": [
      { "bracket": "< 25 ans", "count": 12, "percentage": 10.2 },
      { "bracket": "25-34 ans", "count": 55, "percentage": 46.6 },
      { "bracket": "35-44 ans", "count": 32, "percentage": 27.1 },
      { "bracket": "45-54 ans", "count": 14, "percentage": 11.9 },
      { "bracket": "55+ ans", "count": 5, "percentage": 4.2 }
    ],
    "turnover": {
      "turnoverRate": 4.8,
      "hiresCount": 8,
      "departuresCount": 3,
      "averageHeadcount": 115.5
    },
    "payroll": {
      "totalGrossSalary": 48500000.0,
      "totalNetSalary": 37200000.0,
      "totalEmployerContributions": 6800000.0,
      "payslipsCount": 118,
      "monthlyTrend": [
        {
          "month": 1,
          "monthLabel": "Janvier",
          "grossSalary": 48500000.0,
          "netSalary": 37200000.0,
          "employerContributions": 6800000.0
        }
      ]
    },
    "training": {
      "totalTrainings": 15,
      "activeSessions": 4,
      "completedSessions": 8,
      "totalParticipants": 62
    },
    "leaves": {
      "pendingRequests": 5,
      "approvedRequests": 34,
      "rejectedRequests": 2,
      "totalDaysTaken": 142.5
    }
  }
}
```

---

## 3. Conformité & Sécurité
1. **Contrôle d'accès :** Sécurité basée sur rôles Spring Security (`hasAnyRole('MANAGER', 'DRH', 'DIRECTION')`).
2. **Protection CSRF / CORS :** Configuré dans `SecurityConfig`.
3. **Audit & Traçabilité :** Tout changement de statut de congé enregistre `approvedBy` et `approvedAt`.
4. **Calcul légal des congés payés au Sénégal :**
   - 2 jours ouvrables par mois de présence effective.
   - 5 jours ouvrables par semaine civile (lundi au vendredi).
