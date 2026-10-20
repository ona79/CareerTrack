import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import {
  Calendar,
  Plus,
  Clock,
  CheckCircle2,
  XCircle,
  Loader2,
  AlertTriangle,
  CalendarDays,
  Users,
  Check,
  X,
  Building2,
  RefreshCw,
  Search,
  Filter
} from 'lucide-react';
import type {
  LeaveRequestResponseDto,
  LeaveBalanceResponseDto,
  TeamCalendarItemDto,
  TeamLeaveConflictDto,
  WorkingDaysCalculationDto,
  DepartmentDto,
  ApiResponse
} from '@/types';

const DEFAULT_LEAVE_TYPES = [
  { id: '77777777-7777-7777-7777-777777777701', code: 'CONGE_PAYE', name: 'Congé Payé Annuel (2j/mois)' },
  { id: '77777777-7777-7777-7777-777777777704', code: 'EVENEMENT_FAMILIAL', name: 'Permission Exceptionnelle (Événements familiaux)' },
  { id: '77777777-7777-7777-7777-777777777703', code: 'CONGE_MALADIE', name: 'Congé Maladie' },
  { id: '77777777-7777-7777-7777-777777777702', code: 'CONGE_MATERNITE', name: 'Congé Maternité' },
  { id: '77777777-7777-7777-7777-777777777705', code: 'CONGE_SANS_SOLDE', name: 'Congé Sans Solde' },
];

