# Diagramme de Séquence – Processus d'Évaluation Annuelle

Ce document modélise le cycle complet du processus d'évaluation annuelle et de définition des objectifs SMART dans **CareerTrack**.

---

## 1. Description du Processus Métier

1. **Lancement de la campagne (DRH)** : La DRH crée la campagne annuelle (ex: `Campagne Annuelle 2026`) avec dates limites et critères. Les fiches d'évaluation individuelles sont automatiquement instanciées.
2. **Auto-évaluation (Collaborateur)** : Le collaborateur évalue l'atteinte de ses objectifs SMART de l'année N-1 (taux de complétion, auto-notation sur 5, commentaires) et soumet sa fiche.
3. **Entretien & Évaluation (Manager N+1)** : Le manager N+1 planifie l'entretien physique/virtuel, saisit ses notes, ajuste les scores, rédige sa synthèse et fixe les nouveaux objectifs SMART de l'année N+1 avec leurs pondérations.
4. **Validation conjointe & Clôture (DRH)** : Le collaborateur et le manager signent numériquement/valident la synthèse. La DRH valide définitivement l'évaluation, ce qui déclenche la génération automatique du rapport PDF officiel iText et l'archivage dans le dossier de l'employé.
5. **Impact RH** : Si les résultats dépassent les attentes, le manager peut déclencher une proposition de promotion ou d'augmentation salariale via le module Promotions.

---

## 2. Diagramme de Séquence (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor DRH as DRH
    actor Collab as Collaborateur
    actor Mgr as Manager N+1
    participant UI as Frontend React
    participant EvalCtrl as EvaluationController
    participant EvalSvc as EvaluationService
    participant NotifSvc as NotificationService
    participant PdfGen as PdfGeneratorService
    participant DB as PostgreSQL

    %% Phase 1 : Lancement
    rect rgb(232, 245, 238)
    note right of DRH: Phase 1 : Lancement de la Campagne
    DRH->>UI: Créer campagne "Évaluation 2026"
    UI->>EvalCtrl: POST /api/evaluations/campaigns { title, year, startDate, endDate }
    EvalCtrl->>EvalSvc: createCampaign(dto)
    EvalSvc->>DB: INSERT into evaluation_campaigns
    EvalSvc->>DB: INSERT into evaluations (pour chaque employé actif)
    EvalSvc->>NotifSvc: notifyAllEmployees(campaignId)
    NotifSvc-->>Collab: Email & In-App "Campagne 2026 ouverte"
    NotifSvc-->>Mgr: Email & In-App "Campagne 2026 ouverte"
    EvalCtrl-->>UI: 201 Created (CampaignDto)
    end

    %% Phase 2 : Auto-évaluation
    rect rgb(255, 248, 235)
    note right of Collab: Phase 2 : Auto-évaluation SMART
    Collab->>UI: Accède à son évaluation annuelle
    UI->>EvalCtrl: GET /api/evaluations/my-current
    EvalCtrl->>EvalSvc: getCurrentUserEvaluation()
    EvalSvc->>DB: SELECT evaluation + objectives
    EvalCtrl-->>UI: 200 OK (EvaluationDto)
    Collab->>UI: Saisit auto-scores et commentaires sur objectifs SMART
    UI->>EvalCtrl: PUT /api/evaluations/{id}/self-evaluation
    EvalCtrl->>EvalSvc: submitSelfEvaluation(id, selfScoresDto)
    EvalSvc->>DB: UPDATE evaluations SET self_score = ..., status = 'SELF_EVALUATED'
    EvalSvc->>NotifSvc: notifyManager(evaluationId)
    NotifSvc-->>Mgr: Email: "Auto-évaluation soumise par collaborateur"
    EvalCtrl-->>UI: 200 OK
    end

    %% Phase 3 : Entretien Managérial & Objectifs N+1
    rect rgb(240, 244, 248)
    note right of Mgr: Phase 3 : Entretien & Notation Managériale
    Mgr->>UI: Ouvre la fiche d'évaluation du collaborateur
    Mgr->>UI: Saisit managerScore, feedback et fixe objectifs SMART N+1 (titre, poids, cible)
    UI->>EvalCtrl: PUT /api/evaluations/{id}/manager-evaluation
    EvalCtrl->>EvalSvc: submitManagerEvaluation(id, managerScoresDto)
    EvalSvc->>DB: UPDATE evaluations SET manager_score = ..., final_score = ..., status = 'MANAGER_EVALUATED'
    EvalSvc->>DB: INSERT / UPDATE objectives (objectifs SMART N+1)
    EvalCtrl-->>UI: 200 OK
    end

    %% Phase 4 : Clôture & Génération PDF
    rect rgb(232, 245, 238)
    note right of DRH: Phase 4 : Validation DRH & Génération PDF
    DRH->>UI: Validation finale et clôture
    UI->>EvalCtrl: POST /api/evaluations/{id}/finalize
    EvalCtrl->>EvalSvc: finalizeEvaluation(id)
    EvalSvc->>DB: UPDATE evaluations SET status = 'COMPLETED'
    EvalSvc->>PdfGen: generateEvaluationSummaryPdf(evaluationId)
    PdfGen->>PdfGen: Compilation iText (Scores, KPIs SMART, Synthèse, Signatures)
    PdfGen-->>EvalSvc: byte[] pdfContent (evaluation-2026.pdf)
    EvalSvc->>DB: UPDATE evaluations SET summary_pdf_url = ...
    EvalSvc->>NotifSvc: sendEvaluationReport(Collab, Mgr, pdfContent)
    NotifSvc-->>Collab: Email avec Fiche PDF en pièce jointe
    NotifSvc-->>Mgr: Notification In-App & Email
    EvalCtrl-->>UI: 200 OK { summaryPdfUrl }
    end
```
