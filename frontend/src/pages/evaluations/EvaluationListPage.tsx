import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Award,
  Plus,
  Calendar,
  CheckCircle2,
  Download,
  Target,
  ChevronRight,
  Clock,
  User,
  ShieldAlert,
  Loader2,
  RefreshCw,
  FileText,
  AlertTriangle,
  ArrowRight,
  Check,
  Star,
  Users,
  AlertCircle,
  X
} from 'lucide-react';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import { downloadFile } from '@/lib/downloadFile';
import type {
  EvaluationCampaign,
  EvaluationDto,
  SmartObjectiveDto,
  ObjectiveCategory,
  ApiResponse,
  Employee
} from '@/types';

type StepType = 'OBJECTIVES' | 'SELF_EVAL' | 'MANAGER_EVAL' | 'VALIDATION';

export const EvaluationListPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();

  const isDRH = hasRole('DRH') || hasRole('ADMIN') || hasRole('DIRECTION') || hasRole('ROLE_DRH') || hasRole('ROLE_ADMIN') || hasRole('ROLE_DIRECTION');
  const isManager = hasRole('MANAGER') || hasRole('ROLE_MANAGER');

  const [notificationMsg, setNotificationMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [selectedCampaignId, setSelectedCampaignId] = useState<string>('');
  const [selectedEvalId, setSelectedEvalId] = useState<string>('');
  const [activeStep, setActiveStep] = useState<StepType>('OBJECTIVES');
  const [downloadingPdf, setDownloadingPdf] = useState<boolean>(false);

  // Modales
  const currentYear = new Date().getFullYear();
  const [showNewCampaignModal, setShowNewCampaignModal] = useState<boolean>(false);
  const [newCampaignTitle, setNewCampaignTitle] = useState<string>(`Campagne Annuelle d'Évaluation ${currentYear}`);
  const [newCampaignYear, setNewCampaignYear] = useState<number>(currentYear);
  const [newCampaignStartDate, setNewCampaignStartDate] = useState<string>(`${currentYear}-01-01`);
  const [newCampaignEndDate, setNewCampaignEndDate] = useState<string>(`${currentYear}-12-31`);
  const [campaignError, setCampaignError] = useState<string | null>(null);

  // Modale Auto-évaluation
  const [showSelfEvalModal, setShowSelfEvalModal] = useState<boolean>(false);
  const [selfScoreInput, setSelfScoreInput] = useState<number>(4.0);
  const [selfFeedbackInput, setSelfFeedbackInput] = useState<string>('');

  // Modale Évaluation Managériale
  const [showManagerEvalModal, setShowManagerEvalModal] = useState<boolean>(false);
  const [managerScoreInput, setManagerScoreInput] = useState<number>(4.5);
  const [managerFeedbackInput, setManagerFeedbackInput] = useState<string>('');
  const [interviewDateInput, setInterviewDateInput] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [objectiveRatesInputs, setObjectiveRatesInputs] = useState<Record<string, number>>({});

  // 1. Charger les campagnes
  const {
    data: campaigns = [],
    isLoading: loadingCampaigns,
    refetch: refetchCampaigns
  } = useQuery({
    queryKey: ['evaluation-campaigns'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<EvaluationCampaign[]>>('/evaluations/campaigns');
      return res.data.data || [];
    }
  });

  // Définir la première campagne par défaut si vide
  React.useEffect(() => {
    if (!selectedCampaignId && campaigns.length > 0) {
      setSelectedCampaignId(campaigns[0].id);
    }
  }, [campaigns, selectedCampaignId]);

  // 2. Charger les évaluations selon le rôle
  const {
    data: evaluations = [],
    isLoading: loadingEvaluations,
    refetch: refetchEvaluations
  } = useQuery({
    queryKey: ['evaluations-list', selectedCampaignId, user?.id],
    queryFn: async () => {
      let endpoint = '/evaluations/my';
      if (isDRH && selectedCampaignId) {
        endpoint = `/evaluations/campaigns/${selectedCampaignId}/evaluations`;
      } else if (isManager) {
        endpoint = '/evaluations/team';
      }
      const res = await api.get<ApiResponse<EvaluationDto[]>>(endpoint);
      return res.data.data || [];
    },
    enabled: !!user
  });

  // Définir la première évaluation par défaut
  React.useEffect(() => {
    if (evaluations.length > 0 && !evaluations.some((e) => e.id === selectedEvalId)) {
      setSelectedEvalId(evaluations[0].id);
    }
  }, [evaluations, selectedEvalId]);

  const selectedEval = evaluations.find((e) => e.id === selectedEvalId) || evaluations[0];

  // Mutations
  const createCampaignMutation = useMutation({
    mutationFn: async () => {
      const year = Number(newCampaignYear) || currentYear;
      const start = newCampaignStartDate || `${year}-01-01`;
      const end = newCampaignEndDate || `${year}-12-31`;
      const res = await api.post<ApiResponse<EvaluationCampaign>>('/evaluations/campaigns', {
        title: (newCampaignTitle || `Campagne Annuelle d'Évaluation ${year}`).trim(),
        campaignYear: year,
        startDate: start,
        endDate: end,
        status: 'IN_PROGRESS'
      });
      return res.data;
    },
    onSuccess: (res) => {
      setShowNewCampaignModal(false);
      setCampaignError(null);
      queryClient.invalidateQueries({ queryKey: ['evaluation-campaigns'] });
      const newCamp = res?.data;
      if (newCamp?.id) {
        setSelectedCampaignId(newCamp.id);
        queryClient.invalidateQueries({ queryKey: ['evaluations-list'] });
      }
      setNotificationMsg({
        text: 'Campagne annuelle lancée avec succès ! Les fiches d’évaluation SMART ont été générées pour vos collaborateurs.',
        type: 'success',
      });
      setTimeout(() => setNotificationMsg(null), 6000);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Erreur lors du lancement de la campagne';
      setCampaignError(msg);
      setNotificationMsg({ text: msg, type: 'error' });
    }
  });

  const selfEvalMutation = useMutation({
    mutationFn: async () => {
      if (!selectedEval) return;
      await api.patch(`/evaluations/${selectedEval.id}/self-eval`, {
        selfScore: selfScoreInput,
        selfFeedback: selfFeedbackInput
      });
    },
    onSuccess: () => {
      setShowSelfEvalModal(false);
      refetchEvaluations();
    }
  });

  const managerEvalMutation = useMutation({
    mutationFn: async () => {
      if (!selectedEval) return;
      const rates = selectedEval.objectives.map((obj) => ({
        objectiveId: obj.id,
        achievementRate: objectiveRatesInputs[obj.id || ''] ?? obj.achievementRate ?? 100,
        status: 'ACHIEVED'
      }));

      await api.patch(`/evaluations/${selectedEval.id}/manager-eval`, {
        managerScore: managerScoreInput,
        managerFeedback: managerFeedbackInput,
        interviewDate: interviewDateInput,
        objectiveRates: rates
      });
    },
    onSuccess: () => {
      setShowManagerEvalModal(false);
      refetchEvaluations();
    }
  });

  const completeEvalMutation = useMutation({
    mutationFn: async () => {
      if (!selectedEval) return;
      await api.patch(`/evaluations/${selectedEval.id}/complete`, {
        finalFeedback: 'Bilan d’évaluation annuel validé et clôturé avec succès'
      });
    },
    onSuccess: () => {
      refetchEvaluations();
    }
  });

  // Téléchargement PDF
  const handleDownloadPdf = async (evalId: string, empName?: string) => {
    try {
      setDownloadingPdf(true);
      const name = empName || selectedEval?.employeeFullName || 'evaluation';
      await downloadFile(
        `/evaluations/${evalId}/pdf`,
        `entretien_annuel_${name.replace(/\s+/g, '_')}.pdf`
      );
    } catch (err) {
      console.error('Erreur téléchargement PDF :', err);
    } finally {
      setDownloadingPdf(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'DRAFT':
        return <span className="badge badge-neutral text-xs">Brouillon (Objectifs fixés)</span>;
      case 'SELF_SUBMITTED':
        return <span className="badge badge-warning text-xs">Auto-évaluation soumise</span>;
      case 'MANAGER_EVALUATED':
        return <span className="badge badge-info text-xs">Évaluation N+1 réalisée</span>;
      case 'COMPLETED':
      case 'VALIDATED':
        return <span className="badge badge-success text-xs">Clôturée & Signable</span>;
      default:
        return <span className="badge badge-neutral text-xs">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Évaluations Annuelles & Entretiens N+1</h1>
          <p>Notation de la performance, objectifs SMART mesurables et compte-rendu officiel PDF</p>
        </div>
        <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
          {isDRH && (
            <button
              type="button"
              onClick={() => setShowNewCampaignModal(true)}
              className="btn btn-primary text-xs w-full sm:w-auto min-h-[40px]"
            >
              <Plus className="w-4 h-4 mr-1.5" />
              Lancer une campagne
            </button>
          )}
          <button
            onClick={() => {
              refetchCampaigns();
              refetchEvaluations();
            }}
            className="btn btn-secondary text-xs min-h-[40px] px-3"
            title="Rafraîchir"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Alerte Notification */}
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
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
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

      {/* Grille des Campagnes Actives */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {loadingCampaigns ? (
          <div className="card p-6 col-span-full text-center text-xs text-gray-500">
            <Loader2 className="w-5 h-5 animate-spin mx-auto mb-2 text-[#1A6B3C]" />
            Chargement des campagnes...
          </div>
        ) : campaigns.length === 0 ? (
          <div className="card p-6 col-span-full text-center text-xs text-gray-400">
            Aucune campagne d'évaluation en cours.
          </div>
        ) : (
          campaigns.map((camp) => {
            const isSelected = camp.id === selectedCampaignId;
            return (
              <div
                key={camp.id}
                onClick={() => setSelectedCampaignId(camp.id)}
                className={`card p-5 cursor-pointer transition-all border ${
                  isSelected ? 'border-[#1A6B3C] ring-2 ring-[#1A6B3C]/20 bg-green-50/20' : 'hover:border-gray-300'
                }`}
              >
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="font-bold text-sm text-gray-900">{camp.title}</div>
                  {camp.status === 'CLOSED' ? (
                    <span className="badge badge-success text-[10px]">Clôturée</span>
                  ) : (
                    <span className="badge badge-warning text-[10px]">Active</span>
                  )}
                </div>
                <p className="text-xs text-gray-500 mb-4 line-clamp-2">
                  {camp.description || 'Campagne annuelle d’évaluation et de fixation des objectifs SMART.'}
                </p>
                <div className="flex items-center justify-between text-xs text-gray-400 pt-3 border-t border-gray-100">
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5" />
                    <span>
                      {camp.startDate} au {camp.endDate}
                    </span>
                  </div>
                  <span className="badge badge-primary text-[10px]">Année {camp.campaignYear || camp.year || 2026}</span>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Liste et Sélecteur des Fiches d'Évaluation */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Colonne gauche : liste des fiches */}
        <div className="card lg:col-span-1 p-4 space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-gray-100">
            <span className="card-title text-xs">
              {isDRH ? 'Fiches de la Campagne' : isManager ? 'Évaluations de mon Équipe' : 'Mes Évaluations'}
            </span>
            <span className="text-xs text-gray-400 font-semibold">{evaluations.length} fiche(s)</span>
          </div>

          {loadingEvaluations ? (
            <div className="p-8 text-center text-xs text-gray-400">
              <Loader2 className="w-5 h-5 animate-spin mx-auto mb-2 text-[#1A6B3C]" />
              Chargement des fiches...
            </div>
          ) : evaluations.length === 0 ? (
            <div className="p-8 text-center text-xs text-gray-400">
              Aucune évaluation trouvée pour ce périmètre.
            </div>
          ) : (
            <div className="space-y-2 max-h-[500px] overflow-y-auto">
              {evaluations.map((evalItem) => {
                const isSelected = evalItem.id === selectedEval?.id;
                return (
                  <div
                    key={evalItem.id}
                    onClick={() => {
                      setSelectedEvalId(evalItem.id);
                      // Initialiser les taux pour la modale manager
                      const rates: Record<string, number> = {};
                      evalItem.objectives?.forEach((o) => {
                        if (o.id) rates[o.id] = o.achievementRate ?? 100;
                      });
                      setObjectiveRatesInputs(rates);
                    }}
                    className={`p-3 rounded-lg border text-xs cursor-pointer transition-colors ${
                      isSelected
                        ? 'border-[#1A6B3C] bg-green-50/50'
                        : 'border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-semibold text-gray-900 truncate">
                        {evalItem.employeeFullName}
                      </span>
                      {evalItem.finalScore ? (
                        <span className="font-bold text-[#1A6B3C]">
                          {evalItem.finalScore.toFixed(1)} / 5
                        </span>
                      ) : (
                        <span className="text-gray-400">-- / 5</span>
                      )}
                    </div>
                    <div className="text-[11px] text-gray-500 truncate mb-2">
                      Évaluateur : {evalItem.evaluatorFullName || 'Manager N+1'}
                    </div>
                    <div className="flex items-center justify-between">
                      {getStatusBadge(evalItem.status)}
                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDownloadPdf(evalItem.id, evalItem.employeeFullName);
                          }}
                          className="p-1 text-gray-400 hover:text-[#1A6B3C] rounded hover:bg-gray-100 transition-colors"
                          title={`Télécharger le compte-rendu PDF de ${evalItem.employeeFullName}`}
                        >
                          <Download className="w-3.5 h-3.5" />
                        </button>
                        <ChevronRight className="w-4 h-4 text-gray-400" />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Colonne droite : Parcours en 4 Étapes de l'évaluation sélectionnée */}
        <div className="card lg:col-span-2">
          {selectedEval ? (
            <div>
              {/* Entête Fiche sélectionnée */}
              <div className="card-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <Target className="w-5 h-5 text-[#1A6B3C]" />
                    <span className="card-title text-base">
                      {selectedEval.employeeFullName}
                    </span>
                    {getStatusBadge(selectedEval.status)}
                  </div>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Campagne : {selectedEval.campaignTitle || 'Campagne Annuelle'} • Évaluateur :{' '}
                    {selectedEval.evaluatorFullName || 'Manager N+1'}
                  </p>
                </div>

                {/* Bouton de téléchargement PDF officiel */}
                <button
                  type="button"
                  onClick={() => handleDownloadPdf(selectedEval.id)}
                  disabled={downloadingPdf}
                  className="btn btn-primary text-xs min-h-[40px] px-3 w-full sm:w-auto justify-center"
                  title="Télécharger la synthèse d'entretien signable au format PDF"
                >
                  {downloadingPdf ? (
                    <Loader2 className="w-4 h-4 animate-spin mr-1.5" />
                  ) : (
                    <Download className="w-4 h-4 mr-1.5" />
                  )}
                  Télécharger Bilan Entretien (PDF)
                </button>
              </div>

              {/* Navigation du Parcours en 4 Étapes */}
              <div className="flex border-b border-gray-200 bg-gray-50/50 overflow-x-auto text-xs font-semibold">
                <button
                  onClick={() => setActiveStep('OBJECTIVES')}
                  className={`px-4 py-3 border-b-2 flex items-center gap-1.5 whitespace-nowrap ${
                    activeStep === 'OBJECTIVES'
                      ? 'border-[#1A6B3C] text-[#1A6B3C] bg-white'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                >
                  <Target className="w-4 h-4" />
                  1. Objectifs SMART ({selectedEval.objectives?.length || 0})
                </button>

                <button
                  onClick={() => setActiveStep('SELF_EVAL')}
                  className={`px-4 py-3 border-b-2 flex items-center gap-1.5 whitespace-nowrap ${
                    activeStep === 'SELF_EVAL'
                      ? 'border-[#1A6B3C] text-[#1A6B3C] bg-white'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                >
                  <User className="w-4 h-4" />
                  2. Auto-évaluation Salarié
                  {selectedEval.selfScore ? <Check className="w-3.5 h-3.5 text-green-600" /> : null}
                </button>

                <button
                  onClick={() => setActiveStep('MANAGER_EVAL')}
                  className={`px-4 py-3 border-b-2 flex items-center gap-1.5 whitespace-nowrap ${
                    activeStep === 'MANAGER_EVAL'
                      ? 'border-[#1A6B3C] text-[#1A6B3C] bg-white'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                >
                  <Award className="w-4 h-4" />
                  3. Évaluation Managériale
                  {selectedEval.managerScore ? <Check className="w-3.5 h-3.5 text-green-600" /> : null}
                </button>

                <button
                  onClick={() => setActiveStep('VALIDATION')}
                  className={`px-4 py-3 border-b-2 flex items-center gap-1.5 whitespace-nowrap ${
                    activeStep === 'VALIDATION'
                      ? 'border-[#1A6B3C] text-[#1A6B3C] bg-white'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                >
                  <Star className="w-4 h-4 text-amber-500" />
                  4. Score Final & Clôture
                </button>
              </div>

              {/* Contenu de l'étape active */}
              <div className="p-5 space-y-4">
                {/* ÉTAPE 1 : OBJECTIFS SMART */}
                {activeStep === 'OBJECTIVES' && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between text-xs text-gray-600">
                      <span>Pondération et taux d'atteinte validés lors de l'entretien :</span>
                      <span className="font-semibold text-gray-800">
                        Total Poids :{' '}
                        {(selectedEval.objectives || []).reduce((acc, o) => acc + (o.weight || 0), 0)}%
                      </span>
                    </div>

                    <div className="space-y-3">
                      {(selectedEval.objectives || []).map((obj, idx) => (
                        <div
                          key={obj.id || idx}
                          className="p-4 rounded-lg border border-gray-200 bg-gray-50/50 space-y-2.5"
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div>
                              <div className="font-semibold text-xs text-gray-900">{obj.title}</div>
                              <div className="text-[11px] text-gray-500 mt-0.5">{obj.description}</div>
                            </div>
                            <div className="text-right flex-shrink-0">
                              <span className="badge badge-primary text-[10px] font-mono">
                                Poids : {obj.weight}%
                              </span>
                            </div>
                          </div>

                          <div className="flex flex-wrap items-center justify-between text-xs pt-2 border-t border-gray-100 gap-2">
                            <span className="text-[11px] text-gray-500">
                              <strong>Catégorie :</strong> {obj.category}
                            </span>
                            <span className="font-bold text-xs text-[#1A6B3C]">
                              Taux d'atteinte : {obj.achievementRate ?? 0}%
                            </span>
                          </div>

                          {/* Barre de progression SMART */}
                          <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
                            <div
                              className="bg-[#1A6B3C] h-2 rounded-full transition-all"
                              style={{ width: `${Math.min(100, Math.max(0, obj.achievementRate ?? 0))}%` }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* ÉTAPE 2 : AUTO-ÉVALUATION SALARIÉ */}
                {activeStep === 'SELF_EVAL' && (
                  <div className="space-y-4">
                    <div className="p-4 bg-amber-50/60 border border-amber-200 rounded-lg text-xs space-y-2">
                      <div className="font-semibold text-amber-900 flex items-center gap-1.5">
                        <User className="w-4 h-4 text-amber-700" />
                        Appréciation personnelle du collaborateur
                      </div>
                      <div className="text-amber-800">
                        {selectedEval.selfScore ? (
                          <p>
                            Auto-notation : <strong>{selectedEval.selfScore} / 5</strong>
                          </p>
                        ) : (
                          <p className="italic text-gray-500">Auto-évaluation en attente de soumission.</p>
                        )}
                      </div>
                    </div>

                    {selectedEval.feedback && (
                      <div className="card p-4 text-xs space-y-1">
                        <span className="font-bold text-gray-700">Commentaires enregistrés :</span>
                        <p className="text-gray-600 whitespace-pre-wrap">{selectedEval.feedback}</p>
                      </div>
                    )}

                    {/* Bouton pour ouvrir la modale si éligible */}
                    {((user?.employeeId && selectedEval.employeeId === user.employeeId) || isDRH) && (
                      <button
                        type="button"
                        onClick={() => setShowSelfEvalModal(true)}
                        className="btn btn-secondary text-xs min-h-[40px]"
                      >
                        {selectedEval.selfScore ? 'Modifier mon auto-évaluation' : 'Soumettre mon auto-évaluation'}
                      </button>
                    )}
                  </div>
                )}

                {/* ÉTAPE 3 : ÉVALUATION MANAGÉRIALE */}
                {activeStep === 'MANAGER_EVAL' && (
                  <div className="space-y-4">
                    <div className="p-4 bg-blue-50/60 border border-blue-200 rounded-lg text-xs space-y-2">
                      <div className="font-semibold text-blue-900 flex items-center gap-1.5">
                        <Award className="w-4 h-4 text-blue-700" />
                        Notation et observations du Manager N+1
                      </div>
                      <div className="text-blue-800">
                        {selectedEval.managerScore ? (
                          <p>
                            Note Manager : <strong>{selectedEval.managerScore} / 5</strong> • Date
                            d'entretien : {selectedEval.interviewDate || 'Non renseignée'}
                          </p>
                        ) : (
                          <p className="italic text-gray-500">Entretien managérial non encore formalisé.</p>
                        )}
                      </div>
                    </div>

                    {(isManager || isDRH) && (
                      <button
                        type="button"
                        onClick={() => {
                          const rates: Record<string, number> = {};
                          selectedEval.objectives?.forEach((o) => {
                            if (o.id) rates[o.id] = o.achievementRate ?? 100;
                          });
                          setObjectiveRatesInputs(rates);
                          setShowManagerEvalModal(true);
                        }}
                        className="btn btn-primary text-xs min-h-[40px]"
                      >
                        Saisir l'évaluation managériale N+1
                      </button>
                    )}
                  </div>
                )}

                {/* ÉTAPE 4 : VALIDATION FINALE & SCORE GLOBAL */}
                {activeStep === 'VALIDATION' && (
                  <div className="space-y-6">
                    <div className="p-6 bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-xl flex flex-col sm:flex-row items-center justify-between gap-4">
                      <div>
                        <span className="text-xs font-semibold text-green-800 uppercase tracking-wider">
                          Score Global Calculé Automatiquement
                        </span>
                        <div className="text-3xl font-extrabold text-[#1A6B3C] mt-1">
                          {selectedEval.finalScore !== undefined && selectedEval.finalScore !== null
                            ? `${selectedEval.finalScore.toFixed(2)} / 5.0`
                            : 'En cours de calcul'}
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          Calculé sur la base de la moyenne pondérée des objectifs SMART
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleDownloadPdf(selectedEval.id)}
                        disabled={downloadingPdf}
                        className="btn btn-primary text-xs min-h-[44px] px-5 flex items-center gap-2"
                      >
                        {downloadingPdf ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <Download className="w-4 h-4" />
                        )}
                        Télécharger le Bilan Signable (PDF)
                      </button>
                    </div>

                    {selectedEval.status !== 'COMPLETED' && (isManager || isDRH) && (
                      <div className="card p-4 border-amber-200 bg-amber-50/40 flex items-center justify-between">
                        <div>
                          <div className="font-bold text-xs text-amber-900">
                            Clôture officielle de l'entretien annuel
                          </div>
                          <p className="text-[11px] text-amber-700">
                            Scelle les notes, génère l'attestation définitive et archive le bilan.
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => completeEvalMutation.mutate()}
                          disabled={completeEvalMutation.isPending}
                          className="btn btn-primary text-xs min-h-[40px]"
                        >
                          {completeEvalMutation.isPending && (
                            <Loader2 className="w-3.5 h-3.5 animate-spin mr-1" />
                          )}
                          Valider et Clôturer l'Évaluation
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="p-12 text-center text-gray-400 text-xs">
              Sélectionnez une fiche d'évaluation pour consulter son parcours SMART
            </div>
          )}
        </div>
      </div>

      {/* MODALE : NOUVELLE CAMPAGNE (DRH) */}
      {showNewCampaignModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 space-y-4 shadow-xl">
            <h3 className="text-sm font-bold text-gray-900">Lancer une Nouvelle Campagne Annuelle</h3>
            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-gray-700 mb-1">Titre de la campagne</label>
                <input
                  type="text"
                  value={newCampaignTitle}
                  onChange={(e) => setNewCampaignTitle(e.target.value)}
                  placeholder="Ex: Campagne Annuelle d'Évaluation 2026"
                  className="w-full border rounded-lg p-2.5"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Année</label>
                  <input
                    type="number"
                    value={newCampaignYear}
                    onChange={(e) => setNewCampaignYear(parseInt(e.target.value, 10))}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Date Début</label>
                  <input
                    type="date"
                    value={newCampaignStartDate}
                    onChange={(e) => setNewCampaignStartDate(e.target.value)}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
              </div>
              <div>
                <label className="block font-semibold text-gray-700 mb-1">Date Fin</label>
                <input
                  type="date"
                  value={newCampaignEndDate}
                  onChange={(e) => setNewCampaignEndDate(e.target.value)}
                  className="w-full border rounded-lg p-2.5"
                />
              </div>

              {campaignError && (
                <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                  <span>{campaignError}</span>
                </div>
              )}
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                type="button"
                onClick={() => setShowNewCampaignModal(false)}
                className="btn btn-secondary text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={() => createCampaignMutation.mutate()}
                disabled={createCampaignMutation.isPending}
                className="btn btn-primary text-xs flex items-center gap-1.5"
              >
                {createCampaignMutation.isPending && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                )}
                Lancer la campagne
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODALE : AUTO-ÉVALUATION COLLABORATEUR */}
      {showSelfEvalModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 space-y-4 shadow-xl">
            <h3 className="text-sm font-bold text-gray-900">Soumettre mon Auto-évaluation</h3>
            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Note d'auto-évaluation (/ 5.0)
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  max="5"
                  value={selfScoreInput}
                  onChange={(e) => setSelfScoreInput(parseFloat(e.target.value))}
                  className="w-full border rounded-lg p-2.5"
                />
              </div>
              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Commentaires & Souhaits d'évolution
                </label>
                <textarea
                  rows={4}
                  value={selfFeedbackInput}
                  onChange={(e) => setSelfFeedbackInput(e.target.value)}
                  placeholder="Points forts de l'année, difficultés rencontrées, besoins de formation..."
                  className="w-full border rounded-lg p-2.5"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                type="button"
                onClick={() => setShowSelfEvalModal(false)}
                className="btn btn-secondary text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={() => selfEvalMutation.mutate()}
                disabled={selfEvalMutation.isPending}
                className="btn btn-primary text-xs"
              >
                {selfEvalMutation.isPending && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1" />
                )}
                Enregistrer mon auto-évaluation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODALE : ÉVALUATION MANAGÉRIALE N+1 */}
      {showManagerEvalModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 space-y-4 shadow-xl max-h-[90vh] overflow-y-auto">
            <h3 className="text-sm font-bold text-gray-900">Évaluation Managériale N+1</h3>
            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Note globale (/ 5.0)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="5"
                    value={managerScoreInput}
                    onChange={(e) => setManagerScoreInput(parseFloat(e.target.value))}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Date d'entretien</label>
                  <input
                    type="date"
                    value={interviewDateInput}
                    onChange={(e) => setInterviewDateInput(e.target.value)}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
              </div>

              {/* Taux d'atteinte par objectif SMART */}
              <div className="space-y-2 pt-2 border-t">
                <span className="font-semibold text-gray-800">Taux d'atteinte par Objectif SMART (%) :</span>
                {selectedEval?.objectives.map((obj) => (
                  <div key={obj.id} className="flex items-center justify-between p-2 bg-gray-50 rounded-lg">
                    <span className="text-gray-700 truncate max-w-[240px]">{obj.title}</span>
                    <input
                      type="number"
                      min="0"
                      max="150"
                      value={objectiveRatesInputs[obj.id || ''] ?? obj.achievementRate ?? 100}
                      onChange={(e) =>
                        setObjectiveRatesInputs({
                          ...objectiveRatesInputs,
                          [obj.id || '']: parseFloat(e.target.value) || 0
                        })
                      }
                      className="w-20 border rounded p-1 text-right"
                    />
                  </div>
                ))}
              </div>

              <div>
                <label className="block font-semibold text-gray-700 mb-1">
                  Commentaires du manager & Perspectives
                </label>
                <textarea
                  rows={4}
                  value={managerFeedbackInput}
                  onChange={(e) => setManagerFeedbackInput(e.target.value)}
                  placeholder="Appréciation globale, objectifs pour l'année suivante..."
                  className="w-full border rounded-lg p-2.5"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                type="button"
                onClick={() => setShowManagerEvalModal(false)}
                className="btn btn-secondary text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={() => managerEvalMutation.mutate()}
                disabled={managerEvalMutation.isPending}
                className="btn btn-primary text-xs"
              >
                {managerEvalMutation.isPending && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1" />
                )}
                Calculer score & Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EvaluationListPage;
