# Guide d'Utilisation du Socle PDF CareerTrack (`common/pdf`)

Ce document décrit le socle de génération de documents PDF mis en place pour CareerTrack (module transverse `common.pdf`), son architecture, ses règles de typographie sénégalaise, sa palette graphique, ainsi que les gabarits prêts à l'emploi pour les autres modules (Lot 2 - Collaborateurs, Lot 4 - Performance & Formation).

---

## 1. Principes d'Architecture & Charte Graphique

Le module `common.pdf` est conçu pour être réutilisable, uniforme et robuste aux champs nuls.

### 1.1 Palette de Couleurs CareerTrack
Les couleurs officielles définies dans `PdfTheme` sont :
- **Vert CareerTrack (Principal)** : `#1A6B3C` (En-têtes de tableaux, bannières de total net, bandeaux).
- **Or / Ambre (Accent & Alertes)** : `#F5A623` (Bordures des encarts d'avertissement et mentions légales).
- **Gris Foncé (Texte principal)** : `#0F172A` (Titres, corps de texte principal).
- **Gris Ardoise (Secondaire)** : `#64748B` (Sous-titres, métadonnées, dates, libellés secondaires).
- **Gris Clair (Fond de table)** : `#F8FAFC` (Lignes paires des tableaux zebra-striped).
- **Gris Bordure** : `#E2E8F0` (Lignes de séparation fines et contours discrets).

### 1.2 Typographie et Formatage Sénégalais
- **Gestion des caractères spéciaux et accents français** : utilisation de la police standard Helvetica sous encodage `PdfEncodings.CP1252` (ou polices TrueType système compatibles) supportant l'intégralité des diacritiques français (`é`, `è`, `ê`, `à`, `ç`, `ô`, `î`, `ù`, etc.).
- **Séparateur de milliers (Montants FCFA)** : `PdfFormatUtils.formatCurrency(BigDecimal)` utilise explicitement l'espace insécable standard (`\u00A0`), évitant l'espace fine (`\u202F`) non supportée par Helvetica. Exemple : `450 000 FCFA`, `950 000 FCFA`.
- **Dates** : format français normalisé `dd/MM/yyyy` via `PdfFormatUtils.formatDate(LocalDate)`.

### 1.3 En-tête et Pied de Page Dynamiques
- **En-tête entreprise** : lit dynamiquement les paramètres `COMPANY_NAME`, `COMPANY_CITY`, `COMPANY_NINEA`, `COMPANY_RCCM` depuis la table `company_settings`. Si une clé est absente en base, sa ligne disparaît sans inventer de valeur. En mode développement ou démonstration sans configuration, un fallback explicite `Teranga Tech PME SARL` est utilisé.
- **Pied de page paginé** : géré automatiquement par `PdfHeaderFooterEventHandler` sous la forme :
  `[Mention légale éventuelle] — Document généré le dd/MM/yyyy   Page X`
- **Zones de signature** : génère automatiquement les blocs d'émargement "Fait à [Ville], le [Date]" avec zone réservée aux signatures et cachets.

---

## 2. Exemple Complet en 10 Lignes (Pour les Développeurs des Lots 2 et 4)

Voici comment générer un document PDF standardisé en 10 lignes avec l'API fluide `PdfDocumentBuilder` :

```java
ByteArrayOutputStream out = new ByteArrayOutputStream();
PdfDocumentBuilder builder = new PdfDocumentBuilder(out, "Attestation de stage", "RH", "Attestation");
builder.addCompanyHeader("Teranga Tech PME SARL", "Dakar", "001234567 2G2", "SN.DKR.2020.B.999");
builder.addDocumentTitle("ATTESTATION DE STAGE");
builder.addParagraph("Nous soussignés attestons que le stagiaire a accompli son stage avec succès.", 10.5f, false, TextAlignment.LEFT, 15);
builder.addSignatureBlock("Dakar", LocalDate.now(), "Le Stagiaire", "Lu et approuvé", "La Direction", "Directeur Général");
return builder.build();
```

---

## 3. Gabarits Disponibles pour les Autres Lots

### 3.1 Lot 2 (Collaborateurs & Registre du Personnel)
- **Méthode** : `pdfGeneratorService.generateTabularReportPdf(title, subtitle, headers, rows)`
- **Spécification** : Produit un tableau paginé multi-pages, alternance de fond gris clair, gestion des retours à la ligne automatique.
- **Règle de confidentialité stricte** : Par défaut, aucun export tabulaire ne doit contenir de données sensibles (CNI, salaires nominatifs, date de naissance). Seules les données d'identité, de matricule, de poste et de département doivent figurer dans le registre standard.

### 3.2 Lot 4 (Performance & Campagnes d'Évaluation)
- **Record de données** : `AnnualEvaluationPdfData`
  ```java
  public record AnnualEvaluationPdfData(
      String campaignTitle,
      String employeeName,
      String employeeNumber,
      String positionTitle,
      String departmentName,
      String managerName,
      LocalDate evaluationDate,
      BigDecimal globalScore,
      String generalComment,
      List<ObjectiveItem> objectives
  )
  ```
- **Méthode** : `pdfGeneratorService.generateAnnualEvaluationPdf(AnnualEvaluationPdfData data)`
- **Rendu** : Fiche d'évaluation structurée avec note globale sur 5.0, tableau des objectifs SMART (poids, taux d'atteinte %, commentaires) et double émargement (collaborateur et manager).

### 3.3 Lot 4 (Formations & Certifications)
- **Record de données** : `TrainingCertificatePdfData`
  ```java
  public record TrainingCertificatePdfData(
      String certificateNumber,
      String participantName,
      String employeeNumber,
      String positionTitle,
      String sessionTitle,
      String trainingProvider,
      LocalDate startDate,
      LocalDate endDate,
      int totalHours,
      String skillsAcquired
  )
  ```
- **Méthode** : `pdfGeneratorService.generateTrainingCertificatePdf(TrainingCertificatePdfData data)`
- **Rendu** : Attestation de formation officielle avec dates de session, volume horaire, compétences acquises et cachet du centre/direction.

---

## 4. Robustesse et Gestion des Valeurs Nulles

Tous les services et constructeurs de documents PDF vérifient systématiquement la présence des objets et attributs :
- Si un employé n'a pas de département ou de poste assigné, le document affiche `N/A` ou `Non spécifié` sans générer de `NullPointerException`.
- Si un contrat n'a pas de date de fin (cas usuel d'un CDI) ou pas de salaire renseigné, la clause s'adapte sans crash.
- Les métadonnées iText (`Author`, `Title`, `Subject`) sont systématiquement initialisées.
