# Contrat d'Interface & Spécification API - Lot 4 : Évaluations et Formations

Ce document spécifie les contrats d'API, le workflow métier, la formule de calcul du score pondéré, et les règles de génération de documents PDF pour le **Lot 4 (Évaluations et Formations)** de CareerTrack.

---

## 1. Module Évaluations (`/api/v1/evaluations`)

### 1.1. Campagnes d'Évaluation (`/api/v1/evaluations/campaigns`)

#### GET `/api/v1/evaluations/campaigns`
- **Rôles autorisés :** Authentifié (`RH_ADMIN`, `DRH`, `MANAGER`, `EMPLOYEE`)
- **Description :** Liste toutes les campagnes d'évaluation annuelles.
- **Réponse (200 OK) :** `List<EvaluationCampaignDto>`

#### POST `/api/v1/evaluations/campaigns`
- **Rôles autorisés :** `RH_ADMIN`, `DRH`
- **Description :** Création d'une nouvelle campagne d'évaluation annuelle.
- **Request Body :**
  ```json
  {
    "title": "Campagne Annuelle 2026",
    "year": 2026,
    "startDate": "2026-11-01",
    "endDate": "2026-12-31"
  }
  ```
- **Réponse (201 Created) :** `EvaluationCampaignDto`

#### PATCH `/api/v1/evaluations/campaigns/{id}/close`
- **Rôles autorisés :** `RH_ADMIN`, `DRH`
- **Description :** Clôture d'une campagne.
- **Réponse (200 OK) :** `EvaluationCampaignDto` (`status = CLOSED`)

---

### 1.2. Workflow des Évaluations Annuelles

```
[PLANIFIEE / DRAFT]
         │ (Initialisation de l'évaluation avec objectifs SMART)
         ▼
  [AUTO_EVALUATION]
         │ (Soumission de l'auto-évaluation par l'employé)
         ▼
[EVALUATION_MANAGERIALE]
         │ (Évaluation managériale + calcul automatique du score pondéré)
         ▼
    [A_VALIDER]
         │ (Validation finale et clôture RH / Manager)
         ▼
      [VALIDE]
```

#### GET `/api/v1/evaluations/my`
- **Rôles autorisés :** `EMPLOYEE`, `MANAGER`, `RH_ADMIN`, `DRH`
- **Description :** Récupère les évaluations de l'employé connecté (isolation stricte).
- **Réponse (200 OK) :** `List<EvaluationDto>`

#### GET `/api/v1/evaluations/team`
- **Rôles autorisés :** `MANAGER`, `RH_ADMIN`, `DRH`
- **Description :** Récupère les évaluations des membres directs de l'équipe du manager connecté (`manager_id = currentEmployee.id`).
- **Réponse (200 OK) :** `List<EvaluationDto>`

#### POST `/api/v1/evaluations`
- **Rôles autorisés :** `RH_ADMIN`, `DRH`, `MANAGER`
- **Description :** Initialise une évaluation annuelle pour un employé avec ses objectifs SMART.
- **Request Body (`EvaluationCreateDto`) :**
  ```json
  {
    "employeeId": 42,
    "campaignId": 1,
    "objectives": [
      {
        "title": "Livraison du Lot 4 CareerTrack",
        "weight": 60,
        "indicator": "Zéro bug bloquant, 80% test coverage"
      },
      {
        "title": "Mentorat technique",
        "weight": 40,
        "indicator": "2 revues de code hebdomadaires"
      }
    ]
  }
  ```

#### PATCH `/api/v1/evaluations/{id}/self-eval`
- **Rôles autorisés :** L'employé concerné uniquement (`EvaluationSecurityService.canSelfEvaluate`).
- **Request Body (`EvaluationSelfSubmitDto`) :**
  ```json
  {
    "comments": "Objectifs atteints et dépassement des attentes sur le volet backend.",
    "objectiveSelfRates": {
      "101": 95,
      "102": 100
    }
  }
  ```

#### PATCH `/api/v1/evaluations/{id}/manager-eval`
- **Rôles autorisés :** Le manager N+1 ou RH (`EvaluationSecurityService.canManagerEvaluate`).
- **Effet métier :** Calcule automatiquement le `finalScore` (sur 5.0) via `EvaluationScoreCalculator`.
- **Request Body (`EvaluationManagerSubmitDto`) :**
  ```json
  {
    "managerComments": "Excellent travail sur les composants clés.",
    "managerRatings": {
      "101": 90,
      "102": 95
    }
  }
  ```

