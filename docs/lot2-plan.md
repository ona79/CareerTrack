> Statut : **VALIDÉ** (19/10/2026).
> Branche : `feature/CT-02-employee-orgchart`, créée depuis `develop`.

---

## 0. Audit de l'existant (constaté dans le code, 19/10/2026)

### Backend `employee`
| Élément | État actuel | Problème |
|---|---|---|
| `GET /api/v1/employees` | DRH + MANAGER, paginé (`page`, `size`, `sortBy`) | Renvoie `EmployeeResponseDto` **complet** (nationalId, adresse, date de naissance, téléphone) pour tout le monde, manager compris. Pas de recherche ni de filtre département. `sortBy` libre (une valeur inconnue provoque une erreur 500). |
| `GET /api/v1/employees/{id}` | `@employeeSecurity.canAccessEmployee` (DRH, soi-même, manager direct) | Pas de contrat, de poste détaillé, d'historique ni de documents. Le manager voit aussi les données sensibles. |
| `POST` / `PUT` | DRH | `PUT` ne permet pas de **retirer** un manager (null est ignoré). Pas de contrôle de **cycle** (A manager de B, B manager de A), ni d'auto-référence. `is_cadre`, `marital_status`, `dependents_count` (V12) ne sont pas éditables. |
| `DELETE /{id}` | DRH, **suppression physique** | Contraire à « désactivation » : casse l'historique (bulletins, congés, évaluations liés). |
| `GET /department/{id}` | DRH + MANAGER | Doublon du futur filtre. |
| `SalaryHistory` | Entité + repository, table `salary_histories` (V1) | **Aucun service ni endpoint**. Pas de table d'historique des **postes**. |
| Référentiels | — | **Aucun endpoint** `departments` / `positions` (nécessaires au filtre et aux formulaires). |

### Backend `orgchart`
- `GET /api/v1/orgchart/tree` existe et part bien de `manager_id` (racines = `manager IS NULL`).
- Construction récursive avec **une requête SQL par nœud** (N+1) et **aucune protection contre les cycles** (boucle infinie / StackOverflow si une donnée est corrompue).
- Le nœud expose `email` et `phone` à tous les rôles, y compris EMPLOYE.
- Les employés `TERMINATED` apparaissent dans l'arbre.

### Frontend
- `EmployeeListPage.tsx` : tableau, filtre et recherche **côté client** sur la première page seulement, données fictives en dev (`defaultMockEmployees`).
- `OrgChartPage.tsx` : **`mockOrgTree` codé en dur**, aucun appel API, aucun zoom.
- Aucune bibliothèque de graphe installée (ni React Flow, ni d3).
- PWA (`vite.config.ts`) : `NetworkFirst` sur `/api/v1/(employees|leaves|orgchart)`, avec 24 h de cache. **Le détail `/employees/{id}` (données sensibles) est donc mis en cache sur l'appareil.**

---

## 1. Dossier employé complet

### 1.1 Endpoint de détail : `GET /api/v1/employees/{id}`
Il renvoie un `EmployeeDetailDto` composé de blocs, dont le contenu dépend du rôle :

