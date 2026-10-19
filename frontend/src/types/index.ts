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

export type DepartmentDto = Department;

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

// ─── LOT 2 : DOSSIER EMPLOYÉ & ORGANIGRAMME ──────────────────────────────────
export interface EmployeeSummaryDto {
  id: string;
  employeeNumber: string;
  fullName: string;
  positionTitle?: string;
  departmentName?: string;
  status: EmployeeStatus;
}

export interface SalaryHistoryDto {
  id: string;
  employeeId: string;
  previousSalary: number;
  newSalary: number;
  changeReason: string;
  effectiveDate: string;
  approvedById?: string;
  approvedByName?: string;
  createdAt: string;
}

export interface PositionHistoryDto {
  id: string;
  employeeId: string;
  previousPositionId?: string;
  previousPositionTitle?: string;
  newPositionId?: string;
  newPositionTitle?: string;
  previousDepartmentId?: string;
  previousDepartmentName?: string;
  newDepartmentId?: string;
  newDepartmentName?: string;
  effectiveDate: string;
  changeReason: string;
  changedById?: string;
  changedByName?: string;
  createdAt: string;
}

export interface EmployeeHistoryDto {
  salaryHistories?: SalaryHistoryDto[];
  positionHistories: PositionHistoryDto[];
}

export interface EmployeeDetailDto {
  identity: {
    id: string;
    employeeNumber: string;
    firstName: string;
    lastName: string;
    fullName: string;
    email: string;
    hireDate: string;
    departmentId?: string;
    departmentName?: string;
    positionId?: string;
    positionTitle?: string;
    managerId?: string;
    managerName?: string;
    status: EmployeeStatus;
  };
  personalData?: {
    nationalId: string;
    birthDate: string;
    phone?: string;
    address?: string;
    maritalStatus?: string;
    dependentsCount?: number;
    isCadre?: boolean;
  };
  contract?: {
    id: string;
    contractType: ContractType;
    startDate: string;
    endDate?: string;
    active: boolean;
    grossSalary?: number;
  };
  salaryHistory?: SalaryHistoryDto[];
  positionHistory?: PositionHistoryDto[];
  documents?: DocumentResponseDto[];
  permissions: {
    canEdit: boolean;
    canDeactivate: boolean;
    canEditContact: boolean;
  };
}

export interface EmployeeCreateDto {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  nationalId: string;
  email: string;
  phone?: string;
  address?: string;
  birthDate: string;
  hireDate: string;
  departmentId?: string;
  positionId?: string;
  managerId?: string;
  status?: EmployeeStatus;
}

export interface EmployeeUpdateDto {
  firstName: string;
  lastName: string;
  nationalId: string;
  email: string;
  phone?: string;
  address?: string;
  birthDate: string;
  hireDate: string;
  departmentId?: string;
  positionId?: string;
  managerId?: string;
  status?: EmployeeStatus;
  isCadre?: boolean;
  maritalStatus?: string;
  dependentsCount?: number;
  grossSalary?: number;
  changeReason?: string;
}

export interface EmployeeSelfUpdateDto {
  phone?: string;
  address?: string;
}

export interface EmployeeDeactivateDto {
  reason: string;
  departureDate?: string;
}

export interface OrgNodeDto {
  id: string;
  name: string;
  employeeNumber?: string;
  title: string;
  department: string;
  email?: string;
  phone?: string;
  subordinateCount?: number;
  subordinates: OrgNodeDto[];
}

// ─── LOT 3 : CONGÉS & DASHBOARD ─────────────────────────────────────────────
export interface LeaveRequestResponseDto {
  id: string;
  employeeId: string;
  employeeName: string;
  employeeNumber: string;
  departmentName?: string;
  leaveTypeId: string;
  leaveTypeName: string;
  leaveTypeCode: string;
  startDate: string;
  endDate: string;
  requestedDays: number;
  reason?: string;
  status: 'SUBMITTED' | 'APPROVED_N1' | 'APPROVED_DRH' | 'REJECTED' | 'CANCELLED' | 'EN_ATTENTE' | 'VALIDE' | 'REFUSE' | 'ANNULE' | 'PENDING' | 'APPROVED';
  managerComment?: string;
  approvedById?: string;
  approvedByName?: string;
  approvedAt?: string;
  createdAt: string;
}

export interface LeaveBalanceResponseDto {
  id: string;
  employeeId: string;
  leaveTypeId: string;
  leaveTypeName: string;
  leaveTypeCode?: string;
  periodYear: number;
  allocatedDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface TeamCalendarItemDto {
  requestId: string;
  employeeId: string;
  employeeName: string;
  employeeNumber: string;
  departmentId?: string;
  departmentName?: string;
  positionTitle?: string;
  leaveTypeId: string;
  leaveTypeName: string;
  leaveTypeCode?: string;
  startDate: string;
  endDate: string;
  requestedDays: number;
  status: string;
  conflicting: boolean;
}

export interface TeamLeaveConflictDto {
  departmentId: string;
  startDate: string;
  endDate: string;
  hasConflict: boolean;
  threshold: number;
  overlappingRequestsCount: number;
  overlappingRequests: LeaveRequestResponseDto[];
}

export interface WorkingDaysCalculationDto {
  startDate: string;
  endDate: string;
  workingDays: number;
}

export interface ContractTypeBreakdownDto {
  contractType: string;
  count: number;
  percentage: number;
}

export interface DepartmentHeadcountDto {
  departmentId: string;
  departmentName: string;
  employeeCount: number;
}

export interface HeadcountKpiDto {
  totalEmployees: number;
  activeEmployees: number;
  onLeaveEmployees: number;
  terminatedEmployees: number;
  contractBreakdown: ContractTypeBreakdownDto[];
  departmentBreakdown: DepartmentHeadcountDto[];
}

export interface AgePyramidBracketDto {
  bracket: string;
  count: number;
  percentage: number;
}

export interface TurnoverKpiDto {
  turnoverRate: number;
  hiresCount: number;
  departuresCount: number;
  averageHeadcount: number;
}

export interface MonthlyPayrollTrendDto {
  month: number;
  monthLabel: string;
  grossSalary: number;
  netSalary: number;
  employerContributions: number;
}

export interface PayrollKpiDto {
  totalGrossSalary: number;
  totalNetSalary: number;
  totalEmployerContributions: number;
  payslipsCount: number;
  monthlyTrend: MonthlyPayrollTrendDto[];
}

export interface TrainingKpiDto {
  totalTrainings: number;
  activeSessions: number;
  completedSessions: number;
  totalParticipants: number;
}

export interface LeaveKpiDto {
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  totalDaysTaken: number;
}

export interface DashboardStatsDto {
  periodYear: number;
  headcount: HeadcountKpiDto;
  agePyramid: AgePyramidBracketDto[];
  turnover: TurnoverKpiDto;
  payroll: PayrollKpiDto;
  training: TrainingKpiDto;
  leaves: LeaveKpiDto;
}



