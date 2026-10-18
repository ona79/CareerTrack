import React from 'react';
import { Building, Loader2, AlertTriangle, RefreshCw } from 'lucide-react';
import type { OrgNode } from '@/types';
import { useOrgChart } from '@/hooks/useOrgChart';

const TreeNode: React.FC<{ node: OrgNode }> = ({ node }) => {
  return (
    <div className="flex flex-col items-center">
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
  const { data: roots, isLoading, isError, refetch } = useOrgChart();

  return (
    <div className="space-y-6">
      <div className="page-header">
        <div className="page-header-info">
          <h1>Organigramme de l'Entreprise</h1>
          <p>Hiérarchie managériale N+1 / N+2 et répartition structurelle des départements</p>
        </div>
      </div>

      <div className="card p-8 overflow-x-auto min-h-[600px] flex justify-center items-start bg-gradient-to-b from-gray-50/50 to-white">
        {isLoading && (
          <div className="flex items-center gap-2 text-gray-500 text-sm">
            <Loader2 className="w-5 h-5 animate-spin text-[#1A6B3C]" />
            <span>Chargement de l'organigramme...</span>
          </div>
        )}

        {isError && (
          <div className="flex flex-col items-center gap-3 text-center">
            <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <p className="text-sm text-gray-600">
              Impossible de charger l'organigramme.
            </p>
            <button
              type="button"
              onClick={() => refetch()}
              className="btn btn-primary text-xs flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              Réessayer
            </button>
          </div>
        )}

        {!isLoading && !isError && (!roots || roots.length === 0) && (
          <div className="text-gray-500 text-sm text-center">
            Aucun employé dans l'organigramme.
            <br />
            <span className="text-xs text-gray-400">
              (Vérifiez qu'au moins un employé a un manager renseigné.)
            </span>
          </div>
        )}

        {!isLoading && !isError && roots && roots.length > 0 && (
          <div className="flex flex-col items-center gap-8">
            {roots.map((root) => (
              <TreeNode key={root.id} node={root} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default OrgChartPage;