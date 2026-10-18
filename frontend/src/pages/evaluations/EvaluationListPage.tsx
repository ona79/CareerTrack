import React, { useState } from 'react';
import { Award, Plus, Calendar, CheckCircle2, Download, Target, ChevronRight } from 'lucide-react';
import type { EvaluationCampaign } from '@/types';

interface EvaluationWithObjectives {
  id: string;
  employeeFullName: string;
  evaluatorFullName: string;
  campaignTitle: string;
  finalScore: number;
  status: string;
  interviewDate: string;
  objectives: {
    title: string;
    description: string;
    weight: number;
    achievementRate: number;
    kpi: string;
  }[];
}

const mockCampaigns: EvaluationCampaign[] = [
  {
    id: 'camp-1',
    title: 'Campagne Annuelle d\'Évaluation 2025',
    year: 2025,
    startDate: '2026-01-05',
    endDate: '2026-02-28',
    status: 'CLOSED',
    description: 'Bilan de performance annuel, fixation des objectifs 2026 et souhaits d\'évolution',
  },
  {
    id: 'camp-2',
    title: 'Évaluations Mi-Parcours 2026',
    year: 2026,
    startDate: '2026-06-01',
    endDate: '2026-07-15',
    status: 'PLANNING',
    description: 'Point d\'étape semestriel sur l\'atteinte des objectifs opérationnels',
  },
];

const mockEvaluations: EvaluationWithObjectives[] = [
  {
    id: 'eval-1',
    employeeFullName: 'Amadou Diop',
    evaluatorFullName: 'Cheikh Anta Seck (CTO)',
    campaignTitle: 'Campagne Annuelle 2025',
    finalScore: 4.6,
    status: 'VALIDATED',
    interviewDate: '2026-02-15',
    objectives: [
      {
        title: 'Mise en place de la stack Spring Boot 3 & architecture',
        description: 'Spécification, socle technique et CI/CD',
        weight: 40,
        achievementRate: 100,
        kpi: 'Build propre, couverture >= 60%',
      },
      {
        title: 'Mentorat des développeurs juniors',
        description: 'Revue de code hebdomadaire et ateliers techniques',
        weight: 30,
        achievementRate: 90,
        kpi: '2 revues par semaine',
      },
      {
        title: 'Optimisation des performances d\'accès PostgreSQL',
        description: 'Indexation et réduction des temps de requête Flyway',
        weight: 30,
        achievementRate: 95,
        kpi: '< 50ms par requête standard',
      },
    ],
  },
];

export const EvaluationListPage: React.FC = () => {
  const [selectedEval, setSelectedEval] = useState<EvaluationWithObjectives | null>(mockEvaluations[0]);

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Évaluations Annuelles & Entretiens N+1</h1>
          <p>Notation de la performance, objectifs SMART mesurables et compte-rendu officiel PDF</p>
        </div>
        <button type="button" className="btn btn-primary text-xs w-full sm:w-auto">
          <Plus className="w-4 h-4" />
          Lancer une Campagne
        </button>
      </div>

      {/* Campagnes */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {mockCampaigns.map((camp) => (
          <div key={camp.id} className="card p-5">
            <div className="flex items-start justify-between gap-2 mb-2">
              <div className="font-bold text-sm text-gray-900">{camp.title}</div>
              {camp.status === 'CLOSED' && <span className="badge badge-success">Clôturée</span>}
              {camp.status === 'PLANNING' && <span className="badge badge-warning">Planifiée</span>}
            </div>
            <p className="text-xs text-gray-500 mb-4">{camp.description}</p>
            <div className="flex items-center justify-between text-xs text-gray-400 pt-3 border-t border-gray-100">
              <div className="flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5" />
                <span>Du {camp.startDate} au {camp.endDate}</span>
              </div>
              <span className="badge badge-primary text-[10px]">Année {camp.year}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Détail d'une évaluation avec objectifs SMART */}
      {selectedEval && (
        <div className="card">
          <div className="card-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
            <div>
              <div className="flex items-center gap-2">
                <Target className="w-4 h-4 text-[#1A6B3C]" />
                <span className="card-title">
                  Objectifs SMART & Synthèse d'Entretien : {selectedEval.employeeFullName}
                </span>
              </div>
              <p className="text-xs text-gray-400 mt-0.5">
                Évaluateur : {selectedEval.evaluatorFullName} | Entretien tenu le {selectedEval.interviewDate}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span className="badge badge-success text-xs font-bold px-2.5 py-1">
                Score Global : {selectedEval.finalScore} / 5
              </span>
              {/* Bouton de téléchargement du PDF officiel (désactivé jusqu'au lot 4) */}
              <button
                type="button"
                disabled
                title="Disponible prochainement"
                className="btn btn-secondary btn-sm text-xs inline-flex items-center gap-1.5 opacity-60 cursor-not-allowed"
              >
                <Download className="w-4 h-4 text-gray-400" />
                Télécharger Bilan Entretien (PDF)
              </button>
            </div>
          </div>

          <div className="p-5 space-y-4">
            <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider">
              Grille des Objectifs SMART
            </h3>
            <div className="space-y-3">
              {selectedEval.objectives.map((obj, idx) => (
                <div key={idx} className="p-3.5 rounded-lg border border-gray-200 bg-gray-50/50 space-y-2">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="font-semibold text-xs text-gray-900">{obj.title}</div>
                      <div className="text-[11px] text-gray-500">{obj.description}</div>
                    </div>
                    <div className="text-right">
                      <span className="badge badge-primary text-[10px] font-mono">
                        Poids : {obj.weight}%
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-xs pt-2 border-t border-gray-100">
                    <span className="text-[11px] text-gray-500">
                      <strong>Indicateur KPI :</strong> {obj.kpi}
                    </span>
                    <span className="font-bold text-xs text-[#1A6B3C]">
                      Atteinte : {obj.achievementRate}%
                    </span>
                  </div>

                  {/* Barre de progression SMART */}
                  <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                    <div
                      className="bg-[#1A6B3C] h-1.5 rounded-full"
                      style={{ width: `${obj.achievementRate}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
export default EvaluationListPage;
