import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import {
  FileText,
  DollarSign,
  TrendingDown,
  Building,
  AlertCircle,
  RefreshCw,
  Loader2,
  CheckCircle,
  AlertTriangle,
  Settings,
  Calculator,
  XCircle,
  Search,
  Filter,
  Users,
  ShieldAlert,
  X
} from 'lucide-react';
import { downloadFile } from '@/lib/downloadFile';
import { useAuth } from '@/context/AuthContext';
import type {
  Payslip,
  PayslipStatus,
  PayrollParameter,
  PayrollStats
} from '@/types';

export const PayrollListPage: React.FC = () => {
  const { hasRole } = useAuth();
  const isDrh = hasRole('ROLE_DRH') || hasRole('ROLE_ADMIN');
  const queryClient = useQueryClient();

  // Filtres
  const [selectedPeriod, setSelectedPeriod] = useState('2026-09');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');

  // Téléchargements et erreurs
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Modales
  const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
  const [genYear, setGenYear] = useState<number>(2026);
  const [genMonth, setGenMonth] = useState<number>(9);

  const [isParamsModalOpen, setIsParamsModalOpen] = useState(false);
  const [cancelCandidate, setCancelCandidate] = useState<Payslip | null>(null);

  const formatFCFA = (val?: number) => {
    if (val === undefined || val === null) return '0 FCFA';
    return new Intl.NumberFormat('fr-SN', { style: 'decimal' }).format(Math.round(val)) + ' FCFA';
  };

  // 1. Récupération des bulletins (DRH = entreprise complète, Employé/Manager = personnels)
  const {
    data: payslipsResponse,
    isLoading: isPayslipsLoading,
    isError: isPayslipsError,
    refetch: refetchPayslips,
  } = useQuery<Payslip[]>({
    queryKey: ['payslips', isDrh, selectedPeriod],
    queryFn: async () => {
      if (isDrh) {
        const res = await api.get('/payroll/payslips', {
          params: { period: selectedPeriod },
        });
        return res.data?.data || res.data || [];
      } else {
        const res = await api.get('/payroll/my-payslips');
        return res.data?.data || res.data || [];
      }
    },
  });

  const payslipsList: Payslip[] = Array.isArray(payslipsResponse) ? payslipsResponse : [];

  // 2. Statistiques de masse salariale (DRH uniquement)
  const [periodYearStr, periodMonthStr] = selectedPeriod.split('-');
  const currentYear = parseInt(periodYearStr, 10) || 2026;
  const currentMonth = parseInt(periodMonthStr, 10) || 9;

  const {
    data: statsData,
    isLoading: isStatsLoading,
    refetch: refetchStats,
  } = useQuery<PayrollStats>({
    queryKey: ['payroll-stats', currentYear, currentMonth],
    queryFn: async () => {
      const res = await api.get('/payroll/stats', {
        params: { year: currentYear, month: currentMonth },
      });
      return res.data?.data || res.data;
    },
    enabled: isDrh,
  });

  // 3. Paramètres légaux (DRH uniquement)
  const {
    data: paramsData,
    isLoading: isParamsLoading,
  } = useQuery<PayrollParameter[]>({
    queryKey: ['payroll-parameters'],
    queryFn: async () => {
      const res = await api.get('/payroll/parameters');
      return res.data?.data || res.data || [];
    },
    enabled: isDrh && isParamsModalOpen,
  });

  // Mutations
  const generateMutation = useMutation({
    mutationFn: async ({ year, month }: { year: number; month: number }) => {
      const res = await api.post('/payroll/generate', null, {
        params: { year, month },
      });
      return res.data;
    },
    onSuccess: (data) => {
      const count = Array.isArray(data?.data) ? data.data.length : (Array.isArray(data) ? data.length : 0);
      setActionSuccess(`Paie du ${String(genMonth).padStart(2, '0')}/${genYear} générée avec succès (${count} bulletin(s)).`);
      setIsGenerateModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['payslips'] });
      queryClient.invalidateQueries({ queryKey: ['payroll-stats'] });
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Erreur lors de la génération de la paie';
      setDownloadError(msg);
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: PayslipStatus }) => {
      const res = await api.patch(`/payroll/payslips/${id}/status`, null, {
        params: { status },
      });
      return res.data;
    },
    onSuccess: (_, variables) => {
      setActionSuccess(`Statut du bulletin mis à jour : ${variables.status}`);
      setCancelCandidate(null);
      queryClient.invalidateQueries({ queryKey: ['payslips'] });
      queryClient.invalidateQueries({ queryKey: ['payroll-stats'] });
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Erreur lors de la mise à jour du statut';
      setDownloadError(msg);
      setCancelCandidate(null);
    },
  });

  const handleDownload = async (ps: Payslip) => {
    setDownloadingId(ps.id);
    setDownloadError(null);

    const monthStr = String(ps.periodMonth).padStart(2, '0');
    const filename = `bulletin-paie-${ps.periodYear}-${monthStr}-${ps.employeeNumber || 'salarie'}.pdf`;

    try {
      await downloadFile(`/documents/payslip/${ps.id}`, filename);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Échec du téléchargement du bulletin';
      setDownloadError(msg);
    } finally {
      setDownloadingId(null);
    }
  };

  // Filtrage local
  const filteredPayslips = payslipsList.filter((ps) => {
    if (statusFilter !== 'ALL' && ps.status !== statusFilter) {
      return false;
    }
    if (searchTerm.trim() !== '') {
      const term = searchTerm.toLowerCase();
      const matchName = ps.employeeFullName?.toLowerCase().includes(term);
      const matchNum = ps.employeeNumber?.toLowerCase().includes(term);
      const matchDept = ps.departmentName?.toLowerCase().includes(term);
      return matchName || matchNum || matchDept;
    }
    return true;
  });

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <Calculator className="w-6 h-6 text-[#1A6B3C]" />
            {isDrh ? 'Gestion de la Paie & Masse Salariale' : 'Mes Bulletins de Salaire'}
          </h1>
          <p className="text-xs text-gray-500">
            {isDrh
              ? 'Calcul légal sénégalais : IPRES (RG & RC Cadres), CSS, CFCE, IPM et barème progressif IR avec quotient familial.'
              : 'Consultez et téléchargez vos fiches de paie mensuelles en toute confidentialité.'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {isDrh && (
            <>
              <button
                type="button"
                onClick={() => setIsParamsModalOpen(true)}
                className="btn btn-secondary btn-sm text-xs inline-flex items-center gap-1.5"
                title="Consulter les barèmes et taux légaux configurés"
              >
                <Settings className="w-3.5 h-3.5 text-gray-600" />
                Paramètres légaux
              </button>
              <button
                type="button"
                onClick={() => {
                  setGenYear(currentYear);
                  setGenMonth(currentMonth);
                  setIsGenerateModalOpen(true);
                }}
                className="btn btn-primary btn-sm text-xs inline-flex items-center gap-1.5"
              >
                <Calculator className="w-3.5 h-3.5" />
                Générer la paie
              </button>
            </>
          )}

          {isDrh && (
            <input
              type="month"
              value={selectedPeriod}
              onChange={(e) => setSelectedPeriod(e.target.value)}
              className="input input-sm text-xs border border-gray-300 rounded px-2 py-1"
            />
          )}

          <button
            type="button"
            onClick={() => {
              refetchPayslips();
              if (isDrh) refetchStats();
            }}
            className="btn btn-secondary btn-sm text-xs inline-flex items-center gap-1"
            title="Actualiser la liste"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Actualiser
          </button>
        </div>
      </div>

      {/* Avertissement DRH légal obligatoire */}
      {isDrh && (
        <div className="flex items-start gap-3 p-3.5 rounded-xl border border-amber-200 bg-amber-50/80 text-amber-900 text-xs shadow-xs">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <span className="font-semibold text-amber-950 block mb-0.5">
              Avertissement : Taux et barèmes indicatifs (« VALEUR À CONFIRMER »)
            </span>
            <p className="text-amber-800 leading-relaxed">
              Les paramètres fiscaux et sociaux appliqués (barème IR, tranches TRIMF, cotisations IPRES générale et cadres, cotisations CSS, CFCE, IPM)
              doivent être formellement validés par la direction financière et les textes légaux en vigueur au Sénégal avant tout usage contractuel ou fiscal réel.
            </p>
          </div>
        </div>
      )}

      {/* Messages de succès ou d'erreur */}
      {actionSuccess && (
        <div className="alert alert-success flex items-center justify-between p-3 rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-800 text-xs">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{actionSuccess}</span>
          </div>
          <button
            type="button"
            onClick={() => setActionSuccess(null)}
            className="btn btn-secondary btn-sm text-[11px] py-0.5 px-2"
          >
            Fermer
          </button>
        </div>
      )}

      {downloadError && (
        <div className="alert alert-danger flex items-center justify-between p-3 rounded-lg border border-red-200 bg-red-50 text-red-800 text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
            <span>{downloadError}</span>
          </div>
          <button
            type="button"
            onClick={() => setDownloadError(null)}
            className="btn btn-secondary btn-sm text-[11px] py-0.5 px-2"
          >
            Fermer
          </button>
        </div>
      )}

      {/* Cartes récapitulatives de masse salariale (DRH uniquement) */}
      {isDrh && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="card p-4 flex items-center gap-3 bg-white shadow-xs border border-gray-100">
              <div className="p-2.5 rounded-lg bg-green-50 text-[#1A6B3C]">
                <DollarSign className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs text-gray-500">Masse Salariale Totale</div>
                <div className="text-base font-bold font-mono text-gray-900">
                  {isStatsLoading ? '...' : formatFCFA(statsData?.totalPayrollCost)}
                </div>
                <div className="text-[10px] text-gray-400">Brut + Charges patronales</div>
              </div>
            </div>

            <div className="card p-4 flex items-center gap-3 bg-white shadow-xs border border-gray-100">
              <div className="p-2.5 rounded-lg bg-emerald-50 text-emerald-600">
                <Building className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs text-gray-500">Net Global à Verser</div>
                <div className="text-base font-bold font-mono text-[#1A6B3C]">
                  {isStatsLoading ? '...' : formatFCFA(statsData?.totalNetSalary)}
                </div>
                <div className="text-[10px] text-gray-400">Montant net salariés</div>
              </div>
            </div>

            <div className="card p-4 flex items-center gap-3 bg-white shadow-xs border border-gray-100">
              <div className="p-2.5 rounded-lg bg-amber-50 text-amber-600">
                <TrendingDown className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs text-gray-500">Charges Patronales</div>
                <div className="text-base font-bold font-mono text-amber-700">
                  {isStatsLoading ? '...' : formatFCFA(statsData?.totalEmployerContributions)}
                </div>
                <div className="text-[10px] text-gray-400">IPRES, CSS, CFCE, IPM</div>
              </div>
            </div>

            <div className="card p-4 flex items-center gap-3 bg-white shadow-xs border border-gray-100">
              <div className="p-2.5 rounded-lg bg-blue-50 text-blue-600">
                <Users className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs text-gray-500">Effectif Payé</div>
                <div className="text-base font-bold font-mono text-gray-900">
                  {isStatsLoading ? '...' : `${statsData?.totalEmployeesPaid || 0} salarié(s)`}
                </div>
                <div className="text-[10px] text-gray-400">Période {selectedPeriod}</div>
              </div>
            </div>
          </div>

          {/* Répartition par département avec règle de confidentialité */}
          {statsData && statsData.departmentBreakdowns && statsData.departmentBreakdowns.length > 0 && (
            <div className="card p-4 bg-white shadow-xs border border-gray-100">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-xs font-bold text-gray-800 uppercase tracking-wider flex items-center gap-1.5">
                  <Building className="w-4 h-4 text-[#1A6B3C]" />
                  Ventilation par département
                </h3>
                {statsData.anonymized && (
                  <span className="badge badge-warning text-[10px] flex items-center gap-1">
                    <ShieldAlert className="w-3 h-3" />
                    Règle de confidentialité appliquée (&lt; 3 salariés regroupés)
                  </span>
                )}
              </div>
              <div className="overflow-x-auto">
                <table className="table w-full text-xs">
                  <thead>
                    <tr className="border-b border-gray-100 text-gray-500 text-left">
                      <th className="py-2">Département</th>
                      <th className="py-2 text-center">Effectif</th>
                      <th className="py-2 text-right">Brut Total</th>
                      <th className="py-2 text-right">Net Total</th>
                      <th className="py-2 text-right">Charges Patronales</th>
                    </tr>
                  </thead>
                  <tbody>
                    {statsData.departmentBreakdowns.map((dept, idx) => (
                      <tr key={idx} className="border-b border-gray-50 hover:bg-gray-50/50">
                        <td className="py-2 font-medium text-gray-800">{dept.departmentName}</td>
                        <td className="py-2 text-center font-mono">{dept.employeeCount}</td>
                        <td className="py-2 text-right font-mono">{formatFCFA(dept.totalGrossSalary)}</td>
                        <td className="py-2 text-right font-mono text-[#1A6B3C] font-semibold">{formatFCFA(dept.totalNetSalary)}</td>
                        <td className="py-2 text-right font-mono text-amber-700">{formatFCFA(dept.totalEmployerContributions)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {statsData?.anonymized && (!statsData.departmentBreakdowns || statsData.departmentBreakdowns.length === 0) && (
            <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-600 flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 text-gray-500 shrink-0" />
              <span>
                Ventilation par département masquée par mesure de confidentialité (effectif global inférieur à 3 salariés).
              </span>
            </div>
          )}
        </div>
      )}

      {/* Barre de filtres et recherche */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-xl border border-gray-100 shadow-xs">
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Rechercher par nom ou matricule..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input input-sm text-xs pl-9 w-full border border-gray-200 rounded-lg"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
          <Filter className="w-3.5 h-3.5 text-gray-400" />
          <span className="text-xs text-gray-500">Statut :</span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="input input-sm text-xs border border-gray-200 rounded-lg px-2 py-1 bg-white"
          >
            <option value="ALL">Tous les statuts</option>
            <option value="GENERATED">Généré</option>
            <option value="VALIDATED">Validé</option>
            <option value="PAID">Payé</option>
            <option value="CANCELLED">Annulé</option>
          </select>
        </div>
      </div>

      {/* Tableau des bulletins */}
      <div className="card p-0 overflow-hidden bg-white shadow-xs border border-gray-100">
        <div className="table-container">
          <table className="table w-full">
            <thead>
              <tr className="bg-gray-50/75 border-b border-gray-100 text-left text-xs font-semibold text-gray-600">
                <th className="py-3 px-4">Collaborateur</th>
                <th className="py-3 px-4">Période</th>
                <th className="py-3 px-4 text-right">Salaire Brut</th>
                <th className="py-3 px-4 text-right">Net Imposable</th>
                <th className="py-3 px-4 text-right">Déductions</th>
                <th className="py-3 px-4 text-right">Net à Payer</th>
                {isDrh && <th className="py-3 px-4 text-right">Charges Patr.</th>}
                <th className="py-3 px-4 text-center">Statut</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {isPayslipsLoading ? (
                <tr>
                  <td colSpan={isDrh ? 9 : 8} className="text-center py-10 text-gray-500">
                    <Loader2 className="w-5 h-5 animate-spin mx-auto text-[#1A6B3C] mb-2" />
                    Chargement des bulletins de paie...
                  </td>
                </tr>
              ) : isPayslipsError ? (
                <tr>
                  <td colSpan={isDrh ? 9 : 8} className="text-center py-10 text-red-600">
                    <AlertCircle className="w-5 h-5 mx-auto mb-2 text-red-500" />
                    Erreur lors de la récupération des bulletins de paie.
                    <button
                      type="button"
                      onClick={() => refetchPayslips()}
                      className="btn btn-secondary btn-sm text-xs mt-2 mx-auto block"
                    >
                      Réessayer
                    </button>
                  </td>
                </tr>
              ) : filteredPayslips.length === 0 ? (
                <tr>
                  <td colSpan={isDrh ? 9 : 8} className="text-center py-10 text-gray-500">
                    Aucun bulletin trouvé pour les filtres sélectionnés.
                  </td>
                </tr>
              ) : (
                filteredPayslips.map((ps) => {
                  const isDownloading = downloadingId === ps.id;

                  return (
                    <tr key={ps.id} className="hover:bg-gray-50/60 transition-colors">
                      <td className="py-3 px-4">
                        <div className="font-semibold text-gray-900">
                          {ps.employeeFullName || 'Collaborateur'}
                        </div>
                        <div className="text-[11px] text-gray-400 font-mono">
                          {ps.employeeNumber || 'N/A'} {ps.departmentName ? `• ${ps.departmentName}` : ''}
                        </div>
                      </td>
                      <td className="py-3 px-4 font-mono text-gray-700">
                        {ps.periodYear}-{String(ps.periodMonth).padStart(2, '0')}
                      </td>
                      <td className="py-3 px-4 text-right font-mono text-gray-800">
                        {formatFCFA(ps.grossSalary)}
                      </td>
                      <td className="py-3 px-4 text-right font-mono text-gray-600">
                        {formatFCFA(ps.taxableGross)}
                      </td>
                      <td className="py-3 px-4 text-right font-mono text-red-700">
                        -{formatFCFA(ps.totalDeductions)}
                      </td>
                      <td className="py-3 px-4 text-right font-mono font-bold text-[#1A6B3C]">
                        {formatFCFA(ps.netSalary)}
                      </td>
                      {isDrh && (
                        <td className="py-3 px-4 text-right font-mono text-amber-700">
                          {formatFCFA(ps.employerContributions)}
                        </td>
                      )}
                      <td className="py-3 px-4 text-center">
                        {ps.status === 'GENERATED' && (
                          <span className="badge badge-info inline-flex items-center gap-1 text-[11px] px-2 py-0.5">
                            Généré
                          </span>
                        )}
                        {ps.status === 'VALIDATED' && (
                          <span className="badge badge-success inline-flex items-center gap-1 text-[11px] px-2 py-0.5">
                            <CheckCircle className="w-3 h-3" />
                            Validé
                          </span>
                        )}
                        {ps.status === 'PAID' && (
                          <span className="badge badge-success inline-flex items-center gap-1 text-[11px] px-2 py-0.5 bg-emerald-100 text-emerald-800 border-emerald-300">
                            Payé
                          </span>
                        )}
                        {ps.status === 'CANCELLED' && (
                          <span className="badge badge-neutral inline-flex items-center gap-1 text-[11px] px-2 py-0.5 bg-gray-100 text-gray-600">
                            <XCircle className="w-3 h-3 text-red-500" />
                            Annulé
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Téléchargement PDF */}
                          <button
                            type="button"
                            onClick={() => handleDownload(ps)}
                            disabled={isDownloading}
                            className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1"
                            title="Télécharger le bulletin au format PDF"
                          >
                            {isDownloading ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1A6B3C]" />
                            ) : (
                              <FileText className="w-3.5 h-3.5 text-[#1A6B3C]" />
                            )}
                            PDF
                          </button>

                          {/* Actions DRH selon cycle de vie */}
                          {isDrh && ps.status === 'GENERATED' && (
                            <button
                              type="button"
                              onClick={() => updateStatusMutation.mutate({ id: ps.id, status: 'VALIDATED' })}
                              disabled={updateStatusMutation.isPending}
                              className="btn btn-primary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1"
                              title="Valider le bulletin (étape avant paiement)"
                            >
                              Valider
                            </button>
                          )}

                          {isDrh && ps.status === 'VALIDATED' && (
                            <>
                              <button
                                type="button"
                                onClick={() => updateStatusMutation.mutate({ id: ps.id, status: 'PAID' })}
                                disabled={updateStatusMutation.isPending}
                                className="btn btn-primary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1 bg-emerald-700 hover:bg-emerald-800 text-white"
                                title="Marquer comme payé (irréversible)"
                              >
                                Payer
                              </button>
                              <button
                                type="button"
                                onClick={() => setCancelCandidate(ps)}
                                disabled={updateStatusMutation.isPending}
                                className="btn btn-secondary btn-sm text-[11px] py-1 px-2 text-red-600 hover:bg-red-50 border-red-200"
                                title="Annuler le bulletin pour correction"
                              >
                                Annuler
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal de Confirmation d'annulation */}
      {cancelCandidate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-5 space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-red-600" />
                Annulation de bulletin de paie
              </h3>
              <button
                type="button"
                onClick={() => setCancelCandidate(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="text-xs text-gray-600 space-y-2">
              <p>
                Vous vous apprêtez à annuler le bulletin validé de{' '}
                <strong className="text-gray-900">{cancelCandidate.employeeFullName}</strong> pour la période{' '}
                <strong className="text-gray-900">
                  {cancelCandidate.periodYear}-{String(cancelCandidate.periodMonth).padStart(2, '0')}
                </strong>.
              </p>
              <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 text-amber-900">
                <p>
                  Cette action est auditée (votre identifiant et la date seront consignés). Le bulletin passera au statut
                  <strong> ANNULÉ</strong> et pourra être régénéré lors du prochain calcul de paie.
                </p>
              </div>
            </div>
            <div className="flex items-center justify-end gap-2 pt-2 border-t">
              <button
                type="button"
                onClick={() => setCancelCandidate(null)}
                className="btn btn-secondary btn-sm text-xs"
              >
                Conserver
              </button>
              <button
                type="button"
                onClick={() => updateStatusMutation.mutate({ id: cancelCandidate.id, status: 'CANCELLED' })}
                disabled={updateStatusMutation.isPending}
                className="btn btn-primary btn-sm text-xs bg-red-600 hover:bg-red-700 text-white"
              >
                {updateStatusMutation.isPending ? 'Annulation...' : 'Confirmer l\'annulation'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Génération de Paie */}
      {isGenerateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full p-5 space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2">
                <Calculator className="w-4 h-4 text-[#1A6B3C]" />
                Lancer le calcul de la paie mensuelle
              </h3>
              <button
                type="button"
                onClick={() => setIsGenerateModalOpen(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-4 text-xs text-gray-600">
              <p>
                Sélectionnez la période mensuelle à calculer. Le moteur appliquera les contrats actifs,
                les règles cadres/non-cadres, la situation familiale et les paramètres en vigueur au dernier jour du mois.
              </p>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-gray-700 font-medium mb-1">Mois</label>
                  <select
                    value={genMonth}
                    onChange={(e) => setGenMonth(parseInt(e.target.value, 10))}
                    className="input input-sm w-full border border-gray-300 rounded-lg p-2"
                  >
                    {[
                      { m: 1, label: 'Janvier' },
                      { m: 2, label: 'Février' },
                      { m: 3, label: 'Mars' },
                      { m: 4, label: 'Avril' },
                      { m: 5, label: 'Mai' },
                      { m: 6, label: 'Juin' },
                      { m: 7, label: 'Juillet' },
                      { m: 8, label: 'Août' },
                      { m: 9, label: 'Septembre' },
                      { m: 10, label: 'Octobre' },
                      { m: 11, label: 'Novembre' },
                      { m: 12, label: 'Décembre' },
                    ].map((item) => (
                      <option key={item.m} value={item.m}>
                        {item.m} - {item.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-gray-700 font-medium mb-1">Année</label>
                  <input
                    type="number"
                    value={genYear}
                    onChange={(e) => setGenYear(parseInt(e.target.value, 10))}
                    className="input input-sm w-full border border-gray-300 rounded-lg p-2"
                    min={2020}
                    max={2035}
                  />
                </div>
              </div>

              <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 text-amber-900 space-y-1">
                <span className="font-semibold block">Règle de recalcul :</span>
                <p>
                  Les bulletins existants au statut <strong>GÉNÉRÉ</strong> ou <strong>ANNULÉ</strong> seront recalculés et écrasés.
                  Les bulletins déjà <strong>VALIDÉS</strong> ou <strong>PAYÉS</strong> sont strictement conservés et ne seront pas modifiés.
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t">
              <button
                type="button"
                onClick={() => setIsGenerateModalOpen(false)}
                className="btn btn-secondary btn-sm text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={() => generateMutation.mutate({ year: genYear, month: genMonth })}
                disabled={generateMutation.isPending}
                className="btn btn-primary btn-sm text-xs inline-flex items-center gap-1.5"
              >
                {generateMutation.isPending ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    Calcul en cours...
                  </>
                ) : (
                  <>
                    <Calculator className="w-3.5 h-3.5" />
                    Calculer la paie ({String(genMonth).padStart(2, '0')}/{genYear})
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal des Paramètres Légaux */}
      {isParamsModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between p-4 border-b">
              <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2">
                <Settings className="w-4 h-4 text-[#1A6B3C]" />
                Paramètres Légaux & Barèmes de Paie (Consultation)
              </h3>
              <button
                type="button"
                onClick={() => setIsParamsModalOpen(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-4 overflow-y-auto space-y-4 text-xs">
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-amber-900">
                <p className="font-semibold mb-1">Statut des valeurs : « VALEUR À CONFIRMER »</p>
                <p>
                  Les paramètres ci-dessous sont configurés en base de données avec date d'effet.
                  Toute modification nécessite l'arbitrage du service juridique et de la direction financière.
                </p>
              </div>

              {isParamsLoading ? (
                <div className="py-8 text-center text-gray-500">
                  <Loader2 className="w-5 h-5 animate-spin mx-auto text-[#1A6B3C] mb-2" />
                  Chargement des paramètres légaux...
                </div>
              ) : (
                <div className="border border-gray-100 rounded-lg overflow-hidden">
                  <table className="table w-full text-xs">
                    <thead>
                      <tr className="bg-gray-50 border-b text-left text-gray-600">
                        <th className="py-2 px-3">Clé de paramètre</th>
                        <th className="py-2 px-3 text-right">Valeur</th>
                        <th className="py-2 px-3">Catégorie</th>
                        <th className="py-2 px-3 text-center">Statut</th>
                        <th className="py-2 px-3">Source déclarée</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {paramsData && paramsData.map((p) => (
                        <tr key={p.id} className="hover:bg-gray-50/50">
                          <td className="py-2 px-3 font-mono font-medium text-gray-800">{p.paramKey}</td>
                          <td className="py-2 px-3 text-right font-mono font-bold text-gray-900">{p.paramValue}</td>
                          <td className="py-2 px-3 text-gray-500">{p.category}</td>
                          <td className="py-2 px-3 text-center">
                            <span className="badge badge-warning text-[10px] px-1.5 py-0.5">
                              {p.status}
                            </span>
                          </td>
                          <td className="py-2 px-3 text-gray-400 italic text-[11px]">{p.source}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="p-3 border-t flex justify-end">
              <button
                type="button"
                onClick={() => setIsParamsModalOpen(false)}
                className="btn btn-secondary btn-sm text-xs"
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

export default PayrollListPage;
