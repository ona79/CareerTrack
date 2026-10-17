import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import {
  Calendar,
  Plus,
  Clock,
  CheckCircle,
  XCircle,
  Loader2,
  FileText,
  AlertTriangle,
  RefreshCw,
  CalendarDays,
  ListFilter,
  Users
} from 'lucide-react';
import type { LeaveRequest, LeaveBalance, ApiResponse } from '@/types';

const defaultBalances: LeaveBalance[] = [
  {
    id: 'b1',
    employeeId: 'e1',
    leaveTypeId: 'lt1',
    leaveTypeName: 'Congé Payé Annuel (2j/mois)',
    year: 2026,
    totalDays: 24,
    usedDays: 6,
    remainingDays: 18,
  },
  {
    id: 'b2',
    employeeId: 'e1',
    leaveTypeId: 'lt2',
    leaveTypeName: 'Permissions Exceptionnelles',
    year: 2026,
    totalDays: 10,
    usedDays: 2,
    remainingDays: 8,
  },
  {
    id: 'b3',
    employeeId: 'e1',
    leaveTypeId: 'lt3',
    leaveTypeName: 'Congé Maladie',
    year: 2026,
    totalDays: 30,
    usedDays: 0,
    remainingDays: 30,
  },
];

const defaultRequests: LeaveRequest[] = [
  {
    id: 'lr-1',
    employee: { id: 'e1', fullName: 'Amadou Diop', employeeNumber: 'EMP-001' },
    leaveType: {
      id: 'lt1',
      code: 'CONGE_PAYE',
      name: 'Congé Payé Annuel',
      maxDaysPerYear: 24,
      isPaid: true,
      requiresJustification: false,
    },
    startDate: '2026-04-10',
    endDate: '2026-04-17',
    totalDays: 5,
    status: 'PENDING',
    reason: 'Congés annuels de Pâques en famille à Thiès',
    createdAt: '2026-03-20',
  },
  {
    id: 'lr-2',
    employee: { id: 'e2', fullName: 'Fatou Ndiaye', employeeNumber: 'EMP-002' },
    leaveType: {
      id: 'lt4',
      code: 'EVENEMENT_FAMILIAL',
      name: 'Permissions Exceptionnelles',
      maxDaysPerYear: 10,
      isPaid: true,
      requiresJustification: true,
    },
    startDate: '2026-04-12',
    endDate: '2026-04-15',
    totalDays: 3,
    status: 'APPROVED',
    reason: 'Mariage familial à Saint-Louis',
    approvedBy: { id: 'm1', fullName: 'Directeur Général' },
    approvedAt: '2026-02-10',
    createdAt: '2026-02-08',
  },
  {
    id: 'lr-3',
    employee: { id: 'e4', fullName: 'Aïssatou Sow', employeeNumber: 'EMP-004' },
    leaveType: {
      id: 'lt1',
      code: 'CONGE_PAYE',
      name: 'Congé Payé Annuel',
      maxDaysPerYear: 24,
      isPaid: true,
      requiresJustification: false,
    },
    startDate: '2026-05-02',
    endDate: '2026-05-08',
    totalDays: 5,
    status: 'APPROVED',
    reason: 'Repos annuel',
    createdAt: '2026-04-01',
  },
];

