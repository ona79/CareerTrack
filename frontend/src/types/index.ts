// ─── Types globaux du domaine CareerTrack ─────────────────────────────────────

// Pagination générique
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
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
export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'RESIGNED' | 'RETIRED';
export type ContractType   = 'CDI' | 'CDD' | 'STAGE' | 'ALTERNANCE';
export type Gender         = 'M' | 'F';

export interface Employee {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  fullName: string;
  gender: Gender;
  birthDate?: string;
  phone?: string;
  personalEmail?: string;
  professionalEmail?: string;
  hireDate: string;
  status: EmployeeStatus;
  department?: Department;
  position?: Position;
  manager?: Employee;
  avatarUrl?: string;
  createdAt: string;
  updatedAt: string;
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

// ─── ÉVALUATION ───────────────────────────────────────────────────────────────
export type EvaluationStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'VALIDATED';
export type CampaignStatus   = 'PLANNING' | 'ACTIVE' | 'CLOSED' | 'ARCHIVED';

export interface EvaluationCampaign {
  id: string;
  title: string;
  year: number;
  startDate: string;
  endDate: string;
  status: CampaignStatus;
  description?: string;
}

export interface Evaluation {
  id: string;
  campaign: EvaluationCampaign;
  employee: Pick<Employee, 'id' | 'fullName'>;
  evaluator: Pick<Employee, 'id' | 'fullName'>;
  overallScore?: number;
  status: EvaluationStatus;
  comments?: string;
  createdAt: string;
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

// ─── FORMATION ────────────────────────────────────────────────────────────────
export type TrainingSessionStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type EnrollmentStatus      = 'ENROLLED' | 'IN_PROGRESS' | 'COMPLETED' | 'ABSENT' | 'CANCELLED';

export interface Training {
  id: string;
  code: string;
  title: string;
  description?: string;
  durationHours?: number;
  provider?: string;
  category?: string;
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
  currentSalary: number;
  proposedSalary: number;
  percentageIncrease?: number;
  status: PromotionStatus;
  justification?: string;
  proposedAt?: string;
}

export interface SalarySimulationDto {
  id?: string;
  departmentId?: string;
  departmentName?: string;
  budgetIncreasePercentage: number;
  totalCostImpact: number;
  simulatedAt?: string;
}

// ─── DOCUMENTS RH ─────────────────────────────────────────────────────────────
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


