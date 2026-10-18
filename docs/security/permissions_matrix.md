# Matrice Complète des Permissions et Droits d'Accès (CareerTrack)

> **Document de référence pour le contrôle d'accès basé sur les rôles (RBAC)**  
> Dernière mise à jour : 18 octobre 2026  
> Branche : `feature/CT-01-droits-securite`

---

## 1. Principes Directeurs d'Architecture de Sécurité

1. **Refus par défaut (Deny by Default) :**  
   Tout endpoint REST non explicitement exempté requiert une authentification valide (`@PreAuthorize("isAuthenticated()")` ou rôle spécifique). Les filtres de Spring Security bloquent tout accès anonyme hors de la liste blanche publique stricte (`/api/v1/auth/login`, `/api/v1/auth/refresh`).
2. **Rôle de la DIRECTION (Lecture Seule & Agrégats) :**  
   Conformément au cahier des charges, la **DIRECTION** pilote l'entreprise via des indicateurs macro-économiques (masse salariale globale, tableaux de bord, organigramme, paramètres). Elle **ne possède aucun droit de modification (PATCH/PUT/POST)** sur les données RH opérationnelles et **ne peut consulter aucune donnée nominative individuelle** (salaires individuels, bulletins de paie nominatifs, fiches d'évaluation de la performance, contrats ou alertes personnelles).
3. **Isolation et Propriété des Données Collaborateur :**  
   Un salarié avec le rôle `EMPLOYE` n'a accès qu'à son propre dossier collaborateur, ses propres bulletins de paie, son propre contrat, ses propres demandes de congés et ses propres notifications.