#### PATCH `/api/v1/evaluations/{id}/complete`
- **Rôles autorisés :** `RH_ADMIN`, `DRH`, ou Manager autorisé.
- **Request Body (`EvaluationCompleteDto`) :**
  ```json
  {
    "finalComments": "Validation définitive de l'entretien annuel 2026."
  }
  ```

---

### 1.3. Formule du Score Pondéré (`EvaluationScoreCalculator`)

Pour $N$ objectifs SMART ayant chacun un poids $P_i$ et un taux d'atteinte managérial $T_i \in [0, 100]\%$ :

$$\text{Score final (sur 5.0)} = \frac{\sum_{i=1}^N (T_i \times P_i)}{\sum_{i=1}^N P_i} \times \frac{5}{100}$$

- Si les objectifs n'ont pas de poids (somme des poids = 0), une moyenne arithmétique simple est appliquée.
- Le score final est systématiquement borné entre `0.0` et `5.0` et arrondi à 2 décimales.

---

### 1.4. Document PDF Signable de l'Entretien Annuel

#### GET `/api/v1/evaluations/{id}/pdf`
- **Rôles autorisés :** L'employé titulaire, son manager direct, ou DRH (`EvaluationSecurityService.canDownloadPdf`).
- **Format de retour :** `application/pdf`, document binaire prêt à imprimer ou signer électroniquement.
- **Contenu généré :**
  - Entête officiel CareerTrack & identification de la campagne annuelle.
  - Données collaborateur (Nom, Prénom, Matricule, Département, Poste, Date de l'entretien).
  - Tableau récapitulatif des Objectifs SMART (Description, Indicateur clé, Poids %, Note auto-évaluée %, Note manager %).
  - Score pondéré global calculé sur 5.0 avec barre d'appréciation.
  - Synthèse des commentaires de l'employé et du manager.
  - **Zones de signature officielles :** "Signature du Collaborateur" et "Signature du Manager / DRH" avec mentions "Lu et approuvé" et date.

---

## 2. Module Formations (`/api/v1/trainings`)

### 2.1. Sessions de Formation

#### GET `/api/v1/trainings/sessions`
- **Description :** Liste toutes les sessions de formation programmées avec état de remplissage (`enrolledCount / maxParticipants`).

#### POST `/api/v1/trainings/sessions`
- **Rôles autorisés :** `RH_ADMIN`, `DRH`
- **Request Body (`TrainingSessionCreateDto`) :**
  ```json
  {
    "trainingId": 5,
    "startDate": "2026-11-15T09:00:00",
    "endDate": "2026-11-17T17:00:00",
    "location": "Salle Mistral & Distanciel",
    "trainerName": "Dr. Jean Dupont",
    "maxParticipants": 15
  }
  ```

---

### 2.2. Inscriptions et Parcours Collaborateur

#### POST `/api/v1/trainings/sessions/enroll`
- **Rôles autorisés :** Authentifié
- **Règles métier :**
  - Empêche la double inscription à une même session (`ConflictException`).
  - Vérifie la capacité maximale de la session (`BadRequestException` si complet).
- **Request Body :** `{"sessionId": 12, "employeeId": 42}`

#### GET `/api/v1/trainings/my-enrollments`
- **Rôles autorisés :** Authentifié
- **Description :** Historique de toutes les inscriptions et formations suivies par l'employé connecté.

#### PATCH `/api/v1/trainings/enrollments/{id}/status`
- **Rôles autorisés :** `RH_ADMIN`, `DRH`, Formateur
- **Request Body (`EnrollmentStatusUpdateDto`) :**
  ```json
  {
    "status": "COMPLETED",
    "score": 92.5
  }
  ```

---

### 2.3. Attestation Officielle de Formation en PDF

#### GET `/api/v1/trainings/enrollments/{id}/certificate`
- **Rôles autorisés :** L'employé titulaire ou RH.
- **Règle métier :** Accessible **uniquement** si `status == COMPLETED` (sinon `400 Bad Request`).
- **Format :** `application/pdf`.
- **Contenu généré :**
  - Titre solennel : "ATTESTATION DE RÉUSSITE DE FORMATION".
  - Mention nominative du collaborateur et matricule.
  - Intitulé du module de formation, durée totale en heures, formateur et dates.
  - Mention de la complétion et note finale obtenue (ex. `92.5/100`).
  - Cachet officiel CareerTrack et signature de la Direction des Ressources Humaines.
