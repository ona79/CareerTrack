# Plan d'Implémentation : LOT 4 - Évaluations & Formations

**Branche :** `feature/CT-04-evaluations-trainings`  
**Date :** Octobre 2026  
**Objectif :** Implémenter le workflow complet des évaluations annuelles avec calcul du score pondéré, génération du PDF signable téléchargeable, et le module de gestion des formations avec sessions, inscriptions et attestations PDF automatiques.

---

## 1. Analyse de l'Existant & Écarts

### 1.1. Module Évaluation
- **Entités existantes :**
  - `EvaluationCampaign` : `title`, `campaignYear`, `startDate`, `endDate`, `status` (`DRAFT`, `OPEN`, `CLOSED`).
  - `Evaluation` : `campaign`, `employee`, `evaluator`, `status` (`DRAFT`, `SELF_SUBMITTED`, `MANAGER_EVALUATED`, `COMPLETED`), `selfScore`, `managerScore`, `finalScore`, `feedback`, `interviewDate`.
  - `Objective` : `evaluation`, `title`, `description`, `category`, `weight`, `targetDate`, `achievementRate`, `status`.
- **Manques Backend :**
  - Endpoints manquants dans `EvaluationController` :
    - `POST /api/v1/evaluations` : création d'une fiche d'évaluation pour un employé dans une campagne.
    - `POST /api/v1/evaluations/{id}/objectives` : ajout / mise à jour des objectifs SMART avec poids et KPIs.
    - `PATCH /api/v1/evaluations/{id}/self-eval` : soumission de l'auto-évaluation par l'employé (`selfScore`, auto-évaluation des objectifs, commentaires).
    - `PATCH /api/v1/evaluations/{id}/manager-eval` : évaluation par le manager N+1 (`managerScore`, taux d'atteinte par objectif, commentaires).
    - `PATCH /api/v1/evaluations/{id}/complete` : validation finale et clôture de l'évaluation.
    - `GET /api/v1/evaluations/my` : évaluations de l'employé connecté.
    - `GET /api/v1/evaluations/team` : évaluations des membres de l'équipe du manager connecté.
    - `GET /api/v1/evaluations/{id}/pdf` : téléchargement du PDF de synthèse d'entretien annuel signable.
  - Calcul du score pondéré :
    - Formule : $\text{score} = \frac{\sum (\text{achievementRate}_i \times \text{weight}_i)}{\sum \text{weight}_i} \times \frac{5}{100}$ (sur une échelle standard de 1 à 5).
    - Test unitaire dédié au calcul du score pondéré.

### 1.2. Module Formation
- **Entités existantes :**
  - `Training` : `code`, `title`, `description`, `durationHours`, `provider`, `category`.
  - `TrainingSession` : `training`, `sessionCode`, `startDate`, `endDate`, `location`, `maxParticipants`, `status`.
  - `TrainingEnrollment` : `session`, `employee`, `enrollmentDate`, `status` (`REQUESTED`, `CONFIRMED`, `ATTENDED`, `COMPLETED`, `DROPPED`), `score`, `certificateUrl`, `feedback`.
- **Manques Backend :**
  - `TrainingSessionController` / Endpoints sessions :
    - `GET /api/v1/trainings/{trainingId}/sessions` : lister les sessions d'une formation.
    - `POST /api/v1/trainings/{trainingId}/sessions` : planifier une session (DRH).
    - `POST /api/v1/trainings/sessions/{sessionId}/enroll` : s'inscrire ou inscrire un employé.
    - `GET /api/v1/trainings/my-enrollments` : mes inscriptions en tant qu'employé.
    - `PATCH /api/v1/trainings/enrollments/{enrollmentId}/status` : mise à jour du statut (ex: passage à `COMPLETED` avec note).
    - `GET /api/v1/trainings/enrollments/{enrollmentId}/certificate` : téléchargement de l'attestation PDF officielle iText.

### 1.3. Service PDF iText (`common/pdf`)
- `PdfGeneratorService` contient déjà `generateAnnualEvaluationPdf(AnnualEvaluationPdfData)` et `generateTrainingCertificatePdf(TrainingCertificatePdfData)`.
- Il suffit de brancher les DTOs du domaine sur ces générateurs et retourner un `byte[]` avec les headers HTTP adéquats (`Content-Type: application/pdf`, `Content-Disposition: attachment; filename=...`).

---

## 2. Plan d'Implémentation Détaillé

### Étape 1 : Backend - Module Évaluation
1. **DTOs & Requêtes :**
   - `EvaluationCreateDto`, `EvaluationSelfSubmitDto`, `EvaluationManagerSubmitDto`, `ObjectiveDto`.
2. **Calcul du score pondéré :**
   - Implémentation dans `EvaluationServiceImpl.calculateWeightedScore(List<Objective> objectives)` avec tests unitaires.
3. **Workflow d'évaluation :**
   - Création fiche $\to$ Auto-évaluation $\to$ Évaluation manager $\to$ Validation finale.
4. **Sécurité & Droits :**
   - Méthodes dans `EvaluationSecurityService` pour `canSelfEvaluate`, `canManagerEvaluate`, `canComplete`.
5. **Génération PDF d'entretien :**
   - Endpoint `GET /api/v1/evaluations/{id}/pdf`.

### Étape 2 : Backend - Module Formation
1. **Services & Repositories :**
   - `TrainingSessionRepository`, `TrainingEnrollmentRepository`.
   - `TrainingSessionDto`, `TrainingEnrollmentDto`.
2. **Workflow d'inscription & complétion :**
   - Inscription d'un employé avec vérification de la capacité max.
   - Validation et marquage `COMPLETED`.
3. **Génération PDF d'attestation :**
   - Endpoint `GET /api/v1/trainings/enrollments/{enrollmentId}/certificate`.

### Étape 3 : Tests Backend & Couverture
1. Tests unitaires du calcul de score d'évaluation (`EvaluationScoreCalculationTest`).
2. Tests unitaires du workflow d'évaluation (`EvaluationServiceImplTest`).
3. Tests d'intégration (`EvaluationControllerIntegrationTest`, `TrainingControllerIntegrationTest`).
4. Vérification de la régénération OpenAPI et couverture JaCoCo $\ge 60\%$.

### Étape 4 : Frontend - Évaluations & Formations
1. **Page Évaluations (`EvaluationListPage.tsx`) :**
   - Branchement sur les endpoints réels (`/campaigns`, `/evaluations/my`, `/evaluations/team`, `/evaluations/{id}`).
   - Parcours en étapes :
     1. Objectifs SMART & Compétences
     2. Auto-évaluation & Commentaires collaborateur
     3. Évaluation & Notation N+1
     4. Écran de validation & Score global
     5. Bouton "Télécharger le Bilan d'Entretien (PDF)"
2. **Page Formations (`TrainingListPage.tsx`) :**
   - Catalogue des formations depuis l'API.
   - Liste des sessions disponibles avec bouton "S'inscrire".
   - Onglet "Mes Formations" avec statut (`CONFIRMED`, `COMPLETED`...) et bouton "Télécharger l'Attestation (PDF)".
   - Modale d'ajout de formation / session pour la DRH.
3. **Responsive Mobile 360px :**
   - Cartes adaptées, étapes d'évaluation scrollables, boutons tactiles `min-h-[44px]`.

---

## 3. Critère de Réussite
- Le workflow complet de l'évaluation annuelle (auto-évaluation $\to$ notation manager $\to$ calcul score pondéré $\to$ validation) fonctionne de bout en bout.
- Le PDF de bilan d'entretien annuel est généré avec iText, signable et téléchargeable.
- L'attestation de fin de formation est générée en PDF et téléchargeable à la complétion.
- 100% des tests Maven passent, couverture JaCoCo $\ge 60\%$, build frontend `npm run build` sans erreur.
