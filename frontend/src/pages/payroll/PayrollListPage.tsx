import React from 'react';
import {
  FileSpreadsheet,
  Download,
  CheckCircle,
  FileText,
  DollarSign,
  TrendingDown,
  Building
} from 'lucide-react';
import type { Payslip } from '@/types';

const mockPayslips: Payslip[] = [
  {
    id: 'ps-1',
    employee: { id: 'e1', fullName: 'Amadou Diop', employeeNumber: 'EMP-001' },
    period: '2026-03',
    grossSalary: 1200000,
    netSalary: 985400,
    status: 'VALIDATED',
    generatedAt: '2026-03-25',
  },
  {
    id: 'ps-2',
    employee: { id: 'e2', fullName: 'Fatou Ndiaye', employeeNumber: 'EMP-002' },
    period: '2026-03',
    grossSalary: 950000,
    netSalary: 780200,
    status: 'VALIDATED',
    generatedAt: '2026-03-25',
  },
  {
    id: 'ps-3',
    employee: { id: 'e3', fullName: 'Moussa Ba', employeeNumber: 'EMP-003' },
    period: '2026-03',
    grossSalary: 750000,
    netSalary: 620100,
    status: 'PAID',
    generatedAt: '2026-03-25',
    paidAt: '2026-03-28',
  },
  {
    id: 'ps-4',
    employee: { id: 'e4', fullName: 'Aïssatou Sow', employeeNumber: 'EMP-004' },
    period: '2026-03',
    grossSalary: 550000,
    netSalary: 458900,
    status: 'DRAFT',
    generatedAt: '2026-03-25',
  },
];

export const PayrollListPage: React.FC = () => {
  const formatFCFA = (val: number) => {
    return new Intl.NumberFormat('fr-SN', { style: 'decimal' }).format(val) + ' FCFA';
  };

  return (
    <div className="space-y-6">
      <div className="page-header">
        <div className="page-header-info">
          <h1>Gestion de la Paie & Bulletins de Salaire</h1>
          <p>
            Édition mensuelle conforme : cotisations IPRES (5,6% / 8,4%) et CSS (7%)
          </p>
        </div>
        <div className="flex gap-2">
          <button type="button" className="btn btn-secondary text-xs">
            <Download className="w-4 h-4" />
            Télécharger Tous les PDF (ZIP)
          </button>
          <button type="button" className="btn btn-primary text-xs">
            <FileSpreadsheet className="w-4 h-4" />
            Générer la Paie de Mars 2026
          </button>
        </div>
      </div>

      {/* Cartes récapitulatives */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="stat-card primary">
          <div className="stat-icon primary">
            <DollarSign className="w-6 h-6" />
          </div>
          <div>
            <div className="stat-label">Masse Salariale Brute</div>
            <div className="stat-value text-xl">3 450 000 FCFA</div>
            <div className="text-[11px] text-gray-400 mt-0.5">Période : Mars 2026</div>
          </div>
        </div>

        <div className="stat-card accent">
          <div className="stat-icon accent">
            <TrendingDown className="w-6 h-6" />
          </div>
          <div>
            <div className="stat-label">Total Cotisations IPRES</div>
            <div className="stat-value text-xl">483 000 FCFA</div>
            <div className="text-[11px] text-gray-500 mt-0.5">Part Salariale + Patronale</div>
          </div>
        </div>

        <div className="stat-card info">
          <div className="stat-icon info">
            <Building className="w-6 h-6" />
          </div>
          <div>
            <div className="stat-label">Total Cotisations CSS</div>
            <div className="stat-value text-xl">241 500 FCFA</div>
            <div className="text-[11px] text-gray-500 mt-0.5">Prestations familiales & AT</div>
          </div>
        </div>
      </div>

      {/* Table des bulletins */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Bulletins de Paie du Mois (Mars 2026)</span>
          <span className="text-xs text-gray-400">Total : {mockPayslips.length} fiches</span>
        </div>
        <div className="table-container border-0">
          <table className="table">
            <thead>
              <tr>
                <th>Collaborateur</th>
                <th>Période</th>
                <th>Salaire Brut</th>
                <th>Salaire Net à Payer</th>
                <th>Statut</th>
                <th>Généré le</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {mockPayslips.map((ps) => (
                <tr key={ps.id}>
                  <td>
                    <div className="font-semibold text-xs text-gray-900">
                      {ps.employee.fullName}
                    </div>
                    <div className="text-[11px] text-gray-400 font-mono">
                      {ps.employee.employeeNumber}
                    </div>
                  </td>
                  <td>
                    <span className="text-xs font-mono font-medium text-gray-700">
                      {ps.period}
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
                    {ps.status === 'VALIDATED' && (
                      <span className="badge badge-success flex items-center gap-1 w-fit">
                        <CheckCircle className="w-3 h-3" />
                        Validé
                      </span>
                    )}
                    {ps.status === 'PAID' && (
                      <span className="badge badge-info flex items-center gap-1 w-fit">
                        Payé
                      </span>
                    )}
                    {ps.status === 'DRAFT' && (
                      <span className="badge badge-neutral flex items-center gap-1 w-fit">
                        Brouillon
                      </span>
                    )}
                  </td>
                  <td className="text-xs text-gray-500">{ps.generatedAt}</td>
                  <td className="text-right">
                    <a
                      href={`/api/v1/documents/payslip/${ps.id}`}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5 inline-flex items-center gap-1"
                    >
                      <FileText className="w-3.5 h-3.5 text-[#1A6B3C]" />
                      Télécharger PDF
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
export default PayrollListPage;
