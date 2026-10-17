# Backlog Technique – Modules & Écrans avec Données Simulées (Tickets Équipe)

Ce document répertorie l'inventaire complet des écrans du frontend utilisant encore des données codées en dur (*mock data*) ou nécessitant un raccordement complet avec les contrôleurs backend Spring Boot.  
Chaque section ci-dessous est prête à être convertie en **Ticket / Issue GitHub** pour l'équipe de développement.

---

## 📌 Synthèse Générale des Tâches à Assigner

| Ticket ID | Module | Fichier Frontend | Statut Données | Endpoint Cible Backend | Assignation Recommandée |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **CT-FEAT-01** | **Dashboard** | `DashboardPage.tsx` | 100% Mock | `/api/v1/dashboard/stats` | Dev 1 & Dev 4 |
| **CT-FEAT-02** | **Paie** | `PayrollListPage.tsx` | 100% Mock | `/api/v1/payroll/payslips` | Dev 5 |
| **CT-FEAT-03** | **Évaluations** | `EvaluationListPage.tsx` | 100% Mock | `/api/v1/evaluations/campaigns` | Dev 4 |
| **CT-FEAT-04** | **Formations** | `TrainingListPage.tsx` | 100% Mock | `/api/v1/trainings` | Dev 4 |
| **CT-FEAT-05** | **Promotions** | `PromotionListPage.tsx` | 100% Mock | `/api/v1/promotions/proposals` | Dev 1 |
| **CT-FEAT-06** | **Documents** | `DocumentListPage.tsx` | 100% Mock | `/api/v1/documents/*` | Dev 5 |
| **CT-FEAT-07** | **Organigramme** | `OrgChartPage.tsx` | 100% Mock | `/api/v1/orgchart/tree` | Dev 5 |
| **CT-FEAT-08** | **Recrutement** | `RecruitmentKanbanPage.tsx` | Initialisation Mock | `/api/v1/recruitment/applications` | Dev 3 |

---

## 1. Ticket CT-FEAT-01 : Module Tableau de Bord (Dashboard)

> **Spécification Prioritaire :**  
> Module dashboard backend à créer : /api/v1/dashboard/stats (effectif, pyramide des âges, turnover, masse salariale, plan de formation, filtres département/période/poste), réservé aux rôles DRH et DIRECTION.

- **Fichier frontend concerné** : `frontend/src/pages/dashboard/DashboardPage.tsx`
- **État actuel** :
  - `contractData` (CDI: 85, CDD: 25, Stage: 10, Prestation: 5) est un tableau local statique.
  - `leaveMonthlyData` (histogramme mensuel des congés de janvier à août) est codé en dur.
  - Les cartes KPI (Effectif total 125, alertes CDD 24 mois, etc.) ne sont pas branchées sur l'API.
- **Tâches à réaliser** :
  1. Backend : Créer le contrôleur `DashboardController` (`GET /api/v1/dashboard/stats`) avec `@PreAuthorize("hasAnyRole('DRH', 'DIRECTION')")`.
  2. Backend : Implémenter le service agrégeant les métriques clés (pyramide des âges, turnover, masse salariale mensuelle, répartition des contrats).
  3. Frontend : Remplacer les constantes locales par un appel `useQuery(['dashboard-stats'], () => api.get('/dashboard/stats'))`.

---

## 2. Ticket CT-FEAT-02 : Module Paie & Bulletins de Salaire

- **Fichier frontend concerné** : `frontend/src/pages/payroll/PayrollListPage.tsx`
- **État actuel** :
  - `mockPayslips` contient 4 bulletins statiques en mémoire (`ps-1` à `ps-4`).
  - Les calculs de masse salariale et de cotisations IPRES/CSS affichés en haut de page sont basés sur ce tableau fictif.
- **Tâches à réaliser** :
  1. Remplacer `mockPayslips` par un appel `useQuery(['payslips', period], ...)` vers `GET /api/v1/payroll/payslips`.
  2. Relier le bouton de téléchargement au flux binaire PDF généré par le backend (`/api/v1/documents/payslips/{id}`).
  3. Gérer le filtrage par période (mois/année) et par département via les paramètres de requête.

---

## 3. Ticket CT-FEAT-03 : Module Campagnes d'Évaluation & Objectifs

