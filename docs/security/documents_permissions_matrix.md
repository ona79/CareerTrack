# Matrice des Droits d'Accès aux Documents RH (Lot 1)

Cette matrice définit strictement les règles de contrôle d'accès basé sur les rôles (RBAC) pour tous les documents individuels (Attestations de travail, Bulletins de paie, Contrats de travail, CNI) au sein de la plateforme CareerTrack.

---

## 📋 Matrice des Habilitations par Rôle

| Type de Document | EMPLOYE (Collaborateur) | MANAGER (N+1) | DIRECTION | DRH (Administration) |
| :--- | :---: | :---: | :---: | :---: |
| **Attestation de travail** | ✅ Ses propres attestations uniquement | ✅ Uniquement ses subordonnés directs | ❌ Aucun accès (Confidentialité) | ✅ Tous les collaborateurs |
| **Bulletin de paie (PDF)** | ✅ Ses propres bulletins uniquement | ❌ Refusé (Confidentialité salariale) | ❌ Refusé (Vue agrégée masse salariale uniquement) | ✅ Tous les collaborateurs |
| **Contrat de travail (PDF)** | ✅ Son propre contrat uniquement | ❌ Refusé (Donnée contractuelle privée) | ❌ Refusé | ✅ Tous les collaborateurs |
| **Pièce d'identité / CNI / Diplômes** | ✅ Ses propres pièces justificatives | ❌ Refusé | ❌ Refusé | ✅ Tous les collaborateurs |

---

## 🔒 Principes d'Implémentation & Sécurité

1. **Isolation Stricte du Salarié (`EMPLOYE`)** :
   - Un collaborateur ne peut en aucun cas interroger ou télécharger le document d'un pair (`403 Forbidden`).
   - L'identifiant utilisateur extrait du jeton JWT (`UserPrincipal`) est systématiquement mis en correspondance avec le profil collaborateur cible.

2. **Périmètre Délimité du Responsable Hiérarchique (`MANAGER`)** :
   - Le manager a accès aux attestations de travail de ses **subordonnés directs uniquement** (critère managérial validé par `manager_id`).
   - Le manager n'a **aucun droit d'accès** aux bulletins de paie ni aux contrats de ses collaborateurs afin de préserver la confidentialité des rémunérations et des clauses contractuelles individuelles.

3. **Protection contre la Divulgation envers la DIRECTION** :
   - Le rôle `DIRECTION` dispose d'un accès de synthèse macroéconomique (tableaux de bord, masse salariale globale, pyramide des âges).
   - Les documents nominatifs et fiches individuelles lui sont formellement inaccessibles (`403 Forbidden`).

4. **Gestion des Erreurs et Cas Limites** :
   - **Identifiant non conforme** (format non UUID) : `400 Bad Request`.
   - **Document ou collaborateur inexistant** : `404 Not Found`.
   - **Tentative d'accès non autorisée** : `403 Forbidden`.
   - Aucune exception non interceptée ne produit d'erreur `500 Internal Server Error`.
