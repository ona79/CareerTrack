# Contrat d'Interface & Spécification API - Lot 5 : Recrutement et Promotions

Ce document détaille les spécifications d'API, le workflow du pipeline Kanban de recrutement, le mécanisme de notification e-mail avec journalisation, ainsi que le moteur de simulation de la masse salariale en FCFA pour le **Lot 5** de CareerTrack.

---

## 1. Module Recrutement & Pipeline Kanban (`/api/v1/recruitment`)

### 1.1. Offres d'Emploi (`/api/v1/recruitment/offers`)

#### GET `/api/v1/recruitment/offers`
- **Rôles autorisés :** Authentifié (`EMPLOYEE`, `MANAGER`, `DRH`, `DIRECTION`)
- **Description :** Retourne la liste de toutes les offres de recrutement avec le nombre de candidatures associées.
- **Réponse (200 OK) :** `List<JobOfferDto>`

#### POST `/api/v1/recruitment/offers`
- **Rôles autorisés :** `DRH`, `MANAGER`
- **Description :** Publication d'une nouvelle offre d'emploi.
- **Request Body (`JobOfferCreateDto`) :**
  ```json
  {
    "title": "Ingénieur DevOps Cloud",
    "departmentId": "48f10642-e565-4f4e-a0e4-b778c801b633",
    "positionId": "1b08ce74-4b9b-449a-8c76-5ca4e5c84d67",
    "contractType": "CDI",
    "description": "Gestion des clusters Kubernetes, CI/CD GitHub Actions et observabilité Grafana.",
    "requirements": "3+ ans d'expérience cloud AWS ou GCP, maîtrise de Terraform et Docker.",
    "openPositions": 2,
    "closingDate": "2026-12-31"
  }
  ```
- **Réponse (200 OK) :** `ApiResponse<JobOfferDto>`

#### GET `/api/v1/recruitment/offers/{id}`
- **Description :** Détail complet d'une offre d'emploi.
- **Réponse (200 OK) :** `ApiResponse<JobOfferDto>`

---

### 1.2. Candidatures & Pipeline Kanban (`/api/v1/recruitment/applications`)

#### Pipeline en 6 Colonnes Métier :
1. `RECU` : Candidature enregistrée dans le vivier.
2. `PRESELECTIONNE` : Profil retenu pour premier tri RH.
3. `ENTRETIEN` : Entretien planifié (RH, technique ou direction).
4. `OFFRE` : Proposition formelle d'embauche émise.
5. `EMBAUCHE` : Embauche confirmée, intégration collaborateur.
6. `REFUSE` : Candidature non retenue.

#### GET `/api/v1/recruitment/applications`
- **Rôles autorisés :** `DRH`, `MANAGER`
- **Description :** Liste toutes les candidatures pour affichage dans le tableau Kanban glisser-déposer.
- **Réponse (200 OK) :** `ApiResponse<List<JobApplicationDto>>`

#### POST `/api/v1/recruitment/applications`
- **Rôles autorisés :** `DRH`, `MANAGER`
- **Description :** Dépôt d'une nouvelle candidature. Crée automatiquement le profil `Candidate` si l'email n'existe pas en base.
- **Request Body (`JobApplicationCreateDto`) :**
  ```json
  {
    "jobOfferId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "firstName": "Fatou",
    "lastName": "Ndiaye",
    "email": "fatou.ndiaye@example.sn",
    "phone": "+221 77 123 45 67",
    "cvUrl": "https://storage.careertrack.sn/cv/fatou_ndiaye.pdf",
    "notes": "Recommandation senior lead dev"
  }
  ```

#### PATCH `/api/v1/recruitment/applications/{id}/status?status=...`
- **Rôles autorisés :** `DRH`, `MANAGER`
- **Description :** Déplace la carte candidat sur le Kanban.
- **Mise à jour optimiste frontend :** Déplacement visuel immédiat avec rollback en cas d'erreur réseau.
- **Effets de bord :**
  1. Envoi d'un e-mail automatique au candidat (`mailSender.send`).
  2. **Journalisation obligatoire dans la table `notifications` :**
     - `recipient`: `null` (candidat externe)
     - `recipientEmail`: email du candidat
     - `channel`: `EMAIL`
     - `notificationType`: `RECRUITMENT`
     - `metadata`: JSON contenant l'identifiant de la candidature et le statut cible.

