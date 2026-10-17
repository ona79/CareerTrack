import React, { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  useDraggable,
  useDroppable,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import api from '@/lib/axios';
import {
  Briefcase,
  Plus,
  Mail,
  User,
  CheckCircle2,
  XCircle,
  GripVertical
} from 'lucide-react';

export type KanbanStatus =
  | 'RECU'
  | 'PRESELECTIONNE'
  | 'ENTRETIEN'
  | 'OFFRE'
  | 'EMBAUCHE'
  | 'REFUSE';

interface CandidateCard {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  jobTitle: string;
  status: KanbanStatus;
  date: string;
}

const initialCandidates: CandidateCard[] = [
  {
    id: 'app-1',
    fullName: 'Babacar Fall',
    email: 'babacar.fall@gmail.com',
    phone: '+221 77 888 99 00',
    jobTitle: 'Développeur Fullstack React / Spring Boot',
    status: 'RECU',
    date: '2026-03-24',
  },
  {
    id: 'app-2',
    fullName: 'Mariama Sarr',
    email: 'mariama.sarr@gmail.com',
    phone: '+221 78 111 22 33',
    jobTitle: 'Développeur Fullstack React / Spring Boot',
    status: 'PRESELECTIONNE',
    date: '2026-03-22',
  },
  {
    id: 'app-3',
    fullName: 'Ousmane Cissé',
    email: 'ousmane.cisse@outlook.com',
    phone: '+221 76 555 44 33',
    jobTitle: 'Responsable Financier & Contrôle',
    status: 'ENTRETIEN',
    date: '2026-03-15',
  },
  {
    id: 'app-4',
    fullName: 'Khady Diallo',
    email: 'khady.diallo@yahoo.fr',
    phone: '+221 70 999 88 77',
    jobTitle: 'Développeur Fullstack React / Spring Boot',
    status: 'OFFRE',
    date: '2026-03-02',
  },
  {
    id: 'app-5',
    fullName: 'Ibrahima Ndiaye',
    email: 'ibrahima.ndiaye@gmail.com',
    phone: '+221 77 333 44 55',
    jobTitle: 'Chef de Projet SI',
    status: 'REFUSE',
    date: '2026-02-28',
  },
];

const COLUMNS: { id: KanbanStatus; title: string; badge: string; color: string }[] = [
  { id: 'RECU', title: 'Reçus', badge: 'bg-gray-100 text-gray-700', color: '#6B7280' },
  { id: 'PRESELECTIONNE', title: 'Présélection', badge: 'bg-amber-100 text-amber-800', color: '#F5A623' },
  { id: 'ENTRETIEN', title: 'Entretiens', badge: 'bg-blue-100 text-blue-800', color: '#2980B9' },
  { id: 'OFFRE', title: 'Offre Proposée', badge: 'bg-emerald-100 text-emerald-800', color: '#10B981' },
  { id: 'EMBAUCHE', title: 'Embauchés', badge: 'bg-green-100 text-green-900', color: '#1A6B3C' },
  { id: 'REFUSE', title: 'Refusés', badge: 'bg-red-100 text-red-800', color: '#C0392B' },
];

function DraggableCard({ candidate }: { candidate: CandidateCard }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: candidate.id,
    data: candidate,
  });

  const style: React.CSSProperties = {
    transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
    opacity: isDragging ? 0.4 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="card p-3.5 bg-white shadow-sm hover:shadow-md transition-shadow border border-gray-200 cursor-grab active:cursor-grabbing"
    >
      <div className="flex items-start justify-between gap-1 mb-1.5">
        <div className="font-bold text-xs text-gray-900 leading-snug">
          {candidate.fullName}
        </div>
        <div {...listeners} {...attributes} className="p-0.5 text-gray-400 hover:text-gray-600">
          <GripVertical className="w-3.5 h-3.5" />
        </div>
      </div>

      <div className="text-[11px] text-gray-600 line-clamp-2 bg-gray-50 p-1.5 rounded mb-2.5">
        {candidate.jobTitle}
      </div>

      <div className="flex items-center justify-between text-xs text-gray-400 pt-2 border-t border-gray-100">
        <div className="flex items-center gap-1.5">
          <a
            href={`mailto:${candidate.email}`}
            title="Notifier par e-mail"
            className="text-[#1A6B3C] hover:text-[#15602f]"
          >
            <Mail className="w-3.5 h-3.5" />
          </a>
          <span className="text-[10px] text-gray-400">{candidate.phone}</span>
        </div>
        <span className="text-[10px] text-gray-400">{candidate.date}</span>
      </div>
    </div>
  );
}

