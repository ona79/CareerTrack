# Contrat d'API — Module Employés & Organigramme (Lot 2)

> **Statut** : Aligné rigoureusement sur le code backend CareerTrack (`com.careertrack.modules.employee` et `com.careertrack.modules.orgchart`).  
> **Date de révision** : 19/10/2026

---

## 1. Vue d'ensemble des endpoints

| Méthode | URI | Rôles autorisés | Description |
|---|---|---|---|
| `GET` | `/api/v1/employees` | `DRH`, `MANAGER` | Liste paginée avec DTO réduit (`EmployeeSummaryDto`), recherche et filtres. |
| `GET` | `/api/v1/employees/{id}` | `DRH`, `MANAGER` (subordonné direct), `EMPLOYE` (soi-même) | Dossier modulaire complet (`EmployeeDetailDto`). Données sensibles et salaire masqués pour les managers. |
| `POST` | `/api/v1/employees` | `DRH` | Création d'un collaborateur (renvoie 201 Created). |
| `PUT` | `/api/v1/employees/{id}` | `DRH` | Mise à jour administrative avec contrôle anti-cycle hiérarchique et enregistrement automatique d'historique. |
| `PATCH` | `/api/v1/employees/{id}/contact` | `DRH`, `MANAGER` (soi-même), `EMPLOYE` (soi-même) | Mise à jour des coordonnées personnelles (`phone`, `address`). |
| `PATCH` | `/api/v1/employees/{id}/deactivate` | `DRH` | Désactivation (passe le statut à `TERMINATED`, clôture le contrat actif, désactive le compte utilisateur, refuse avec 409 si subordonnés actifs). |
| `GET` | `/api/v1/employees/{id}/history` | `DRH`, `MANAGER` (subordonné direct), `EMPLOYE` (soi-même) | Historique des postes et salaires (salaire masqué pour manager). |
| `DELETE` | `/api/v1/employees/{id}` | `DRH` | Suppression physique d'un dossier sans dépendances (200 OK). |
| `GET` | `/api/v1/departments` | Tout utilisateur authentifié | Référentiel des départements. |
| `GET` | `/api/v1/positions` | Tout utilisateur authentifié | Référentiel des postes (filtrable par `departmentId`). |
| `GET` | `/api/v1/orgchart/tree` | `DRH`, `MANAGER`, `EMPLOYE`, `DIRECTION` | Organigramme complet de l'entreprise construit en une seule requête SQL, avec protection anti-cycle et temps réel au rechargement. |
| `GET` | `/api/v1/orgchart/subordinates/{employeeId}` | `DRH`, `MANAGER`, `DIRECTION` | Sous-arbre hiérarchique d'un collaborateur donné. |

---

## 2. Spécification détaillée des endpoints

### 2.1 `GET /api/v1/employees`

#### Paramètres de requête
- `page` (int, défaut 0)
- `size` (int, défaut 10, borné à [1, 100])
- `search` (string, optionnel) : Recherche insensible à la casse sur `firstName`, `lastName`, `employeeNumber`
- `departmentId` (UUID, optionnel) : Filtre par département
- `status` (string, optionnel) : Filtre de statut (`ACTIVE`, `ON_LEAVE`, `TERMINATED`). Par défaut, exclut `TERMINATED`. Seule la DRH est autorisée à filtrer par `TERMINATED`.
- `sortBy` (string, défaut `lastName`) : Liste blanche autorisée : `lastName`, `firstName`, `employeeNumber`, `hireDate`, `department`. Toute autre valeur renvoie **400 Bad Request**.
- `sortDir` (string, défaut `asc`) : `asc` ou `desc`.

#### Périmètre par rôle
- **`ROLE_DRH`** : visualise tous les collaborateurs répondant aux filtres.
- **`ROLE_MANAGER`** : visualise **uniquement** ses subordonnés directs (`manager.id = currentEmp.id`) et lui-même (`id = currentEmp.id`).
- **`ROLE_EMPLOYE` / `ROLE_DIRECTION`** : renvoie **403 Forbidden**.

