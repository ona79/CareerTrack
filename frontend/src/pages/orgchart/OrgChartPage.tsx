import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ReactFlow,
  Background,
  Controls,
  Handle,
  Position,
  type Edge,
  type Node,
  type NodeProps,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import dagre from '@dagrejs/dagre';
import { Building, Loader2, AlertTriangle, RefreshCw, X, User, ExternalLink } from 'lucide-react';
import type { OrgNode } from '@/types';
import { useOrgChart } from '@/hooks/useOrgChart';

const NODE_WIDTH = 220;
const NODE_HEIGHT = 105;

type EmployeeNodeData = { org: OrgNode; isSelected: boolean };
type EmployeeFlowNode = Node<EmployeeNodeData, 'employee'>;

function initialsOf(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

function buildLayout(roots: OrgNode[]): { nodes: EmployeeFlowNode[]; edges: Edge[] } {
  const graph = new dagre.graphlib.Graph();
  graph.setGraph({ rankdir: 'TB', nodesep: 24, ranksep: 56 });
  graph.setDefaultEdgeLabel(() => ({}));

  const flat: OrgNode[] = [];
  const edges: Edge[] = [];

  const walk = (node: OrgNode, parentId?: string) => {
    flat.push(node);
    graph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT });
    if (parentId) {
      graph.setEdge(parentId, node.id);
      edges.push({ id: `${parentId}-${node.id}`, source: parentId, target: node.id, type: 'smoothstep' });
    }
    node.children?.forEach((child) => walk(child, node.id));
  };
  roots.forEach((root) => walk(root));

  dagre.layout(graph);

  const nodes: EmployeeFlowNode[] = flat.map((org) => {
    const pos = graph.node(org.id);
    return {
      id: org.id,
      type: 'employee',
      position: { x: pos.x - NODE_WIDTH / 2, y: pos.y - NODE_HEIGHT / 2 },
      data: { org, isSelected: false },
    };
  });

  return { nodes, edges };
}

const EmployeeNodeCard: React.FC<NodeProps<EmployeeFlowNode>> = ({ data }) => {
  const { org, isSelected } = data;
  return (
    <div
      className={`bg-white rounded-xl p-3 shadow-xs text-center border-2 hover:border-[#1A6B3C] hover:shadow-md transition-all cursor-pointer group ${
        isSelected ? 'border-[#1A6B3C] ring-2 ring-[#1A6B3C]/20' : 'border-[#1A6B3C]/20'
      }`}
      style={{ width: NODE_WIDTH }}
      title="Cliquez pour ouvrir le dossier collaborateur"
    >
      <Handle type="target" position={Position.Top} style={{ opacity: 0 }} />
      <div className="w-9 h-9 rounded-full bg-[#1A6B3C] text-white font-bold flex items-center justify-center mx-auto mb-1 text-xs shadow-2xs">
        {initialsOf(org.name)}
      </div>
      <div className="font-bold text-xs text-gray-900 leading-tight truncate group-hover:text-[#1A6B3C] transition-colors">{org.name}</div>
      <div className="text-[11px] font-medium text-[#1A6B3C] mt-0.5 truncate">{org.title}</div>
      <div className="text-[10px] text-gray-500 mt-0.5 flex items-center justify-center gap-1">
        <Building className="w-3 h-3 text-gray-400 shrink-0" />
        <span className="truncate">{org.department}</span>
      </div>
      <div className="mt-1.5 pt-1.5 border-t border-gray-100 flex items-center justify-center gap-1 text-[10px] font-semibold text-[#1A6B3C] group-hover:underline">
        <User className="w-3 h-3" />
        <span>Accéder au dossier</span>
        <ExternalLink className="w-2.5 h-2.5" />
      </div>
      <Handle type="source" position={Position.Bottom} style={{ opacity: 0 }} />
    </div>
  );
};

const nodeTypes = { employee: EmployeeNodeCard };

export const OrgChartPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: roots, isLoading, isError, refetch, isFetching } = useOrgChart();
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const layout = useMemo(() => buildLayout(roots ?? []), [roots]);

  const nodes = useMemo(
    () => layout.nodes.map((n) => ({ ...n, data: { ...n.data, isSelected: n.id === selectedId } })),
    [layout, selectedId],
  );

  const selected = useMemo(
    () => layout.nodes.find((n) => n.id === selectedId)?.data.org ?? null,
    [layout, selectedId],
  );

  const handleNodeClick = (_: React.MouseEvent, node: Node) => {
    // Clic pour accéder directement au dossier de l'employé (exigence cahier des charges)
    navigate(`/employees/${node.id}`);
  };

  return (
    <div className="space-y-4">
      <div className="page-header">
        <div className="page-header-info">
          <h1>Organigramme interactif de l'entreprise</h1>
          <p>Cliquez sur n'importe quel collaborateur pour accéder directement à son dossier RH. Pincez ou utilisez la molette pour zoomer.</p>
        </div>
      </div>

      <div className="card overflow-hidden" style={{ height: 'calc(100dvh - 240px)', minHeight: 420 }}>
        {isLoading && (
          <div className="h-full flex items-center justify-center gap-2 text-gray-500">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Chargement de l'organigramme...</span>
          </div>
        )}

        {isError && (
          <div className="h-full flex flex-col items-center justify-center gap-3 text-center p-6">
            <AlertTriangle className="w-8 h-8 text-red-500" />
            <p className="text-sm text-red-600">Impossible de charger l'organigramme.</p>
            <button
              type="button"
              onClick={() => refetch()}
              disabled={isFetching}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#1A6B3C] text-white text-sm cursor-pointer"
            >
              <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
              Réessayer
            </button>
          </div>
        )}

        {!isLoading && !isError && layout.nodes.length === 0 && (
          <div className="h-full flex items-center justify-center text-gray-500 text-sm">
            Aucun collaborateur actif dans l'organigramme.
          </div>
        )}

        {!isLoading && !isError && layout.nodes.length > 0 && (
          <ReactFlow
            nodes={nodes}
            edges={layout.edges}
            nodeTypes={nodeTypes}
            onNodeClick={handleNodeClick}
            onPaneClick={() => setSelectedId(null)}
            nodesDraggable={false}
            nodesConnectable={false}
            minZoom={0.1}
            maxZoom={1.5}
            fitView
            fitViewOptions={{ padding: 0.2 }}
          >
            <Background gap={20} />
            <Controls showInteractive={false} />
          </ReactFlow>
        )}
      </div>

      {selected && (
        <div className="card p-4 flex items-center justify-between gap-3 bg-white border border-gray-200">
          <div className="min-w-0">
            <div className="font-bold text-gray-900">{selected.name}</div>
            <div className="text-sm text-[#1A6B3C]">{selected.title}</div>
            <div className="text-xs text-gray-500 mt-0.5">
              {selected.department}
              {selected.employeeNumber ? ` · Matricule ${selected.employeeNumber}` : ''}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => navigate(`/employees/${selected.id}`)}
              className="btn btn-primary text-xs flex items-center gap-1.5 px-3 py-1.5 cursor-pointer shadow-xs"
            >
              <User className="w-3.5 h-3.5" />
              <span>Voir le dossier complet</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </button>
            <button
              type="button"
              onClick={() => setSelectedId(null)}
              aria-label="Fermer"
              className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default OrgChartPage;