# Contrat d'API : Consultation des Documents Collaborateur (Module Documents & Dossier RH)

> **Référence immuable pour le Lot 2 (Dossier Employé)**  
> **Route :** `GET /api/v1/documents/employee/{employeeId}`  
> **Statut :** Figé pour intégration dans le dossier employé (Lot 2). Aucun endpoint parallèle ne doit être créé.

---

## 1. Description & Règles d'accès

Cet endpoint renvoie la liste complète des documents administratifs téléversés d'un employé (**métadonnées uniquement**).  
Pour des raisons strictes de performance, d'empreinte mémoire et de sécurité, **aucun contenu binaire (`bytea` / octets)** n'est renvoyé par cette route. Le téléchargement effectif du fichier s'effectue via l'endpoint dédié `GET /api/v1/documents/{id}/download`.

### Contrôle d'accès (RBAC) :
- **`ROLE_DRH`** : Accès complet à la liste des documents de n'importe quel employé.
- **`ROLE_EMPLOYE`** : Accès restreint exclusivement à ses propres documents (`employeeId == currentEmployee.id`).
- **`ROLE_MANAGER`** : **403 Forbidden** (aucun accès aux pièces d'identité, diplômes ou contrats de ses subordonnés).
- **`ROLE_DIRECTION`** : **403 Forbidden** (aucun accès aux documents individuels nominatifs).

---

## 2. Format de la Réponse HTTP

- **Statut HTTP :** `200 OK`
- **Content-Type :** `application/json;charset=UTF-8`

### Schéma JSON (Tableau enveloppé dans `ApiResponse`) :

```json
{
  "success": true,
  "message": "Opération réussie",
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
      "uploadedBy": "11111111-1111-1111-1111-111111111101",
      "downloadUrl": "/api/v1/documents/3fa85f64-5717-4562-b3fc-2c963f66afa6/download"
    }
  ]
}
```

### Description détaillée des champs du DTO (`DocumentResponseDto`) :

| Champ | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` (String) | Identifiant unique du document en base. |
| `employeeId` | `UUID` (String) | Identifiant de l'employé auquel est rattaché le document. |
| `documentType` | `String` (Enum) | Type officiel : `CNI`, `DIPLOME`, `CONTRAT`, `AUTRE`, `PASSEPORT`, `CV`, `ATTESTATION`, `FICHE_PAIE`, `CERTIFICAT_MEDICAL`. |
| `title` | `String` | Libellé / titre explicatif renseigné lors de l'envoi. |
| `fileName` | `String` | Nom nettoyé et assaini du fichier d'origine (sans chemin, sans caractères de contrôle). |
| `fileSize` | `Long` | Taille physique du fichier en octets. |
| `mimeType` | `String` | Type MIME déterminé par la signature réelle binaire (ex: `application/pdf`, `image/png`, `image/jpeg`). |
| `uploadedAt` | `Instant` (ISO-8601) | Horodatage précis du téléversement. |
| `uploadedBy` | `UUID` (String, nullable) | Identifiant de l'utilisateur (`User.id`) ayant effectué le téléversement (permet au collaborateur de savoir s'il peut supprimer le document). |
| `downloadUrl` | `String` | URL relative officielle pour télécharger la pièce jointe avec en-têtes sécurisés. |