- **Fichier frontend concerné** : `frontend/src/pages/evaluations/EvaluationListPage.tsx`
- **État actuel** :
  - `mockCampaigns` (Campagne 2025 clôturée, Mi-Parcours 2026 en planification) est statique.
  - `mockEvaluations` (bilan d'Amadou Diop avec note 4.6/5 et détail des objectifs) est codé en dur.
- **Tâches à réaliser** :
  1. Backend : Exposer les endpoints REST du module `evaluation` (`/api/v1/evaluations/campaigns` et `/api/v1/evaluations`).
  2. Frontend : Récupérer dynamiquement les campagnes ouvertes et l'historique d'évaluation selon le rôle de l'utilisateur connecté (collaborateur ou manager évaluateur).

---

## 4. Ticket CT-FEAT-04 : Module Catalogue & Sessions de Formation

- **Fichier frontend concerné** : `frontend/src/pages/trainings/TrainingListPage.tsx`
- **État actuel** :
  - `mockTrainings` répertorie 3 formations en dur (`TECH-SPB`, `MGT-LEAD`, `RH-DROIT`).
  - L'action « Ajouter une Formation » et l'inscription d'un salarié n'ont pas d'effets de bord persistants.
- **Tâches à réaliser** :
  1. Raccorder la vue au catalogue de formations via `GET /api/v1/trainings`.
  2. Implémenter le formulaire modal de création de session (`POST /api/v1/trainings`) réservé aux rôles DRH.
  3. Ajouter la gestion des inscriptions de collaborateurs aux sessions.

---

## 5. Ticket CT-FEAT-05 : Module Revalorisations & Propositions de Promotion

- **Fichier frontend concerné** : `frontend/src/pages/promotions/PromotionListPage.tsx`
- **État actuel** :
  - `mockPromotions` simule deux propositions (Amadou Diop vers Lead Architecte, Moussa Ba vers Chef Comptable).
  - Le simulateur d'impact masse salariale est simulé localement dans le state du composant.
- **Tâches à réaliser** :
  1. Connecter l'affichage à `GET /api/v1/promotions/proposals`.
  2. Brancher le workflow d'approbation (`PATCH /api/v1/promotions/proposals/{id}/approve` ou `reject`) pour les rôles Direction et DRH.
  3. Connecter le simulateur d'impact masse salariale au moteur de calcul backend.

---

## 6. Ticket CT-FEAT-06 : Module Gestion Électronique des Documents RH

- **Fichier frontend concerné** : `frontend/src/pages/documents/DocumentListPage.tsx`
- **État actuel** :
  - `mockDocs` liste 4 documents fictifs (attestations, contrats de travail, bulletins).
  - Les liens de téléchargement pointent sur des URL de démonstration.
- **Tâches à réaliser** :
  1. Raccorder à l'API de listing des documents archivés ou générés de l'employé.
  2. Connecter les actions de téléchargement au contrôleur iTextPDF (`GET /api/v1/documents/attestation/{employeeId}`, `GET /api/v1/documents/contract/{employeeId}`).

---

## 7. Ticket CT-FEAT-07 : Module Visualisation de l'Organigramme

- **Fichier frontend concerné** : `frontend/src/pages/orgchart/OrgChartPage.tsx`
- **État actuel** :
  - `mockOrgTree` contient une hiérarchie statique (DG Mamadou Ndiaye -> DRH, CTO, DAF).
- **Tâches à réaliser** :
  1. Connecter le composant d'arbre hiérarchique au contrôleur existant :  
     `GET /api/v1/orgchart/tree` (ou `GET /api/v1/orgchart/subordinates/{employeeId}` pour les managers).
  2. Conserver l'intégration du chemin unique `/api/v1/orgchart` dans la stratégie de cache PWA hors-ligne.

---

## 8. Ticket CT-FEAT-08 : Module Recrutement & Pipeline Kanban

- **Fichier frontend concerné** : `frontend/src/pages/recruitment/RecruitmentKanbanPage.tsx`
- **État actuel** :
  - `initialCandidates` initialise les colonnes Kanban (Babacar Fall, Mariama Sarr, Ousmane Cissé).
  - Le glisser-déposer effectue déjà un appel `api.patch('/recruitment/applications/{id}/status')`, mais la liste initiale n'est pas chargée depuis l'API.
- **Tâches à réaliser** :
  1. Charger les candidatures au démarrage via `useQuery` sur `GET /api/v1/recruitment/applications?jobOfferId={id}`.
  2. Lier le bouton d'ajout de candidat à la modale de soumission `POST /api/v1/recruitment/applications`.
