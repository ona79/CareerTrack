import React, { useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import {
  ReactFlow,
  Controls,
  MiniMap,
  Background,
  useNodesState,
  useEdgesState,
  Handle,
  Position,
  useReactFlow,
  ReactFlowProvider,
  type Node,
  type Edge
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import dagre from '@dagrejs/dagre';
import {
  Network,
  Users,
  Building,
  Mail,
  Search,
  RefreshCw,
  AlertCircle,
  ExternalLink,
  ChevronDown,
  X,
  UserCheck
} from 'lucide-react';
import type { OrgNodeDto, ApiResponse } from '@/types';

const NODE_WIDTH = 250;
const NODE_HEIGHT = 120;

// Disposition hiérarchique avec Dagre
const getLayoutedElements = (nodes: Node[], edges: Edge[]) => {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));
  dagreGraph.setGraph({ rankdir: 'TB', nodesep: 40, ranksep: 60 });

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  const layoutedNodes = nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - NODE_WIDTH / 2,
        y: nodeWithPosition.y - NODE_HEIGHT / 2
      }
    };
  });

  return { nodes: layoutedNodes, edges };
};

// Composant Nœud Collaborateur Personnalisé
interface CustomNodeData {
  id: string;
  name: string;
  employeeNumber?: string;
  title: string;
  department: string;
  email?: string;
  subordinateCount?: number;
  onNodeClick: (data: CustomNodeData) => void;
}

const EmployeeCustomNode: React.FC<{ data: CustomNodeData }> = ({ data }) => {
  const initials = data.name
    ? data.name
        .split(' ')
        .map((n) => n[0])
        .join('')
        .slice(0, 2)
    : 'RH';

  return (
    <div
      onClick={() => data.onNodeClick(data)}
      className="bg-white border-2 border-emerald-700/20 hover:border-[#1A6B3C] rounded-2xl p-3 shadow-md hover:shadow-xl transition-all w-[250px] cursor-pointer group text-left relative select-none"
    >
      <Handle type="target" position={Position.Top} className="!bg-[#1A6B3C] !w-2.5 !h-2.5" />

      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#1A6B3C] to-[#15602f] text-white font-bold flex items-center justify-center text-xs shrink-0 shadow-sm">
          {initials}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-1">
            <h4 className="font-bold text-xs text-gray-900 truncate group-hover:text-[#1A6B3C] transition-colors">
              {data.name}
            </h4>
          </div>
          <p className="text-[11px] font-medium text-[#1A6B3C] truncate leading-tight mt-0.5">
            {data.title}
          </p>
          <div className="text-[10px] text-gray-500 flex items-center gap-1 mt-1 truncate">
            <Building className="w-3 h-3 text-gray-400 shrink-0" />
            <span className="truncate">{data.department}</span>
          </div>
        </div>
      </div>

      <div className="mt-2.5 pt-2 border-t border-gray-100 flex items-center justify-between text-[10px]">
        {data.subordinateCount != null && data.subordinateCount > 0 ? (
          <span className="badge badge-success text-[10px] py-0 px-2 font-semibold">
            {data.subordinateCount} subordonné{data.subordinateCount > 1 ? 's' : ''}
          </span>
        ) : (
          <span className="text-gray-400 italic">Collaborateur</span>
        )}

        <span className="text-[#1A6B3C] font-semibold flex items-center gap-0.5 group-hover:underline">
          Dossier
          <ExternalLink className="w-2.5 h-2.5" />
        </span>
      </div>

      <Handle type="source" position={Position.Bottom} className="!bg-[#1A6B3C] !w-2.5 !h-2.5" />
    </div>
  );
};

const nodeTypes = {
  employeeNode: EmployeeCustomNode
};

