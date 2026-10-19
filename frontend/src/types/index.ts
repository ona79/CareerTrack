// ─── Types globaux du domaine CareerTrack ─────────────────────────────────────

// Pagination générique
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  // Aligné sur PageResponse backend
  pageNumber: number;
  pageSize: number;
  last: boolean;
  // Champs historiques côté front (facultatifs, conservés pour compat)
  number?: number;
  size?: number;
  first?: boolean;
}

// Réponse API standard
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
}

// Réponse d'erreur
export interface ErrorResponse {
  success: false;
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  validationErrors?: Record<string, string>;
}

// ─── AUTH ─────────────────────────────────────────────────────────────────────
export interface LoginRequest {
  username: string;
  password: string;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
}

export interface UserDto {
  id: string;
  username: string;
  email: string;
  roles: string[];
  employeeId?: string;
}

export type RoleType =
  | 'ROLE_DRH'
  | 'ROLE_MANAGER'
  | 'ROLE_EMPLOYE'
  | 'ROLE_DIRECTION';

// ─── EMPLOYEE ─────────────────────────────────────────────────────────────────
export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'PROBATION' | 'SUSPENDED' | 'TERMINATED';
export type ContractType   = 'CDI' | 'CDD' | 'STAGE' | 'ALTERNANCE';
export type Gender         = 'M' | 'F';

export interface Employee {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  nationalId?: string;
  phone?: string;
  address?: string;
  birthDate?: string;
  hireDate: string;
  status: EmployeeStatus;

  // Champs aplatis renvoyés par EmployeeResponseDto
  departmentId?: string;
  departmentName?: string;
  positionId?: string;
  positionTitle?: string;
  managerId?: string;
  managerName?: string;

