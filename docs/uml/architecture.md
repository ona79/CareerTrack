# Diagrammes UML & Spécifications d'Architecture – CareerTrack

Ce document contient les diagrammes d'architecture globale du système **CareerTrack**.
Les diagrammes de modélisation détaillée sont également consultables dans les documents dédiés :
- 📋 [Diagramme de Cas d'Utilisation](file:///home/diallo/for_me/Semestre_6/project/CareerTrack/docs/uml/use_cases.md)
- 🏗️ [Diagramme de Classes Métier](file:///home/diallo/for_me/Semestre_6/project/CareerTrack/docs/uml/class_diagram.md)
- 🔄 [Diagramme de Séquence du Processus d'Évaluation](file:///home/diallo/for_me/Semestre_6/project/CareerTrack/docs/uml/evaluation_sequence.md)

---


## 1. Diagramme de Composants & Architecture Globale

```mermaid
graph TD
    subgraph Client["Frontend Client (React 19 + Vite)"]
        UI[Design System Senegal UI]
        Router[React Router v7]
        TanStack[TanStack Query Cache]
        Axios[Axios Interceptors JWT]
        UI --> Router
        Router --> TanStack
        TanStack --> Axios
    end

    subgraph API["Backend API (Spring Boot 3 - Java 17)"]
        Security[Spring Security & Filtre JWT]
        Controller[REST Controllers /api/v1/*]
        Services[Couche Métier / Services]
        PdfGen[Générateur PDF iText]
        MailNotif[JavaMailSender SMTP]
        JPA[Spring Data JPA Repositories]

        Axios -->|HTTPS / REST| Security
        Security --> Controller
        Controller --> Services
        Services --> PdfGen
        Services --> MailNotif
        Services --> JPA
    end

    subgraph Persistence["Couche Données"]
        Flyway[Flyway Migrations V1, V2, V3]
        Postgres[(PostgreSQL 16)]
        Flyway --> Postgres
        JPA --> Postgres
    end
```

---

## 2. Schéma Entité-Association (ERD Simplifié)

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : has
    USERS }o--o{ ROLES : belongs_to
    EMPLOYEES ||--o| USERS : linked_to
    DEPARTMENTS ||--o{ EMPLOYEES : employs
    POSITIONS ||--o{ EMPLOYEES : assigns
    EMPLOYEES ||--o{ CONTRACTS : signs
    EMPLOYEES ||--o{ PAYSLIPS : receives
    PAYSLIPS ||--o{ PAYSLIP_LINES : details
    EMPLOYEES ||--o{ LEAVE_REQUESTS : requests
    LEAVE_TYPES ||--o{ LEAVE_REQUESTS : classifies
    LEAVE_TYPES ||--o{ LEAVE_BALANCES : tracks
    EMPLOYEES ||--o{ EVALUATIONS : evaluated_in
    EVALUATION_CAMPAIGNS ||--o{ EVALUATIONS : conducts
    JOB_OFFERS ||--o{ JOB_APPLICATIONS : contains
    CANDIDATES ||--o{ JOB_APPLICATIONS : submits
    TRAININGS ||--o{ TRAINING_SESSIONS : schedules
    TRAINING_SESSIONS ||--o{ TRAINING_ENROLLMENTS : enrolls
```

---

## 3. Diagramme de Séquence : Authentification JWT & Refresh

```mermaid
sequenceDiagram
    autonumber
    actor User as Collaborateur
    participant Front as Frontend (React)
    participant Auth as AuthController
    participant Sec as Spring Security
    participant DB as Base PostgreSQL

    User->>Front: Saisit username + password
    Front->>Auth: POST /api/auth/login { username, password }
    Auth->>Sec: authenticate(UsernamePasswordAuthenticationToken)
    Sec->>DB: Recherche utilisateur + hachage BCrypt
    DB-->>Sec: Utilisateur valide + rôles
    Sec-->>Auth: Authentication réussie
    Auth->>DB: Enregistre nouveau refresh_token
    Auth-->>Front: 200 OK { accessToken, refreshToken, roles }
    Front->>Front: Stocke accessToken en mémoire & refreshToken en localStorage
    Front-->>User: Redirection vers /dashboard
```

---

## 4. Diagramme d'États : Cycle de Vie d'une Demande de Congé

```mermaid
stateDiagram-v2
    [*] --> PENDING : Soumission par le collaborateur (Déduction prévisionnelle)
    PENDING --> APPROVED : Validation par le Manager N+1 / DRH
    PENDING --> REJECTED : Refus motivé par le Manager N+1
    PENDING --> CANCELLED : Annulation par le collaborateur avant validation
    APPROVED --> CANCELLED : Annulation exceptionnelle validée par DRH (Solde rétabli)
    APPROVED --> [*] : Congé consommé
    REJECTED --> [*]
    CANCELLED --> [*]
```
