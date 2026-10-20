import React, { useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ReactFlow,
  Background,
  Controls,
  Handle,
  Position,
  useNodesState,
  useEdgesState,
  type Edge,
  type Node,
  type NodeProps,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import dagre from '@dagrejs/dagre';
import { Building, Loader2, AlertTriangle, RefreshCw, RotateCcw, User, ExternalLink } from 'lucide-react';
import type { OrgNode } from '@/types';
import { useOrgChart } from '@/hooks/useOrgChart';

const NODE_WIDTH = 220;
const NODE_HEIGHT = 105;

type EmployeeNodeData = { org: OrgNode };
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
  graph.setGraph({ rankdir: 'TB', nodesep: 28, ranksep: 60 });
  graph.setDefaultEdgeLabel(() => ({}));

  const flat: OrgNode[] = [];
  const edges: Edge[] = [];

  const walk = (node: OrgNode, parentId?: string) => {
    flat.push(node);
    graph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT });
    if (parentId) {
      graph.setEdge(parentId, node.id);
      edges.push({
        id: `${parentId}-${node.id}`,
        source: parentId,
        target: node.id,
        type: 'smoothstep',
        style: { stroke: '#1A6B3C', strokeWidth: 1.5 },
      });
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
      data: { org },
      draggable: true,
    };
  });

  return { nodes, edges };
}

const EmployeeNodeCard: React.FC<NodeProps<EmployeeFlowNode>> = ({ data }) => {
  const { org } = data;
  const navigate = useNavigate();

  return (
    <div
      className="bg-white rounded-xl p-3 shadow-xs hover:shadow-md text-center border-2 border-[#1A6B3C]/20 hover:border-[#1A6B3C] transition-shadow cursor-grab active:cursor-grabbing group select-none"
      style={{ width: NODE_WIDTH }}
      title={`Glisser pour réorganiser · Cliquer sur 'Accéder au dossier' pour voir la fiche de ${org.name}`}
    >
      <Handle type="target" position={Position.Top} className="!opacity-0" />
      <div className="w-9 h-9 rounded-full bg-[#1A6B3C] text-white font-bold flex items-center justify-center mx-auto mb-1 text-xs shadow-2xs">
        {initialsOf(org.name)}
      </div>
      <div className="font-bold text-xs text-gray-900 leading-tight truncate group-hover:text-[#1A6B3C] transition-colors">
        {org.name}
      </div>
      <div className="text-[11px] font-medium text-[#1A6B3C] mt-0.5 truncate">{org.title}</div>
      <div className="text-[10px] text-gray-500 mt-0.5 flex items-center justify-center gap-1">
        <Building className="w-3 h-3 text-gray-400 shrink-0" />
        <span className="truncate">{org.department}</span>
      </div>

      <div className="mt-1.5 pt-1.5 border-t border-gray-100 flex items-center justify-center">
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/employees/${org.id}`);
          }}
          className="nodrag inline-flex items-center gap-1 px-2.5 py-1 rounded text-[10px] font-semibold text-[#1A6B3C] bg-[#1A6B3C]/5 hover:bg-[#1A6B3C]/15 transition-colors cursor-pointer group-hover:underline"
        >
          <User className="w-3 h-3" />
          <span>Accéder au dossier</span>
          <ExternalLink className="w-2.5 h-2.5" />
        </button>
      </div>
      <Handle type="source" position={Position.Bottom} className="!opacity-0" />
    </div>
  );
};

const nodeTypes = { employee: EmployeeNodeCard };

export const OrgChartPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: roots, isLoading, isError, refetch, isFetching } = useOrgChart();
  const [nodes, setNodes, onNodesChange] = useNodesState<EmployeeFlowNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);

  useEffect(() => {
    if (roots && roots.length > 0) {
      const layout = buildLayout(roots);
      setNodes(layout.nodes);
      setEdges(layout.edges);
    } else {
      setNodes([]);
      setEdges([]);
    }
  }, [roots, setNodes, setEdges]);

  const handleResetLayout = useCallback(() => {
    if (roots && roots.length > 0) {
      const layout = buildLayout(roots);
      setNodes(layout.nodes);
      setEdges(layout.edges);
    }
  }, [roots, setNodes, setEdges]);

  const handleNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      navigate(`/employees/${node.id}`);
    },
    [navigate],
  );

  return (
    <div className="space-y-4">
      <div className="page-header flex flex-wrap items-center justify-between gap-4">
        <div className="page-header-info">
          <h1>Organigramme interactif de l'entreprise</h1>
          <p>
            Glissez-déposez les blocs pour modifier et écarter la structure. Cliquez sur « Accéder au dossier » pour ouvrir la fiche.
          </p>
        </div>
        {roots && roots.length > 0 && (
          <button
            type="button"
            onClick={handleResetLayout}
            className="btn btn-secondary text-xs flex items-center gap-1.5 cursor-pointer"
            title="Réorganiser automatiquement l'arbre hiérarchique"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Réorganiser automatiquement</span>
          </button>
        )}
      </div>

      <div className="card overflow-hidden" style={{ height: 'calc(100dvh - 220px)', minHeight: 460 }}>
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

        {!isLoading && !isError && nodes.length === 0 && (
          <div className="h-full flex items-center justify-center text-gray-500 text-sm">
            Aucun collaborateur actif dans l'organigramme.
          </div>
        )}

        {!isLoading && !isError && nodes.length > 0 && (
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeClick={handleNodeClick}
            nodesDraggable={true}
            nodesConnectable={false}
            elementsSelectable={true}
            minZoom={0.1}
            maxZoom={1.5}
            fitView
            fitViewOptions={{ padding: 0.2 }}
          >
            <Background gap={20} color="#E2E8F0" />
            <Controls showInteractive={false} />
          </ReactFlow>
        )}
      </div>
    </div>
  );
};

export default OrgChartPage;