import React from 'react';
import { GraduationCap, Plus, Clock, Users, BookOpen } from 'lucide-react';
import type { Training } from '@/types';

const mockTrainings: Training[] = [
  {
    id: 'tr-1',
    code: 'TECH-SPB',
    title: 'Architecture Microservices avec Spring Boot 3 & Docker',
    durationHours: 35,
    provider: 'Cabinet SupInfo Sénégal',
    category: 'Technique',
    description: 'Conception d\'APIs REST sécurisées, conteneurisation et CI/CD',
  },
  {
    id: 'tr-2',
    code: 'MGT-LEAD',
    title: 'Management Bienveillant & Leadership d\'Équipe PME',
    durationHours: 20,
    provider: 'Institut Sénégalais de Management (ISM)',
    category: 'Management',
    description: 'Postures managériales, animation des entretiens et résolution de conflits',
  },
  {
    id: 'tr-3',
    code: 'RH-DROIT',
    title: 'Pratique du Droit du Travail Sénégalais & Contentieux',
    durationHours: 16,
    provider: 'Cabinet Juridique & Social Dakar',
    category: 'Juridique / RH',
    description: 'Ruptures conventionnelles, congés, déclarations IPRES et CSS',
  },
];

export const TrainingListPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="page-header">
        <div className="page-header-info">
          <h1>Catalogue & Plans de Formation</h1>
          <p>Développement continu des compétences et montées en qualification des équipes</p>
        </div>
        <button type="button" className="btn btn-primary text-xs">
          <Plus className="w-4 h-4" />
          Ajouter une Formation
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {mockTrainings.map((tr) => (
          <div key={tr.id} className="card p-5 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="badge badge-primary font-mono text-[10px]">{tr.code}</span>
                <span className="badge badge-neutral text-[10px]">{tr.category}</span>
              </div>
              <h3 className="font-bold text-sm text-gray-900 mb-2 leading-snug">{tr.title}</h3>
              <p className="text-xs text-gray-500 mb-4">{tr.description}</p>
            </div>

            <div className="pt-3 border-t border-gray-100 flex items-center justify-between text-xs text-gray-500">
              <div className="flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 text-gray-400" />
                <span>{tr.durationHours} heures</span>
              </div>
              <span className="text-[11px] font-semibold text-[#1A6B3C] truncate max-w-[120px]">
                {tr.provider}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
export default TrainingListPage;
