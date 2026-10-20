import React, { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  useDraggable,
  useDroppable,
  PointerSensor,
  TouchSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import {
  Briefcase,
  Plus,
  Mail,
  Phone,
  User,
  CheckCircle2,
  XCircle,
  GripVertical,
  RefreshCw,
  Filter,
  Calendar,
  FileText,
  X,
  Loader2,
  Eye,
  ExternalLink,
  Users,
  Building,
} from 'lucide-react';
import type {
  JobApplicationDto,
  JobOfferDto,
  JobOfferCreateDto,
  JobApplicationCreateDto,
  KanbanStatus,
  DepartmentDto,
  PositionDto,
  ApiResponse,
} from '@/types';

const COLUMNS: { id: KanbanStatus; title: string; badge: string; border: string; color: string }[] = [
  { id: 'RECU', title: '1. Reçus', badge: 'bg-gray-100 text-gray-800', border: 'border-gray-200', color: '#6B7280' },
  { id: 'PRESELECTIONNE', title: '2. Présélection', badge: 'bg-amber-100 text-amber-900', border: 'border-amber-200', color: '#F5A623' },
  { id: 'ENTRETIEN', title: '3. Entretiens', badge: 'bg-blue-100 text-blue-900', border: 'border-blue-200', color: '#2980B9' },
  { id: 'OFFRE', title: '4. Offre Émise', badge: 'bg-purple-100 text-purple-900', border: 'border-purple-200', color: '#8E44AD' },
  { id: 'EMBAUCHE', title: '5. Embauchés', badge: 'bg-emerald-100 text-emerald-900', border: 'border-emerald-300', color: '#1A6B3C' },
  { id: 'REFUSE', title: '6. Non Retenus', badge: 'bg-red-100 text-red-900', border: 'border-red-200', color: '#C0392B' },
];

function DraggableCard({
  candidate,
  onOpenDetails,
}: {
  candidate: JobApplicationDto;
  onOpenDetails: (candidate: JobApplicationDto) => void;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: candidate.id,
    data: candidate,
  });

  const style: React.CSSProperties = {
    transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
    opacity: isDragging ? 0.4 : 1,
    touchAction: 'none',
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      onClick={() => onOpenDetails(candidate)}
      className="card p-3.5 bg-white shadow-sm hover:shadow-md transition-all border border-gray-200 cursor-pointer rounded-xl group relative"
    >
      <div className="flex items-start justify-between gap-1.5 mb-1.5">
        <div className="font-bold text-xs text-gray-900 leading-snug line-clamp-1 group-hover:text-[#1A6B3C] transition-colors">
          {candidate.candidateFullName || 'Candidat Anonyme'}
        </div>
        <div
          {...listeners}
          {...attributes}
          onClick={(e) => e.stopPropagation()}
          className="p-1 -mr-1 -mt-1 text-gray-400 hover:text-gray-700 cursor-grab active:cursor-grabbing rounded hover:bg-gray-100"
          title="Faire glisser pour changer d'étape"
        >
          <GripVertical className="w-3.5 h-3.5" />
        </div>
      </div>

      <div className="text-[11px] text-gray-600 line-clamp-2 bg-gray-50/80 p-2 rounded-lg mb-2 border border-gray-100">
        <div className="font-medium text-gray-800 line-clamp-1">{candidate.jobOfferTitle || 'Poste non renseigné'}</div>
        {candidate.notes && (
          <div className="text-[10px] text-gray-400 italic line-clamp-1 mt-0.5">{candidate.notes}</div>
        )}
      </div>

      <div className="flex items-center justify-between text-xs text-gray-400 pt-2 border-t border-gray-100">
        <div className="flex items-center gap-2">
          {candidate.candidateEmail && (
            <a
              href={`mailto:${candidate.candidateEmail}`}
              onClick={(e) => e.stopPropagation()}
              title={`Écrire à ${candidate.candidateEmail}`}
              className="text-[#1A6B3C] hover:text-[#15602f] p-0.5 rounded hover:bg-green-50"
            >
              <Mail className="w-3.5 h-3.5" />
            </a>
          )}
          {candidate.candidatePhone && (
            <span className="text-[10px] text-gray-500 font-mono">{candidate.candidatePhone}</span>
          )}
        </div>
        {candidate.cvUrl && (
          <span
            onClick={(e) => {
              e.stopPropagation();
              window.open(candidate.cvUrl, '_blank');
            }}
            title="Consulter le CV"
            className="text-[10px] text-blue-600 hover:underline flex items-center gap-0.5 font-semibold"
          >
            <FileText className="w-3 h-3" /> CV
          </span>
        )}
      </div>
    </div>
  );
}

