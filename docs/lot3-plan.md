# Plan — Lot 3 : Congés, absences et tableau de bord (Dashboard)

> Statut : **PROPOSÉ, en attente de validation**. Aucune ligne de code n'est écrite avant validation.  
> Branche : `feature/CT-03-leaves-dashboard`, créée depuis `develop` à jour.

---

## 1. Périmètre et Exigences du Cahier des Charges

Le Lot 3 couvre la gestion des absences, le workflow de validation managérial, le calendrier d'équipe avec détection des conflits, et la création du module de tableau de bord RH avec KPIs réels issus de la base de données.

### Objectifs Clés
1. **Workflow des congés** :
   - Cycle de vie : Demande (`EN_ATTENTE` / `SUBMITTED`) $\to$ Validation par le manager N+1 (`VALIDE` / `APPROVED_N1`) ou DRH (`APPROVED_DRH`) $\to$ Mise à jour automatique des compteurs $\to$ Inscription au calendrier d'équipe.
   - États supportés : `EN_ATTENTE`, `VALIDE`, `REFUSE`, `ANNULE`.
   - Notification automatique dans l'application envoyée à l'employé lors de la décision (acceptation, refus).
2. **Règles métier Backend (rigoureusement testées)** :
   - **Refus de dépassement de solde** : Rejet immédiat (`400 Bad Request`) si `requestedDays > remainingDays`.
   - **Calcul des jours ouvrables** : Décompte strict du lundi au vendredi (5 jours ouvrés/semaine), hors week-ends.
   - **Acquisition mensuelle** : Lecture dynamique du paramètre `LEAVE_DAYS_PER_MONTH` (2.0 jours/mois) depuis la table `company_settings`.
   - **Détection des conflits d'équipe** : Règle métier explicite : détection de conflit si $\ge N$ personnes du même département sont absentes sur la même période (seuil paramétrable, défaut $N \ge 2$).
   - **Calendrier d'équipe** : Endpoint `GET /api/v1/leaves/calendar` filtrable par `departmentId`, `startDate`, `endDate`.
