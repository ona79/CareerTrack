import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import {
  TrendingUp,
  Plus,
  CheckCircle,
  Clock,
  ArrowRight,
  Calculator,
  UserCheck,
  X,
  Loader2,
  RefreshCw,
  Building,
  DollarSign,
  AlertCircle,
  Eye,
  FileCheck,
  ChevronRight,
  Sparkles,
} from 'lucide-react';
import type {
  PromotionProposalDto,
  SalarySimulationRequestDto,
  SalarySimulationResultDto,
  EmployeeSummaryDto,
  PositionDto,
  DepartmentDto,
  ApiResponse,
  Page,
} from '@/types';

export const PromotionListPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();

  const isAdmin = hasRole('ADMIN') || hasRole('ROLE_ADMIN');
  const isDrh = hasRole('DRH') || hasRole('ROLE_DRH') || isAdmin;
  const isDirection = hasRole('DIRECTION') || hasRole('ROLE_DIRECTION') || isAdmin;
  const canViewSalaries = isDrh || isDirection;
  const isManagerOrDrh = hasRole('MANAGER') || hasRole('ROLE_MANAGER') || isDrh || isDirection;

  const [notificationMsg, setNotificationMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // Modales
  const [showSimulateModal, setShowSimulateModal] = useState<boolean>(false);
  const [showProposeModal, setShowProposeModal] = useState<boolean>(false);
  const [showSimulationsListModal, setShowSimulationsListModal] = useState<boolean>(false);
  const [approvalModalProposal, setApprovalModalProposal] = useState<PromotionProposalDto | null>(null);
  const [approvalComment, setApprovalComment] = useState<string>('');

  // 1. Liste des propositions de promotion
  const {
    data: promotions = [],
    isLoading: isLoadingPromotions,
    refetch: refetchPromotions,
  } = useQuery<PromotionProposalDto[]>({
    queryKey: ['promotions'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PromotionProposalDto[]>>('/promotions');
      return res.data?.data || [];
    },
  });

  // 2. Liste des simulations salariales
  const {
    data: simulations = [],
    isLoading: isLoadingSimulations,
    refetch: refetchSimulations,
  } = useQuery<SalarySimulationResultDto[]>({
    queryKey: ['salary-simulations'],
    queryFn: async () => {
      if (!canViewSalaries) return [];
      const res = await api.get<ApiResponse<SalarySimulationResultDto[]>>('/promotions/simulations');
      return res.data?.data || [];
    },
    enabled: canViewSalaries,
  });

  // 3. Départements et Postes
  const { data: departments = [] } = useQuery<DepartmentDto[]>({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<DepartmentDto[]>>('/employees/departments');
      return res.data?.data || [];
    },
  });

  const { data: positions = [] } = useQuery<PositionDto[]>({
    queryKey: ['positions'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PositionDto[]>>('/employees/positions');
      return res.data?.data || [];
    },
  });

  // 4. Liste des employés pour la proposition de promotion
  const { data: employeesPage } = useQuery<Page<EmployeeSummaryDto>>({
    queryKey: ['employees-for-promotion'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Page<EmployeeSummaryDto>>>('/employees?size=100');
      return res.data?.data;
    },
    enabled: showProposeModal,
  });
  const employees = employeesPage?.content || [];

  // État du formulaire de proposition de promotion
  const [proposalForm, setProposalForm] = useState<{
    employeeId: string;
    proposedPositionId: string;
    proposedSalary: number | '';
    justification: string;
  }>({
    employeeId: '',
    proposedPositionId: '',
    proposedSalary: '',
    justification: '',
  });

  // État du simulateur
  const [simulationForm, setSimulationForm] = useState<SalarySimulationRequestDto>({
    title: 'Plan Revalorisation Annuelle 2026',
    departmentId: '',
    averageIncreasePercentage: 5.0,
  });
  const [latestSimulationResult, setLatestSimulationResult] = useState<SalarySimulationResultDto | null>(null);

  // Formatage FCFA
  const formatFCFA = (val?: number | null) => {
    if (val === null || val === undefined) return '•••••• FCFA';
    return new Intl.NumberFormat('fr-SN', { style: 'decimal' }).format(val) + ' FCFA';
  };

  // Mutation Proposition de Promotion
  const createProposalMutation = useMutation({
    mutationFn: async (payload: typeof proposalForm) => {
      const res = await api.post<ApiResponse<PromotionProposalDto>>('/promotions', {
        employeeId: payload.employeeId,
        proposedPositionId: payload.proposedPositionId ? payload.proposedPositionId : null,
        proposedSalary: Number(payload.proposedSalary),
        justification: payload.justification || '',
      });
      return res.data?.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['promotions'] });
      setShowProposeModal(false);
      setProposalForm({
        employeeId: '',
        proposedPositionId: '',
        proposedSalary: '',
        justification: '',
      });
      setNotificationMsg({
        text: `Proposition de promotion soumise avec succès pour ${data.employeeFullName} !`,
        type: 'success',
      });
      setTimeout(() => setNotificationMsg(null), 5000);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Erreur lors de la soumission de la proposition';
      setNotificationMsg({ text: msg, type: 'error' });
    },
  });

  // Mutation Validation DRH
  const validatePromotionMutation = useMutation({
    mutationFn: async ({ id, status, comment }: { id: string; status: 'APPROVED' | 'REJECTED'; comment?: string }) => {
      const res = await api.patch<ApiResponse<PromotionProposalDto>>(
        `/promotions/${id}/status?status=${status}&comment=${encodeURIComponent(comment || '')}`
      );
      return res.data?.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['promotions'] });
      setApprovalModalProposal(null);
      setApprovalComment('');
      setNotificationMsg({
        text: `Promotion validée avec succès pour ${data.employeeFullName} ! Contrat et poste mis à jour.`,
        type: 'success',
      });
      setTimeout(() => setNotificationMsg(null), 5000);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Erreur lors de la validation de la promotion';
      setNotificationMsg({ text: msg, type: 'error' });
    },
  });

  // Mutation Simulation Salariale
  const runSimulationMutation = useMutation({
    mutationFn: async (payload: SalarySimulationRequestDto) => {
      const cleanPayload = {
        title: (payload.title || 'Simulation budgétaire').trim(),
        departmentId: payload.departmentId ? payload.departmentId : null,
        averageIncreasePercentage: Number(payload.averageIncreasePercentage) || 0,
      };
      const res = await api.post<ApiResponse<SalarySimulationResultDto>>('/promotions/simulations', cleanPayload);
      return res.data?.data;
    },
    onSuccess: (data) => {
      setLatestSimulationResult(data);
      queryClient.invalidateQueries({ queryKey: ['salary-simulations'] });
      setNotificationMsg({ text: 'Simulation salariale calculée avec succès en FCFA !', type: 'success' });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Erreur lors du calcul de la simulation';
      setNotificationMsg({ text: msg, type: 'error' });
    },
  });

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <TrendingUp className="w-6 h-6 text-[#1A6B3C]" />
            Promotions & Revalorisations Salariales
          </h1>
          <p className="text-xs text-gray-500 mt-0.5">
            Gestion des avancements de carrière, validation hiérarchique DRH et simulations budgétaires en FCFA.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
          {/* Bouton Simulateur Masse Salariale */}
          {canViewSalaries && (
            <button
              type="button"
              onClick={() => {
                setLatestSimulationResult(null);
                setShowSimulateModal(true);
              }}
              className="btn btn-secondary text-xs flex-1 sm:flex-none inline-flex items-center gap-1.5 min-h-[40px] px-3.5"
              title="Calculer l'impact financier d'un plan d'augmentation"
            >
              <Calculator className="w-4 h-4 text-emerald-700" />
              Simulateur Masse Salariale
            </button>
          )}

          {/* Bouton Proposer une Promotion */}
          {isManagerOrDrh && (
            <button
              type="button"
              onClick={() => setShowProposeModal(true)}
              className="btn btn-primary text-xs flex-1 sm:flex-none inline-flex items-center gap-1.5 min-h-[40px] px-3.5"
              title="Soumettre une proposition d'avancement pour un collaborateur"
            >
              <Plus className="w-4 h-4" />
              Proposer une Promotion
            </button>
          )}

          {/* Bouton Actualiser */}
          <button
            type="button"
            onClick={() => {
              refetchPromotions();
              if (canViewSalaries) refetchSimulations();
            }}
            className="btn btn-secondary text-xs min-h-[40px] px-2.5"
            title="Rafraîchir"
          >
            <RefreshCw className="w-4 h-4 text-gray-600" />
          </button>
        </div>
      </div>

      {/* Alerte Notification */}
      {notificationMsg && (
        <div
          className={`p-3.5 rounded-xl border flex items-center justify-between text-xs animate-fadeIn ${
            notificationMsg.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-[#1A6B3C]'
              : 'bg-red-50 border-red-200 text-red-700'
          }`}
        >
          <div className="flex items-center gap-2">
            {notificationMsg.type === 'success' ? (
              <CheckCircle className="w-4 h-4 shrink-0 text-emerald-600" />
            ) : (
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
            )}
            <span className="font-medium">{notificationMsg.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setNotificationMsg(null)}
            className="text-gray-400 hover:text-gray-600 p-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Bannière de confidentialité pour les salaires */}
      {!canViewSalaries && (
        <div className="p-3 bg-amber-50/80 border border-amber-200 rounded-xl text-xs text-amber-900 flex items-center gap-2">
          <Eye className="w-4 h-4 text-amber-600 shrink-0" />
          <span>
            Les montants nominatifs des salaires sont confidentiels et réservés à la Direction RH et à la Direction Générale.
          </span>
        </div>
      )}

      {/* Grille des propositions de promotion */}
      {isLoadingPromotions ? (
        <div className="card p-12 text-center text-xs text-gray-400 flex flex-col items-center justify-center gap-2">
          <Loader2 className="w-6 h-6 animate-spin text-[#1A6B3C]" />
          <span>Chargement des propositions de promotion...</span>
        </div>
      ) : promotions.length === 0 ? (
        <div className="card p-12 text-center text-xs text-gray-400 space-y-3">
          <TrendingUp className="w-10 h-10 mx-auto text-gray-300" />
          <p className="font-semibold text-gray-600">Aucune proposition de promotion en cours</p>
          <p className="text-gray-400 max-w-sm mx-auto">
            Utilisez le bouton "Proposer une Promotion" pour soumettre un avancement de carrière ou une revalorisation.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {promotions.map((p) => (
            <div key={p.id} className="card p-5 space-y-4 hover:shadow-md transition-shadow border border-gray-200">
              <div className="flex items-start justify-between">
                <div>
                  <span className="font-bold text-sm text-gray-900">{p.employeeFullName}</span>
                  <div className="text-xs text-gray-500 mt-0.5">
                    {p.currentPositionTitle || 'Poste actuel'}
                  </div>
                </div>

                {p.status === 'SUBMITTED' && (
                  <span className="badge badge-warning flex items-center gap-1 text-[11px]">
                    <Clock className="w-3 h-3" />
                    En attente DRH
                  </span>
                )}
                {p.status === 'APPROVED' && (
                  <span className="badge badge-success flex items-center gap-1 text-[11px]">
                    <CheckCircle className="w-3 h-3" />
                    Validé
                  </span>
                )}
                {p.status === 'REJECTED' && (
                  <span className="badge badge-error flex items-center gap-1 text-[11px]">
                    Non retenu
                  </span>
                )}
              </div>

              {/* Comparatif poste & salaire */}
              <div className="bg-gray-50 p-3.5 rounded-xl border border-gray-100 flex items-center justify-between text-xs">
                <div>
                  <div className="text-gray-400 text-[10px] font-semibold uppercase">Poste Cible</div>
                  <div className="font-bold text-gray-800 text-xs mt-0.5">
                    {p.proposedPositionTitle || 'Maintien de poste'}
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-gray-400 shrink-0" />
                <div className="text-right">
                  <div className="text-gray-400 text-[10px] font-semibold uppercase">
                    Nouveau Salaire {p.percentageIncrease ? `(+${p.percentageIncrease}%)` : ''}
                  </div>
                  <div className="font-bold text-[#1A6B3C] text-xs mt-0.5">
                    {canViewSalaries ? formatFCFA(p.proposedSalary) : 'Confidentiel'}
                  </div>
                </div>
              </div>

              {/* Justification */}
              {p.justification && (
                <p className="text-xs text-gray-600 bg-amber-50/50 p-3 rounded-xl border border-amber-100/80 leading-relaxed">
                  <strong className="text-gray-800">Justification : </strong>
                  {p.justification}
                </p>
              )}

              {/* Boutons d'action */}
              <div className="flex items-center justify-between pt-2 border-t border-gray-100 text-xs">
                {/* Bouton Détails Simulation */}
                {canViewSalaries ? (
                  <button
                    type="button"
                    onClick={() => setShowSimulationsListModal(true)}
                    className="btn btn-secondary btn-sm text-[11px] py-1 inline-flex items-center gap-1 text-gray-600"
                  >
                    <Calculator className="w-3.5 h-3.5 text-gray-400" />
                    Détails de simulation
                  </button>
                ) : <span />}

                {/* Bouton Valider la promotion (DRH) */}
                {p.status === 'SUBMITTED' && isDrh && (
                  <button
                    type="button"
                    onClick={() => setApprovalModalProposal(p)}
                    className="btn btn-primary btn-sm text-[11px] py-1 inline-flex items-center gap-1"
                  >
                    <UserCheck className="w-3.5 h-3.5" />
                    Valider la promotion
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ─── MODALE : SIMULATEUR MASSE SALARIALE ─────────────────────────────── */}
      {showSimulateModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-emerald-100 text-[#1A6B3C] flex items-center justify-center font-bold">
                  <Calculator className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-gray-900">Simulateur d'Impact sur la Masse Salariale</h3>
                  <p className="text-[11px] text-gray-500">Calcul financier en FCFA basé sur les contrats actifs</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowSimulateModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                runSimulationMutation.mutate(simulationForm);
              }}
              className="space-y-3.5 text-xs"
            >
              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Intitulé du plan de simulation <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={simulationForm.title}
                  onChange={(e) => setSimulationForm({ ...simulationForm, title: e.target.value })}
                  placeholder="Ex: Revalorisation générale 2026 / Plan Cadres"
                  className="input w-full rounded-lg border-gray-300"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Périmètre / Département</label>
                  <select
                    value={simulationForm.departmentId || ''}
                    onChange={(e) => setSimulationForm({ ...simulationForm, departmentId: e.target.value })}
                    className="input w-full rounded-lg border-gray-300"
                  >
                    <option value="">Toute l'entreprise (Global)</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-gray-700 mb-1">
                    Pourcentage d'augmentation (%) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="100"
                    required
                    value={simulationForm.averageIncreasePercentage}
                    onChange={(e) =>
                      setSimulationForm({
                        ...simulationForm,
                        averageIncreasePercentage: parseFloat(e.target.value) || 0,
                      })
                    }
                    className="input w-full rounded-lg border-gray-300 font-bold text-gray-800"
                  />
                </div>
              </div>

              <div className="flex justify-end pt-1">
                <button
                  type="submit"
                  disabled={runSimulationMutation.isPending}
                  className="btn btn-primary px-4 text-xs inline-flex items-center gap-1.5"
                >
                  {runSimulationMutation.isPending ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <Sparkles className="w-3.5 h-3.5" />
                  )}
                  Lancer la Simulation
                </button>
              </div>
            </form>

            {/* Résultat du calcul */}
            {latestSimulationResult && (
              <div className="mt-4 p-4 rounded-xl bg-emerald-50/70 border border-emerald-200 space-y-3 animate-fadeIn text-xs">
                <div className="flex items-center justify-between border-b border-emerald-200 pb-2">
                  <span className="font-bold text-sm text-[#1A6B3C] flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4" />
                    Impact Budgétaire Projeté
                  </span>
                  <span className="badge badge-success text-[10px]">
                    {latestSimulationResult.affectedEmployeesCount} collaborateur(s)
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-white p-3 rounded-lg border border-emerald-100">
                    <span className="text-[10px] text-gray-400 block uppercase">Masse Salariale Mensuelle Actuelle</span>
                    <strong className="text-sm text-gray-900 block mt-0.5">
                      {formatFCFA(latestSimulationResult.currentMonthlyPayroll)}
                    </strong>
                  </div>

                  <div className="bg-white p-3 rounded-lg border border-emerald-100">
                    <span className="text-[10px] text-gray-400 block uppercase">Masse Salariale Mensuelle Projetée</span>
                    <strong className="text-sm text-[#1A6B3C] block mt-0.5">
                      {formatFCFA(latestSimulationResult.projectedMonthlyPayroll)}
                    </strong>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-white p-3 rounded-lg border border-emerald-100">
                    <span className="text-[10px] text-gray-400 block uppercase">Surcoût Mensuel Additionnel</span>
                    <strong className="text-sm text-amber-700 block mt-0.5">
                      +{formatFCFA(latestSimulationResult.monthlyDifference)}
                    </strong>
                  </div>

                  <div className="bg-white p-3 rounded-lg border border-emerald-100">
                    <span className="text-[10px] text-gray-400 block uppercase">Impact Budgétaire Annuel</span>
                    <strong className="text-sm text-emerald-800 block mt-0.5">
                      +{formatFCFA(latestSimulationResult.projectedAnnualPayrollImpact)}
                    </strong>
                  </div>
                </div>
              </div>
            )}

            <div className="flex justify-end pt-3 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowSimulateModal(false)}
                className="btn btn-secondary px-4 text-xs"
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── MODALE : PROPOSER UNE PROMOTION ─────────────────────────────────── */}
      {showProposeModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-emerald-100 text-[#1A6B3C] flex items-center justify-center font-bold">
                  <Plus className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-gray-900">Proposer une Promotion / Revalorisation</h3>
                  <p className="text-[11px] text-gray-500">Soumission pour validation par la Direction RH</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowProposeModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                createProposalMutation.mutate(proposalForm);
              }}
              className="space-y-3.5 text-xs"
            >
              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Collaborateur concerné <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={proposalForm.employeeId}
                  onChange={(e) => setProposalForm({ ...proposalForm, employeeId: e.target.value })}
                  className="input w-full rounded-lg border-gray-300"
                >
                  <option value="">Sélectionner un collaborateur...</option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.fullName} — {emp.positionTitle || 'Poste non défini'} ({emp.employeeNumber})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Nouveau Poste Cible <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={proposalForm.proposedPositionId}
                  onChange={(e) => setProposalForm({ ...proposalForm, proposedPositionId: e.target.value })}
                  className="input w-full rounded-lg border-gray-300"
                >
                  <option value="">Sélectionner le poste cible...</option>
                  {positions.map((pos) => (
                    <option key={pos.id} value={pos.id}>
                      {pos.title}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Nouveau Salaire Brut Proposé (FCFA) <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  min={1}
                  value={proposalForm.proposedSalary}
                  onChange={(e) =>
                    setProposalForm({
                      ...proposalForm,
                      proposedSalary: parseFloat(e.target.value) || '',
                    })
                  }
                  placeholder="Ex: 1500000"
                  className="input w-full rounded-lg border-gray-300 font-bold text-[#1A6B3C]"
                />
              </div>

              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Justification managériale <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={3}
                  required
                  value={proposalForm.justification}
                  onChange={(e) => setProposalForm({ ...proposalForm, justification: e.target.value })}
                  placeholder="Arguments, accomplissements récents, prise de responsabilités..."
                  className="input w-full rounded-lg border-gray-300 py-2"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowProposeModal(false)}
                  className="btn btn-secondary px-4 text-xs"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={createProposalMutation.isPending}
                  className="btn btn-primary px-4 text-xs inline-flex items-center gap-1.5"
                >
                  {createProposalMutation.isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  Soumettre la Proposition
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ─── MODALE : CONFIRMATION VALIDATION PROMOTION (DRH) ────────────────── */}
      {approvalModalProposal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-emerald-100 text-[#1A6B3C] flex items-center justify-center font-bold">
                  <UserCheck className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-gray-900">Valider la Promotion</h3>
                  <p className="text-[11px] text-gray-500">{approvalModalProposal.employeeFullName}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setApprovalModalProposal(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl space-y-2 text-xs border border-gray-100">
              <div className="flex justify-between">
                <span className="text-gray-500">Poste Cible :</span>
                <span className="font-bold text-gray-900">{approvalModalProposal.proposedPositionTitle}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Nouveau Salaire :</span>
                <span className="font-bold text-[#1A6B3C]">
                  {formatFCFA(approvalModalProposal.proposedSalary)}
                </span>
              </div>
            </div>

            <div className="text-xs">
              <label className="block font-semibold text-gray-700 mb-1">Commentaire d'approbation DRH (optionnel)</label>
              <textarea
                rows={2}
                value={approvalComment}
                onChange={(e) => setApprovalComment(e.target.value)}
                placeholder="Ex: Avis favorable Direction générale - prise d'effet immédiate"
                className="input w-full rounded-lg border-gray-300 py-1.5"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-gray-100 text-xs">
              <button
                type="button"
                onClick={() => setApprovalModalProposal(null)}
                className="btn btn-secondary px-3"
              >
                Annuler
              </button>
              <button
                type="button"
                disabled={validatePromotionMutation.isPending}
                onClick={() =>
                  validatePromotionMutation.mutate({
                    id: approvalModalProposal.id!,
                    status: 'REJECTED',
                    comment: approvalComment,
                  })
                }
                className="btn btn-secondary px-3 text-red-600 hover:bg-red-50 inline-flex items-center gap-1.5"
              >
                Refuser
              </button>
              <button
                type="button"
                disabled={validatePromotionMutation.isPending}
                onClick={() =>
                  validatePromotionMutation.mutate({
                    id: approvalModalProposal.id!,
                    status: 'APPROVED',
                    comment: approvalComment,
                  })
                }
                className="btn btn-primary px-3 inline-flex items-center gap-1.5"
              >
                {validatePromotionMutation.isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Confirmer l'Approbation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── MODALE : DÉTAILS DE SIMULATION (HISTORIQUE) ────────────────────── */}
      {showSimulationsListModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-4 max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-blue-100 text-blue-800 flex items-center justify-center font-bold">
                  <Calculator className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-gray-900">Historique des Simulations de Masse Salariale</h3>
                  <p className="text-[11px] text-gray-500">Traçabilité des études budgétaires réalisées</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowSimulationsListModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 flex-1 overflow-y-auto pr-1 text-xs">
              {simulations.length === 0 ? (
                <div className="text-center py-10 text-gray-400">
                  Aucune simulation salariale n'a encore été enregistrée.
                </div>
              ) : (
                simulations.map((sim) => (
                  <div
                    key={sim.id}
                    className="p-3.5 rounded-xl border border-gray-200 bg-gray-50/70 hover:bg-white space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <strong className="text-gray-900 text-xs block">{sim.title}</strong>
                        <span className="text-[10px] text-gray-500">
                          {sim.departmentName} • {sim.createdByName || 'Direction RH'}
                        </span>
                      </div>
                      <span className="badge badge-success text-[10px]">+{sim.averageIncreasePercentage}%</span>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-[11px] pt-1 border-t border-gray-100">
                      <div>
                        <span className="text-gray-400 text-[10px] block">Impact mensuel :</span>
                        <strong className="text-amber-800">{formatFCFA(sim.monthlyDifference)}</strong>
                      </div>
                      <div>
                        <span className="text-gray-400 text-[10px] block">Impact annuel estimé :</span>
                        <strong className="text-[#1A6B3C]">{formatFCFA(sim.projectedAnnualPayrollImpact)}</strong>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>

            <div className="flex justify-end pt-3 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowSimulationsListModal(false)}
                className="btn btn-secondary px-4 text-xs"
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PromotionListPage;
