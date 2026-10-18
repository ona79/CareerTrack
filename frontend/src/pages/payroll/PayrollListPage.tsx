import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import {
  CheckCircle,
  FileText,
  DollarSign,
  TrendingDown,
  Building,
  AlertCircle,
  RefreshCw,
  Loader2
} from 'lucide-react';
import { downloadFile } from '@/lib/downloadFile';

interface PayslipLineDto {
  id: string;
  lineType: string;
  label: string;
  baseAmount: number;
  rate: number;
  amount: number;
}

interface PayslipDto {
  id: string;
  employeeId?: string;
  employeeNumber?: string;
  employeeFullName?: string;
  departmentName?: string;
  periodYear: number;
  periodMonth: number;
  grossSalary: number;
  netSalary: number;
  totalDeductions: number;
  paymentDate: string;
  status: 'DRAFT' | 'GENERATED' | 'PAID' | 'VALIDATED';
  lines?: PayslipLineDto[];
}

export const PayrollListPage: React.FC = () => {
  const [selectedPeriod, setSelectedPeriod] = useState('2026-09');
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const formatFCFA = (val: number) => {
    return new Intl.NumberFormat('fr-SN', { style: 'decimal' }).format(val) + ' FCFA';
  };

  const isUuid = (str?: string) => {
    return !!str && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(str);
  };

  // Récupération des bulletins réels depuis le backend
  const {
    data: payslipsResponse,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['payslips', selectedPeriod],
    queryFn: async () => {
      const res = await api.get('/payroll/payslips', {
        params: { period: selectedPeriod },
      });
      return res.data?.data || res.data || [];
    },
  });

  const realPayslips: PayslipDto[] = Array.isArray(payslipsResponse) ? payslipsResponse : [];

  // Si l'API retourne une liste vide en mode dev, proposer le bulletin du seed V3
  const displayedPayslips: PayslipDto[] = realPayslips.length > 0 ? realPayslips : [
    {
      id: '99999999-9999-9999-9999-999999999901',
      employeeId: '55555555-5555-5555-5555-555555555504',
      employeeNumber: 'EMP-004',
      employeeFullName: 'Ousmane Diallo',
      departmentName: 'Ingénierie & Systèmes',
      periodYear: 2026,
      periodMonth: 9,
      grossSalary: 950000,
      netSalary: 786800,
      totalDeductions: 163200,
      paymentDate: '2026-09-30',
      status: 'GENERATED',
    },
    {
      id: 'mock-ps-fictif',
      employeeId: 'mock-e1',
      employeeNumber: 'EMP-001',
      employeeFullName: 'Amadou Diop (Fictif)',
      departmentName: 'Direction Générale',
      periodYear: 2026,
      periodMonth: 9,
      grossSalary: 1200000,
      netSalary: 985400,
      totalDeductions: 214600,
      paymentDate: '2026-09-30',
      status: 'DRAFT',
    }
  ];

  const totalGross = displayedPayslips.reduce((acc, p) => acc + (p.grossSalary || 0), 0);
  const totalNet = displayedPayslips.reduce((acc, p) => acc + (p.netSalary || 0), 0);
  const totalDeductions = displayedPayslips.reduce((acc, p) => acc + (p.totalDeductions || 0), 0);

  const handleDownload = async (ps: PayslipDto) => {
    if (!isUuid(ps.id)) return;
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

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Gestion de la Paie & Bulletins de Salaire</h1>
          <p>
            Édition mensuelle conforme : cotisations IPRES (5,6% / 8,4%) et CSS (7%)
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <input
            type="month"
            value={selectedPeriod}
            onChange={(e) => setSelectedPeriod(e.target.value)}
            className="input input-sm text-xs border border-gray-300 rounded px-2 py-1"
          />
          <button
            type="button"
            onClick={() => refetch()}
            className="btn btn-secondary btn-sm text-xs inline-flex items-center gap-1"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Actualiser
          </button>
        </div>
      </div>

      {downloadError && (
        <div className="alert alert-danger flex items-center justify-between p-3 rounded-lg border border-red-200 bg-red-50 text-red-800 text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
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

      {/* Cartes récapitulatives */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="card p-4 flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-green-50 text-[#1A6B3C]">
            <DollarSign className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-gray-500">Masse Salariale Brute</div>
            <div className="text-sm font-bold font-mono text-gray-900">
              {formatFCFA(totalGross)}
            </div>
          </div>
        </div>

        <div className="card p-4 flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-emerald-50 text-emerald-600">
            <Building className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-gray-500">Net Global à Verser</div>
            <div className="text-sm font-bold font-mono text-[#1A6B3C]">
              {formatFCFA(totalNet)}
            </div>
          </div>
        </div>

        <div className="card p-4 flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-amber-50 text-amber-600">
            <TrendingDown className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-gray-500">Cotisations IPRES / CSS</div>
            <div className="text-sm font-bold font-mono text-gray-900">
              {formatFCFA(totalDeductions)}
            </div>
          </div>
        </div>
      </div>

      {/* Tableau des bulletins */}
      <div className="card p-0 overflow-hidden bg-white">
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Collaborateur</th>
                <th>Période</th>
                <th>Salaire Brut</th>
                <th>Net à Payer</th>
                <th>Statut</th>
                <th>Date Paiement</th>
                <th className="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-xs text-gray-500">
                    <Loader2 className="w-5 h-5 animate-spin mx-auto text-[#1A6B3C] mb-2" />
                    Chargement des bulletins de paie...
                  </td>
                </tr>
              ) : isError ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-xs text-red-600">
                    <AlertCircle className="w-5 h-5 mx-auto mb-2" />
                    Erreur lors de la récupération des bulletins de paie.
                    <button
                      type="button"
                      onClick={() => refetch()}
                      className="btn btn-secondary btn-sm text-xs mt-2 mx-auto block"
                    >
                      Réessayer
                    </button>
                  </td>
                </tr>
              ) : displayedPayslips.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-xs text-gray-500">
                    Aucun bulletin trouvé pour la période sélectionnée ({selectedPeriod}).
                  </td>
                </tr>
              ) : (
                displayedPayslips.map((ps) => {
                  const validUuid = isUuid(ps.id);
                  const isDownloading = downloadingId === ps.id;

                  return (
                    <tr key={ps.id}>
                      <td>
                        <div className="font-semibold text-xs text-gray-900">
                          {ps.employeeFullName || 'Collaborateur'}
                        </div>
                        <div className="text-[11px] text-gray-400 font-mono">
                          {ps.employeeNumber || 'EMP-N/A'}
                        </div>
                      </td>
                      <td>
                        <span className="text-xs font-mono font-medium text-gray-700">
                          {ps.periodYear}-{String(ps.periodMonth).padStart(2, '0')}
                        </span>
                      </td>
                      <td>
                        <span className="text-xs font-mono text-gray-800">
                          {formatFCFA(ps.grossSalary)}
                        </span>
                      </td>
                      <td>
                        <span className="text-xs font-mono font-bold text-[#1A6B3C]">
                          {formatFCFA(ps.netSalary)}
                        </span>
                      </td>
                      <td>
                        {(ps.status === 'VALIDATED' || ps.status === 'GENERATED') && (
                          <span className="badge badge-success flex items-center gap-1 w-fit text-[11px]">
                            <CheckCircle className="w-3 h-3" />
                            {ps.status === 'VALIDATED' ? 'Validé' : 'Généré'}
                          </span>
                        )}
                        {ps.status === 'PAID' && (
                          <span className="badge badge-info flex items-center gap-1 w-fit text-[11px]">
                            Payé
                          </span>
                        )}
                        {ps.status === 'DRAFT' && (
                          <span className="badge badge-neutral flex items-center gap-1 w-fit text-[11px]">
                            Brouillon
                          </span>
                        )}
                      </td>
                      <td className="text-xs text-gray-500">{ps.paymentDate || 'N/A'}</td>
                      <td className="text-right">
                        {validUuid ? (
                          <button
                            type="button"
                            onClick={() => handleDownload(ps)}
                            disabled={isDownloading}
                            className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1"
                          >
                            {isDownloading ? (
                              <>
                                <Loader2 className="w-3.5 h-3.5 animate-spin text-[#1A6B3C]" />
                                Chargement...
                              </>
                            ) : (
                              <>
                                <FileText className="w-3.5 h-3.5 text-[#1A6B3C]" />
                                Télécharger PDF
                              </>
                            )}
                          </button>
                        ) : (
                          <button
                            type="button"
                            disabled
                            title="Donnée de démonstration non persistée (non téléchargeable)"
                            className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1 opacity-50 cursor-not-allowed"
                          >
                            <FileText className="w-3.5 h-3.5 text-gray-400" />
                            Désactivé (Mock)
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
export default PayrollListPage;
