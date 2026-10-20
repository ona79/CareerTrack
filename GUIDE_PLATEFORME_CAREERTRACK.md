# CAREERTRACK — Guide Fonctionnel & Barème des Tests

> **Document de Référence Équipe**  
> *Plateforme SIRH & Accompagnement de Carrière (Sénégal / UEMOA)*  
> **Fichier PDF généré à la racine :** [`GUIDE_PLATEFORME_CAREERTRACK.pdf`](file:///home/diallo/for_me/Semestre_6/project/CareerTrack/GUIDE_PLATEFORME_CAREERTRACK.pdf)

---

## 1. Vue d'Ensemble de la Plateforme

**CareerTrack** est une solution SIRH moderne conçue pour automatiser et piloter l'intégralité du cycle de vie des collaborateurs en entreprise : recrutement, gestion administrative, suivi des congés avec calcul d'impact, évaluations de performance annuelles (SMART), politique de rémunération en Franc CFA (FCFA) et plans de formation.

- **Frontend Web :** [career-track-steel.vercel.app](https://career-track-steel.vercel.app) *(React 18 + TypeScript + Vite + Tailwind)*
- **Backend API :** [careertrack-to49.onrender.com](https://careertrack-to49.onrender.com) *(Spring Boot 3 + Java 21 + Spring Security 6)*
- **Base de données :** PostgreSQL Serverless Neon Cloud *(AWS Francfort)*
- **Devise de Référence :** Franc CFA (FCFA / XOF)

---

## 2. Matrice des Rôles & Périmètre d'Accès

L'accès est cloisonné via une authentification JWT et un contrôle RBAC strict. Chaque membre de l'équipe doit comprendre son périmètre :

| Rôle | Périmètre & Visibilité | Actions Autorisées | Restrictions |
| :--- | :--- | :--- | :--- |
| **DRH** | Supervision globale de l'entreprise | Création de collaborateurs, publication d'offres d'emploi, validation budgétaire finale des promotions, ouverture des campagnes d'évaluation, gestion du catalogue de formations. | Aucune restriction opérationnelle. |
| **Manager** | Gestion de son département / équipe | Validation niveau 1 des demandes de congés de son équipe, soumission de propositions de promotion, réalisation des entretiens managériaux annuels, suivi du Kanban recrutement. | Ne peut pas créer de profil employé ni valider définitivement les promotions salariales. |
| **Employé** | Espace personnel & self-service | Dépôt de demandes de congés avec calcul automatique des jours ouvrés, réalisation de l'auto-évaluation annuelle, inscription aux sessions de formation, consultation de l'organigramme. | Accès strictement restreint à son dossier et à ses bulletins de paie. |
| **Direction** | **Lecture Seule Totale (Audit & Vision 360°)** | Consultation illimitée de tous les modules : registre des employés, organigramme hiérarchique, pipeline de recrutement, évaluations de performance, simulations de masse salariale. | **Aucun bouton d'action d'écriture** (formulaires et boutons de modification automatiquement masqués). |

---

## 3. Fonctionnement des Modules Clés

### 1. Collaborateurs & Organigramme Interactif
- Fiche RH unifiée (matricule unique, numéro CNI, type de contrat CDI/CDD/Stage, département et poste).
- **Navigation directe depuis l'organigramme :** un simple clic sur un collaborateur dans l'arborescence ouvre directement son dossier complet (`/employees/:id`).

### 2. Recrutement & Pipeline Kanban
- Suivi visuel des candidatures en 5 étapes (`Reçu`, `Tri CV`, `Entretien`, `Proposition`, `Embauché`).
- Déplacement fluide par drag-and-drop.
- **Fermeture automatique :** La modale de création d'offre et la modale de dépôt de candidature disparaissent immédiatement après confirmation.

### 3. Congés & Calendrier d'Équipe
- Calcul automatique des jours ouvrés décomptés (lundi au vendredi, hors week-ends).
- **Détection des conflits en direct :** Alerte visuelle si plusieurs collègues du même département posent un congé sur les mêmes dates.
- Workflow d'approbation N+1 avec commentaire obligatoire en cas de refus.

### 4. Évaluations Annuelles SMART
- Campagnes ouvertes annuellement par la DRH.
- Double notation : auto-évaluation du collaborateur puis entretien d'évaluation managériale N+1.
- Calcul pondéré du taux d'atteinte des objectifs SMART pour dégager une note globale sur 5.0.

### 5. Rémunération & Promotions
- **Simulateur d'impact sur la masse salariale :** Projection instantanée du surcoût mensuel brut et annuel (+20% charges patronales) en FCFA.
- Workflow en deux étapes : proposition argumentée par le Manager N+1 puis approbation finale par la DRH.

### 6. Formations Professionnelles
- Catalogue de modules classés par thématiques (Technique, Management, Soft Skills, Juridique).
- Planification de sessions avec jauge maximale de participants et gestion des présences.

---

## 4. Ergonomie & Optimisations Récentes
1. **Fermeture automatique des modales :** Dès qu'un ajout ou une validation est réussi(e), la fenêtre disparaît d'elle-même sans action manuelle.
2. **Affichage Mobile Bottom-Sheet :** Sur smartphone, les fenêtres s'affichent directement ancrées en bas d'écran sans défilement préalable inutile.
3. **Champs de saisie tactiles (min 44px) :** Tous les champs de saisie (notamment l'intitulé de poste) sont agrandis pour un confort optimal au toucher sur mobile.
4. **Fuseau horaire automatique :** L'heure affichée dans l'en-tête s'adapte en temps réel au fuseau horaire du navigateur client.

---

## 5. Barème Officiel & Bilan des Tests

| Critère de Validation | Barème / Exigence | Résultat Obtenu | Statut |
| :--- | :--- | :--- | :--- |
| **Tests Automatisés Backend** *(Unitaires & Intégration)* | 100% de réussite sans régression | **219 tests exécutés** (0 erreur, 0 échec, 0 ignoré) | **VALIDÉ (100%)** |
| **Couverture de Code JaCoCo** *(Logique métier)* | Seuil minimal requis : **≥ 60%** | **77.2%** de couverture de lignes | **DÉPASSÉ (+17.2%)** |
| **Intégrité Base de Données** *(H2 / Neon PostgreSQL)* | Zéro violation de contrainte unique | Correction collision `users.email` validée dans `LeaveControllerIntegrationTest` | **CONFORME** |
| **Compilation Frontend** *(TypeScript / Vite)* | 0 erreur de typage / syntaxe | Build production généré en **618 ms** avec PWA Service Worker | **VALIDÉ (0 erreur)** |
| **Sécurité RBAC** *(JWT & Contrôle d'accès)* | Protection stricte des endpoints | Authentification et autorisations testées sur tous les contrôleurs | **CONFORME** |

---
*Document généré automatiquement à la racine du projet : `GUIDE_PLATEFORME_CAREERTRACK.pdf`.*