// Composant interne utilisant les hooks de React Flow
const OrgChartFlow: React.FC<{
  orgTree: OrgNodeDto[];
  onSelectEmployee: (node: CustomNodeData) => void;
}> = ({ orgTree, onSelectEmployee }) => {
  const { fitView, setCenter } = useReactFlow();
  const [searchQuery, setSearchQuery] = useState('');

  // Aplatissement récursif de l'arbre en nœuds et arêtes
  const { nodes: initialNodes, edges: initialEdges } = useMemo(() => {
    const nodes: Node[] = [];
    const edges: Edge[] = [];

    const traverse = (item: OrgNodeDto, parentId?: string) => {
      nodes.push({
        id: item.id,
        type: 'employeeNode',
        data: {
          id: item.id,
          name: item.name,
          employeeNumber: item.employeeNumber,
          title: item.title,
          department: item.department,
          email: item.email,
          subordinateCount: item.subordinateCount,
          onNodeClick: onSelectEmployee
        },
        position: { x: 0, y: 0 }
      });

      if (parentId) {
        edges.push({
          id: `e-${parentId}-${item.id}`,
          source: parentId,
          target: item.id,
          type: 'smoothstep',
          style: { stroke: '#1A6B3C', strokeWidth: 2 }
        });
      }

      if (item.subordinates && item.subordinates.length > 0) {
        item.subordinates.forEach((child) => traverse(child, item.id));
      }
    };

    orgTree.forEach((root) => traverse(root));
    return getLayoutedElements(nodes, edges);
  }, [orgTree, onSelectEmployee]);

  const [nodes, , onNodesChange] = useNodesState(initialNodes);
  const [edges, , onEdgesChange] = useEdgesState(initialEdges);

  // Recherche d'un collaborateur et centrage automatique
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    const term = searchQuery.trim().toLowerCase();
    const targetNode = nodes.find((n) => {
      const data = n.data as unknown as CustomNodeData;
      return (
        data.name.toLowerCase().includes(term) ||
        data.title.toLowerCase().includes(term) ||
        (data.employeeNumber && data.employeeNumber.toLowerCase().includes(term))
      );
    });

    if (targetNode) {
      setCenter(targetNode.position.x + NODE_WIDTH / 2, targetNode.position.y + NODE_HEIGHT / 2, {
        zoom: 1.2,
        duration: 800
      });
    }
  };

  return (
    <div className="relative w-full h-[650px] bg-slate-50 rounded-2xl border border-gray-200 shadow-inner overflow-hidden">
      {/* Barre de recherche et actions au-dessus du canvas */}
      <div className="absolute top-4 left-4 z-10 flex flex-wrap items-center gap-2 max-w-sm w-full">
        <form onSubmit={handleSearch} className="flex items-center gap-1.5 w-full bg-white/90 backdrop-blur p-1.5 rounded-xl shadow border border-gray-200">
          <Search className="w-4 h-4 text-gray-400 ml-2" />
          <input
            type="text"
            placeholder="Rechercher dans l'organigramme..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input input-sm border-0 focus:ring-0 text-xs w-full bg-transparent"
          />
          <button type="submit" className="btn btn-primary btn-sm text-xs py-1 px-3">
            Localiser
          </button>
        </form>
      </div>

      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        fitView
        minZoom={0.2}
        maxZoom={2}
        panOnDrag
        zoomOnPinch
        zoomOnDoubleClick
      >
        <Background color="#cbd5e1" gap={20} size={1} />
        <Controls className="!bg-white !border-gray-200 !shadow-md !rounded-xl !p-1" />
        <MiniMap
          nodeColor="#1A6B3C"
          maskColor="rgba(240, 242, 245, 0.7)"
          className="!hidden sm:!block !border !border-gray-200 !rounded-xl !bg-white !shadow-md"
        />
      </ReactFlow>
    </div>
  );
};