  // Champs historiques (facultatifs, conservés pour compatibilité avec
  // LeaveRequest, Evaluation, Payroll, etc.)
  gender?: Gender;
  personalEmail?: string;
  professionalEmail?: string;
  department?: Department;
  position?: Position;
  manager?: Employee;
  avatarUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface EmployeeSummary {
  id: string;
  employeeNumber: string;
  fullName: string;
  departmentId?: string;
  departmentName?: string;
  positionTitle?: string;
  status: EmployeeStatus;
  hireDate: string;
}

export interface Department {
  id: string;
  code: string;
  name: string;
  description?: string;
  manager?: Employee;
  headCount?: number;
}

export interface Position {
  id: string;
  code: string;
  title: string;
  level?: string;
  department?: Department;
  minSalary?: number;
  maxSalary?: number;
}

export type PositionDto = Position;

export interface Contract {
  id: string;
  employeeId: string;
  contractType: ContractType;
  startDate: string;
  endDate?: string;
  baseSalary: number;
  currency: string;
  isProbation: boolean;
  probationEndDate?: string;
  isActive: boolean;
}

// ─── CONGÉS ───────────────────────────────────────────────────────────────────
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveType {
  id: string;
  code: string;
  name: string;
  maxDaysPerYear: number;
  isPaid: boolean;
  requiresJustification: boolean;
}

export interface LeaveBalance {
  id: string;
  employeeId: string;
  leaveTypeId: string;
  leaveTypeName: string;
  year: number;
  totalDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface LeaveRequest {
  id: string;
  employee: Pick<Employee, 'id' | 'fullName' | 'employeeNumber'>;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  totalDays: number;
  status: LeaveStatus;
  reason?: string;
  approvedBy?: Pick<Employee, 'id' | 'fullName'>;
  approvedAt?: string;
  createdAt: string;
}

// ─── ÉVALUATION & PERFORMANCE ─────────────────────────────────────────────────
export type EvaluationStatus = 'DRAFT' | 'SELF_SUBMITTED' | 'MANAGER_EVALUATED' | 'COMPLETED' | 'VALIDATED';
export type CampaignStatus   = 'PLANNED' | 'PLANNING' | 'IN_PROGRESS' | 'ACTIVE' | 'CLOSED' | 'ARCHIVED';
export type ObjectiveCategory = 'OPERATIONAL' | 'MANAGERIAL' | 'TECHNICAL' | 'BEHAVIORAL' | 'STRATEGIC';

export interface SmartObjectiveDto {
  id?: string;
  title: string;
  description?: string;
  category: ObjectiveCategory;
  weight: number;
  targetDate?: string;
  achievementRate: number;
  status?: string;
  kpi?: string;
}

export interface EvaluationCampaign {
  id: string;
  title: string;
  campaignYear?: number;
  year?: number;
  startDate: string;
  endDate: string;
  status: CampaignStatus;
  description?: string;
}

export type EvaluationCampaignDto = EvaluationCampaign;

export interface EvaluationDto {
  id: string;
  campaignId?: string;
  campaignTitle?: string;
  employeeId?: string;
  employeeFullName: string;
  evaluatorId?: string;
  evaluatorFullName?: string;
  status: EvaluationStatus;
  selfScore?: number;
  managerScore?: number;
  finalScore?: number;
  feedback?: string;
  summaryPdfUrl?: string;
  interviewDate?: string;
  objectives: SmartObjectiveDto[];
}

export interface Evaluation {
  id: string;
  campaign?: EvaluationCampaign;
  campaignTitle?: string;
  employee?: Pick<Employee, 'id' | 'fullName'>;
  employeeFullName?: string;
  evaluator?: Pick<Employee, 'id' | 'fullName'>;
  evaluatorFullName?: string;
  overallScore?: number;
  finalScore?: number;
  status: EvaluationStatus;
  comments?: string;
  feedback?: string;
  summaryPdfUrl?: string;
  interviewDate?: string;
  objectives?: SmartObjectiveDto[];
  createdAt?: string;
}

export interface EvaluationCreateDto {
  campaignId: string;
  employeeId: string;
  evaluatorId?: string;
  interviewDate?: string;
  objectives?: {
    title: string;
    description?: string;
    category: ObjectiveCategory;
    weight: number;
    targetDate?: string;
    achievementRate?: number;
  }[];
}

export interface EvaluationSelfSubmitDto {
  selfScore: number;
  selfFeedback: string;
  objectiveRates?: {
    objectiveId: string;
    selfRate: number;
    selfComment?: string;
  }[];
}

export interface EvaluationManagerSubmitDto {
  managerScore: number;
  managerFeedback: string;
  interviewDate?: string;
  objectiveRates?: {
    objectiveId: string;
    achievementRate: number;
    status?: string;
  }[];
}

export interface EvaluationCompleteDto {
  finalFeedback?: string;
  finalScore?: number;
}

// ─── RECRUTEMENT ──────────────────────────────────────────────────────────────
export type JobOfferStatus   = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'CANCELLED';
export type ApplicationStatus = 'APPLIED' | 'SCREENING' | 'INTERVIEW' | 'OFFER' | 'HIRED' | 'REJECTED';

export interface JobOffer {
  id: string;
  title: string;
  department?: Department;
  position?: Position;
  description?: string;
  requirements?: string;
  salary_min?: number;
  salary_max?: number;
  status: JobOfferStatus;
  publishedAt?: string;
  closingDate?: string;
  applicationCount?: number;
}

export interface Candidate {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  linkedInUrl?: string;
  cvUrl?: string;
}

export interface JobApplication {
  id: string;
  jobOffer: Pick<JobOffer, 'id' | 'title'>;
  candidate: Candidate;
  status: ApplicationStatus;
  motivationLetter?: string;
  applicationDate: string;
}

// ─── FORMATION & COMPÉTENCES ──────────────────────────────────────────────────
export type TrainingSessionStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type EnrollmentStatus      = 'REQUESTED' | 'CONFIRMED' | 'ATTENDED' | 'COMPLETED' | 'DROPPED' | 'ENROLLED' | 'IN_PROGRESS' | 'ABSENT' | 'CANCELLED';

export interface Training {
  id: string;
  code: string;
  title: string;
  description?: string;
  durationHours?: number;
  provider?: string;
  category?: string;
}

export type TrainingDto = Training;

export interface TrainingSessionDto {
  id: string;
  trainingId?: string;
  trainingTitle?: string;
  trainingCode?: string;
  sessionCode: string;
  startDate: string;
  endDate: string;
  location?: string;
  maxParticipants: number;
  enrolledParticipantsCount: number;
  status: TrainingSessionStatus;
}

export interface TrainingEnrollmentDto {
  id: string;
  sessionId?: string;
  sessionCode?: string;
  trainingId?: string;
  trainingTitle?: string;
  durationHours?: number;
  provider?: string;
  sessionStartDate?: string;
  sessionEndDate?: string;
  employeeId?: string;
  employeeFullName?: string;
  employeeNumber?: string;
  enrollmentDate: string;
  status: EnrollmentStatus;
  score?: number;
  certificateUrl?: string;
  feedback?: string;
}

export interface TrainingSessionCreateDto {
  trainingId: string;
  sessionCode: string;
  startDate: string;
  endDate: string;
  location?: string;
  maxParticipants?: number;
}

export interface EnrollmentRequestDto {
  sessionId: string;
  employeeId?: string;
}

export interface EnrollmentStatusUpdateDto {
  status: EnrollmentStatus;
  score?: number;
  feedback?: string;
}

// ─── PAIE ─────────────────────────────────────────────────────────────────────
export type PayslipStatus = 'GENERATED' | 'VALIDATED' | 'PAID' | 'CANCELLED';
export type PayslipLineCategory = 'GAIN_BRUT' | 'DEDUCTION_SALARIALE' | 'COTISATION_PATRONALE';

export interface PayslipLine {
  id: string;
  lineType: string;
  category?: PayslipLineCategory;
  label: string;
  baseAmount?: number;
  rate?: number;
  amount: number;
}

export interface Payslip {
  id: string;
  employeeId?: string;
  employeeNumber?: string;
  employeeFullName?: string;
  departmentName?: string;
  periodYear: number;
  periodMonth: number;
  grossSalary: number;
  taxableGross?: number;
  taxParts?: number;
  netSalary: number;
  totalDeductions?: number;
  employerContributions?: number;
  paymentDate?: string;
  status: PayslipStatus;
  cancelledBy?: string;
  cancelledAt?: string;
  lines?: PayslipLine[];
}

export interface PayrollParameter {
  id: string;
  paramKey: string;
  paramValue: string;
  category: string;
  status: string;
  source: string;
  validFrom: string;
}

export interface DepartmentPayrollStats {
  departmentName: string;
  employeeCount: number;
  totalGrossSalary: number;
  totalNetSalary: number;
  totalEmployerContributions: number;
}

export interface PayrollStats {
  periodYear: number;
  periodMonth: number;
  totalEmployeesPaid: number;
  totalGrossSalary: number;
  totalNetSalary: number;
  totalEmployerContributions: number;
  totalPayrollCost: number;
  anonymized: boolean;
  departmentBreakdowns: DepartmentPayrollStats[];
}

// ─── NOTIFICATION ─────────────────────────────────────────────────────────────
export type NotificationType    = 'SYSTEM' | 'RECRUITMENT' | 'LEAVE' | 'EVALUATION' | 'PAYROLL';
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'BOTH';

export interface Notification {
  id: string;
  type: NotificationType;
  channel: NotificationChannel;
  subject: string;
  body: string;
  recipientId: string;
  isRead: boolean;
  sentAt: string;
}

// ─── ORGCHART ─────────────────────────────────────────────────────────────────
export interface OrgNode {
  id: string;
  name: string;
  employeeNumber?: string;
  title: string;
  department: string;
  avatarUrl?: string;
  children?: OrgNode[];
}

// ─── PROMOTION & SIMULATION ──────────────────────────────────────────────────
export type PromotionStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface PromotionProposalDto {
  id?: string;
  employeeId: string;
  employeeFullName?: string;
  currentPositionTitle?: string;
  proposedPositionId?: string;
  proposedPositionTitle?: string;
  currentSalary?: number;
  proposedSalary?: number;
  percentageIncrease?: number;
  status: PromotionStatus;
  justification?: string;
  proposedAt?: string;
}

export interface SalarySimulationRequestDto {
  title: string;
  departmentId?: string;
  averageIncreasePercentage: number;
}

export interface SalarySimulationResultDto {
  id?: string;
  title: string;
  departmentId?: string;
  departmentName?: string;
  averageIncreasePercentage: number;
  currentMonthlyPayroll: number;
  projectedMonthlyPayroll: number;
  monthlyDifference: number;
  projectedAnnualPayrollImpact: number;
  affectedEmployeesCount: number;
  simulatedAt?: string;
  createdByName?: string;
}

// ─── RECRUTEMENT & KANBAN ──────────────────────────────────────────────────────
export type KanbanStatus =
  | 'RECU'
  | 'PRESELECTIONNE'
  | 'ENTRETIEN'
  | 'OFFRE'
  | 'EMBAUCHE'
  | 'REFUSE';

export interface JobOfferDto {
  id: string;
  code: string;
  title: string;
  departmentId?: string;
  departmentName?: string;
  positionId?: string;
  positionTitle?: string;
  contractType: string;
  description: string;
  requirements?: string;
  status: 'DRAFT' | 'OPEN' | 'CLOSED' | 'ARCHIVED';
  openPositions: number;
  closingDate?: string;
  applicationsCount: number;
}

export interface JobOfferCreateDto {
  code?: string;
  title: string;
  departmentId?: string;
  positionId?: string;
  contractType: string;
  description: string;
  requirements?: string;
  openPositions?: number;
  closingDate?: string;
}

export interface JobApplicationDto {
  id: string;
  jobOfferId?: string;
  jobOfferTitle?: string;
  candidateId?: string;
  candidateFullName?: string;
  candidateEmail?: string;
  candidatePhone?: string;
  status: KanbanStatus;
  currentStep?: string;
  rating?: number;
  notes?: string;
  cvUrl?: string;
  appliedAt?: string;
}

export interface JobApplicationCreateDto {
  jobOfferId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  cvUrl?: string;
  linkedinUrl?: string;
  notes?: string;
}

// ─── DOCUMENTS RH ────────────────────────────────────────────────────────────
export type DocumentType =
  | 'CNI'
  | 'PASSEPORT'
  | 'DIPLOME'
  | 'CV'
  | 'CONTRAT'
  | 'ATTESTATION'
  | 'FICHE_PAIE'
  | 'CERTIFICAT_MEDICAL'
  | 'AUTRE';

export interface DocumentResponseDto {
  id: string;
  employeeId: string;
  documentType: DocumentType;
  title: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
  uploadedBy?: string;
  downloadUrl: string;
}