4. **Périmètre Hiérarchique du MANAGER :**  
   Un utilisateur avec le rôle `MANAGER` n'a de visibilité que sur les collaborateurs qui lui sont directement rattachés (`manager_id = manager.employee_id`). Il peut consulter l'attestation de travail de ses subordonnés directs pour des besoins opérationnels, mais **ne peut en aucun cas consulter leurs bulletins de paie ni leurs contrats de travail** (réservés à la DRH et à l'employé).

---

## 2. Matrice Exhaustive des Endpoints par Module

### Légende :
* **✅ AUTORISÉ :** Accès accordé sans restriction sur le module.
* **⚠️ RESTREINT (Règle de propriété) :** Accès conditionné à la vérification que l'utilisateur consulte ses propres données ou celles de son équipe directe.
* **❌ REFUSÉ (403 Forbidden) :** Accès interdit par `@PreAuthorize`.
* **🌐 PUBLIC (permitAll) :** Accessible sans jeton d'authentification.

| Module | Méthode & Route | DRH | MANAGER | EMPLOYE | DIRECTION | Règle de Sécurité & Expression `@PreAuthorize` |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **Authentification** | `POST /api/v1/auth/login` | 🌐 | 🌐 | 🌐 | 🌐 | `permitAll()` — Authentification par identifiant / mot de passe |
| **Authentification** | `POST /api/v1/auth/refresh` | 🌐 | 🌐 | 🌐 | 🌐 | `permitAll()` — Rotation du token d'accès avec refresh token valide |
| **Authentification** | `POST /api/v1/auth/logout` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` — Révocation du refresh token en base |
| **Authentification** | `GET /api/v1/auth/me` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` — Profil personnel de l'utilisateur connecté |
| **Documents RH** | `GET /api/v1/documents/attestation/{id}` | ✅ | ⚠️ Subordonné | ⚠️ Soi-même | ❌ | `@documentSecurity.canAccessWorkCertificate(#id)` (Direction exclue de tout document nominatif) |
| **Documents RH** | `GET /api/v1/documents/payslip/{id}` | ✅ | ⚠️ Soi-même | ⚠️ Soi-même | ❌ | `@documentSecurity.canAccessPayslip(#id)` (Direction exclue) |
| **Documents RH** | `GET /api/v1/documents/contract/{id}` | ✅ | ⚠️ Soi-même | ⚠️ Soi-même | ❌ | `@documentSecurity.canAccessContract(#id)` (Direction exclue) |
| **Documents Collaborateur** | `POST /api/v1/documents` | ✅ | ❌ | ⚠️ Soi-même (hors CONTRAT) | ❌ | `@documentSecurity.canUploadDocument(#employeeId, #type)` (DRH tous types ; EMPLOYE CNI, DIPLOME, AUTRE pour lui-même uniquement ; CONTRAT réservé DRH) |
| **Documents Collaborateur** | `GET /api/v1/documents/employee/{id}` | ✅ | ❌ | ⚠️ Soi-même | ❌ | `@documentSecurity.canListEmployeeDocuments(#id)` (DRH tous ; EMPLOYE ses documents ; Manager et Direction exclus) |
| **Documents Collaborateur** | `GET /api/v1/documents/{id}/download` | ✅ | ❌ | ⚠️ Soi-même | ❌ | `@documentSecurity.canDownloadDocument(#id)` (DRH tous ; EMPLOYE ses documents ; Manager et Direction exclus) |
| **Documents Collaborateur** | `DELETE /api/v1/documents/{id}` | ✅ | ❌ | ⚠️ Soi-même (déposé par lui) | ❌ | `@documentSecurity.canDeleteDocument(#id)` (DRH tous ; EMPLOYE uniquement les documents qu'il a lui-même déposés) |
| **Dossiers Employés** | `GET /api/v1/employees` | ✅ | ✅ | ❌ | ❌ | `hasAnyRole('DRH', 'MANAGER')` (Direction exclue des listes nominatives) |
| **Dossiers Employés** | `GET /api/v1/employees/{id}` | ✅ | ⚠️ Subordonné/Soi | ⚠️ Soi-même | ❌ | `@employeeSecurity.canAccessEmployee(#id)` (Dossier nominatif privé) |
| **Dossiers Employés** | `POST /api/v1/employees` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Dossiers Employés** | `PUT /api/v1/employees/{id}` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Dossiers Employés** | `DELETE /api/v1/employees/{id}` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Dossiers Employés** | `GET /api/v1/employees/department/{id}`| ✅ | ✅ | ❌ | ❌ | `hasAnyRole('DRH', 'MANAGER')` (Direction exclue) |
| **Paie & Salaires** | `GET /api/v1/payroll/payslips?period=` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` (Liste nominative des salaires mensuels) |
| **Paie & Salaires** | `GET /api/v1/payroll/payslips/employee/{id}`| ✅ | ❌ | ⚠️ Soi-même | ❌ | `@payrollSecurity.canAccessEmployeePayslips(#id)` |
| **Paie & Salaires** | `GET /api/v1/payroll/payslips/{id}` | ✅ | ❌ | ⚠️ Soi-même | ❌ | `@payrollSecurity.canAccessPayslip(#id)` |
| **Paie & Salaires** | `POST /api/v1/payroll/generate?period=` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Paie & Salaires** | `PATCH /api/v1/payroll/payslips/{id}/status`| ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Congés & Absences**| `GET /api/v1/leaves` | ✅ | ⚠️ Équipe | ⚠️ Soi-même | ✅ | `hasAnyRole('EMPLOYE', 'MANAGER', 'DRH', 'DIRECTION')` |
| **Congés & Absences**| `POST /api/v1/leaves` | ✅ | ✅ | ✅ | ❌ | `hasAnyRole('EMPLOYE', 'MANAGER', 'DRH')` |
| **Congés & Absences**| `PUT /api/v1/leaves/{id}/validate` | ✅ | ⚠️ Subordonné | ❌ | ❌ | `hasAnyRole('MANAGER', 'DRH')` |
| **Congés & Absences**| `GET /api/v1/leaves/employee/{id}` | ✅ | ⚠️ Subordonné | ⚠️ Soi-même | ❌ | `@leaveSecurity.canAccessEmployeeLeaves(#id)` |
| **Congés & Absences**| `GET /api/v1/leaves/manager/{id}/pending` | ✅ | ⚠️ Soi-même | ❌ | ❌ | `hasAnyRole('MANAGER', 'DRH')` |
| **Congés & Absences**| `GET /api/v1/leaves/balances/{id}` | ✅ | ⚠️ Subordonné | ⚠️ Soi-même | ❌ | `@leaveSecurity.canAccessEmployeeBalances(#id)` |
| **Congés & Absences**| `GET /api/v1/leaves/conflicts` | ✅ | ✅ | ❌ | ❌ | `hasAnyRole('MANAGER', 'DRH')` |
| **Évaluations** | `GET /api/v1/evaluations/campaigns` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` |
| **Évaluations** | `POST /api/v1/evaluations/campaigns` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Évaluations** | `GET /api/v1/evaluations/campaigns/{id}/evaluations` | ✅ | ⚠️ Équipe | ❌ | ❌ | `hasAnyRole('DRH', 'MANAGER')` (Notations individuelles exclues pour Direction) |
| **Évaluations** | `GET /api/v1/evaluations/{id}` | ✅ | ⚠️ Évaluateur | ⚠️ Évalué | ❌ | `@evaluationSecurity.canAccessEvaluation(#id)` |
| **Promotions** | `GET /api/v1/promotions` | ✅ | ⚠️ Équipe | ❌ | ❌ | `hasAnyRole('DRH', 'MANAGER')` (Détail salarial nominatif exclu pour Direction) |
| **Promotions** | `POST /api/v1/promotions` | ✅ | ✅ | ❌ | ❌ | `hasAnyRole('DRH', 'MANAGER')` |
| **Promotions** | `PATCH /api/v1/promotions/{id}/status` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` (Validation réservée à la DRH) |
| **Paramètres Légaux**| `GET /api/v1/settings` | ✅ | ❌ | ❌ | ✅ | `hasAnyRole('DRH', 'DIRECTION')` (Taux IPRES/CSS et plafonds) |
| **Paramètres Légaux**| `PUT /api/v1/settings/{key}` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Recrutement** | `GET /api/v1/recruitment/applications` | ✅ | ✅ | ❌ | ❌ | `hasAnyRole('DRH', 'MANAGER')` |
| **Recrutement** | `PATCH /api/v1/recruitment/applications/{id}/status`| ✅ | ✅ | ❌ | ❌ | `hasAnyRole('DRH', 'MANAGER')` |
| **Recrutement** | `GET /api/v1/recruitment/offers` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` |
| **Plans Formation** | `GET /api/v1/trainings` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` |
| **Plans Formation** | `GET /api/v1/trainings/{id}` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` |
| **Plans Formation** | `POST /api/v1/trainings` | ✅ | ❌ | ❌ | ❌ | `hasRole('DRH')` |
| **Organigramme** | `GET /api/v1/orgchart/tree` | ✅ | ✅ | ✅ | ✅ | `hasAnyRole('DRH', 'MANAGER', 'EMPLOYE', 'DIRECTION')` |
| **Organigramme** | `GET /api/v1/orgchart/subordinates/{id}`| ✅ | ✅ | ❌ | ✅ | `hasAnyRole('DRH', 'MANAGER', 'DIRECTION')` |
| **Notifications** | `GET /api/v1/notifications/me` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` (In-App notifications de l'utilisateur connecté) |
| **Notifications** | `PATCH /api/v1/notifications/{id}/read` | ✅ | ✅ | ✅ | ✅ | `isAuthenticated()` (Marquer sa notification comme lue) |

---

## 3. Dispositif de Sécurisation par Réflexion (Architecture CI)

Un test unitaire d'architecture (`ArchitectureSecurityTest`) est exécuté à chaque build `mvn clean verify` :
* Il inspecte l'intégralité des classes annotées `@RestController` du backend.
* Il vérifie que chaque méthode associée à un mapping HTTP dispose explicitement d'une annotation `@PreAuthorize`.
* Il valide qu'aucune méthode n'utilise `@PreAuthorize("permitAll()")` en dehors des routes publiques déclarées (`/login`, `/refresh`).