export const OrgChartPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();
  const [selectedNode, setSelectedNode] = useState<CustomNodeData | null>(null);

  const isDrh = hasRole('ROLE_DRH');
  const isManager = hasRole('ROLE_MANAGER');

  // Récupération de l'organigramme réel depuis le backend
  const {
    data: orgTree,
    isLoading,
    isError,
    refetch
  } = useQuery<OrgNodeDto[]>({
    queryKey: ['orgchart-tree'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<OrgNodeDto[]>>('/orgchart/tree');
      return res.data.data;
    }
  });

  // Gestion du clic sur un nœud
  const handleNodeClick = useCallback(
    (node: CustomNodeData) => {
      const isSelf = user?.employeeId === node.id;
      // Si DRH ou soi-même : accès direct au dossier complet
      if (isDrh || isSelf) {
        navigate(`/employees/${node.id}`);
        return;
      }

      // Si Manager : ouvrir la modale pour proposer la navigation ou afficher la fiche
      // afin d'éviter un 403 intempestif si hors équipe
      setSelectedNode(node);
    },
    [isDrh, user, navigate]
  );

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 py-4">
      {/* En-tête */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2.5">
            <Network className="w-7 h-7 text-[#1A6B3C]" />
            Organigramme de l'Entreprise
          </h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Arbre hiérarchique temps réel, relations managers et équipes
          </p>
        </div>

        <button
          onClick={() => refetch()}
          className="btn btn-secondary inline-flex items-center gap-2 text-xs min-h-[44px] self-start sm:self-auto"
        >
          <RefreshCw className="w-4 h-4" />
          Actualiser la structure
        </button>
      </div>

      {/* Contenu principal */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center min-h-[50vh] gap-3 bg-white rounded-2xl border border-gray-100 p-12">
          <div className="spinner spinner-lg text-[#1A6B3C]" />
          <span className="text-sm text-gray-500 font-medium">Construction de l'arbre hiérarchique...</span>
        </div>
      ) : isError || !orgTree ? (
        <div className="p-8 bg-red-50 rounded-2xl border border-red-100 text-center space-y-3">
          <AlertCircle className="w-10 h-10 text-red-500 mx-auto" />
          <h3 className="text-base font-bold text-red-900">Erreur de chargement de l'organigramme</h3>
          <p className="text-xs text-red-600">
            Impossible de charger les relations hiérarchiques de l'entreprise.
          </p>
          <button onClick={() => refetch()} className="btn btn-secondary text-xs min-h-[44px]">
            Réessayer
          </button>
        </div>
      ) : orgTree.length === 0 ? (
        <div className="p-12 bg-white rounded-2xl border border-gray-100 text-center space-y-3 shadow-sm">
          <Users className="w-12 h-12 text-gray-300 mx-auto" />
          <h3 className="text-base font-bold text-gray-800">Aucun collaborateur actif</h3>
          <p className="text-xs text-gray-500">
            L'entreprise ne compte aucun collaborateur actif dans l'organigramme.
          </p>
        </div>
      ) : (
        <ReactFlowProvider>
          <OrgChartFlow orgTree={orgTree} onSelectEmployee={handleNodeClick} />
        </ReactFlowProvider>
      )}

      {/* Modale d'aperçu d'un collaborateur (pour éviter les erreurs 403 non désirées) */}
      {selectedNode && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 space-y-4 shadow-2xl animate-scale-up">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                <UserCheck className="w-5 h-5 text-[#1A6B3C]" />
                Fiche Collaborateur
              </h3>
              <button onClick={() => setSelectedNode(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-[#1A6B3C]/10 text-[#1A6B3C] font-bold text-sm flex items-center justify-center shrink-0">
                  {selectedNode.name
                    .split(' ')
                    .map((n) => n[0])
                    .join('')
                    .slice(0, 2)}
                </div>
                <div>
                  <h4 className="font-bold text-gray-900">{selectedNode.name}</h4>
                  <p className="text-xs font-semibold text-[#1A6B3C]">{selectedNode.title}</p>
                </div>
              </div>

              <div className="bg-gray-50 p-3 rounded-xl space-y-2 text-xs text-gray-600">
                <div className="flex items-center gap-2">
                  <Building className="w-4 h-4 text-gray-400" />
                  <span>{selectedNode.department}</span>
                </div>
                {selectedNode.email && (
                  <div className="flex items-center gap-2">
                    <Mail className="w-4 h-4 text-gray-400" />
                    <a href={`mailto:${selectedNode.email}`} className="text-[#1A6B3C] hover:underline">
                      {selectedNode.email}
                    </a>
                  </div>
                )}
                {selectedNode.subordinateCount != null && selectedNode.subordinateCount > 0 && (
                  <div className="flex items-center gap-2 font-medium text-gray-700">
                    <Users className="w-4 h-4 text-[#1A6B3C]" />
                    <span>{selectedNode.subordinateCount} collaborateur(s) dans l'équipe</span>
                  </div>
                )}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setSelectedNode(null)}
                className="btn btn-secondary text-xs min-h-[44px]"
              >
                Fermer
              </button>
              {(isDrh || isManager) && (
                <button
                  type="button"
                  onClick={() => {
                    navigate(`/employees/${selectedNode.id}`);
                    setSelectedNode(null);
                  }}
                  className="btn btn-primary text-xs min-h-[44px] inline-flex items-center gap-1.5"
                >
                  Ouvrir le dossier
                  <ExternalLink className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default OrgChartPage;
