# Plan de Mise en Œuvre - LOT 5 : Recrutement et Promotions

## Contexte & Objectifs du Lot
- **Branche :** `feature/CT-05-recruitment-promotions` (basée sur `develop`)
- **Modules backend :** `recruitment`, `promotion`, `notification`
- **Pages frontend :** `RecruitmentKanbanPage.tsx` (Kanban glisser-déposer), `PromotionListPage.tsx` (promotions et simulations), et activation/correction des boutons identifiés :
  - "Ajouter une formation" (catalogue & sessions)
  - "Simulateur Masse Salariale"
  - "Proposer une promotion"
  - "Détails de simulation"
  - "Valider la promotion"
  - "Lancer une campagne" (évaluations)
  - "Télécharger Bilan Entretien" (évaluations)
  - "Créer une offre" (recrutement)
  - "Voir les offres" (recrutement)
  - "Nouveau salarié" (employés)
  - "Exporter Registre" (registre unique du personnel)

---

## 1. Module Recrutement & Pipeline Kanban
### Backend
- **Entités & Tables :** `job_offers`, `candidates`, `job_applications`, `application_interviews`.
- **Publication d'offres :**
  - DTOs : `JobOfferDto`, `JobOfferCreateDto`.
  - Endpoints :
    - `POST /api/v1/recruitment/offers` (création d'offre DRH/Manager : code, title, departmentId, positionId, contractType, description, requirements, openPositions, closingDate).
    - `GET /api/v1/recruitment/offers` (liste des offres d'emploi, avec filtres par statut).
    - `GET /api/v1/recruitment/offers/{id}` (détail d'une offre).
- **Candidatures :**
  - DTOs : `JobApplicationCreateDto`.
  - Endpoints :
    - `GET /api/v1/recruitment/applications` (liste complète des candidatures pour le Kanban).
    - `POST /api/v1/recruitment/applications` (dépôt/création d'une candidature pour une offre).
    - `PATCH /api/v1/recruitment/applications/{id}/status?status=...` (mise à jour de l'étape : `RECU`, `PRESELECTIONNE`, `ENTRETIEN`, `OFFRE`, `EMBAUCHE`, `REFUSE`).
- **Notification email automatique & journalisation :**
  - À chaque changement de statut pertinent (`PRESELECTIONNE`, `ENTRETIEN`, `OFFRE`, `EMBAUCHE`, `REFUSE`).
  - Envoi via `JavaMailSender` (support SMTP Mailpit / fallback sans crash).
  - **Enregistrement dans la table `notifications` :**
    - `channel = NotificationChannel.EMAIL`
    - `notificationType = NotificationType.RECRUITMENT`
    - Titre, contenu du message, date d'envoi, métadonnées JSON (candidature, statut, email candidat).
    - Mise à jour de `Notification.java` pour supporter `recipient` nullable et `recipientEmail`.

### Frontend (`RecruitmentKanbanPage.tsx`)
- Remplacer les données simulées `initialCandidates` par les appels API réels `GET /recruitment/applications`.
- Glisser-déposer `@dnd-kit` :
  - Support mobile tactile : `PointerSensor` / `TouchSensor` avec délai/tolérance pour permettre à la fois le scroll horizontal et le drag & drop au doigt.
  - Mise à jour optimiste du statut avec rollback (retour arrière) immédiat en cas d'échec réseau.
  - Alerte de confirmation visuelle de l'email envoyé.
- **Bouton "Créer une Offre" :** Modale de saisie complète connectée à `POST /recruitment/offers` avec rafraîchissement immédiat.
- **Bouton "Voir les Offres" :** Modale/Drawer affichant la liste des offres avec statuts, postes vacants, dates de clôture et candidatures associées.

---

## 2. Module Promotions & Masse Salariale
### Backend
- **Propositions de promotion :**
  - DTO : `PromotionProposalCreateDto`, `PromotionProposalDto`.
  - `POST /api/v1/promotions` : soumission par le manager ou la DRH.
  - `PATCH /api/v1/promotions/{id}/status?status=APPROVED|REJECTED` : validation par la DRH.
  - Mise à jour automatique de `SalaryHistory` et `PositionHistory` lors de l'approbation (`APPROVED`).
- **Simulateur d'impact sur la masse salariale en FCFA :**
  - DTOs : `SalarySimulationRequestDto`, `SalarySimulationResultDto`.
  - Calcul métier précis :
    - Récupération des contrats actifs (`Contract.active = true`) filtrés ou globaux.
    - Masse salariale mensuelle actuelle $\sum \text{grossSalary}$.
    - Nouvelle masse salariale projetée avec le taux d'augmentation.
    - Écart mensuel en FCFA et impact annuel en FCFA ($\times 12$).
    - Nombre de salariés impactés.
    - Sauvegarde dans la table `salary_simulations`.
  - Endpoints :
    - `POST /api/v1/promotions/simulations` : exécuter et enregistrer une simulation.
    - `GET /api/v1/promotions/simulations` : lister les simulations passées.
    - `GET /api/v1/promotions/simulations/{id}` : afficher les détails d'une simulation.
- **Sécurité et scoping salarial :**
  - Salaires visibles uniquement pour les rôles `DRH` et `DIRECTION` (`ROLE_DRH`, `ROLE_DIRECTION`). Pour les managers sans ces rôles, masquer les montants non autorisés.

### Frontend (`PromotionListPage.tsx`)
- Remplacer `mockPromotions` par `GET /promotions`.
- **Bouton "Simulateur Masse Salariale" :** Modale interactive permettant de choisir un département (ou toute l'entreprise), un pourcentage d'augmentation, de lancer le calcul et d'afficher les résultats en FCFA (masse actuelle, projetée, écart mensuel et annuel).
- **Bouton "Proposer une promotion" :** Modale interactive avec sélection de l'employé, poste cible, saisie du nouveau salaire avec calcul instantané du % d'augmentation, justification et soumission API.
- **Bouton "Valider la promotion" :** Action directe pour la DRH avec boîte de confirmation et commentaire optionnel, déclenchant le `PATCH /promotions/{id}/status?status=APPROVED`.
- **Bouton "Détails simulation" :** Affichage d'une vue détaillée avec l'historique des simulations salariales et leurs impacts budgétaires.

---

## 3. Activation & Fiabilisation des Boutons Applicatifs
1. **Rôles et AuthContext :**
   - Améliorer `hasRole()` dans `AuthContext.tsx` pour reconnaître automatiquement les rôles qu'ils soient préfixés par `ROLE_` ou non (`DRH` <=> `ROLE_DRH`, `MANAGER` <=> `ROLE_MANAGER`, etc.).
2. **"Ajouter une formation" & "Planifier une session" :**
   - Dans `TrainingListPage.tsx`, rendre les boutons et modales visibles et pleinement interactifs pour la DRH / Admin.
3. **"Lancer une campagne" & "Télécharger Bilan Entretien" :**
   - Dans `EvaluationListPage.tsx`, s'assurer que le bouton "Nouvelle Campagne" / "Lancer une campagne" s'affiche pour la DRH et que "Télécharger Bilan Entretien" télécharge immédiatement le PDF officiel signable.
4. **"Nouveau salarié" :**
   - Dans `EmployeeListPage.tsx`, vérifier la visibilité du bouton et ajouter un raccourci ou intitulé clair "Nouveau Salarié".
5. **"Exporter Registre" :**
   - Dans `EmployeeListPage.tsx`, ajouter le bouton "Exporter Registre" qui génère et télécharge le Registre Unique du Personnel officiel (fichier CSV conforme OHADA / droit du travail sénégalais avec matricule, nom, prénom, nationalité, date de naissance, CNI, date d'embauche, département, poste, contrat).

---

## 4. Tests & Validation
- Tests unitaires `SalarySimulationServiceTest`, `RecruitmentServiceTest`, `PromotionServiceImplTest`.
- Tests d'intégration `RecruitmentControllerIntegrationTest`, `PromotionControllerIntegrationTest`.
- Maintien du taux de tests réussis à 100% (200+ tests) et couverture JaCoCo >= 60%.
- Validation du build frontend (`npm run build`).