export const LeaveListPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();

  const isManagerOrDrh = hasRole('MANAGER') || hasRole('DRH') || hasRole('DIRECTION') || hasRole('ADMIN');
  const isDrh = hasRole('DRH') || hasRole('DIRECTION') || hasRole('ADMIN');

  // Onglets actifs : 'MY_LEAVES' | 'PENDING_APPROVALS' | 'TEAM_CALENDAR'
  const [activeTab, setActiveTab] = useState<'MY_LEAVES' | 'PENDING_APPROVALS' | 'TEAM_CALENDAR'>('MY_LEAVES');

  // Modales
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [approvalModalData, setApprovalModalData] = useState<{ request: LeaveRequestResponseDto; action: 'APPROVE' | 'REJECT' } | null>(null);
  const [managerComment, setManagerComment] = useState('');
  const [selectedTargetEmployeeId, setSelectedTargetEmployeeId] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 0. Types de congés dynamiques
  const { data: serverLeaveTypes } = useQuery({
    queryKey: ['leave-types'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Array<{ id: string; code: string; name: string }>>>('/leaves/types');
      return res.data?.data || [];
    },
  });
  const availableLeaveTypes = (serverLeaveTypes && serverLeaveTypes.length > 0) ? serverLeaveTypes : DEFAULT_LEAVE_TYPES;

  // Formulaire de demande
  const [leaveTypeId, setLeaveTypeId] = useState(DEFAULT_LEAVE_TYPES[0].id);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');

  // Filtres calendrier
  const [selectedDeptId, setSelectedDeptId] = useState<string>('');
  const [calendarMonth, setCalendarMonth] = useState<string>('2026-06');

  // 1. Charger les départements pour le filtre calendrier
  const { data: departments = [] } = useQuery({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<DepartmentDto[]>>('/departments');
      return res.data?.data || [];
    },
  });

  // Liste des employés si l'utilisateur est DRH ou n'a pas d'employeeId lié
  const { data: employeeList = [] } = useQuery({
    queryKey: ['employees-leave-list'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<any>>('/employees?size=100');
      return res.data?.data?.content || [];
    },
    enabled: isDrh || !user?.employeeId,
  });

  const effectiveEmployeeId = user?.employeeId || selectedTargetEmployeeId || (employeeList.length > 0 ? employeeList[0].id : '');

  // 2. Charger les soldes de congés de l'utilisateur connecté
  const { data: balances = [], isLoading: isLoadingBalances } = useQuery({
    queryKey: ['leave-balances', effectiveEmployeeId],
    queryFn: async () => {
      if (!effectiveEmployeeId) return [];
      const res = await api.get<ApiResponse<LeaveBalanceResponseDto[]>>(`/leaves/balances/${effectiveEmployeeId}?year=2026`);
      return res.data?.data || [];
    },
    enabled: !!effectiveEmployeeId,
  });

  // 3. Charger les demandes personnelles
  const { data: myRequests = [], isLoading: isLoadingMyRequests } = useQuery({
    queryKey: ['my-leave-requests', effectiveEmployeeId],
    queryFn: async () => {
      if (!effectiveEmployeeId) return [];
      const res = await api.get<ApiResponse<LeaveRequestResponseDto[]>>(`/leaves/employee/${effectiveEmployeeId}`);
      return res.data?.data || [];
    },
    enabled: !!effectiveEmployeeId,
  });

  // 4. Charger les demandes en attente pour le manager ou le DRH
  const { data: pendingRequests = [], isLoading: isLoadingPending } = useQuery({
    queryKey: ['pending-leave-requests', effectiveEmployeeId],
    queryFn: async () => {
      if (!isManagerOrDrh) return [];
      const res = await api.get<ApiResponse<LeaveRequestResponseDto[]>>('/leaves/pending');
      return res.data?.data || [];
    },
    enabled: isManagerOrDrh,
  });

  // 5. Calcul automatique des jours ouvrables lors du choix des dates
  const { data: workingDaysCalc } = useQuery({
    queryKey: ['working-days', startDate, endDate],
    queryFn: async () => {
      if (!startDate || !endDate || endDate < startDate) return null;
      const res = await api.get<ApiResponse<WorkingDaysCalculationDto>>(`/leaves/calculate-days?startDate=${startDate}&endDate=${endDate}`);
      return res.data?.data || null;
    },
    enabled: !!startDate && !!endDate && endDate >= startDate,
  });

  // 6. Vérification des conflits dans le formulaire
  const { data: formConflict } = useQuery({
    queryKey: ['form-conflict', selectedDeptId, startDate, endDate],
    queryFn: async () => {
      if (!startDate || !endDate || endDate < startDate) return null;
      const dept = selectedDeptId || (departments.length > 0 ? departments[0].id : '');
      if (!dept) return null;
      const res = await api.get<ApiResponse<TeamLeaveConflictDto>>(`/leaves/conflicts?departmentId=${dept}&startDate=${startDate}&endDate=${endDate}`);
      return res.data?.data || null;
    },
    enabled: !!startDate && !!endDate && endDate >= startDate,
  });

  // 7. Calendrier d'équipe
  const calendarStartDate = `${calendarMonth}-01`;
  const calendarEndDate = `${calendarMonth}-28`; // approximation fin de mois
  const { data: calendarItems = [], isLoading: isLoadingCalendar } = useQuery({
    queryKey: ['team-calendar', selectedDeptId, calendarMonth],
    queryFn: async () => {
      const url = selectedDeptId
        ? `/leaves/calendar?departmentId=${selectedDeptId}&startDate=${calendarStartDate}&endDate=${calendarEndDate}`
        : `/leaves/calendar?startDate=${calendarStartDate}&endDate=${calendarEndDate}`;
      const res = await api.get<ApiResponse<TeamCalendarItemDto[]>>(url);
      return res.data?.data || [];
    },
  });

  // Mutations
  const createMutation = useMutation({
    mutationFn: async () => {
      const targetEmpId = effectiveEmployeeId;
      if (!targetEmpId) throw new Error('Aucun collaborateur identifié pour poser cette demande');
      const res = await api.post<ApiResponse<LeaveRequestResponseDto>>('/leaves', {
        employeeId: targetEmpId,
        leaveTypeId,
        startDate,
        endDate,
        requestedDays: workingDaysCalc?.workingDays || 1,
        reason,
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-leave-requests'] });
      queryClient.invalidateQueries({ queryKey: ['leave-balances'] });
      queryClient.invalidateQueries({ queryKey: ['team-calendar'] });
      queryClient.invalidateQueries({ queryKey: ['pending-leave-requests'] });
      setShowCreateModal(false);
      setStartDate('');
      setEndDate('');
      setReason('');
      setErrorMessage(null);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Erreur lors de la soumission de la demande';
      setErrorMessage(msg);
    },
  });

  const approvalMutation = useMutation({
    mutationFn: async ({ requestId, status, comment }: { requestId: string; status: 'APPROVED_N1' | 'REJECTED'; comment: string }) => {
      const res = await api.put<ApiResponse<LeaveRequestResponseDto>>(`/leaves/${requestId}/validate`, {
        status,
        managerComment: comment,
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-leave-requests'] });
      queryClient.invalidateQueries({ queryKey: ['team-calendar'] });
      setApprovalModalData(null);
      setManagerComment('');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: async (requestId: string) => {
      const res = await api.patch<ApiResponse<LeaveRequestResponseDto>>(`/leaves/${requestId}/cancel`);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-leave-requests'] });
      queryClient.invalidateQueries({ queryKey: ['leave-balances'] });
      queryClient.invalidateQueries({ queryKey: ['team-calendar'] });
    },
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUBMITTED':
      case 'EN_ATTENTE':
      case 'PENDING':
        return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800"><Clock className="w-3 h-3" /> En attente</span>;
      case 'APPROVED_N1':
      case 'APPROVED_DRH':
      case 'VALIDE':
      case 'APPROVED':
        return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800"><CheckCircle2 className="w-3 h-3" /> Validé</span>;
      case 'REJECTED':
      case 'REFUSE':
        return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800"><XCircle className="w-3 h-3" /> Refusé</span>;
      case 'CANCELLED':
      case 'ANNULE':
        return <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700">Annulé</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Congés & Absences</h1>
          <p className="text-sm text-gray-500 mt-1">
            Gérez vos demandes de congés, suivez vos soldes et consultez le calendrier d'équipe
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg text-sm font-semibold text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm transition-colors min-h-[44px]"
        >
          <Plus className="w-4 h-4" />
          Poser un congé
        </button>
      </div>

      {/* Cartes de soldes */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {balances.map((bal) => (
          <div key={bal.id} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">
                {bal.leaveTypeName}
              </span>
              <span className="text-xs font-medium text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                Année {bal.periodYear}
              </span>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-gray-900">{bal.remainingDays}</span>
              <span className="text-sm font-medium text-gray-500">jours restants</span>
            </div>
            <div className="mt-3 pt-3 border-t border-gray-100 grid grid-cols-2 text-xs text-gray-500">
              <div>Acquis : <span className="font-semibold text-gray-800">{bal.allocatedDays} j</span></div>
              <div>Consommés : <span className="font-semibold text-gray-800">{bal.usedDays} j</span></div>
            </div>
          </div>
        ))}
      </div>

      {/* Navigation par onglets */}
      <div className="border-b border-gray-200 flex gap-4 overflow-x-auto">
        <button
          onClick={() => setActiveTab('MY_LEAVES')}
          className={`pb-3 px-1 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 min-h-[44px] ${
            activeTab === 'MY_LEAVES' ? 'border-emerald-600 text-emerald-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Calendar className="w-4 h-4" />
          Mes Demandes ({myRequests.length})
        </button>

        {isManagerOrDrh && (
          <button
            onClick={() => setActiveTab('PENDING_APPROVALS')}
            className={`pb-3 px-1 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 min-h-[44px] ${
              activeTab === 'PENDING_APPROVALS' ? 'border-emerald-600 text-emerald-600' : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Clock className="w-4 h-4" />
            À Valider
            {pendingRequests.length > 0 && (
              <span className="bg-amber-100 text-amber-800 text-xs px-2 py-0.5 rounded-full font-bold">
                {pendingRequests.length}
              </span>
            )}
          </button>
        )}

        <button
          onClick={() => setActiveTab('TEAM_CALENDAR')}
          className={`pb-3 px-1 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 min-h-[44px] ${
            activeTab === 'TEAM_CALENDAR' ? 'border-emerald-600 text-emerald-600' : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <CalendarDays className="w-4 h-4" />
          Calendrier d'Équipe
        </button>
      </div>

      {/* Contenu Onglet 1 : Mes Demandes */}
      {activeTab === 'MY_LEAVES' && (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          {isLoadingMyRequests ? (
            <div className="p-8 text-center text-gray-500 flex items-center justify-center gap-2">
              <Loader2 className="w-5 h-5 animate-spin" /> Chargement de vos demandes...
            </div>
          ) : myRequests.length === 0 ? (
            <div className="p-12 text-center text-gray-400">
              <Calendar className="w-12 h-12 mx-auto mb-3 text-gray-300" />
              <p className="font-medium text-gray-600">Aucune demande de congé enregistrée</p>
              <p className="text-xs text-gray-400 mt-1">Cliquez sur « Poser un congé » pour formuler votre première demande</p>
            </div>
          ) : (
            <>
              {/* Tableau desktop */}
              <div className="hidden sm:block overflow-x-auto">
                <table className="w-full text-left text-sm text-gray-700">
                  <thead className="bg-gray-50 text-xs uppercase text-gray-400 border-b border-gray-100">
                    <tr>
                      <th className="py-3.5 px-4 font-semibold">Type de congé</th>
                      <th className="py-3.5 px-4 font-semibold">Dates</th>
                      <th className="py-3.5 px-4 font-semibold">Jours ouvrés</th>
                      <th className="py-3.5 px-4 font-semibold">Motif</th>
                      <th className="py-3.5 px-4 font-semibold">Statut</th>
                      <th className="py-3.5 px-4 font-semibold text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {myRequests.map((req) => (
                      <tr key={req.id} className="hover:bg-gray-50 transition-colors">
                        <td className="py-3.5 px-4 font-medium text-gray-900">{req.leaveTypeName}</td>
                        <td className="py-3.5 px-4 text-xs text-gray-600">
                          Du <span className="font-semibold">{req.startDate}</span> au <span className="font-semibold">{req.endDate}</span>
                        </td>
                        <td className="py-3.5 px-4 font-bold text-gray-900">{req.requestedDays} j</td>
                        <td className="py-3.5 px-4 text-xs text-gray-500 max-w-xs truncate">{req.reason || '—'}</td>
                        <td className="py-3.5 px-4">{getStatusBadge(req.status)}</td>
                        <td className="py-3.5 px-4 text-right">
                          {(req.status === 'SUBMITTED' || req.status === 'EN_ATTENTE' || req.status === 'PENDING') && (
                            <button
                              onClick={() => cancelMutation.mutate(req.id)}
                              className="text-xs text-red-600 hover:text-red-800 font-semibold px-2 py-1 rounded hover:bg-red-50 transition-colors"
                            >
                              Annuler
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Cartes mobile (<640px) */}
              <div className="sm:hidden divide-y divide-gray-100">
                {myRequests.map((req) => (
                  <div key={req.id} className="p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-gray-900 text-sm">{req.leaveTypeName}</span>
                      {getStatusBadge(req.status)}
                    </div>
                    <div className="text-xs text-gray-600">
                      Du <span className="font-medium text-gray-900">{req.startDate}</span> au <span className="font-medium text-gray-900">{req.endDate}</span>
                      <span className="ml-2 font-bold text-gray-900">({req.requestedDays} j)</span>
                    </div>
                    {req.reason && <p className="text-xs text-gray-500 bg-gray-50 p-2 rounded">{req.reason}</p>}
                    {(req.status === 'SUBMITTED' || req.status === 'EN_ATTENTE' || req.status === 'PENDING') && (
                      <button
                        onClick={() => cancelMutation.mutate(req.id)}
                        className="w-full text-xs text-red-600 font-semibold py-2 rounded border border-red-200 hover:bg-red-50 text-center min-h-[44px]"
                      >
                        Annuler la demande
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}

      {/* Contenu Onglet 2 : Validation N+1 */}
      {activeTab === 'PENDING_APPROVALS' && isManagerOrDrh && (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          {isLoadingPending ? (
            <div className="p-8 text-center text-gray-500 flex items-center justify-center gap-2">
              <Loader2 className="w-5 h-5 animate-spin" /> Chargement des demandes en attente...
            </div>
          ) : pendingRequests.length === 0 ? (
            <div className="p-12 text-center text-gray-400">
              <CheckCircle2 className="w-12 h-12 mx-auto mb-3 text-emerald-400" />
              <p className="font-medium text-gray-600">Toutes les demandes de votre équipe ont été traitées !</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {pendingRequests.map((req) => (
                <div key={req.id} className="p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-gray-900 text-sm">{req.employeeName}</span>
                      <span className="text-xs text-gray-400">({req.employeeNumber})</span>
                      {req.departmentName && (
                        <span className="text-xs bg-gray-100 text-gray-700 px-2 py-0.5 rounded font-medium">
                          {req.departmentName}
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-gray-600">
                      Demande de <span className="font-semibold text-gray-900">{req.leaveTypeName}</span> : du{' '}
                      <span className="font-semibold text-gray-900">{req.startDate}</span> au{' '}
                      <span className="font-semibold text-gray-900">{req.endDate}</span> ({req.requestedDays} jours ouvrables)
                    </div>
                    {req.reason && (
                      <p className="text-xs text-gray-500 italic bg-gray-50 p-2 rounded mt-1">« {req.reason} »</p>
                    )}
                  </div>
                  <div className="flex items-center gap-2 self-end sm:self-center">
                    <button
                      onClick={() => setApprovalModalData({ request: req, action: 'APPROVE' })}
                      className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 min-h-[44px]"
                    >
                      <Check className="w-3.5 h-3.5" /> Valider
                    </button>
                    <button
                      onClick={() => setApprovalModalData({ request: req, action: 'REJECT' })}
                      className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold text-red-700 bg-red-50 hover:bg-red-100 border border-red-200 min-h-[44px]"
                    >
                      <X className="w-3.5 h-3.5" /> Refuser
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Contenu Onglet 3 : Calendrier d'Équipe & Conflits */}
      {activeTab === 'TEAM_CALENDAR' && (
        <div className="space-y-4">
          {/* Barre de filtre calendrier */}
          <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col sm:flex-row gap-4 items-center justify-between">
            <div className="flex items-center gap-3 w-full sm:w-auto">
              <Building2 className="w-4 h-4 text-gray-400" />
              <select
                value={selectedDeptId}
                onChange={(e) => setSelectedDeptId(e.target.value)}
                className="text-xs border border-gray-300 rounded-lg p-2 focus:ring-emerald-500 focus:border-emerald-500 w-full sm:w-64"
              >
                <option value="">Tous les départements</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <Calendar className="w-4 h-4 text-gray-400" />
              <input
                type="month"
                value={calendarMonth}
                onChange={(e) => setCalendarMonth(e.target.value)}
                className="text-xs border border-gray-300 rounded-lg p-2 focus:ring-emerald-500 focus:border-emerald-500"
              />
            </div>
          </div>

          {/* Liste des absences de l'équipe */}
          <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
            {isLoadingCalendar ? (
              <div className="p-8 text-center text-gray-500 flex items-center justify-center gap-2">
                <Loader2 className="w-5 h-5 animate-spin" /> Chargement du planning d'équipe...
              </div>
            ) : calendarItems.length === 0 ? (
              <div className="p-12 text-center text-gray-400">
                <Users className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                <p className="font-medium text-gray-600">Aucune absence enregistrée sur cette période</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-100">
                {calendarItems.map((item) => (
                  <div
                    key={item.requestId}
                    className={`p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 ${
                      item.conflicting ? 'bg-red-50/50 border-l-4 border-l-red-500' : ''
                    }`}
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-gray-900 text-sm">{item.employeeName}</span>
                        {item.departmentName && (
                          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                            {item.departmentName}
                          </span>
                        )}
                        {item.conflicting && (
                          <span className="inline-flex items-center gap-1 text-xs font-bold text-red-700 bg-red-100 px-2 py-0.5 rounded-full">
                            <AlertTriangle className="w-3 h-3" /> Conflit d'équipe (&ge; 2 absents)
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-gray-600 mt-1">
                        {item.leaveTypeName} • Du <span className="font-medium text-gray-800">{item.startDate}</span> au{' '}
                        <span className="font-medium text-gray-800">{item.endDate}</span> ({item.requestedDays} j)
                      </div>
                    </div>
                    <div>{getStatusBadge(item.status)}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODALE : Poser un congé */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-hidden">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-lg w-full shadow-2xl flex flex-col max-h-[92vh] sm:max-h-[90vh]">
            <div className="flex items-center justify-between border-b border-gray-100 p-4 sm:p-5 shrink-0">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-100 text-[#1A6B3C] flex items-center justify-center font-bold">
                  <Calendar className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Demande de Congé</h3>
                  <p className="text-xs text-gray-500">Posez vos jours de repos ou absences autorisées</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={(e) => { e.preventDefault(); createMutation.mutate(); }} className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-4">
              {(isDrh || !user?.employeeId) && employeeList.length > 0 && (
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Collaborateur concerné <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={effectiveEmployeeId}
                    onChange={(e) => setSelectedTargetEmployeeId(e.target.value)}
                    className="input w-full text-sm"
                  >
                    {employeeList.map((emp: any) => (
                      <option key={emp.id} value={emp.id}>
                        {emp.fullName} ({emp.employeeNumber}) — {emp.departmentName || 'Département non assigné'}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Type de congé</label>
                <select
                  value={leaveTypeId}
                  onChange={(e) => setLeaveTypeId(e.target.value)}
                  className="input w-full text-sm"
                >
                  {availableLeaveTypes.map((lt: any) => (
                    <option key={lt.id} value={lt.id}>{lt.name}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Date de début</label>
                  <input
                    type="date"
                    required
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="input w-full text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Date de fin</label>
                  <input
                    type="date"
                    required
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    className="input w-full text-sm"
                  />
                </div>
              </div>

              {/* Indicateur de calcul automatique des jours ouvrés */}
              {workingDaysCalc && (
                <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800">
                  <div className="font-semibold flex items-center gap-1.5 text-sm">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    {workingDaysCalc.workingDays} jour(s) ouvrable(s) décompté(s)
                  </div>
                  <div className="text-xs text-emerald-600 mt-1">
                    Lundi au vendredi inclus, hors samedis et dimanches
                  </div>
                </div>
              )}

              {/* Alerte de conflit d'équipe en direct */}
              {formConflict?.hasConflict && (
                <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 flex items-start gap-2">
                  <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-semibold">Alerte de chevauchement d'équipe :</span> {formConflict.overlappingRequestsCount} collègue(s) de votre département ont déjà posé un congé sur ces dates.
                  </div>
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Motif (facultatif)</label>
                <textarea
                  rows={3}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Ex : Repos familial, convenances personnelles..."
                  className="input w-full text-sm"
                />
              </div>

              {(errorMessage || createMutation.isError) && (
                <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-start gap-2">
                  <AlertTriangle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                  <span>{errorMessage || (createMutation.error as any)?.response?.data?.message || 'Erreur lors de la soumission de la demande'}</span>
                </div>
              )}

              <div className="flex gap-2.5 justify-end pt-4 border-t border-gray-100 shrink-0">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn btn-secondary px-5 text-sm min-h-[44px]"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending || !startDate || !endDate}
                  className="btn btn-primary px-5 text-sm flex items-center gap-2 min-h-[44px]"
                >
                  {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Soumettre la demande
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODALE : Validation / Refus Manager */}
      {approvalModalData && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-hidden">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-md w-full shadow-2xl flex flex-col max-h-[92vh] sm:max-h-[90vh]">
            <div className="flex items-center justify-between border-b border-gray-100 p-4 sm:p-5 shrink-0">
              <h3 className="text-base font-bold text-gray-900">
                {approvalModalData.action === 'APPROVE' ? 'Valider la demande de congé' : 'Refuser la demande de congé'}
              </h3>
              <button
                type="button"
                onClick={() => setApprovalModalData(null)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-4">
              <p className="text-sm text-gray-600">
                Demande de <span className="font-semibold text-gray-900">{approvalModalData.request.employeeName}</span> du{' '}
                <span className="font-semibold text-gray-900">{approvalModalData.request.startDate}</span> au{' '}
                <span className="font-semibold text-gray-900">{approvalModalData.request.endDate}</span> ({approvalModalData.request.requestedDays} j).
              </p>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Commentaire du manager {approvalModalData.action === 'REJECT' && <span className="text-red-500">*</span>}
                </label>
                <textarea
                  rows={3}
                  value={managerComment}
                  onChange={(e) => setManagerComment(e.target.value)}
                  placeholder="Ex : Accordé / Période de forte activité nécessitant un report..."
                  className="input w-full text-sm"
                />
              </div>
            </div>

            <div className="flex gap-2.5 justify-end p-4 border-t border-gray-100 shrink-0">
              <button
                type="button"
                onClick={() => setApprovalModalData(null)}
                className="btn btn-secondary px-5 text-sm min-h-[44px]"
              >
                Annuler
              </button>
              <button
                type="button"
                disabled={approvalMutation.isPending}
                onClick={() => approvalMutation.mutate({
                  requestId: approvalModalData.request.id,
                  status: approvalModalData.action === 'APPROVE' ? 'APPROVED_N1' : 'REJECTED',
                  comment: managerComment,
                })}
                className={`btn text-sm text-white flex items-center gap-2 min-h-[44px] px-5 ${
                  approvalModalData.action === 'APPROVE' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-red-600 hover:bg-red-700'
                }`}
              >
                {approvalMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Confirmer la décision
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default LeaveListPage;

