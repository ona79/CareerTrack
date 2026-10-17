# Guide de Contribution & Organisation d'Équipe – CareerTrack

Bienvenue sur le projet **CareerTrack**, la plateforme RH et de gestion des carrières pour PME sénégalaise.  
Ce document fixe les règles communes de développement pour permettre aux **5 développeurs** de travailler simultanément sur le monorepo sans conflits.

---

## 1. Répartition des 5 Lots de Travail (Assignation)

Chaque membre de l'équipe prend en charge un module complet de bout en bout (Backend API + Entités + Services + Contrôleurs + Tests + Écrans Frontend React).

| Développeur | Domaine / Module | Packages Backend | Écrans Frontend | Dépendances / Tiers |
| :--- | :--- | :--- | :--- | :--- |
| **Dev 1 (Lead)** | **Socle, Auth & Employés** | `common.*`, `modules.auth.*`, `modules.employee.*` | `/login`, `/employees`, Dashboard global | Spring Security, JWT, BCrypt, Auditing |
| **Dev 2** | **Congés & Présences** | `modules.leave.*` | `/leaves` (demandes, soldes, approbation) | Règle 2j ouvrables/mois, calendrier |
| **Dev 3** | **Recrutement & Candidats** | `modules.recruitment.*`, `modules.notification.*` | `/recruitment` (Kanban, offres, entretiens) | Mail JavaMailSender, dnd-kit Kanban |
| **Dev 4** | **Évaluations & Formations** | `modules.evaluation.*`, `modules.training.*` | `/evaluations`, `/trainings` | Notations objectifs, catalogues formations |
| **Dev 5** | **Paie, Documents & Organigramme** | `modules.payroll.*`, `modules.document.*`, `modules.orgchart.*` | `/payroll`, `/orgchart` | iTextPDF (bulletins, attestations), cotisations IPRES/CSS |

---

## 2. Stratégie de Branches Git (GitFlow Simplifié)

- **`main`** : Branche de production stable. Ne reçoit que des fusions de releases validées.
- **`develop`** : Branche principale d'intégration. Tout le travail collaboratif est fusionné ici.
- **`feature/CT-<ticket>-<description>`** : Branches de fonctionnalités. Créées depuis `develop`.
  - Exemples :
    - `feature/CT-101-authentification-jwt`
    - `feature/CT-201-demande-conges-calcul-solde`
    - `feature/CT-301-kanban-pipeline-recrutement`
    - `feature/CT-401-campagnes-evaluations-annuelles`
    - `feature/CT-501-generation-bulletin-paie-itext`
- **`fix/CT-<ticket>-<description>`** : Correction d'anomalies sur `develop`.

---

## 3. Format des Messages de Commit (Conventional Commits)

Chaque commit doit respecter le format :
```
<type>(<scope>): <description claire au présent>
```

- **Types autorisés** : `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- **Scopes autorisés** : `auth`, `employee`, `leave`, `payroll`, `recruitment`, `evaluation`, `training`, `orgchart`, `pdf`, `db`, `ci`.

**Exemples :**
- `feat(payroll): calcul automatique des cotisations IPRES (5.6% salarial)`
- `feat(leave): vérification du plafond de solde acquis de 2j/mois`
- `fix(recruitment): correction du statut de candidature dans le kanban`
- `test(backend): ajout du test d'intégration Testcontainers pour les congés`

---

## 4. Règles d'Or pour Éviter les Conflits

1. **Ne jamais modifier les migrations Flyway déjà existantes** :
   - Les fichiers `V1__init_schema.sql` et `V2__reference_data.sql` sont scellés.
   - Toute nouvelle modification de structure doit se faire dans un nouveau fichier de migration horodaté ou numéroté (ex: `V4__add_index_leave_request.sql`).
2. **Isolation des DTOs et Mappers** :
   - Chaque module dispose de ses propres sous-packages `dto`, `entity`, `repository`, `service`, `controller`.
   - Ne pas coupler directement les entités d'un module aux services d'un autre ; passer par les interfaces de service.
3. **Rebase avant Pull Request** :
   - Toujours exécuter `git checkout develop && git pull && git checkout feature/... && git rebase develop` avant d'ouvrir votre PR.
4. **Validation obligatoire du Build** :
   - Le backend doit compiler sans avertissement critique : `mvn clean test-compile`.
   - Le frontend doit passer le build TypeScript : `npm run build`.

---

## 5. Revue de Code (Pull Request)

- Chaque Pull Request doit être relue et approuvée par au moins **1 binôme** de l'équipe avant merge.
- La CI GitHub Actions doit être au vert (tests unitaires + intégration PostgreSQL + build frontend).