3. **Module Dashboard (`/api/v1/dashboard/stats`)** :
   - Création du package backend `com.careertrack.modules.dashboard` (Controller, Service, DTOs, Repository d'agrégation).
   - Accès sécurisé :
     - `DRH` et `DIRECTION` : accès à l'ensemble des données de l'entreprise avec filtres par département, poste et période.
     - `MANAGER` : données automatiquement restreintes à son département / équipe.
     - `EMPLOYE` : accès interdit (403 Forbidden).
   - KPIs réels issus de requêtes d'agrégation SQL testées :
     - **Effectif** : total actif, répartition par type de contrat (CDI, CDD, Stage, etc.), répartition par genre.
     - **Pyramide des âges** : tranches d'âge (<25, 25-34, 35-44, 45-54, 55+) calculées depuis `birth_date`.
     - **Taux de turnover** : départs / effectif moyen sur la période.
     - **Masse salariale** : brut total, cotisations patronales, net total agrégés depuis les bulletins `payslips` existants.
     - **Plan de formation** : formations terminées vs en cours, participants, budget engagé.
     - **Indicateurs congés** : volume en attente, jours consommés.
4. **Frontend & Graphiques Recharts** :
   - Remplacement de tous les mocks de `DashboardPage.tsx` par l'appel à `/api/v1/dashboard/stats`.
   - Graphiques dynamiques Recharts : barres pour la pyramide des âges, anneau (Pie) pour les contrats, barres/aires pour l'évolution et la paie.
   - Remplacement des mocks de `LeaveListPage.tsx` par l'API des congés, affichage du calendrier avec conflits et modale d'approbation N+1.
   - Rendu responsive adapté mobile (360px).

---

## 2. Architecture Backend

### A. Module `leave`
```
com.careertrack.modules.leave/
├── controller/
│   └── LeaveController.java           # Endpoints demandes, validation, soldes, calendrier, conflits
├── dto/
│   ├── LeaveRequestCreateDto.java     # Soumission
│   ├── LeaveApprovalDto.java          # Approbation/Refus avec managerComment
│   ├── LeaveRequestResponseDto.java   # DTO complet de demande
│   ├── LeaveBalanceDto.java           # Compteurs (acquis, pris, restant)
│   ├── TeamLeaveConflictDto.java      # Rapport de conflit d'équipe
│   ├── TeamCalendarItemDto.java       # Entrée du calendrier d'équipe
│   └── WorkingDaysCalculationDto.java # Calcul dynamique de jours ouvrables
├── entity/
│   ├── LeaveRequest.java
│   ├── LeaveBalance.java
│   ├── LeaveType.java
│   └── LeaveStatus.java               # SUBMITTED (EN_ATTENTE), APPROVED_N1 (VALIDE), REJECTED (REFUSE), CANCELLED (ANNULE)
├── service/
│   ├── LeaveService.java
│   └── LeaveServiceImpl.java          # Logique métier, calcul jours ouvrables, déduction solde, notifications
└── repository/
    ├── LeaveRequestRepository.java    # Requêtes de chevauchement et calendrier
    └── LeaveBalanceRepository.java
```

### B. Module `dashboard` (Nouveau)
```
com.careertrack.modules.dashboard/
├── controller/
│   └── DashboardController.java       # GET /api/v1/dashboard/stats
├── dto/
│   ├── DashboardStatsDto.java         # DTO racine consolidé
│   ├── HeadcountKpiDto.java           # Effectif, répartition contrats et genres
│   ├── AgePyramidBracketDto.java      # Tranches d'âge par tranche et genre
│   ├── TurnoverKpiDto.java            # Taux de turnover, arrivées, départs
│   ├── PayrollKpiDto.java             # Masse salariale brute, charges, net
│   ├── TrainingKpiDto.java            # Formations en cours, terminées, participants, budget
│   └── LeaveKpiDto.java               # Congés en attente, pris, taux d'absence
├── repository/
│   └── DashboardAggregationRepository.java # Requêtes SQL natives d'agrégation optimisées
└── service/
    ├── DashboardService.java
    └── DashboardServiceImpl.java      # Filtrage par rôle, département, poste, période
```

---

## 3. Matrice de Sécurité & Droits d'Accès

| Endpoint | Rôle DRH | Rôle DIRECTION | Rôle MANAGER | Rôle EMPLOYE |
| :--- | :---: | :---: | :---: | :---: |
| `POST /api/v1/leaves` (poser un congé) | ✅ Pour soi/tous | ❌ | ✅ Pour soi | ✅ Pour soi |
| `PUT /api/v1/leaves/{id}/validate` | ✅ Tout employé | ❌ | ✅ Ses subordonnés | ❌ 403 |
| `PATCH /api/v1/leaves/{id}/cancel` | ✅ Tout employé | ❌ | ✅ Ses demandes | ✅ Ses demandes |
| `GET /api/v1/leaves/employee/{id}` | ✅ Tous | ❌ | ✅ Ses subordonnés / soi | ✅ Son dossier |
| `GET /api/v1/leaves/balances/{id}` | ✅ Tous | ❌ | ✅ Ses subordonnés / soi | ✅ Son dossier |
| `GET /api/v1/leaves/manager/{id}/pending` | ✅ Tous managers | ❌ | ✅ Ses subordonnés | ❌ 403 |
| `GET /api/v1/leaves/calendar` | ✅ Toute l'entreprise | ✅ Consultation | ✅ Son département | ✅ Son département |
| `GET /api/v1/leaves/conflicts` | ✅ Tous dépts | ✅ Consultation | ✅ Son département | ❌ 403 |
| `GET /api/v1/dashboard/stats` | ✅ Tous dépts | ✅ Tous dépts | ✅ Son département | ❌ 403 |

---

## 4. Règles Métier Détaillées

1. **Jours ouvrables (`calculateWorkingDays`)** :
   - Parcourt chaque date entre `startDate` et `endDate` (incluses).
   - Ignore les samedis et dimanches (`DayOfWeek.SATURDAY`, `DayOfWeek.SUNDAY`).
   - Renvoie le nombre de jours ouvrés exact (ex: du vendredi au lundi = 2 jours ouvrables).
2. **Vérification et Décompte du Solde** :
   - Lors de la soumission : vérifie que le solde `remainingDays >= requestedDays`. Sinon `BadRequestException`.
   - Lors de la validation finale (`VALIDE`) : `usedDays += requestedDays`, `remainingDays -= requestedDays`.
   - En cas d'annulation ultérieure d'un congé validé : réincrémente `remainingDays` et décrémente `usedDays`.
3. **Acquisition mensuelle** :
   - Lecture du paramètre `LEAVE_DAYS_PER_MONTH` (valeur 2.0) depuis `CompanySettingRepository`.
   - Règle de cumul : chaque mois de présence ouvre droit à 2 jours de congé payé.
4. **Détection des Conflits d'Équipe** :
   - Une période présente un conflit d'équipe au sein du département si :
     $$\text{Nombre d'employés distincts absents (validés ou en attente) sur la période} \ge N$$ (avec $N = 2$ par défaut).
   - Le DTO renvoie `hasConflict`, `conflictingCount`, et la liste détaillée des collègues concernés avec leurs dates.
5. **Tableau de Bord & Agrégation SQL** :
   - Utilisation de requêtes SQL natives / HQL projetées dans des DTOs pour garantir des performances instantanées sans charger les entités en mémoire.
   - Période : filtre possible sur l'année en cours (ex: 2026) ou dates spécifiques.

---

## 5. Étapes de Réalisation

### Étape 1 : Backend — Module Leave & Notifications
- Harmoniser `LeaveStatus` (`SUBMITTED`/`EN_ATTENTE`, `APPROVED_N1`/`VALIDE`, `REJECTED`/`REFUSE`, `CANCELLED`/`ANNULE`).
- Implémenter le calcul des jours ouvrables dans `LeaveServiceImpl`.
- Implémenter la validation du solde à la création et l'annulation sécurisée avec restitution des jours.
- Implémenter la détection de conflit ($\ge 2$ absents du même département).
- Implémenter l'endpoint `GET /api/v1/leaves/calendar` (liste des congés sur une plage avec métadonnées employé).
- Déclencher l'envoi de `Notification` (type `LEAVE_STATUS`) au collaborateur lors de la décision du manager.
- Écrire les tests unitaires et d'intégration (`LeaveServiceImplTest`, `LeaveControllerIntegrationTest`).

### Étape 2 : Backend — Module Dashboard
- Créer le package `com.careertrack.modules.dashboard`.
- Implémenter `DashboardAggregationRepository` (requêtes SQL d'agrégation d'effectifs, contrats, âges, paie, formation).
- Implémenter `DashboardServiceImpl` avec filtrage automatique selon le rôle de l'utilisateur connecté.
- Créer `DashboardController` (`GET /api/v1/dashboard/stats`).
- Écrire les tests d'agrégation et de sécurité (`DashboardControllerIntegrationTest`).

### Étape 3 : Frontend — Page Congés & Calendrier d'Équipe (`LeaveListPage.tsx`)
- Brancher les soldes réels de l'employé connecté.
- Formulaire de demande avec sélection du type de congé et décompte automatique des jours ouvrables.
- Onglet Calendrier d'équipe avec filtre par département et badge visuel d'avertissement en cas de conflit détecté.
- File d'approbation pour les managers avec modale de validation/refus et commentaire.
- Responsive design 360px.

### Étape 4 : Frontend — Page Dashboard RH (`DashboardPage.tsx`)
- Remplacer l'ensemble des données statiques par l'appel à l'API `/api/v1/dashboard/stats`.
- Barre de filtres (Département, Poste, Période).
- Intégrer les graphiques Recharts (Pyramide des âges, Répartition des contrats, Masse salariale, Formations).
- Affichage conditionnel selon le rôle (MANAGER voit l'en-tête de son département).
- Vérification responsive sur écran 360px.

### Étape 5 : Documentation & Vérification
- Mettre à jour `docs/api/openapi.yaml` et créer `docs/api/leaves_dashboard_contract.md`.
- Exécuter la suite complète `mvn verify` (0 test ignoré, couverture JaCoCo >= 60%).
- Exécuter `npm run build` côté frontend.
- Vérifier les endpoints réels sur les conteneurs Docker.

---

## 6. Questions Ouvertes pour Validation
1. **Seuil de conflit d'équipe** : Êtes-vous d'accord pour fixer le seuil par défaut à **2 collaborateurs ou plus** absents simultanément dans le même département pour déclencher l'alerte de conflit ?
2. **Jours ouvrables** : Le calcul standard retient les jours du **lundi au vendredi inclus** (5 jours ouvrables par semaine). Cela vous convient-il ?
3. **Statuts des congés** : Pour maintenir la compatibilité avec la base existante tout en respectant l'énoncé, nous conservons les valeurs techniques `SUBMITTED`, `APPROVED_N1`, `REJECTED`, `CANCELLED` avec mapping JSON / affichage français `EN_ATTENTE`, `VALIDE`, `REFUSE`, `ANNULE`. Confirmez-vous ce choix ?