function DroppableColumn({
  column,
  candidates,
  onOpenDetails,
}: {
  column: (typeof COLUMNS)[0];
  candidates: JobApplicationDto[];
  onOpenDetails: (candidate: JobApplicationDto) => void;
}) {
  const { setNodeRef, isOver } = useDroppable({
    id: column.id,
  });

  return (
    <div
      ref={setNodeRef}
      className={`rounded-2xl p-3 flex flex-col min-h-[560px] border transition-colors ${
        isOver
          ? 'bg-emerald-50/70 border-[#1A6B3C] shadow-inner'
          : `bg-gray-50/80 ${column.border}`
      }`}
    >
      <div className="flex items-center justify-between mb-3 px-1">
        <div className="flex items-center gap-1.5">
          <span
            className="w-2.5 h-2.5 rounded-full shrink-0"
            style={{ backgroundColor: column.color }}
          />
          <span className="text-xs font-bold text-gray-800">{column.title}</span>
        </div>
        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${column.badge}`}>
          {candidates.length}
        </span>
      </div>

      <div className="space-y-2.5 flex-1 overflow-y-auto max-h-[calc(100vh-280px)] pr-0.5">
        {candidates.map((c) => (
          <DraggableCard key={c.id} candidate={c} onOpenDetails={onOpenDetails} />
        ))}
        {candidates.length === 0 && (
          <div className="h-32 border-2 border-dashed border-gray-200 rounded-xl flex flex-col items-center justify-center text-xs text-gray-400 text-center p-3">
            <span>Déposer un candidat ici</span>
            <span className="text-[10px] text-gray-300 mt-1">Glisser ou cliquer</span>
          </div>
        )}
      </div>
    </div>
  );
}

export const RecruitmentKanbanPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();

  const isDrhOrManager = hasRole('DRH') || hasRole('MANAGER') || hasRole('ROLE_DRH') || hasRole('ROLE_MANAGER');

  // Filtre par offre
  const [selectedOfferFilter, setSelectedOfferFilter] = useState<string>('ALL');

  // Modales
  const [showCreateOfferModal, setShowCreateOfferModal] = useState<boolean>(false);
  const [showOffersListModal, setShowOffersListModal] = useState<boolean>(false);
  const [showNewApplicationModal, setShowNewApplicationModal] = useState<boolean>(false);
  const [selectedCandidate, setSelectedCandidate] = useState<JobApplicationDto | null>(null);

  // Notifications visuelles
  const [notificationMsg, setNotificationMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // Configuration des capteurs dnd-kit avec support tactile mobile
  const pointerSensor = useSensor(PointerSensor, {
    activationConstraint: { distance: 5 },
  });
  const touchSensor = useSensor(TouchSensor, {
    activationConstraint: { delay: 150, tolerance: 5 },
  });
  const sensors = useSensors(pointerSensor, touchSensor);

  // 1. Récupération des candidatures depuis l'API
  const {
    data: applications = [],
    isLoading: isLoadingApps,
    refetch: refetchApps,
  } = useQuery<JobApplicationDto[]>({
    queryKey: ['recruitment-applications'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<JobApplicationDto[]>>('/recruitment/applications');
      return res.data?.data || [];
    },
  });

  // 2. Récupération des offres d'emploi depuis l'API
  const {
    data: offers = [],
    isLoading: isLoadingOffers,
    refetch: refetchOffers,
  } = useQuery<JobOfferDto[]>({
    queryKey: ['recruitment-offers'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<JobOfferDto[]>>('/recruitment/offers');
      return res.data?.data || [];
    },
  });

  // 3. Départements et postes pour formulaires
  const { data: departments = [] } = useQuery<DepartmentDto[]>({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<DepartmentDto[]>>('/employees/departments');
      return res.data?.data || [];
    },
  });

  const { data: positions = [] } = useQuery<PositionDto[]>({
    queryKey: ['positions'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PositionDto[]>>('/employees/positions');
      return res.data?.data || [];
    },
  });

  // État local des candidatures pour mise à jour optimiste
  const [localApplications, setLocalApplications] = useState<JobApplicationDto[]>([]);
  const [activeCard, setActiveCard] = useState<JobApplicationDto | null>(null);

  React.useEffect(() => {
    setLocalApplications(applications);
  }, [applications]);

  // Formulaire Création d'Offre
  const [newOfferForm, setNewOfferForm] = useState<JobOfferCreateDto>({
    title: '',
    departmentId: '',
    positionId: '',
    contractType: 'CDI',
    description: '',
    requirements: '',
    openPositions: 1,
    closingDate: '',
  });

  // Formulaire Nouvelle Candidature
  const [newAppForm, setNewAppForm] = useState<JobApplicationCreateDto>({
    jobOfferId: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    cvUrl: '',
    linkedinUrl: '',
    notes: '',
  });

  // Mutation Statut Candidature
  const updateStatusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: KanbanStatus }) => {
      const res = await api.patch<ApiResponse<JobApplicationDto>>(
        `/recruitment/applications/${id}/status?status=${status}`
      );
      return res.data?.data;
    },
    onSuccess: (updatedApp) => {
      queryClient.invalidateQueries({ queryKey: ['recruitment-applications'] });
      setNotificationMsg({
        text: `Candidat(e) ${updatedApp.candidateFullName} déplacé(e) vers [${updatedApp.status}]. Notification e-mail envoyée automatiquement.`,
        type: 'success',
      });
      setTimeout(() => setNotificationMsg(null), 6000);
    },
    onError: (err: any) => {
      // Rollback en cas d'erreur
      setLocalApplications(applications);
      const msg = err.response?.data?.message || 'Erreur lors du changement de statut du candidat';
      setNotificationMsg({ text: msg, type: 'error' });
      setTimeout(() => setNotificationMsg(null), 6000);
    },
  });

  // Mutation Création Offre
  const createOfferMutation = useMutation({
    mutationFn: async (form: JobOfferCreateDto) => {
      const payload = {
        title: form.title.trim(),
        contractType: form.contractType,
        description: form.description.trim(),
        requirements: form.requirements ? form.requirements.trim() : null,
        openPositions: Number(form.openPositions) || 1,
        departmentId: form.departmentId ? form.departmentId : null,
        positionId: form.positionId ? form.positionId : null,
        closingDate: form.closingDate ? form.closingDate : null,
      };
      const res = await api.post<ApiResponse<JobOfferDto>>('/recruitment/offers', payload);
      return res.data?.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruitment-offers'] });
      setShowCreateOfferModal(false);
      setNewOfferForm({
        title: '',
        departmentId: '',
        positionId: '',
        contractType: 'CDI',
        description: '',
        requirements: '',
        openPositions: 1,
        closingDate: '',
      });
      setNotificationMsg({ text: 'Offre d’emploi publiée avec succès !', type: 'success' });
      setTimeout(() => setNotificationMsg(null), 5000);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || "Erreur lors de la publication de l'offre";
      setNotificationMsg({ text: msg, type: 'error' });
    },
  });

  // Mutation Dépôt Candidature
  const createApplicationMutation = useMutation({
    mutationFn: async (form: JobApplicationCreateDto) => {
      const payload = {
        jobOfferId: form.jobOfferId,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone ? form.phone.trim() : null,
        cvUrl: form.cvUrl ? form.cvUrl.trim() : null,
        linkedinUrl: form.linkedinUrl ? form.linkedinUrl.trim() : null,
        notes: form.notes ? form.notes.trim() : null,
      };
      const res = await api.post<ApiResponse<JobApplicationDto>>('/recruitment/applications', payload);
      return res.data?.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruitment-applications'] });
      queryClient.invalidateQueries({ queryKey: ['recruitment-offers'] });
      setShowNewApplicationModal(false);
      setNewAppForm({
        jobOfferId: '',
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        cvUrl: '',
        linkedinUrl: '',
        notes: '',
      });
      setNotificationMsg({ text: 'Nouvelle candidature enregistrée avec succès !', type: 'success' });
      setTimeout(() => setNotificationMsg(null), 5000);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || "Erreur lors de l'enregistrement de la candidature";
      setNotificationMsg({ text: msg, type: 'error' });
    },
  });

  // Drag & Drop Handlers
  const handleDragStart = (event: DragStartEvent) => {
    const found = localApplications.find((c) => c.id === event.active.id);
    if (found) setActiveCard(found);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveCard(null);

    if (!over) return;

    const targetColumn = over.id as KanbanStatus;
    const cardId = active.id as string;

    const currentCard = localApplications.find((c) => c.id === cardId);
    if (!currentCard || currentCard.status === targetColumn) return;

    // Mise à jour optimiste immédiate
    setLocalApplications((prev) =>
      prev.map((c) => (c.id === cardId ? { ...c, status: targetColumn } : c))
    );

    // Déclenchement de la mutation API
    updateStatusMutation.mutate({ id: cardId, status: targetColumn });
  };

  // Filtrage des candidatures selon l'offre sélectionnée
  const filteredApplications = localApplications.filter((app) => {
    if (selectedOfferFilter === 'ALL') return true;
    return app.jobOfferId === selectedOfferFilter;
  });

  return (
    <div className="space-y-6">
      {/* En-tête de la page */}
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <Briefcase className="w-6 h-6 text-[#1A6B3C]" />
            Pipeline de Recrutement (Kanban Glisser-Déposer)
          </h1>
          <p className="text-xs text-gray-500 mt-0.5">
            Glissez un candidat d’une colonne à l’autre : le statut est mis à jour en temps réel et un e-mail officiel est notifié.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
          {/* Bouton Voir les Offres */}
          <button
            type="button"
            onClick={() => setShowOffersListModal(true)}
            className="btn btn-secondary text-xs flex-1 sm:flex-none inline-flex items-center gap-1.5 min-h-[40px] px-3.5"
            title="Consulter toutes les offres d'emploi publiées"
          >
            <Briefcase className="w-4 h-4 text-gray-600" />
            Voir les Offres ({offers.length})
          </button>

          {/* Bouton Créer une Offre */}
          {isDrhOrManager && (
            <button
              type="button"
              onClick={() => setShowCreateOfferModal(true)}
              className="btn btn-primary text-xs flex-1 sm:flex-none inline-flex items-center gap-1.5 min-h-[40px] px-3.5"
              title="Publier une nouvelle offre de recrutement"
            >
              <Plus className="w-4 h-4" />
              Créer une Offre
            </button>
          )}

          {/* Bouton Ajouter Candidat */}
          {isDrhOrManager && (
            <button
              type="button"
              onClick={() => {
                if (offers.length > 0 && !newAppForm.jobOfferId) {
                  setNewAppForm((prev) => ({ ...prev, jobOfferId: offers[0].id }));
                }
                setShowNewApplicationModal(true);
              }}
              className="btn btn-secondary text-xs flex-1 sm:flex-none inline-flex items-center gap-1.5 min-h-[40px] px-3.5"
              title="Ajouter manuellement une candidature"
            >
              <User className="w-4 h-4 text-emerald-600" />
              Nouvelle Candidature
            </button>
          )}

          {/* Bouton Actualiser */}
          <button
            type="button"
            onClick={() => {
              refetchApps();
              refetchOffers();
            }}
            className="btn btn-secondary text-xs min-h-[40px] px-2.5"
            title="Rafraîchir les données"
          >
            <RefreshCw className="w-4 h-4 text-gray-600" />
          </button>
        </div>
      </div>

      {/* Barre de filtres et statistiques */}
      <div className="card p-3.5 flex flex-col sm:flex-row items-center justify-between gap-3 bg-white border border-gray-200">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter className="w-4 h-4 text-gray-400 shrink-0" />
          <span className="text-xs font-semibold text-gray-700 whitespace-nowrap">Filtrer par poste :</span>
          <select
            value={selectedOfferFilter}
            onChange={(e) => setSelectedOfferFilter(e.target.value)}
            className="input input-sm text-xs rounded-lg border-gray-300 w-full sm:w-64"
          >
            <option value="ALL">Toutes les offres ({applications.length} candidatures)</option>
            {offers.map((o) => (
              <option key={o.id} value={o.id}>
                {o.title} ({o.applicationsCount} cand.)
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-4 text-xs text-gray-500 w-full sm:w-auto justify-between sm:justify-end">
          <span>
            Total candidatures : <strong className="text-gray-900">{filteredApplications.length}</strong>
          </span>
          <span className="text-gray-300">•</span>
          <span>
            Postes ouverts : <strong className="text-[#1A6B3C]">{offers.filter((o) => o.status === 'OPEN').length}</strong>
          </span>
        </div>
      </div>

      {/* Alertes de notification */}
      {notificationMsg && (
        <div
          className={`p-3.5 rounded-xl border flex items-center justify-between text-xs animate-fadeIn ${
            notificationMsg.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-[#1A6B3C]'
              : 'bg-red-50 border-red-200 text-red-700'
          }`}
        >
          <div className="flex items-center gap-2">
            {notificationMsg.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
            ) : (
              <XCircle className="w-4 h-4 shrink-0 text-red-600" />
            )}
            <span className="font-medium">{notificationMsg.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setNotificationMsg(null)}
            className="text-gray-400 hover:text-gray-600 p-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Tableau Kanban avec défilement horizontal et toucher mobile */}
      <div className="overflow-x-auto pb-4 -mx-2 px-2 scrollbar-thin">
        <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 min-w-[320px] lg:min-w-[1240px]">
            {COLUMNS.map((col) => (
              <DroppableColumn
                key={col.id}
                column={col}
                candidates={filteredApplications.filter((c) => c.status === col.id)}
                onOpenDetails={(cand) => setSelectedCandidate(cand)}
              />
            ))}
          </div>

          <DragOverlay>
            {activeCard ? (
              <div className="card p-3.5 bg-white shadow-2xl border-2 border-[#1A6B3C] w-64 rotate-2 scale-105 pointer-events-none">
                <div className="font-bold text-xs text-gray-900">{activeCard.candidateFullName}</div>
                <div className="text-[11px] text-[#1A6B3C] font-medium mt-0.5 line-clamp-1">
                  {activeCard.jobOfferTitle}
                </div>
                <div className="text-[10px] text-gray-400 mt-1">{activeCard.candidateEmail}</div>
              </div>
            ) : null}
          </DragOverlay>
        </DndContext>
      </div>

      {/* ─── MODALE : CRÉER UNE OFFRE D'EMPLOI ──────────────────────────────── */}
      {showCreateOfferModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-hidden">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-xl w-full shadow-2xl flex flex-col max-h-[92vh] sm:max-h-[90vh]">
            <div className="flex items-center justify-between border-b border-gray-100 p-4 sm:p-5 shrink-0">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-100 text-[#1A6B3C] flex items-center justify-center font-bold">
                  <Briefcase className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Publier une Nouvelle Offre d'Emploi</h3>
                  <p className="text-xs text-gray-500">L'offre sera ouverte à la réception des candidatures</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowCreateOfferModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                createOfferMutation.mutate(newOfferForm);
              }}
              className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Intitulé du poste <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={newOfferForm.title}
                  onChange={(e) => setNewOfferForm({ ...newOfferForm, title: e.target.value })}
                  placeholder="Ex: Ingénieur DevOps Cloud / Développeur React"
                  className="input w-full text-sm font-medium"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Département</label>
                  <select
                    value={newOfferForm.departmentId}
                    onChange={(e) => setNewOfferForm({ ...newOfferForm, departmentId: e.target.value })}
                    className="input w-full text-sm"
                  >
                    <option value="">Sélectionner un département...</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Type de Contrat</label>
                  <select
                    value={newOfferForm.contractType}
                    onChange={(e) => setNewOfferForm({ ...newOfferForm, contractType: e.target.value })}
                    className="input w-full text-sm"
                  >
                    <option value="CDI">CDI (Indéterminée)</option>
                    <option value="CDD">CDD (Déterminée)</option>
                    <option value="STAGE">Stage Professionnel</option>
                    <option value="PRESTATION">Prestation Externe</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Nombre de postes</label>
                  <input
                    type="number"
                    min={1}
                    value={newOfferForm.openPositions}
                    onChange={(e) => setNewOfferForm({ ...newOfferForm, openPositions: parseInt(e.target.value) || 1 })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Date limite de candidature</label>
                  <input
                    type="date"
                    value={newOfferForm.closingDate}
                    onChange={(e) => setNewOfferForm({ ...newOfferForm, closingDate: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Description des missions <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={3}
                  required
                  value={newOfferForm.description}
                  onChange={(e) => setNewOfferForm({ ...newOfferForm, description: e.target.value })}
                  placeholder="Responsabilités, contexte d'équipe et livrables attendus..."
                  className="input w-full text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Prérequis & Compétences requises</label>
                <textarea
                  rows={2}
                  value={newOfferForm.requirements}
                  onChange={(e) => setNewOfferForm({ ...newOfferForm, requirements: e.target.value })}
                  placeholder="Expérience, diplôme, outils et technologies..."
                  className="input w-full text-sm"
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-4 border-t border-gray-100 shrink-0">
                <button
                  type="button"
                  onClick={() => setShowCreateOfferModal(false)}
                  className="btn btn-secondary px-5 text-sm min-h-[44px]"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={createOfferMutation.isPending}
                  className="btn btn-primary px-5 text-sm inline-flex items-center gap-2 min-h-[44px]"
                >
                  {createOfferMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Publier l'Offre
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ─── MODALE : VOIR LES OFFRES ────────────────────────────────────────── */}
      {showOffersListModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-hidden">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-3xl w-full shadow-2xl flex flex-col max-h-[92vh] sm:max-h-[85vh]">
            <div className="flex items-center justify-between border-b border-gray-100 p-4 sm:p-5 shrink-0">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-800 flex items-center justify-center font-bold">
                  <Briefcase className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Catalogue des Offres de Recrutement ({offers.length})</h3>
                  <p className="text-xs text-gray-500">Offres ouvertes et suivi des postes vacants</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowOffersListModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 sm:p-6 space-y-3 flex-1 overflow-y-auto">
              {offers.length === 0 ? (
                <div className="text-center py-12 text-sm text-gray-400">
                  Aucune offre d'emploi n'est actuellement publiée.
                </div>
              ) : (
                offers.map((offer) => (
                  <div
                    key={offer.id}
                    className="p-4 rounded-xl border border-gray-200 bg-gray-50/50 hover:bg-white hover:border-[#1A6B3C] transition-all space-y-2 text-xs"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-gray-900">{offer.title}</span>
                          <span className="font-mono text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">
                            {offer.code}
                          </span>
                        </div>
                        <div className="flex items-center gap-3 text-gray-500 text-[11px] mt-0.5">
                          <span>{offer.departmentName || 'Tous départements'}</span>
                          <span>•</span>
                          <span className="badge badge-secondary py-0 text-[10px]">{offer.contractType}</span>
                          <span>•</span>
                          <span>{offer.openPositions} poste(s) ouvert(s)</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <span className="badge badge-success text-[10px]">
                          {offer.applicationsCount} candidature(s)
                        </span>
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedOfferFilter(offer.id);
                            setShowOffersListModal(false);
                          }}
                          className="btn btn-secondary btn-sm text-xs py-1.5 min-h-[38px]"
                        >
                          Filtrer le Kanban
                        </button>
                      </div>
                    </div>

                    <p className="text-gray-600 text-xs line-clamp-2">{offer.description}</p>
                  </div>
                ))
              )}
            </div>

            <div className="flex justify-end p-4 border-t border-gray-100 shrink-0">
              <button
                type="button"
                onClick={() => setShowOffersListModal(false)}
                className="btn btn-secondary px-5 text-sm min-h-[44px]"
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ─── MODALE : NOUVELLE CANDIDATURE ──────────────────────────────────── */}
      {showNewApplicationModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-hidden">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-lg w-full shadow-2xl flex flex-col max-h-[92vh] sm:max-h-[90vh]">
            <div className="flex items-center justify-between border-b border-gray-100 p-4 sm:p-5 shrink-0">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-100 text-[#1A6B3C] flex items-center justify-center font-bold">
                  <User className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900">Enregistrer une Nouvelle Candidature</h3>
                  <p className="text-xs text-gray-500">Ajout dans la colonne "Reçus" du pipeline</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowNewApplicationModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                createApplicationMutation.mutate(newAppForm);
              }}
              className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Offre d'emploi associée <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={newAppForm.jobOfferId}
                  onChange={(e) => setNewAppForm({ ...newAppForm, jobOfferId: e.target.value })}
                  className="input w-full text-sm"
                >
                  <option value="">Sélectionner une offre...</option>
                  {offers.map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.title} ({o.code})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Prénom <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={newAppForm.firstName}
                    onChange={(e) => setNewAppForm({ ...newAppForm, firstName: e.target.value })}
                    placeholder="Moussa"
                    className="input w-full text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Nom <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={newAppForm.lastName}
                    onChange={(e) => setNewAppForm({ ...newAppForm, lastName: e.target.value })}
                    placeholder="Diallo"
                    className="input w-full text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Adresse Email <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="email"
                    required
                    value={newAppForm.email}
                    onChange={(e) => setNewAppForm({ ...newAppForm, email: e.target.value })}
                    placeholder="candidat@email.com"
                    className="input w-full text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Téléphone</label>
                  <input
                    type="tel"
                    value={newAppForm.phone}
                    onChange={(e) => setNewAppForm({ ...newAppForm, phone: e.target.value })}
                    placeholder="+221 77 000 00 00"
                    className="input w-full text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Lien vers le CV (URL)</label>
                <input
                  type="url"
                  value={newAppForm.cvUrl}
                  onChange={(e) => setNewAppForm({ ...newAppForm, cvUrl: e.target.value })}
                  placeholder="https://..."
                  className="input w-full text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Commentaires / Profil</label>
                <textarea
                  rows={2}
                  value={newAppForm.notes}
                  onChange={(e) => setNewAppForm({ ...newAppForm, notes: e.target.value })}
                  placeholder="Expérience clé, prétentions salariales..."
                  className="input w-full text-sm"
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-4 border-t border-gray-100 shrink-0">
                <button
                  type="button"
                  onClick={() => setShowNewApplicationModal(false)}
                  className="btn btn-secondary px-5 text-sm min-h-[44px]"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={createApplicationMutation.isPending}
                  className="btn btn-primary px-5 text-sm inline-flex items-center gap-2 min-h-[44px]"
                >
                  {createApplicationMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Enregistrer
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ─── MODALE : DÉTAILS CANDIDAT & CHANGEMENT RAPIDE DE STATUT ───────── */}
      {selectedCandidate && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-hidden">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-lg w-full shadow-2xl flex flex-col max-h-[92vh] sm:max-h-[90vh]">
            <div className="flex items-center justify-between border-b border-gray-100 p-4 sm:p-5 shrink-0">
              <div>
                <h3 className="text-base font-bold text-gray-900">{selectedCandidate.candidateFullName}</h3>
                <p className="text-xs text-[#1A6B3C] font-semibold">{selectedCandidate.jobOfferTitle}</p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedCandidate(null)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 sm:p-6 space-y-4 text-sm overflow-y-auto flex-1">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 bg-gray-50 p-3.5 rounded-xl border border-gray-100">
                <div>
                  <span className="text-gray-400 text-xs">Email :</span>
                  <div className="font-semibold text-gray-800 text-sm truncate">{selectedCandidate.candidateEmail}</div>
                </div>
                <div>
                  <span className="text-gray-400 text-xs">Téléphone :</span>
                  <div className="font-semibold text-gray-800 text-sm">{selectedCandidate.candidatePhone || 'Non renseigné'}</div>
                </div>
              </div>

              {selectedCandidate.cvUrl && (
                <div className="flex items-center justify-between p-3.5 bg-blue-50/50 rounded-xl border border-blue-100">
                  <span className="text-gray-700 font-medium text-sm">Curriculum Vitae (PDF)</span>
                  <a
                    href={selectedCandidate.cvUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="btn btn-primary btn-sm text-xs inline-flex items-center gap-1.5 min-h-[38px] px-3.5"
                  >
                    <ExternalLink className="w-4 h-4" /> Ouvrir le CV
                  </a>
                </div>
              )}

              {selectedCandidate.notes && (
                <div className="p-3.5 bg-amber-50/60 rounded-xl border border-amber-100">
                  <span className="font-bold text-gray-800 text-xs block mb-1">Notes du recruteur :</span>
                  <p className="text-gray-700 text-sm leading-relaxed">{selectedCandidate.notes}</p>
                </div>
              )}

              {/* Action rapide : Déplacer vers une autre étape */}
              <div className="pt-2">
                <span className="font-bold text-gray-700 text-xs block mb-2">
                  Changer d'étape (Notifie immédiatement le candidat) :
                </span>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {COLUMNS.map((col) => (
                    <button
                      key={col.id}
                      type="button"
                      disabled={selectedCandidate.status === col.id}
                      onClick={() => {
                        const candId = selectedCandidate.id;
                        setSelectedCandidate(null);
                        updateStatusMutation.mutate({ id: candId, status: col.id });
                      }}
                      className={`px-3 py-2.5 rounded-xl text-xs font-semibold text-left transition-colors border min-h-[42px] ${
                        selectedCandidate.status === col.id
                          ? 'bg-gray-100 border-gray-300 text-gray-400 cursor-default'
                          : 'bg-white hover:bg-gray-50 border-gray-200 text-gray-800 hover:border-[#1A6B3C]'
                      }`}
                    >
                      {col.title}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex justify-end p-4 border-t border-gray-100 shrink-0">
              <button
                type="button"
                onClick={() => setSelectedCandidate(null)}
                className="btn btn-secondary px-5 text-sm min-h-[44px]"
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RecruitmentKanbanPage;
