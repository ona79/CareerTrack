# Contrat d'API : Module Documents Collaborateur (Gestion documentaire & Dossier RH)

> **Référence immuable pour le Lot 2 (Dossier Employé)**  
> **Préfixe :** `/api/v1/documents`  
> **Statut :** Figé pour intégration dans le dossier employé (Lot 2). Aucun endpoint parallèle ne doit être créé.

---

## 1. Description & Règles d'accès (RBAC)

Ce module assure la gestion complète des pièces justificatives administratives (CNI, diplômes, contrats, autres).  
Pour des raisons strictes de performance, d'empreinte mémoire et de sécurité, **aucun contenu binaire (`bytea` / octets)** n'est renvoyé lors de la consultation des listes ou métadonnées. Le téléchargement effectif du fichier s'effectue exclusivement via l'endpoint dédié `GET /api/v1/documents/{id}/download`.

### Matrice des rôles pour la gestion documentaire :
- **`ROLE_DRH`** : Accès complet (consultation tous employés, téléversement de tout type y compris `CONTRAT`, téléchargement, suppression de tout document).
- **`ROLE_EMPLOYE`** : Accès restreint exclusivement à son propre dossier (`employeeId == currentEmployee.id`) :
  - Peut téléverser `CNI`, `DIPLOME`, `AUTRE`. Type `CONTRAT` strictement **interdit** (réservé DRH).
  - Peut télécharger ses propres documents.
  - Peut supprimer **uniquement** les documents qu'il a lui-même déposés (`uploadedBy == currentUser.id`).
- **`ROLE_MANAGER`** : Règle identique à `ROLE_EMPLOYE` pour ses **propres** documents uniquement :
  - Gère son propre dossier personnel (consultation, upload `CNI`, `DIPLOME`, `AUTRE`, suppression de ses dépôts).
  - **403 Forbidden** strict sur les documents de ses subordonnés et de tout autre tiers.
- **`ROLE_DIRECTION`** : **403 Forbidden** (aucun accès aux pièces justificatives individuelles).

---

## 2. Endpoints du Service Documentaire

### A. Téléverser un document
- **Route :** `POST /api/v1/documents`
- **Content-Type :** `multipart/form-data`
- **Paramètres de formulaire :**
  - `file` : `MultipartFile` (requis, max 5 Mo, types autorisés : PDF `%PDF-`, PNG `89 50 4E 47`, JPEG `FF D8 FF`).
  - `employeeId` : `UUID` (requis).
  - `type` : `DocumentType` enum (`CNI`, `DIPLOME`, `CONTRAT`, `AUTRE`).
  - `title` : `String` (requis, libellé explicatif).
- **Statut HTTP de succès :** `201 Created`
- **Corps de réponse :**
```json
{
  "success": true,
  "message": "Document téléversé avec succès",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "employeeId": "55555555-5555-5555-5555-555555555504",
    "documentType": "CNI",
    "title": "Carte Nationale d'Identité CEDEAO",
    "fileName": "CNI_Diallo_Ousmane.pdf",
    "fileSize": 245890,
    "mimeType": "application/pdf",
    "uploadedAt": "2026-10-18T18:45:00.000Z",
    "uploadedBy": "22222222-2222-2222-2222-222222222203",
    "downloadUrl": "/api/v1/documents/3fa85f64-5717-4562-b3fc-2c963f66afa6/download"
  },
  "timestamp": "2026-10-18T18:45:00.123Z"
}
```

---

### B. Consulter les documents d'un collaborateur (Métadonnées)
- **Route :** `GET /api/v1/documents/employee/{employeeId}`
- **Statut HTTP de succès :** `200 OK`
- **Content-Type :** `application/json;charset=UTF-8`
- **Garantie d'absence de binaire :** Le DTO `DocumentResponseDto` ne contient AUCUN champ binaire (`data`, `bytes`, `content`).
- **Corps de réponse :**
```json
{
  "success": true,
  "data": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "employeeId": "55555555-5555-5555-5555-555555555504",
      "documentType": "CNI",
      "title": "Carte Nationale d'Identité CEDEAO",
      "fileName": "CNI_Diallo_Ousmane.pdf",
      "fileSize": 245890,
      "mimeType": "application/pdf",
      "uploadedAt": "2026-10-18T18:45:00.000Z",
      "uploadedBy": "22222222-2222-2222-2222-222222222203",
      "downloadUrl": "/api/v1/documents/3fa85f64-5717-4562-b3fc-2c963f66afa6/download"
    }
  ],
  "timestamp": "2026-10-18T18:45:00.456Z"
}
```

---

### C. Télécharger un document téléversé
- **Route :** `GET /api/v1/documents/{id}/download`
- **Statut HTTP de succès :** `200 OK`
- **En-têtes de sécurité obligatoires :**
  - `Content-Type`: MIME réel validé (ex: `application/pdf`, `image/png`, `image/jpeg`).
  - `Content-Disposition`: `attachment; filename="..."; filename*=UTF-8''...` (encodage RFC 5987 pour les accents).
  - `X-Content-Type-Options: nosniff` (protection contre le MIME sniffing).
- **Corps de réponse :** Flux binaire d'octets (`byte[]`).

---

### D. Supprimer un document
- **Route :** `DELETE /api/v1/documents/{id}`
- **Règles :** Accessible à la DRH (tout document) ou au collaborateur / manager pour les documents qu'il a lui-même téléversés (`uploadedBy == currentUser.id`).
- **Statut HTTP de succès :** `204 No Content`
- **Corps de réponse :** Aucun (corps vide).

---

## 3. Schéma DTO : `DocumentResponseDto`

| Champ | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` (String) | Non | Identifiant unique du document en base. |
| `employeeId` | `UUID` (String) | Non | Identifiant de l'employé associé. |
| `documentType` | `String` (Enum) | Non | `CNI`, `DIPLOME`, `CONTRAT`, `AUTRE`, etc. |
| `title` | `String` | Non | Libellé / titre explicatif renseigné lors de l'envoi. |
| `fileName` | `String` | Non | Nom nettoyé et assaini du fichier d'origine. |
| `fileSize` | `Long` | Non | Taille physique du fichier en octets. |
| `mimeType` | `String` | Non | Type MIME réel certifié par signature binaire. |
| `uploadedAt` | `Instant` (ISO-8601) | Non | Horodatage du téléversement. |
| `uploadedBy` | `UUID` (String) | Oui | Identifiant utilisateur de l'auteur du dépôt. |
| `downloadUrl` | `String` | Non | URL relative de téléchargement direct. |
