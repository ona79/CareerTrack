# CareerTrack – Plateforme RH & Gestion des Carrières pour PME Sénégalaise

[![CI/CD](https://github.com/votre-org/careertrack/actions/workflows/ci.yml/badge.svg)](https://github.com/votre-org/careertrack/actions)
[![Java 17](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)](https://www.postgresql.org/)

**CareerTrack** est une solution complète de gestion des ressources humaines et des carrières conçue spécialement pour les PME sénégalaises (50 à 150 collaborateurs), intégrant nativement les spécificités réglementaires et sociales locales :
- **Acquisition légale des congés** : 2 jours ouvrables par mois effectif.
- **Encadrement strict des CDD** : surveillance automatique du plafond cumulé de 24 mois (Art. L.44 du Code du Travail).
- **Cotisations sociales sénégalaises** : calculs automatisés pour l'IPRES (retraite salariale 5,6% / patronale 8,4%) et la Caisse de Sécurité Sociale (CSS - 7%).
- **Devise locale** : Franc CFA (XOF / FCFA).
- **Documents officiels en PDF** : contrats de travail, attestations de travail et bulletins de paie édités via iTextPDF.

---

## 🏗️ Architecture Technique

```
CareerTrack/
├── backend/                       # Application Spring Boot 3 (Java 17+)
│   ├── src/main/java/com/careertrack/
│   │   ├── common/                # Sécurité JWT, Exceptions, PDF iText, Auditing
│   │   └── modules/               # Modules métier isolés par domaine
│   │       ├── auth/              # Authentification, RBAC & Tokens
│   │       ├── employee/          # Salariés, Contrats, Historique salaires
│   │       ├── leave/             # Soldes et demandes de congés
│   │       ├── payroll/           # Bulletins de paie et lignes de cotisations
│   │       ├── recruitment/       # Offres d'emploi et suivi des candidatures
│   │       ├── evaluation/        # Campagnes d'évaluation et objectifs
│   │       ├── training/          # Plans et catalogues de formation
│   │       ├── orgchart/          # Arborescence hiérarchique managériale
│   │       ├── document/          # Génération et téléchargement PDF
│   │       ├── notification/      # Envoi d'alertes in-app et e-mails
│   │       └── setting/           # Paramètres configurables de l'entreprise
│   └── src/main/resources/
│       ├── application.yml        # Configuration transverse & règles légales
│       ├── application-dev.yml    # Profil développement local
│       ├── application-prod.yml   # Profil production avec SSL
│       └── db/
│           ├── migration/         # V1 (28 tables) & V2 (données de référence)
│           └── dev-migration/     # V3 (seed de test dev uniquement)
│
├── frontend/                      # Application React 19 + TypeScript + Vite
│   ├── src/
│   │   ├── components/            # Layout (Sidebar, Navbar) & Composants UI
│   │   ├── context/               # Contexte d'authentification AuthContext
│   │   ├── lib/                   # Clients Axios & TanStack QueryClient
│   │   ├── pages/                 # Écrans des modules métier
│   │   ├── types/                 # Typage complet du domaine TypeScript
│   │   └── index.css              # Design System 100% clair & palette Sénégal
│
├── docs/                          # Spécifications, diagrammes UML & guides
│   ├── uml/architecture.md        # Diagrammes de séquence, ERD et composants
│   └── deployment/DEPLOYMENT.md   # Guide de déploiement cloud gratuit
├── docker-compose.yml             # Orchestration complète PostgreSQL + Backend + Frontend
├── CONTRIBUTING.md                # Guide GitFlow et assignation des 5 développeurs
└── .env.example                   # Modèle de variables d'environnement
```

---

## 🚀 Démarrage Rapide

### Prérequis
- **Java 17+** et **Maven 3.9+**
- **Node.js 20+** et **npm**
- **Docker** et **Docker Compose**

### Option 1 : Démarrage complet via Docker Compose (Recommandé)
```bash
# 1. Copier le fichier d'environnement
cp .env.example .env

# 2. Lancer tous les conteneurs (PostgreSQL + Backend + Frontend)
docker compose up --build
```
- Frontend : [http://localhost:5173](http://localhost:5173)
- Backend API : [http://localhost:8080](http://localhost:8080)
- Swagger UI (OpenAPI 3.0) : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Option 2 : Démarrage Local Manuel

#### 1. Démarrer PostgreSQL
```bash
docker compose up -d db
```

#### 2. Démarrer le Backend
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
*Flyway applique automatiquement les migrations V1, V2 et le seed de développement V3.*

#### 3. Démarrer le Frontend
```bash
cd frontend
npm install
npm run dev
```

---

## 👥 Comptes de Test Préconfigurés (Environnement Dev)

| Rôle | Nom d'utilisateur | Mot de passe | Accès & Droits |
| :--- | :--- | :--- | :--- |
| **DRH** | `drh` | `password123` | Accès complet tous modules, contrats, paie |
| **Manager** | `manager` | `password123` | Validation des congés de son équipe, évaluations |
| **Employé** | `saliou` | `password123` | Espace collaborateur, demande de congé, formations |
| **Direction** | `direction` | `password123` | Tableaux de bord de synthèse, suivi effectif |

---

## 🧪 Tests & Qualité de Code

```bash
# Exécuter les tests du backend (PostgreSQL Testcontainers + JaCoCo)
cd backend
mvn clean verify

# Vérifier la compilation et le typage strict du frontend
cd frontend
npm run build
```

---

## 🤝 Organisation de l'Équipe (5 Développeurs)

Consultez le fichier [`CONTRIBUTING.md`](./CONTRIBUTING.md) pour le découpage des tâches et la stratégie Git :
1. **Dev 1** : Socle technique, Authentification JWT, Sécurité, Gestion des Collaborateurs & Contrats.
2. **Dev 2** : Gestion des Congés & Absences (calcul 2j/mois, workflow N+1).
3. **Dev 3** : Module Recrutement (Pipeline Kanban, entretiens, notifications e-mail).
4. **Dev 4** : Évaluations de la Performance & Catalogue de Formations.
5. **Dev 5** : Paie (bulletins PDF iText, calculs IPRES/CSS) & Organigramme d'entreprise.

---

## ☁️ Déploiement Cloud Gratuit

Un guide exhaustif étape par étape est disponible dans [`docs/deployment/DEPLOYMENT.md`](./docs/deployment/DEPLOYMENT.md) pour déployer :
- **Base PostgreSQL** sur Neon.tech ou Supabase.
- **Backend API** sur Render.com ou Railway.
- **Frontend SPA** sur Vercel ou Netlify.
# CareerTrack
