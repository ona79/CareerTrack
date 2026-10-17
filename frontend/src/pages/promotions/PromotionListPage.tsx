import React, { useState } from 'react';
import {
  TrendingUp,
  Plus,
  CheckCircle,
  Clock,
  ArrowRight,
  Calculator,
  UserCheck
} from 'lucide-react';
import type { PromotionProposalDto } from '@/types';

const mockPromotions: any[] = [
  {
    id: 'pr-1',
    employeeFullName: 'Amadou Diop',
    currentPositionTitle: 'Développeur Fullstack Senior',
    proposedPositionTitle: 'Lead Architecte Technique',
    currentSalary: 1200000,
    proposedSalary: 1500000,
    percentageIncrease: 25.0,
    status: 'SUBMITTED',
    justification: 'Excellente gestion du socle technique et mentorat des profils juniors.',
  },
  {
    id: 'pr-2',
    employeeFullName: 'Moussa Ba',
    currentPositionTitle: 'Comptable Général',
    proposedPositionTitle: 'Chef Comptable & Trésorerie',
    currentSalary: 750000,
    proposedSalary: 900000,
    percentageIncrease: 20.0,
    status: 'APPROVED',
    justification: 'Clôture sans réserve des comptes annuels et gestion optimale des télédéclarations IPRES/CSS.',
  },
];

export const PromotionListPage: React.FC = () => {
  const [promotions, setPromotions] = useState<any[]>(mockPromotions);

  const formatFCFA = (val: number) => {
    return new Intl.NumberFormat('fr-SN', { style: 'decimal' }).format(val) + ' FCFA';
  };

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Promotions & Revalorisations Salariales</h1>
          <p>Gestion des montées en compétences, simulations d'impact masse salariale et validations</p>
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <button type="button" className="btn btn-secondary text-xs flex-1 sm:flex-none">
            <Calculator className="w-4 h-4" />
            Simulateur Masse Salariale
          </button>
          <button type="button" className="btn btn-primary text-xs flex-1 sm:flex-none">
            <Plus className="w-4 h-4" />
            Proposer une Promotion
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {promotions.map((p) => (
          <div key={p.id} className="card p-5 space-y-4">
            <div className="flex items-start justify-between">
              <div>
                <span className="font-bold text-sm text-gray-900">{p.employeeFullName}</span>
                <div className="text-xs text-gray-500 mt-0.5">{p.currentPositionTitle}</div>
              </div>
              {p.status === 'SUBMITTED' && (
                <span className="badge badge-warning flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  En attente Direction
                </span>
              )}
              {p.status === 'APPROVED' && (
                <span className="badge badge-success flex items-center gap-1">
                  <CheckCircle className="w-3 h-3" />
                  Validé
                </span>
              )}
            </div>

            {/* Comparatif poste & salaire */}
            <div className="bg-gray-50 p-3 rounded-lg border border-gray-100 flex items-center justify-between text-xs">
              <div>
                <div className="text-gray-400 text-[11px]">Poste Cible</div>
                <div className="font-semibold text-gray-800">{p.proposedPositionTitle}</div>
              </div>
              <ArrowRight className="w-4 h-4 text-gray-400" />
              <div className="text-right">
                <div className="text-gray-400 text-[11px]">Nouveau Salaire (+{p.percentageIncrease}%)</div>
                <div className="font-bold text-[#1A6B3C]">{formatFCFA(p.proposedSalary)}</div>
              </div>
            </div>

            <p className="text-xs text-gray-600 bg-amber-50/50 p-2.5 rounded border border-amber-100">
              <span className="font-semibold text-gray-800">Justification : </span>
              {p.justification}
            </p>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-gray-100">
              <button type="button" className="btn btn-secondary btn-sm text-[11px] py-1">
                Détails simulation
              </button>
              {p.status === 'SUBMITTED' && (
                <button type="button" className="btn btn-primary btn-sm text-[11px] py-1">
                  Valider la promotion
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
export default PromotionListPage;