#### Structure JSON de réponse (DTO Réduit)
> **Garantie de sécurité** : Aucun champ sensible (`nationalId`, `birthDate`, `phone`, `email`, `address`, `salary`) n'est présent dans le DTO.

```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": "e7b0a1d4-8d90-4c32-b7e1-889814526df1",
        "employeeNumber": "EMP-003",
        "fullName": "Ousmane Ba",
        "positionTitle": "Développeur",
        "departmentName": "Technologie",
        "status": "ACTIVE"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 2.2 `GET /api/v1/employees/{id}`

Renvoie le dossier modulaire `EmployeeDetailDto` :
- `identity` : Identité professionnelle (toujours visible).
- `personalData` : Données personnelles. **`null` pour un manager consultant un subordonné**.
- `contract` : Contrat actif. `grossSalary` est **`null` pour un manager**.
- `salaryHistory` : Historique salarial. **`null` pour un manager**.
- `positionHistory` : Historique des postes (visible par DRH, collaborateur et manager).
- `documents` : Métadonnées des documents officiels. **`null` pour un manager**.
- `permissions` : Drapeaux booléens (`canEdit`, `canDeactivate`, `canEditContact`).

---

### 2.3 `PATCH /api/v1/employees/{id}/contact`

Permet à l'employé ou au manager de mettre à jour son propre téléphone ou son adresse, sans intervention de la DRH.
```json
{
  "phone": "+221 77 123 45 67",
  "address": "Sacré-Cœur 3, Dakar"
}
```

---

### 2.4 `PATCH /api/v1/employees/{id}/deactivate`

Désactive un collaborateur (`ROLE_DRH` uniquement).
- Si l'employé est responsable de collaborateurs actifs : renvoie **409 Conflict** :
  `"Impossible de désactiver cet employé car il a encore X collaborateur(s) actif(s) sous sa responsabilité. Veuillez réaffecter ses subordonnés avant de le désactiver."`
- Si succès :
  - `status = TERMINATED`
  - Contrat actif fermé (`active = false`, `endDate = departureDate`)
  - Compte `User` lié désactivé (`enabled = false`)
  - Réponse HTTP **200 OK**.

---

### 2.5 `PUT /api/v1/employees/{id}`

Mise à jour administrative intégrale (`ROLE_DRH` uniquement).
- Contrôle anti-cycle hiérarchique :
  - Empêche l'auto-affectation (`managerId == id`) -> **400 Bad Request**.
  - Empêche les cycles hiérarchiques (ex: A subordonné de B, et assigner A comme manager de B) -> **400 Bad Request**.
  - Retrait du manager possible via `managerId: null`.
- Enregistrement automatique d'historique :
  - Changement de poste ou département -> entrée automatique dans `position_histories`.
  - Changement de salaire brut -> entrée automatique dans `salary_histories` et mise à jour du contrat actif.

---

### 2.6 `GET /api/v1/orgchart/tree`

Construit l'arbre hiérarchique complet depuis la direction générale :
- **Optimisation performance** : Une seule requête SQL avec `LEFT JOIN FETCH` des postes, départements et managers.
- **Protection anti-cycle** : Gestion des cycles en mémoire pour empêcher tout dépassement de pile (`StackOverflowError`).
- **Filtrage statut** : Les collaborateurs `TERMINATED` sont exclus de l'arbre.
- **Données nœud** :
  - `id`, `name`, `employeeNumber`, `title`, `department`.
  - `email` : email professionnel inclus (annuaire interne d'entreprise).
  - `phone` : **exclu** (`null`).
  - `subordinateCount` : nombre total de collaborateurs sous la responsabilité (directs et indirects).
  - `subordinates` : liste des nœuds enfants.
- **Temps réel** : Aucun cache serveur ; toute modification de manager est immédiatement reflétée au rechargement.
