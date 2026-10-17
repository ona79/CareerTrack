# CareerTrack – Plateforme RH & Gestion des Carrières pour PME Sénégalaise

[![CI/CD](https://github.com/ona79/CareerTrack/actions/workflows/ci.yml/badge.svg)](https://github.com/ona79/CareerTrack/actions/workflows/ci.yml)
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

## 📥 Clonage et Installation du Projet

### 1. Prérequis Système

Assurez-vous que les outils suivants sont installés sur votre machine hôte :

| Outil | Version Minimale | Commande de vérification |
| :--- | :--- | :--- |
| **Git** | 2.30+ | `git --version` |
| **Java JDK** | 17 LTS | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Node.js** | 20 LTS ou 22 LTS | `node -v` |
| **npm** | 10+ | `npm -v` |
| **Docker & Docker Compose** | Docker 24+ / Compose v2+ | `docker --version && docker compose version` |

---

### 2. Cloner le Dépôt

Ouvrez un terminal et clonez le dépôt officiel :

```bash
# Option A : Clonage via HTTPS (recommandé si vous n'avez pas de clé SSH configurée)
git clone https://github.com/ona79/CareerTrack.git

# Option B : Clonage via SSH
git clone git@github.com:ona79/CareerTrack.git

# Entrer dans le répertoire du projet
cd CareerTrack
```

### 3. Choisir la Branche de Travail

Le projet suit une stratégie GitFlow avec la branche active `develop` :

```bash
# Pour développer ou tester les dernières fonctionnalités
git checkout develop

# Pour voir l'état stable de production
git checkout main
```

---

### 4. Configuration des Variables d'Environnement

Le projet fournit un fichier modèle `.env.example` prêt à l'emploi :

```bash
# Copier le fichier modèle à la racine
cp .env.example .env
```

> [!NOTE]
> Les valeurs par défaut dans `.env.example` conviennent directement pour un environnement local de développement.  
> Par défaut, la base PostgreSQL écoute sur le port **5434** (mappé vers le port interne 5432) afin d'éviter tout conflit si vous avez déjà un PostgreSQL local actif sur le port 5432.

---

## 🚀 Lancement Rapide

### Option 1 : Tout-en-un avec Docker Compose (Recommandé)

Cette méthode démarre PostgreSQL, le backend Spring Boot et le frontend React Vite en conteneurs isolés :

```bash
# Construire les images et démarrer tous les services en arrière-plan
docker compose up --build -d

# Vérifier que tous les conteneurs sont sains et actifs
docker compose ps
```

Accès aux services :
- 🌐 **Frontend Web** : [http://localhost:5173](http://localhost:5173)
- ⚙️ **Backend API REST** : [http://localhost:8080](http://localhost:8080)
- 📖 **Swagger UI / OpenAPI** : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 🗄️ **PostgreSQL** : `localhost:5434` (Base: `careertrack_db`, Utilisateur: `careertrack_user`)

Pour arrêter les services :
```bash
docker compose down
```

---

### Option 2 : Démarrage Local Manuel (Mode Développeur)

Si vous souhaitez modifier le code et profiter du rechargement à chaud (Hot Reload) :

#### Étape 1 : Démarrer uniquement la base PostgreSQL
```bash
docker compose up -d db
```

#### Étape 2 : Démarrer le Backend (Spring Boot)
```bash
cd backend

# Compiler et lancer l'application avec le profil 'dev' (charge la base PostgreSQL + le seed de démo V3)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
*Le serveur démarrera sur [http://localhost:8080](http://localhost:8080). Les migrations Flyway V1, V2 et le seed V3 sont appliqués automatiquement.*

#### Étape 3 : Démarrer le Frontend (React + Vite)
Dans un nouveau terminal :
```bash
cd frontend

# Installer les dépendances exactement selon package-lock.json
npm ci

# Lancer le serveur de développement Vite
npm run dev
```
*L'application s'ouvre sur [http://localhost:5173](http://localhost:5173) (ou 5174 si le port 5173 est occupé).*

---

## 👥 Comptes de Démonstration Préconfigurés (Environnement Dev & Seed V3)

| Rôle | Nom d'utilisateur | Mot de passe | Droits et Modules accessibles |
| :--- | :--- | :--- | :--- |
| **DRH** | `drh.ndiaye` | `password123` | Accès complet tous modules, collaborateurs, paie, validation |
| **Manager N+1** | `mgr.sow` | `password123` | Validation des congés de son équipe, évaluations annuelles |
| **Collaborateur** | `emp.diallo` | `password123` | Espace personnel, demande de congés, historique formations |
| **Direction** | `dir.fall` | `password123` | Tableaux de bord de synthèse, suivi effectif, masse salariale |

---

## 🏗️ Architecture Technique

```
CareerTrack/
├── backend/                       # Application Spring Boot 3 (Java 17+)
│   ├── src/main/java/com/careertrack/
│   │   ├── common/                # Sécurité JWT, Exceptions, PDF iText, Auditing, CORS
│   │   └── modules/               # Modules métier modulaires
│   │       ├── auth/              # Authentification, RBAC & Tokens JWT
│   │       ├── employee/          # Salariés, Contrats, Historique salaires
│   │       ├── leave/             # Soldes et demandes de congés (2j/mois)
│   │       ├── payroll/           # Bulletins de paie et lignes de cotisations
│   │       ├── recruitment/       # Offres d'emploi et pipeline de candidatures
│   │       ├── evaluation/        # Campagnes d'évaluation et objectifs
│   │       ├── training/          # Plans et catalogues de formation
│   │       ├── orgchart/          # Arborescence hiérarchique managériale
│   │       ├── document/          # Génération et téléchargement PDF
│   │       ├── notification/      # Alertes in-app et e-mails
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
│   │   ├── pages/                 # Écrans des modules métier (lazy-loaded)
│   │   ├── types/                 # Typage complet du domaine TypeScript
│   │   └── index.css              # Design System responsive & palette Sénégal
│
├── docs/                          # Spécifications, diagrammes UML & guides
│   ├── uml/architecture.md        # Diagrammes de séquence, ERD et composants
│   ├── deployment/DEPLOYMENT.md   # Guide de déploiement cloud gratuit
│   └── BACKLOG.md                 # Inventaire et backlog des modules
├── docker-compose.yml             # Orchestration complète PostgreSQL + Backend + Frontend
├── CONTRIBUTING.md                # Guide GitFlow et assignation des 5 développeurs
└── .env.example                   # Modèle de variables d'environnement
```

---

## 🧪 Tests & Qualité de Code

```bash
# Exécuter les 47 tests du backend (Tests unitaires + Testcontainers PostgreSQL + JaCoCo >= 60%)
cd backend
mvn clean verify

# Vérifier la qualité et le typage du Frontend
cd ../frontend
npm run lint      # Linter Oxlint ultra-rapide
npm run build     # Compilation TypeScript stricte & bundle Vite
```

---

## 📴 Procédure de Test Manuel du Mode Hors-Ligne (PWA & Service Worker)

L'application intègre un Service Worker (Workbox) avec précache de tous les bundles JavaScript/CSS et une stratégie de cache pour les données hors-ligne (`careertrack-api-cache`).

Pour tester le fonctionnement hors connexion :
1. **Connexion en ligne** : Rendez-vous sur `http://localhost:5173/login` et connectez-vous avec `drh.ndiaye` / `password123`.
2. **Visite des pages (remplissage du précache)** : Naviguez dans les modules (`/dashboard`, `/employees`, `/leaves`, `/payroll`, `/orgchart`, etc.). Le Service Worker met automatiquement en cache les chunks correspondants.
3. **Passage hors connexion** :
   - Ouvrez la console développeur du navigateur (`F12` ou `Ctrl+Shift+I`).
   - Rendez-vous dans l'onglet **Réseau (Network)** et activez l'état **Hors-ligne (Offline)**.
4. **Rechargement de la page** :
   - Actualisez la page (`F5` ou `Ctrl+R`). L'application se recharge instantanément sans écran blanc ni erreur 404.
5. **Navigation hors ligne** :
   - Cliquez sur `/leaves` (Congés) et `/employees` (Collaborateurs) : les données s'affichent depuis le cache Workbox.

---

## 🤝 Organisation de l'Équipe (5 Développeurs)

Consultez le fichier [`CONTRIBUTING.md`](./CONTRIBUTING.md) pour les règles GitFlow et le découpage par développeur :
1. **Ibrahima ** : Socle technique, Authentification JWT, Sécurité, Collaborateurs & Contrats.
2. **Eric joel** : Gestion des Congés & Absences (calcul 2j/mois, workflow N+1).
3. **Assete** : Module Recrutement (Pipeline Kanban, entretiens, notifications e-mail).
4. **Aminata** : Évaluations de la Performance & Catalogue de Formations.
5. **Sambay** : Paie (bulletins PDF iText, calculs IPRES/CSS) & Organigramme d'entreprise.

---

## ☁️ Déploiement Cloud Gratuit

Un guide exhaustif étape par étape est disponible dans [`docs/deployment/DEPLOYMENT.md`](./docs/deployment/DEPLOYMENT.md) pour déployer :
- **Base PostgreSQL** sur Neon.tech ou Supabase.
- **Backend API** sur Render.com ou Railway.
- **Frontend SPA** sur Vercel ou Netlify.
