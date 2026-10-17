# Diagramme de Classes Métier – Système CareerTrack

Ce document décrit le modèle de domaine orienté objet de **CareerTrack**, avec les entités clés, leurs attributs, types et cardinalités.

---

## 1. Vue d'Ensemble du Domaine Métier

Le modèle métier s'articule autour de 10 sous-domaines :
1. **Sécurité & Utilisateurs** : `User`, `Role`, `RefreshToken`
2. **Organisation & Collaborateurs** : `Employee`, `Department`, `Position`, `Contract`
3. **Temps & Absences** : `LeaveRequest`, `LeaveType`, `LeaveBalance`
4. **Rémunération & Cotisations Sénégalaises** : `Payslip`, `PayslipLine`, `SalarySimulation`, `CompanySetting`
5. **Évaluations & Objectifs** : `EvaluationCampaign`, `Evaluation`, `Objective`
6. **Recrutement & Candidatures** : `JobOffer`, `JobApplication`, `Candidate`, `ApplicationInterview`
7. **Formations** : `Training`, `TrainingSession`, `TrainingEnrollment`
8. **Évolution & Carrière** : `PromotionProposal`
9. **Communication & Notifications** : `Notification`
10. **Documents** : `EmployeeDocument`

---

## 2. Diagramme de Classes (Mermaid)

```mermaid
classDiagram
    %% Module Utilisateurs & Sécurité
    class User {
        +UUID id
        +String username
        +String password
        +String email
        +boolean enabled
        +Set~Role~ roles
    }

    class Role {
        +Long id
        +String name
    }

    class RefreshToken {
        +UUID id
        +String token
        +Instant expiryDate
        +User user
    }

    User "1" --> "*" Role : possesses
    User "1" --> "*" RefreshToken : has

    %% Module Organisation & Employés
    class Employee {
        +UUID id
        +String employeeNumber
        +String firstName
        +String lastName
        +String email
        +String phone
        +LocalDate hireDate
        +String nationalIdNumber
        +String ipresNumber
        +String cssNumber
        +Department department
        +Position position
        +Employee manager
        +User user
    }

    class Department {
        +UUID id
        +String code
        +String name
        +Employee manager
    }

    class Position {
        +UUID id
        +String code
        +String title
        +Department department
    }

    class Contract {
        +UUID id
        +String contractType
        +LocalDate startDate
        +LocalDate endDate
        +BigDecimal grossSalary
        +boolean active
        +Employee employee
    }

    Employee "1" --> "0..1" User : linked_to
    Department "1" --> "*" Employee : belongs_to
    Position "1" --> "*" Employee : occupies
    Employee "1" --> "*" Contract : signs
    Employee "1" --> "0..1" Employee : reports_to (N+1)

    %% Module Congés
    class LeaveType {
        +UUID id
        +String code
        +String name
        +int maxDaysPerYear
        +boolean requiresProof
    }

    class LeaveBalance {
        +UUID id
        +int periodYear
        +double totalAccrued
        +double usedDays
        +double remainingDays
        +Employee employee
        +LeaveType leaveType
    }

    class LeaveRequest {
        +UUID id
        +LocalDate startDate
        +LocalDate endDate
        +double durationDays
        +String status
        +String reason
        +Employee employee
        +LeaveType leaveType
    }

    Employee "1" --> "*" LeaveBalance : holds
    LeaveType "1" --> "*" LeaveBalance : categorizes
    Employee "1" --> "*" LeaveRequest : submits
    LeaveType "1" --> "*" LeaveRequest : specifies

    %% Module Paie Sénégalaise
    class Payslip {
        +UUID id
        +int periodYear
        +int periodMonth
        +BigDecimal grossSalary
        +BigDecimal netSalary
        +BigDecimal totalDeductions
        +String status
        +LocalDate paymentDate
        +String pdfUrl
        +Employee employee
    }

    class PayslipLine {
        +UUID id
        +String lineType
        +String label
        +BigDecimal baseAmount
        +BigDecimal rate
        +BigDecimal amount
        +int sortOrder
        +Payslip payslip
    }

    class CompanySetting {
        +UUID id
        +String settingKey
        +String settingValue
        +String description
    }

    Employee "1" --> "*" Payslip : receives
    Payslip "1" --> "*" PayslipLine : items

    %% Module Évaluation & Objectifs SMART
    class EvaluationCampaign {
        +UUID id
        +String title
        +int campaignYear
        +LocalDate startDate
        +LocalDate endDate
        +String status
    }

    class Evaluation {
        +UUID id
        +LocalDate interviewDate
        +BigDecimal selfScore
        +BigDecimal managerScore
        +BigDecimal finalScore
        +String feedback
        +String summaryPdfUrl
        +String status
        +EvaluationCampaign campaign
        +Employee employee
        +Employee evaluator
    }

    class Objective {
        +UUID id
        +String title
        +String description
        +String category
        +int weight
        +LocalDate targetDate
        +BigDecimal achievementRate
        +String status
        +Evaluation evaluation
    }

    EvaluationCampaign "1" --> "*" Evaluation : conducts
    Employee "1" --> "*" Evaluation : evaluated_in
    Evaluation "1" --> "*" Objective : contains_SMART_goals

    %% Module Recrutement
    class JobOffer {
        +UUID id
        +String title
        +String description
        +String profileRequirements
        +String status
        +LocalDate publicationDate
    }

    class Candidate {
        +UUID id
        +String firstName
        +String lastName
        +String email
        +String phone
        +String cvUrl
    }

    class JobApplication {
        +UUID id
        +String status
        +LocalDate applicationDate
        +String resumeUrl
        +String motivationLetter
        +JobOffer jobOffer
        +Candidate candidate
    }

    JobOffer "1" --> "*" JobApplication : receives
    Candidate "1" --> "*" JobApplication : submits
```