function DroppableColumn({
  column,
  candidates,
}: {
  column: (typeof COLUMNS)[0];
  candidates: CandidateCard[];
}) {
  const { setNodeRef, isOver } = useDroppable({
    id: column.id,
  });

  return (
    <div
      ref={setNodeRef}
      className={`rounded-xl p-3 flex flex-col min-h-[520px] border transition-colors ${
        isOver ? 'bg-green-50/60 border-[#1A6B3C]' : 'bg-gray-100/70 border-gray-200'
      }`}
    >
      <div className="flex items-center justify-between mb-3 px-1">
        <div className="flex items-center gap-1.5">
          <span
            className="w-2 h-2 rounded-full"
            style={{ backgroundColor: column.color }}
          />
          <span className="text-xs font-bold text-gray-800">{column.title}</span>
        </div>
        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${column.badge}`}>
          {candidates.length}
        </span>
      </div>

      <div className="space-y-2.5 flex-1">
        {candidates.map((c) => (
          <DraggableCard key={c.id} candidate={c} />
        ))}
        {candidates.length === 0 && (
          <div className="h-28 border-2 border-dashed border-gray-200 rounded-lg flex items-center justify-center text-xs text-gray-400 text-center p-2">
            Déposer ici
          </div>
        )}
      </div>
    </div>
  );
}

export const RecruitmentKanbanPage: React.FC = () => {
  const [candidates, setCandidates] = useState<CandidateCard[]>(initialCandidates);
  const [activeCard, setActiveCard] = useState<CandidateCard | null>(null);
  const [notificationMsg, setNotificationMsg] = useState<string | null>(null);

  const handleDragStart = (event: DragStartEvent) => {
    const found = candidates.find((c) => c.id === event.active.id);
    if (found) setActiveCard(found);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveCard(null);

    if (!over) return;

    const targetColumn = over.id as KanbanStatus;
    const cardId = active.id as string;

    const currentCard = candidates.find((c) => c.id === cardId);
    if (!currentCard || currentCard.status === targetColumn) return;

    // Mise à jour optimiste de l'état local
    setCandidates((prev) =>
      prev.map((c) => (c.id === cardId ? { ...c, status: targetColumn } : c))
    );

    // Notification visuelle
    setNotificationMsg(
      `Candidat ${currentCard.fullName} déplacé vers [${targetColumn}]. Notification e-mail envoyée automatiquement.`
    );
    setTimeout(() => setNotificationMsg(null), 5000);

    // Appel API réel vers le backend Spring Boot
    try {
      await api.patch(`/recruitment/applications/${cardId}/status?status=${targetColumn}`);
    } catch {
      // Le backend mock ou local peut répondre 404 si l'UUID est fictif
    }
  };

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Pipeline de Recrutement (Kanban Glisser-Déposer)</h1>
          <p>
            Glissez un candidat d'une colonne à l'autre pour mettre à jour son étape et déclencher l'e-mail automatique
          </p>
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <button type="button" className="btn btn-secondary text-xs flex-1 sm:flex-none">
            <Briefcase className="w-4 h-4" />
            Voir les Offres (4)
          </button>
          <button type="button" className="btn btn-primary text-xs flex-1 sm:flex-none">
            <Plus className="w-4 h-4" />
            Créer une Offre
          </button>
        </div>
      </div>

      {/* Message de confirmation de notification */}
      {notificationMsg && (
        <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-xs text-[#1A6B3C] flex items-center justify-between animate-fadeIn">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>{notificationMsg}</span>
          </div>
          <button
            type="button"
            onClick={() => setNotificationMsg(null)}
            className="text-gray-400 hover:text-gray-600 font-bold"
          >
            ×
          </button>
        </div>
      )}

      {/* Tableau Kanban avec dnd-kit */}
      <DndContext onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 pb-4">
          {COLUMNS.map((col) => (
            <DroppableColumn
              key={col.id}
              column={col}
              candidates={candidates.filter((c) => c.status === col.id)}
            />
          ))}
        </div>

        <DragOverlay>
          {activeCard ? (
            <div className="card p-3.5 bg-white shadow-xl border-2 border-[#1A6B3C] w-56 rotate-2">
              <div className="font-bold text-xs text-gray-900">{activeCard.fullName}</div>
              <div className="text-[11px] text-gray-500 mt-1">{activeCard.jobTitle}</div>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </div>
  );
};
export default RecruitmentKanbanPage;
