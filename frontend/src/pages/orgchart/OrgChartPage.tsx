import React, { useMemo, useState } from 'react';
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
import { Building, Loader2, AlertTriangle, RefreshCw, X } from 'lucide-react';
import type { OrgNode } from '@/types';
import { useOrgChart } from '@/hooks/useOrgChart';

const NODE_WIDTH = 220;
const NODE_HEIGHT = 92;

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
      className={`bg-white rounded-xl p-3 shadow-sm text-center border-2 hover:border-[#1A6B3C] transition-all cursor-pointer ${
        isSelected ? 'border-[#1A6B3C]' : 'border-[#1A6B3C]/20'
      }`}
      style={{ width: NODE_WIDTH }}
    >
      <Handle type="target" position={Position.Top} style={{ opacity: 0 }} />
      <div className="w-9 h-9 rounded-full bg-[#1A6B3C] text-white font-bold flex items-center justify-center mx-auto mb-1 text-xs">
        {initialsOf(org.name)}
      </div>
      <div className="font-bold text-xs text-gray-900 leading-tight truncate">{org.name}</div>
      <div className="text-[11px] font-medium text-[#1A6B3C] mt-0.5 truncate">{org.title}</div>
      <div className="text-[10px] text-gray-500 mt-0.5 flex items-center justify-center gap-1">
        <Building className="w-3 h-3 text-gray-400" />
        <span className="truncate">{org.department}</span>
      </div>
      <Handle type="source" position={Position.Bottom} style={{ opacity: 0 }} />
    </div>
  );
};

const nodeTypes = { employee: EmployeeNodeCard };

export const OrgChartPage: React.FC = () => {
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

  return (
    <div className="space-y-4">
      <div className="page-header">
        <div className="page-header-info">
          <h1>Organigramme de l'entreprise</h1>
          <p>Touchez un collaborateur pour voir sa fiche. Pincez ou utilisez la molette pour zoomer.</p>
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
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#1A6B3C] text-white text-sm"
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
            onNodeClick={(_, node) => setSelectedId(node.id)}
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
        <div className="card p-4 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <div className="font-bold text-gray-900">{selected.name}</div>
            <div className="text-sm text-[#1A6B3C]">{selected.title}</div>
            <div className="text-xs text-gray-500 mt-1">
              {selected.department}
              {selected.employeeNumber ? ` · Matricule ${selected.employeeNumber}` : ''}
            </div>
          </div>
          <button
            type="button"
            onClick={() => setSelectedId(null)}
            aria-label="Fermer"
            className="p-1 rounded hover:bg-gray-100"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
};

export default OrgChartPage;