export const LeaveListPage: React.FC = () => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [viewMode, setViewMode] = useState<'LIST' | 'CALENDAR'>('LIST');

  // Formulaire d'ajout
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');

  // 1. Récupération des demandes depuis l'API backend
  const { data: requestsData, isLoading: isLoadingRequests, isError: isErrorRequests, refetch } = useQuery({
    queryKey: ['leaves'],
    queryFn: async () => {
      const res = await api.get<{ data?: LeaveRequest[] }>('/leaves');
      return Array.isArray(res.data?.data) ? res.data.data : [];
    },
    placeholderData: import.meta.env.DEV ? defaultRequests : undefined,
  });

  const requests: LeaveRequest[] = requestsData || (import.meta.env.DEV ? defaultRequests : []);

  // Détection des conflits de congés (chevauchements de dates)
  const conflicts: { reqA: LeaveRequest; reqB: LeaveRequest }[] = [];
  for (let i = 0; i < requests.length; i++) {
    for (let j = i + 1; j < requests.length; j++) {
      const a = requests[i];
      const b = requests[j];
      if (a.status !== 'REJECTED' && b.status !== 'REJECTED') {
        // Conflit si startA <= endB et endA >= startB
        if (a.startDate <= b.endDate && a.endDate >= b.startDate) {
          conflicts.push({ reqA: a, reqB: b });
        }
      }
    }
  }

  // Vérification de conflit potentiel pour le formulaire courant
  const potentialFormConflict = startDate && endDate
    ? requests.find((r) => r.status !== 'REJECTED' && startDate <= r.endDate && endDate >= r.startDate)
    : null;

  // 2. Mutation pour soumettre une nouvelle demande
  const createMutation = useMutation({
    mutationFn: async (newReqPayload: { startDate: string; endDate: string; reason: string }) => {
      const res = await api.post<ApiResponse<LeaveRequest>>('/leaves', {
        employeeId: user?.employeeId || '11111111-1111-1111-1111-111111111103',
        leaveTypeId: '77777777-7777-7777-7777-777777777701',
        startDate: newReqPayload.startDate,
        endDate: newReqPayload.endDate,
        reason: newReqPayload.reason,
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leaves'] });
      setShowModal(false);
      setReason('');
      setStartDate('');
      setEndDate('');
    },
  });

  const handleCreateRequest = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({ startDate, endDate, reason });
  };

  // Écran d'erreur en production si API injoignable
  if (isErrorRequests && !import.meta.env.DEV) {
    return (
      <div className="card p-12 text-center flex flex-col items-center justify-center gap-4 my-8">
        <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div className="space-y-1">
          <h2 className="text-lg font-bold text-gray-900">Échec du chargement des congés</h2>
          <p className="text-xs text-gray-500 max-w-md">
            Impossible de récupérer l'état des soldes et demandes auprès du serveur API.
          </p>
        </div>
        <button
          type="button"
          onClick={() => refetch()}
          className="btn btn-primary text-xs flex items-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          Réessayer
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Gestion des Congés & Absences</h1>
          <p className="flex items-center gap-2">
            <span>Règle sénégalaise : 2 jours ouvrables acquis par mois (Code du Travail)</span>
            {!isLoadingRequests && !isErrorRequests && (
              <span className="badge badge-success text-[10px]">API Connectée</span>
            )}
            {isErrorRequests && (
              <span className="badge badge-warning text-[10px]">Mode Démo / Repli</span>
            )}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowModal(true)}
          className="btn btn-primary text-xs w-full sm:w-auto"
        >
          <Plus className="w-4 h-4" />
          Nouvelle Demande de Congé
        </button>
      </div>

      {/* Soldes personnels de congés */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {defaultBalances.map((bal) => (
          <div key={bal.id} className="card p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-gray-700">{bal.leaveTypeName}</span>
              <span className="badge badge-primary">{bal.year}</span>
            </div>
            <div className="flex items-baseline justify-between mt-3">
              <div>
                <span className="text-2xl font-bold text-[#1A6B3C]">{bal.remainingDays}</span>
                <span className="text-xs text-gray-500 ml-1">jours restants</span>
              </div>
              <span className="text-xs text-gray-400">
                {bal.usedDays} pris sur {bal.totalDays}j
              </span>
            </div>
            <div className="w-full bg-gray-100 rounded-full h-1.5 mt-3 overflow-hidden">
              <div
                className="bg-[#1A6B3C] h-1.5 rounded-full"
                style={{ width: `${(bal.remainingDays / bal.totalDays) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>

      {/* Bannière d'alerte Conflits de congés dans l'équipe */}
      {conflicts.length > 0 && (
        <div className="card border-l-4 border-l-amber-500 bg-amber-50/60 p-4">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <h4 className="text-xs font-bold text-amber-950 uppercase tracking-wider">
                Alerte RH : {conflicts.length} chevauchement(s) de congés détecté(s)
              </h4>
              {conflicts.map((c, idx) => (
                <p key={idx} className="text-xs text-amber-900">
                  <span className="font-semibold">{c.reqA.employee.fullName}</span> et{' '}
                  <span className="font-semibold">{c.reqB.employee.fullName}</span> sont absents simultanément
                  (période commune : du <span className="font-mono font-medium">{c.reqB.startDate}</span> au{' '}
                  <span className="font-mono font-medium">{c.reqB.endDate}</span>). Risque de sous-effectif opérationnel.
                </p>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Switcher d'affichage : Liste vs Calendrier d'équipe */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-2">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setViewMode('LIST')}
            className={`btn btn-sm ${viewMode === 'LIST' ? 'btn-primary' : 'btn-ghost'}`}
          >
            <ListFilter className="w-4 h-4" />
            Liste des Demandes ({requests.length})
          </button>
          <button
            type="button"
            onClick={() => setViewMode('CALENDAR')}
            className={`btn btn-sm ${viewMode === 'CALENDAR' ? 'btn-primary' : 'btn-ghost'}`}
          >
            <CalendarDays className="w-4 h-4" />
            Calendrier d'équipe & Conflits
            {conflicts.length > 0 && (
              <span className="ml-1 px-1.5 py-0.2 bg-amber-500 text-gray-900 font-bold rounded-full text-[10px]">
                {conflicts.length}
              </span>
            )}
          </button>
        </div>
        <span className="text-xs text-gray-500 hidden sm:inline">Période active : Avril - Mai 2026</span>
      </div>

      {/* Vue Calendrier d'équipe */}
      {viewMode === 'CALENDAR' && (
        <div className="card p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-gray-900">Planning & Continuité de Service</h3>
              <p className="text-xs text-gray-500">Visualisation chronologique des départs et alertes de présence simultanée</p>
            </div>
            <div className="flex items-center gap-3 text-xs">
              <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded bg-emerald-600 inline-block" /> Validé</span>
              <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded bg-amber-500 inline-block" /> En attente</span>
              <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded bg-rose-500 inline-block" /> Conflit</span>
            </div>
          </div>

          <div className="space-y-3 pt-2">
            {requests.map((req) => {
              const hasConflict = conflicts.some(c => c.reqA.id === req.id || c.reqB.id === req.id);
              return (
                <div key={req.id} className="p-3 bg-gray-50 border border-gray-200 rounded-lg flex flex-col md:flex-row md:items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="avatar avatar-sm avatar-primary font-bold">
                      {req.employee.fullName.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <div className="font-semibold text-xs text-gray-900 flex items-center gap-2">
                        {req.employee.fullName}
                        {hasConflict && (
                          <span className="badge badge-warning text-[10px] flex items-center gap-1">
                            <AlertTriangle className="w-3 h-3 text-amber-700" />
                            Chevauchement
                          </span>
                        )}
                      </div>
                      <div className="text-[11px] text-gray-500">
                        {req.leaveType?.name} • {req.totalDays} jours ouvrables
                      </div>
                    </div>
                  </div>

                  {/* Barre visuelle du congé */}
                  <div className="flex-1 max-w-md bg-white border border-gray-200 rounded-md p-2 flex items-center justify-between text-xs">
                    <div className="font-mono text-gray-600">Du {req.startDate}</div>
                    <div className={`h-2 flex-1 mx-3 rounded-full ${hasConflict ? 'bg-amber-400' : (req.status === 'APPROVED' ? 'bg-emerald-500' : 'bg-blue-400')}`} />
                    <div className="font-mono text-gray-600">Au {req.endDate}</div>
                  </div>

                  <span className={`badge ${req.status === 'APPROVED' ? 'badge-success' : 'badge-warning'} text-[11px]`}>
                    {req.status === 'APPROVED' ? 'Validé N+1' : 'En attente'}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Liste des demandes (mode LIST) */}
      {viewMode === 'LIST' && (
      <div className="card">
        <div className="card-header">
          <span className="card-title">Historique des Demandes de Congés</span>
          {isLoadingRequests && <Loader2 className="w-4 h-4 animate-spin text-[#1A6B3C]" />}
        </div>
        <div className="hidden md:block table-container border-0">
          <table className="table">
            <thead>
              <tr>
                <th>Collaborateur</th>
                <th>Type de Congé</th>
                <th>Période</th>
                <th>Durée</th>
                <th>Motif</th>
                <th>Statut</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((req) => (
                <tr key={req.id}>
                  <td>
                    <div className="font-semibold text-xs text-gray-900">
                      {req.employee.fullName}
                    </div>
                    <div className="text-[11px] text-gray-400 font-mono">
                      {req.employee.employeeNumber}
                    </div>
                  </td>
                  <td>
                    <span className="text-xs font-medium text-gray-800">
                      {req.leaveType?.name || 'Congé Payé Annuel'}
                    </span>
                  </td>
                  <td>
                    <div className="text-xs text-gray-700">
                      Du {req.startDate} au {req.endDate}
                    </div>
                  </td>
                  <td>
                    <span className="text-xs font-bold text-gray-900">
                      {req.totalDays} jours ouvrables
                    </span>
                  </td>
                  <td>
                    <span className="text-xs text-gray-500 max-w-xs truncate block" title={req.reason}>
                      {req.reason || '—'}
                    </span>
                  </td>
                  <td>
                    {req.status === 'PENDING' && (
                      <span className="badge badge-warning flex items-center gap-1 w-fit">
                        <Clock className="w-3 h-3" />
                        En attente N+1
                      </span>
                    )}
                    {req.status === 'APPROVED' && (
                      <span className="badge badge-success flex items-center gap-1 w-fit">
                        <CheckCircle className="w-3 h-3" />
                        Validé
                      </span>
                    )}
                    {req.status === 'REJECTED' && (
                      <span className="badge badge-danger flex items-center gap-1 w-fit">
                        <XCircle className="w-3 h-3" />
                        Refusé
                      </span>
                    )}
                  </td>
                  <td className="text-right">
                    {req.status === 'PENDING' && (
                      <div className="flex items-center justify-end gap-1">
                        <button type="button" className="btn btn-sm btn-primary text-[11px] py-1 px-2">
                          Valider
                        </button>
                        <button type="button" className="btn btn-sm btn-danger text-[11px] py-1 px-2">
                          Refuser
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Vue mobile cartes (responsive 360px) */}
        <div className="md:hidden divide-y divide-gray-100">
          {requests.map((req) => (
            <div key={req.id} className="p-4 space-y-2">
              <div className="flex items-start justify-between">
                <div>
                  <div className="font-bold text-xs text-gray-900">{req.employee.fullName}</div>
                  <div className="text-[11px] text-gray-500">{req.leaveType?.name}</div>
                </div>
                <span className="badge badge-warning text-[10px]">{req.status}</span>
              </div>
              <div className="text-xs text-gray-700">
                Du <span className="font-semibold">{req.startDate}</span> au <span className="font-semibold">{req.endDate}</span> ({req.totalDays}j)
              </div>
              {req.reason && <p className="text-[11px] text-gray-500 italic">"{req.reason}"</p>}
            </div>
          ))}
        </div>
      </div>
      )}

      {/* Modal Nouvelle Demande */}
      {showModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <span className="modal-title">Demande de Congé</span>
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 text-lg font-bold"
              >
                ×
              </button>
            </div>
            <form onSubmit={handleCreateRequest}>
              <div className="modal-body space-y-4">
                <div className="form-group">
                  <label className="form-label">Type de congé légal</label>
                  <select className="form-control text-xs">
                    <option>Congé Payé Annuel (Acquisition : 2j/mois)</option>
                    <option>Permissions Exceptionnelles (Événement familial)</option>
                    <option>Congé Maternité (14 semaines légales)</option>
                    <option>Congé Maladie</option>
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="form-group">
                    <label className="form-label">Date de début</label>
                    <input
                      type="date"
                      required
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className="form-control text-xs"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Date de fin</label>
                    <input
                      type="date"
                      required
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      className="form-control text-xs"
                    />
                  </div>
                </div>

                {/* Détection en direct d'un conflit de congés dans l'équipe */}
                {potentialFormConflict && (
                  <div className="p-3 bg-amber-50 border border-amber-300 rounded-lg text-xs text-amber-900 flex items-start gap-2">
                    <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                    <span>
                      <strong>Alerte chevauchement :</strong> Cette période coïncide avec le congé de{' '}
                      <strong>{potentialFormConflict.employee.fullName}</strong> (du{' '}
                      {potentialFormConflict.startDate} au {potentialFormConflict.endDate}). Assurez-vous de la continuité de service.
                    </span>
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">Motif de la demande</label>
                  <textarea
                    rows={3}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Préciser le motif de l'absence..."
                    className="form-control text-xs"
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn btn-secondary text-xs"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="btn btn-primary text-xs"
                >
                  {createMutation.isPending ? 'Envoi en cours...' : 'Soumettre la demande'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
export default LeaveListPage;
