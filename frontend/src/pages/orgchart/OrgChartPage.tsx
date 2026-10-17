import React from 'react';
import { Network, Users, ChevronDown, Building, Mail } from 'lucide-react';
import type { OrgNode } from '@/types';

const mockOrgTree: OrgNode = {
  id: 'dg-1',
  name: 'Mamadou Ndiaye',
  title: 'Directeur Général',
  department: 'Direction Générale',
  children: [
    {
      id: 'drh-1',
      name: 'Fatou Bintou Sall',
      title: 'Directrice des Ressources Humaines',
      department: 'Ressources Humaines',
      children: [
        {
          id: 'rrh-1',
          name: 'Fatou Ndiaye',
          title: 'Responsable Recrutement & Carrières',
          department: 'Ressources Humaines',
        },
      ],
    },
    {
      id: 'cto-1',
      name: 'Cheikh Anta Seck',
      title: 'Directeur Technique (CTO)',
      department: 'Direction Technique',
      children: [
        {
          id: 'dev-1',
          name: 'Amadou Diop',
          title: 'Développeur Fullstack Senior (Lead)',
          department: 'Direction Technique',
          children: [
            {
              id: 'dev-2',
              name: 'Aïssatou Sow',
              title: 'Développeuse Frontend (CDD)',
              department: 'Direction Technique',
            },
          ],
        },
      ],
    },
    {
      id: 'daf-1',
      name: 'Ousmane Kane',
      title: 'Directeur Administratif & Financier',
      department: 'Finance & Comptabilité',
      children: [
        {
          id: 'cpt-1',
          name: 'Moussa Ba',
          title: 'Comptable Général',
          department: 'Finance & Comptabilité',
        },
      ],
    },
  ],
};

const TreeNode: React.FC<{ node: OrgNode }> = ({ node }) => {
  return (
    <div className="flex flex-col items-center">
      {/* Carte du collaborateur */}
      <div className="bg-white border-2 border-[#1A6B3C]/20 hover:border-[#1A6B3C] rounded-xl p-3 shadow-sm hover:shadow-md transition-all w-60 text-center relative z-10">
        <div className="w-10 h-10 rounded-full bg-[#1A6B3C] text-white font-bold flex items-center justify-center mx-auto mb-2 text-sm shadow">
          {node.name.split(' ').map((n: string) => n[0]).join('')}
        </div>
        <div className="font-bold text-xs text-gray-900 leading-tight">{node.name}</div>
        <div className="text-[11px] font-medium text-[#1A6B3C] mt-0.5">{node.title}</div>
        <div className="text-[10px] text-gray-500 mt-1 flex items-center justify-center gap-1">
          <Building className="w-3 h-3 text-gray-400" />
          <span>{node.department}</span>
        </div>
      </div>

      {/* Lignes hiérarchiques et sous-nœuds */}
      {node.children && node.children.length > 0 && (
        <div className="flex flex-col items-center">
          <div className="w-0.5 h-6 bg-[#1A6B3C]/30" />
          <div className="flex gap-6 relative">
            {node.children.map((child: OrgNode) => (
              <TreeNode key={child.id} node={child} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export const OrgChartPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="page-header">
        <div className="page-header-info">
          <h1>Organigramme de l'Entreprise</h1>
          <p>Hiérarchie managériale N+1 / N+2 et répartition structurelle des départements</p>
        </div>
      </div>

      <div className="card p-8 overflow-x-auto min-h-[600px] flex justify-center items-start bg-gradient-to-b from-gray-50/50 to-white">
        <TreeNode node={mockOrgTree} />
      </div>
    </div>
  );
};
export default OrgChartPage;