| Bloc | DRH | Employé lui-même | Manager (subordonné direct) | DIRECTION |
|---|---|---|---|---|
| Identité pro (matricule, nom, poste, département, manager, date d'embauche, statut, email pro) | ✅ | ✅ | ✅ | 403 |
| Données personnelles (nationalId, date de naissance, adresse, téléphone perso, situation familiale, enfants, cadre) | ✅ | ✅ | ❌ (bloc `null`) | 403 |
| Contrat actif (type, début, fin, période d'essai) | ✅ | ✅ | type et dates seulement | 403 |
| Salaire (contrat actif + historique salarial) | ✅ | ✅ | ❌ | 403 |
| Historique des postes / avancements | ✅ | ✅ | ✅ | 403 |
| Documents liés (métadonnées seules, lecture) | ✅ | ✅ | ❌ (règle Lot 1 : 403 sur les documents des subordonnés) | 403 |

- Le filtrage est fait **côté serveur** : le bloc interdit n'est pas sérialisé, il n'est pas simplement masqué côté client. Des tests vérifient l'absence des clés pour chaque rôle.
- Les documents sont lus via le service du Lot 1 (`EmployeeDocumentService`, projection sans octets) ; aucun endpoint d'upload n'est ajouté ici. Le bouton de téléchargement réutilise `/api/v1/documents/{id}/download`.

### 1.2 Historique des avancements
- **Salaire** : on expose `salary_histories` (déjà en V1) via `GET /api/v1/employees/{id}/history`.
- **Postes** : nouvelle table `position_histories` (`employee_id`, `previous_position_id`, `new_position_id`, `previous_department_id`, `new_department_id`, `effective_date`, `reason`, `changed_by`, `created_at`) dans une migration **V14**. V13 est déjà prise par `db/dev-migration`. À vérifier : Flyway lit les deux dossiers dans le même espace de versions ; si c'est le cas, on prend **V14**, sinon la version libre suivante. Ce point sera vérifié avant d'écrire la migration.
- Écriture automatique : tout changement de poste, de département ou de salaire via `PUT` crée une ligne d'historique dans la **même transaction**.
- Lien avec le module `promotion` : un `promotion_proposal` approuvé alimente aussi cet historique. Cela touche un fichier d'un autre module : il sera listé dans la PR. **Question 3.**

### 1.3 Création, modification, désactivation selon le rôle
| Action | DRH | MANAGER | EMPLOYE | DIRECTION |
|---|---|---|---|---|
| Créer | ✅ | ❌ | ❌ | ❌ |
| Modifier tout le dossier (dont manager, poste, cadre, situation familiale) | ✅ | ❌ | ❌ | ❌ |
| Modifier ses coordonnées (téléphone, adresse) | ✅ | lui-même | lui-même (**Question 2**) | ❌ |
| Désactiver | ✅ | ❌ | ❌ | ❌ |

- **Désactivation** : `PATCH /api/v1/employees/{id}/deactivate` (motif et date de sortie). Elle passe le statut à `TERMINATED`, clôt le contrat actif (`end_date`, `active=false`) et désactive le compte utilisateur lié (`users.enabled=false`).
  - Si l'employé a des subordonnés : on renvoie **409**, avec un message en français qui demande de réaffecter d'abord ses N-1. Pas de réaffectation silencieuse.
- **`DELETE` physique** : supprimé, ou réservé à un employé sans aucune donnée liée. **Question 1.**
- **Manager** : il est retirable (`managerId: null` explicite). Le serveur refuse l'auto-référence et les **cycles** (on remonte la chaîne des managers ; 400 avec message français). Le manager doit être `ACTIVE`.
- Les mises à jour partielles passent par un `EmployeeUpdateDto` distinct de `EmployeeCreateDto` (matricule non modifiable).

### 1.4 Référentiels (lecture)
- `GET /api/v1/departments` et `GET /api/v1/positions?departmentId=` (DRH, MANAGER) pour le filtre et les formulaires. Pas de CRUD des référentiels dans ce lot (hors périmètre).

---

## 2. Liste `/employees` réduite, paginée et filtrable

`GET /api/v1/employees?page=0&size=20&search=diop&departmentId=…&status=ACTIVE&sort=lastName,asc`

- Réponse : `PageResponse<EmployeeSummaryDto>` contenant **uniquement** `id`, `employeeNumber`, `fullName`, `positionTitle`, `departmentName`, `status`. Ni email, ni téléphone, ni nationalId, ni salaire, ni date de naissance.
- La recherche est faite **côté serveur** (insensible à la casse, sur prénom, nom, matricule) via `Specification` JPA, avec fetch join du poste et du département (pas de N+1).
- Le tri passe par une liste blanche (`lastName`, `firstName`, `employeeNumber`, `hireDate`, `department`). Toute autre valeur renvoie 400, plus jamais 500. `size` est plafonné à 100.
- Périmètre par rôle :
  - DRH : tous les employés.
  - MANAGER : **ses subordonnés directs + lui-même**. Aujourd'hui il voit tout le monde. **Question 4.**
  - EMPLOYE, DIRECTION : 403 (inchangé).
- Statut `TERMINATED` exclu par défaut, visible avec `status=TERMINATED` (DRH uniquement).
- Tests : un test **échoue si une clé sensible** (`nationalId`, `birthDate`, `address`, `phone`, `email`, `salary`…) apparaît dans un élément de la liste. Même principe que le test « aucun champ binaire » du Lot 1.
- `GET /employees/department/{id}` : déprécié et remplacé par le filtre. On le supprime s'il n'a aucun appelant frontend (vérification par grep).

---

## 3. Organigramme interactif

### 3.1 Backend `GET /api/v1/orgchart/tree`
- **Une seule requête** charge les employés non `TERMINATED` (fetch join du poste et du département). L'arbre est construit en mémoire à partir de `manager_id` (map `managerId -> enfants`), en O(n).
- Protection contre les cycles : ensemble des nœuds visités ; un nœud pris dans un cycle est rattaché à la racine avec `cycleDetected=true` et l'incident est journalisé. Plus de boucle infinie possible.
- Un employé dont le manager est `TERMINATED` ou absent devient une racine. Le cas de plusieurs racines est géré (liste).
- Nœud réduit : `id`, `fullName`, `positionTitle`, `departmentName`, `subordinateCount`, `children`. **Email et téléphone retirés** (**Question 5**).
- Lecture seule, **sans cache serveur** : un changement de manager est visible au rechargement suivant (critère du cahier). Le cache PWA est traité au § 4.
- Accès : tous les rôles authentifiés (inchangé).
- Tests : arbre de 3 niveaux, plusieurs racines, cycle A↔B sans boucle, exclusion des `TERMINATED`, nombre de requêtes SQL borné (statistiques Hibernate), et le **scénario du cahier** : `PUT` du manager de X, puis `GET /tree`, où X apparaît sous son nouveau manager.

### 3.2 Frontend
- Bibliothèque recommandée : **React Flow (`@xyflow/react`, MIT)**, avec **`@dagrejs/dagre`** pour la disposition hiérarchique.
  - Pourquoi : composant React déclaratif, zoom, déplacement et pincement tactile natifs, mini-carte et contrôles inclus, typé TypeScript.
  - `d3-org-chart` est plus impératif (manipulation directe du DOM), s'intègre moins bien dans React 19, et sa maintenance est moins active.
  - Les versions exactes compatibles React 19 seront vérifiées avant installation.
- Nœud personnalisé : initiales, nom, poste, département, badge du nombre de N-1, bouton replier/déplier.
- **Clic ou tap sur un nœud** : ouverture du dossier `/employees/{id}`.
  - Si l'utilisateur n'a pas le droit de voir ce dossier (ex. EMPLOYE qui clique sur un collègue), il obtient une fiche réduite issue du nœud (nom, poste, département), **sans appel 403 ni écran d'erreur**.
- Recherche d'un employé dans l'arbre, avec centrage et zoom sur le nœud trouvé.
- `mockOrgTree` est supprimé. En cas d'erreur API : message et bouton Réessayer, pas de faux arbre.

---

## 4. Mobile (360 px) et cache

- **Liste** : en dessous de 640 px, le tableau devient une liste de **cartes** (nom, poste, département, badge de statut, zone tactile ≥ 44 px). La pagination passe en « Charger plus ». La recherche et le filtre département restent dans une barre repliable.
- **Dossier** : onglets (Infos, Contrat, Historique, Documents) qui deviennent un accordéon vertical sur mobile.
- **Organigramme** : `panOnDrag`, `zoomOnPinch`, `zoomOnDoubleClick`, boutons + / − / recentrer de taille ≥ 44 px, mini-carte masquée sous 640 px, disposition verticale.
- Vérification : captures à **360×740** (liste, dossier, organigramme) avec le navigateur intégré, jointes à la PR.
- **PWA** :
  - Le motif de cache est restreint à la **liste et à l'organigramme** ; le détail `/employees/{id}` et `/history` sont **exclus du cache** (données sensibles).
  - `NetworkFirst` est conservé : en ligne, le rechargement affiche toujours la donnée fraîche, ce qui respecte le critère « temps réel au rechargement ».
  - Cache vidé à la déconnexion (déjà en place).

---

## 5. Fichiers partagés ou hors module (à lister dans la PR)
- `db/migration/V14__position_histories.sql` (numéro à confirmer, § 1.2).
- `common/security/EmployeeSecurityService.java` (règles par bloc).
- `modules/promotion/...` si la Question 3 est acceptée.
- `modules/document/service` : lecture seule, réutilisation sans modification prévue.
- `frontend/vite.config.ts` (motif de cache PWA), `frontend/package.json` (React Flow, dagre), `frontend/src/types/index.ts`.
- `docs/api/openapi.yaml` (régénéré), nouveau `docs/api/employees_orgchart_contract.md`, aligné **à l'identique** sur le code, avec relecture intégrale comme pour les documents.

---

## 6. Étapes d'implémentation (après validation)
Après **chaque** étape : `mvn verify` (0 test ignoré, % JaCoCo) et `npm run build` pour le frontend, avec un résumé court.

- **(a)** Migration de l'historique des postes, DTOs (`EmployeeSummaryDto`, `EmployeeDetailDto`, `EmployeeUpdateDto`), référentiels departments et positions.
- **(b)** Liste paginée, recherche et filtre côté serveur, périmètre par rôle, tests d'absence des champs sensibles.
- **(c)** Détail par blocs et rôles, historique, désactivation, contrôle des cycles du manager, tests.
- **(d)** `/orgchart/tree` en une requête avec anti-cycle, tests dont le scénario « changement de manager visible au rechargement ».
- **(e)** Frontend : liste et cartes mobiles, page dossier, formulaire DRH, organigramme React Flow, cache PWA, captures 360 px.
- **Une seule PR** à la fin, vers `develop`.

---

## 7. Décisions validées (19/10/2026)
1. **DELETE physique supprimé** : Remplacé par la désactivation (`PATCH /api/v1/employees/{id}/deactivate`). L'endpoint `DELETE /api/v1/employees/{id}` est supprimé ou désactivé pour protéger l'intégrité de l'historique (bulletins, congés, évaluations).
2. **Auto-modification des coordonnées** : L'employé et le manager peuvent modifier leur propre `phone` et `address` via `PATCH /api/v1/employees/{id}/contact`. La DRH modifie tout le reste.
3. **Promotions et historique** : Une promotion approuvée dans `PromotionServiceImpl` enregistre automatiquement les entrées dans `salary_histories` et `position_histories`.
4. **Périmètre liste pour MANAGER** : Limité à ses subordonnés directs (`manager_id = currentEmp.id`) + lui-même (`id = currentEmp.id`).
5. **Organigramme** : L'email professionnel est conservé (annuaire d'entreprise), mais le numéro de téléphone personnel est exclu des nœuds.