---

## 2. Module Promotions & Masse Salariale (`/api/v1/promotions`)

### 2.1. Propositions de Promotion

#### GET `/api/v1/promotions`
- **Rôles autorisés :** `DRH`, `MANAGER`, `DIRECTION`
- **Description :** Liste toutes les propositions de promotion.
- **Scoping de confidentialité salariale :**
  - Si l'utilisateur est `DRH` ou `DIRECTION`, les champs `currentSalary` et `proposedSalary` sont restitués en clair (en FCFA).
  - Pour les autres rôles (Managers sans droit DRH), les montants nominatifs sont masqués (`null`) pour garantir la stricte confidentialité des salaires.

#### POST `/api/v1/promotions`
- **Rôles autorisés :** `DRH`, `MANAGER`
- **Description :** Soumission d'une proposition d'avancement ou de revalorisation par le manager N+1 ou la DRH.
- **Request Body (`PromotionProposalDto`) :**
  ```json
  {
    "employeeId": "1a3e6f9b-1234-4a5b-9c7d-8e9f0a1b2c3d",
    "proposedPositionId": "2c4f8a0e-5678-4b6c-8d9e-0f1a2b3c4d5e",
    "proposedSalary": 1400000,
    "justification": "Excellente gestion de projet et montée en compétences techniques."
  }
  ```

#### PATCH `/api/v1/promotions/{id}/status?status=APPROVED|REJECTED`
- **Rôles autorisés :** `DRH` uniquement
- **Effets lors de l'approbation (`APPROVED`) :**
  1. Création d'une entrée dans `position_histories` (traçabilité du changement de poste).
  2. Mise à jour du poste de l'employé dans `employees`.
  3. Création d'une entrée dans `salary_histories` (traçabilité de l'augmentation salariale).
  4. Mise à jour du contrat de travail actif dans `contracts` (`gross_salary = proposedSalary`).

---

### 2.2. Simulateur d'Impact sur la Masse Salariale en FCFA

#### POST `/api/v1/promotions/simulations`
- **Rôles autorisés :** `DRH`, `DIRECTION`
- **Description :** Calcule en temps réel l'impact financier d'un pourcentage d'augmentation sur la masse salariale brute des contrats actifs.
- **Request Body (`SalarySimulationRequestDto`) :**
  ```json
  {
    "title": "Revalorisation Cadres IT 2026",
    "departmentId": "48f10642-e565-4f4e-a0e4-b778c801b633",
    "averageIncreasePercentage": 7.5
  }
  ```
  *(Note : si `departmentId` est omis ou `null`, la simulation est globale pour toute l'entreprise).*

- **Formule de Calcul Backend (`SalarySimulationCalculator`) :**
  $$\text{Masse Actuelle} = \sum_{c \in C_{\text{actifs}}} c.\text{grossSalary}$$
  $$\text{Écart Mensuel (Surcoût)} = \text{Masse Actuelle} \times \frac{\text{Taux}}{100}$$
  $$\text{Masse Projetée} = \text{Masse Actuelle} + \text{Écart Mensuel}$$
  $$\text{Impact Annuel} = \text{Écart Mensuel} \times 12$$

- **Réponse (200 OK) :** `SalarySimulationResultDto`
  ```json
  {
    "id": "e9b72a44-0123-4567-89ab-cdef01234567",
    "title": "Revalorisation Cadres IT 2026",
    "departmentId": "48f10642-e565-4f4e-a0e4-b778c801b633",
    "departmentName": "Direction des Systèmes d'Information",
    "averageIncreasePercentage": 7.5,
    "currentMonthlyPayroll": 24500000.00,
    "projectedMonthlyPayroll": 26337500.00,
    "monthlyDifference": 1837500.00,
    "projectedAnnualPayrollImpact": 22050000.00,
    "affectedEmployeesCount": 18,
    "simulatedAt": "2026-10-19T17:30:00Z",
    "createdByName": "drh.user"
  }
  ```

#### GET `/api/v1/promotions/simulations`
- **Rôles autorisés :** `DRH`, `DIRECTION`
- **Description :** Historique des simulations salariales sauvegardées en base dans la table `salary_simulations`.
