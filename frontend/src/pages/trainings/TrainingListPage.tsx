import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  GraduationCap,
  Plus,
  Clock,
  Users,
  BookOpen,
  Calendar,
  MapPin,
  CheckCircle2,
  Download,
  AlertTriangle,
  Loader2,
  RefreshCw,
  Award,
  ChevronRight,
  Filter
} from 'lucide-react';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import { downloadFile } from '@/lib/downloadFile';
import type {
  TrainingDto,
  TrainingSessionDto,
  TrainingEnrollmentDto,
  ApiResponse
} from '@/types';

type ActiveTab = 'CATALOGUE' | 'SESSIONS' | 'MY_ENROLLMENTS';

export const TrainingListPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();
  const isDRH = hasRole('DRH') || hasRole('ADMIN') || hasRole('ROLE_DRH') || hasRole('ROLE_ADMIN');

  const [activeTab, setActiveTab] = useState<ActiveTab>('CATALOGUE');
  const [downloadingCertId, setDownloadingCertId] = useState<string | null>(null);

  // Modale Nouveau Module (DRH)
  const [showNewTrainingModal, setShowNewTrainingModal] = useState<boolean>(false);
  const [newCode, setNewCode] = useState<string>('');
  const [newTitle, setNewTitle] = useState<string>('');
  const [newCategory, setNewCategory] = useState<string>('Technique');
  const [newDuration, setNewDuration] = useState<number>(35);
  const [newProvider, setNewProvider] = useState<string>('');
  const [newDescription, setNewDescription] = useState<string>('');

  // Modale Planifier Session (DRH)
  const [showNewSessionModal, setShowNewSessionModal] = useState<boolean>(false);
  const [selectedTrainingIdForSession, setSelectedTrainingIdForSession] = useState<string>('');
  const [newSessionCode, setNewSessionCode] = useState<string>('');
  const [newStartDate, setNewStartDate] = useState<string>('');
  const [newEndDate, setNewEndDate] = useState<string>('');
  const [newLocation, setNewLocation] = useState<string>('Dakar Plateau');
  const [newMaxParticipants, setNewMaxParticipants] = useState<number>(15);

  // 1. Catalogue des formations
  const {
    data: trainings = [],
    isLoading: loadingTrainings,
    refetch: refetchTrainings
  } = useQuery({
    queryKey: ['trainings-catalogue'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TrainingDto[]>>('/trainings');
      return res.data.data || [];
    }
  });

  // 2. Sessions de formation
  const {
    data: sessions = [],
    isLoading: loadingSessions,
    refetch: refetchSessions
  } = useQuery({
    queryKey: ['training-sessions'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TrainingSessionDto[]>>('/trainings/sessions');
      return res.data.data || [];
    }
  });

  // 3. Mes inscriptions
  const {
    data: myEnrollments = [],
    isLoading: loadingEnrollments,
    refetch: refetchEnrollments
  } = useQuery({
    queryKey: ['my-training-enrollments', user?.id],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TrainingEnrollmentDto[]>>('/trainings/my-enrollments');
      return res.data.data || [];
    },
    enabled: !!user
  });

  // Mutations
  const createTrainingMutation = useMutation({
    mutationFn: async () => {
      await api.post('/trainings', {
        code: newCode,
        title: newTitle,
        category: newCategory,
        durationHours: newDuration,
        provider: newProvider,
        description: newDescription
      });
    },
    onSuccess: () => {
      setShowNewTrainingModal(false);
      refetchTrainings();
    }
  });

  const createSessionMutation = useMutation({
    mutationFn: async () => {
      await api.post('/trainings/sessions', {
        trainingId: selectedTrainingIdForSession,
        sessionCode: newSessionCode,
        startDate: new Date(newStartDate).toISOString(),
        endDate: new Date(newEndDate).toISOString(),
        location: newLocation,
        maxParticipants: newMaxParticipants
      });
    },
    onSuccess: () => {
      setShowNewSessionModal(false);
      refetchSessions();
    }
  });

  const enrollMutation = useMutation({
    mutationFn: async (sessionId: string) => {
      await api.post('/trainings/sessions/enroll', { sessionId });
    },
    onSuccess: () => {
      refetchSessions();
      refetchEnrollments();
      setActiveTab('MY_ENROLLMENTS');
    }
  });

  // Téléchargement Attestation PDF
  const handleDownloadCertificate = async (enrollmentId: string, trainingTitle: string) => {
    try {
      setDownloadingCertId(enrollmentId);
      await downloadFile(
        `/trainings/enrollments/${enrollmentId}/certificate`,
        `attestation_formation_${trainingTitle.replace(/\s+/g, '_')}.pdf`
      );
    } catch (err) {
      console.error('Erreur téléchargement attestation PDF :', err);
    } finally {
      setDownloadingCertId(null);
    }
  };

  const getEnrollmentBadge = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
      case 'ENROLLED':
        return <span className="badge badge-primary text-xs">Inscrit & Confirmé</span>;
      case 'ATTENDED':
      case 'IN_PROGRESS':
        return <span className="badge badge-info text-xs">Session en cours</span>;
      case 'COMPLETED':
        return <span className="badge badge-success text-xs">Formation Complétée</span>;
      case 'DROPPED':
      case 'CANCELLED':
        return <span className="badge badge-danger text-xs">Annulée</span>;
      default:
        return <span className="badge badge-neutral text-xs">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Catalogue & Plans de Formation</h1>
          <p>Développement des compétences, sessions certifiantes et attestations officielles PDF</p>
        </div>
        <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
          {isDRH && (
            <>
              <button
                type="button"
                onClick={() => setShowNewTrainingModal(true)}
                className="btn btn-primary text-xs min-h-[40px] flex-1 sm:flex-none justify-center"
              >
                <Plus className="w-4 h-4 mr-1.5" />
                Ajouter une formation
              </button>
              <button
                type="button"
                onClick={() => {
                  if (trainings.length > 0) {
                    setSelectedTrainingIdForSession(trainings[0].id);
                  }
                  setShowNewSessionModal(true);
                }}
                className="btn btn-secondary text-xs min-h-[40px] flex-1 sm:flex-none justify-center"
              >
                <Calendar className="w-4 h-4 mr-1.5" />
                Planifier Session
              </button>
            </>
          )}
          <button
            onClick={() => {
              refetchTrainings();
              refetchSessions();
              refetchEnrollments();
            }}
            className="btn btn-secondary text-xs min-h-[40px] px-3"
            title="Rafraîchir"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Onglets Principaux */}
      <div className="flex border-b border-gray-200 bg-white rounded-t-xl overflow-x-auto text-xs font-semibold">
        <button
          onClick={() => setActiveTab('CATALOGUE')}
          className={`px-5 py-3.5 border-b-2 flex items-center gap-2 whitespace-nowrap transition-colors ${
            activeTab === 'CATALOGUE'
              ? 'border-[#1A6B3C] text-[#1A6B3C] bg-green-50/20'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <BookOpen className="w-4 h-4" />
          Catalogue des Formations ({trainings.length})
        </button>

        <button
          onClick={() => setActiveTab('SESSIONS')}
          className={`px-5 py-3.5 border-b-2 flex items-center gap-2 whitespace-nowrap transition-colors ${
            activeTab === 'SESSIONS'
              ? 'border-[#1A6B3C] text-[#1A6B3C] bg-green-50/20'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Calendar className="w-4 h-4" />
          Sessions Disponibles ({sessions.length})
        </button>

        <button
          onClick={() => setActiveTab('MY_ENROLLMENTS')}
          className={`px-5 py-3.5 border-b-2 flex items-center gap-2 whitespace-nowrap transition-colors ${
            activeTab === 'MY_ENROLLMENTS'
              ? 'border-[#1A6B3C] text-[#1A6B3C] bg-green-50/20'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Award className="w-4 h-4" />
          Mon Parcours & Attestations ({myEnrollments.length})
        </button>
      </div>

      {/* ─── ONGLET 1 : CATALOGUE DES FORMATIONS ─────────────────────────────── */}
      {activeTab === 'CATALOGUE' && (
        <div>
          {loadingTrainings ? (
            <div className="card p-12 text-center text-xs text-gray-400">
              <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-[#1A6B3C]" />
              Chargement du catalogue...
            </div>
          ) : trainings.length === 0 ? (
            <div className="card p-12 text-center text-xs text-gray-400">
              Aucun module de formation disponible dans le catalogue.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {trainings.map((tr) => (
                <div key={tr.id} className="card p-5 flex flex-col justify-between hover:border-gray-300 transition-all">
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="badge badge-primary font-mono text-[10px]">{tr.code}</span>
                      <span className="badge badge-neutral text-[10px]">{tr.category}</span>
                    </div>
                    <h3 className="font-bold text-sm text-gray-900 mb-2 leading-snug">{tr.title}</h3>
                    <p className="text-xs text-gray-500 mb-4 line-clamp-3">{tr.description}</p>
                  </div>

                  <div className="pt-3 border-t border-gray-100 flex items-center justify-between text-xs text-gray-500">
                    <div className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5 text-gray-400" />
                      <span>{tr.durationHours || 0} heures</span>
                    </div>
                    <span className="text-[11px] font-semibold text-[#1A6B3C] truncate max-w-[140px]">
                      {tr.provider || 'CareerTrack Academy'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ─── ONGLET 2 : SESSIONS PLANIFIÉES ──────────────────────────────────── */}
      {activeTab === 'SESSIONS' && (
        <div>
          {loadingSessions ? (
            <div className="card p-12 text-center text-xs text-gray-400">
              <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-[#1A6B3C]" />
              Chargement des sessions...
            </div>
          ) : sessions.length === 0 ? (
            <div className="card p-12 text-center text-xs text-gray-400">
              Aucune session de formation actuellement planifiée.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {sessions.map((sess) => {
                const isFull = sess.enrolledParticipantsCount >= sess.maxParticipants;
                const isAlreadyEnrolled = myEnrollments.some((e) => e.sessionId === sess.id);

                return (
                  <div key={sess.id} className="card p-5 flex flex-col justify-between space-y-4">
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="badge badge-primary font-mono text-[10px]">{sess.sessionCode}</span>
                        {isFull ? (
                          <span className="badge badge-danger text-[10px]">Complet</span>
                        ) : (
                          <span className="badge badge-success text-[10px]">Inscriptions Ouvertes</span>
                        )}
                      </div>
                      <h3 className="font-bold text-sm text-gray-900 mb-1">{sess.trainingTitle}</h3>
                      <div className="flex items-center gap-1.5 text-xs text-gray-500 mb-3">
                        <MapPin className="w-3.5 h-3.5 text-gray-400" />
                        <span>{sess.location || 'Dakar'}</span>
                      </div>

                      {/* Quota des participants */}
                      <div className="space-y-1">
                        <div className="flex justify-between text-xs text-gray-500">
                          <span>Places réservées</span>
                          <span className="font-semibold text-gray-800">
                            {sess.enrolledParticipantsCount} / {sess.maxParticipants}
                          </span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                          <div
                            className={`h-1.5 rounded-full ${isFull ? 'bg-red-500' : 'bg-[#1A6B3C]'}`}
                            style={{
                              width: `${Math.min(100, (sess.enrolledParticipantsCount / sess.maxParticipants) * 100)}%`
                            }}
                          />
                        </div>
                      </div>
                    </div>

                    <div className="pt-3 border-t border-gray-100 flex items-center justify-between gap-2">
                      <div className="text-xs text-gray-400">
                        {new Date(sess.startDate).toLocaleDateString('fr-FR')} au{' '}
                        {new Date(sess.endDate).toLocaleDateString('fr-FR')}
                      </div>

                      {isAlreadyEnrolled ? (
                        <span className="text-xs font-semibold text-green-700 bg-green-50 px-2.5 py-1 rounded">
                          Déjà inscrit
                        </span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => enrollMutation.mutate(sess.id)}
                          disabled={isFull || enrollMutation.isPending}
                          className="btn btn-primary text-xs min-h-[38px] px-3"
                        >
                          {enrollMutation.isPending && (
                            <Loader2 className="w-3.5 h-3.5 animate-spin mr-1" />
                          )}
                          S'inscrire
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ─── ONGLET 3 : MON PARCOURS & ATTESTATIONS PDF ──────────────────────── */}
      {activeTab === 'MY_ENROLLMENTS' && (
        <div>
          {loadingEnrollments ? (
            <div className="card p-12 text-center text-xs text-gray-400">
              <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-[#1A6B3C]" />
              Chargement de vos inscriptions...
            </div>
          ) : myEnrollments.length === 0 ? (
            <div className="card p-12 text-center text-xs text-gray-400">
              Vous n'avez pas encore d'inscription à une formation.
            </div>
          ) : (
            <div className="space-y-3">
              {myEnrollments.map((enr) => (
                <div
                  key={enr.id}
                  className="card p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="badge badge-primary font-mono text-[10px]">{enr.sessionCode}</span>
                      {getEnrollmentBadge(enr.status)}
                    </div>
                    <h3 className="font-bold text-sm text-gray-900">{enr.trainingTitle}</h3>
                    <div className="text-xs text-gray-500">
                      Organisme : {enr.provider || 'CareerTrack Academy'} • Durée : {enr.durationHours || 0}h •
                      Inscrit le {enr.enrollmentDate}
                    </div>
                    {enr.score && (
                      <div className="text-xs text-[#1A6B3C] font-semibold">
                        Score final validé : {enr.score}%
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-3 w-full md:w-auto">
                    {/* Bouton de téléchargement de l'attestation PDF */}
                    <button
                      type="button"
                      onClick={() => handleDownloadCertificate(enr.id, enr.trainingTitle || 'formation')}
                      disabled={downloadingCertId === enr.id}
                      className="btn btn-primary text-xs min-h-[40px] px-4 w-full md:w-auto flex items-center justify-center gap-2"
                      title="Télécharger l'attestation officielle de formation signée"
                    >
                      {downloadingCertId === enr.id ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <Download className="w-4 h-4" />
                      )}
                      Télécharger l'Attestation (PDF)
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* MODALE : NOUVEAU MODULE DE FORMATION (DRH) */}
      {showNewTrainingModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 space-y-4 shadow-xl">
            <h3 className="text-sm font-bold text-gray-900">Ajouter une Formation au Catalogue</h3>
            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Code Module</label>
                  <input
                    type="text"
                    value={newCode}
                    onChange={(e) => setNewCode(e.target.value)}
                    placeholder="Ex: TECH-SPRING"
                    className="w-full border rounded-lg p-2.5 font-mono"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Catégorie</label>
                  <select
                    value={newCategory}
                    onChange={(e) => setNewCategory(e.target.value)}
                    className="w-full border rounded-lg p-2.5 bg-white"
                  >
                    <option value="Technique">Technique</option>
                    <option value="Management">Management</option>
                    <option value="Juridique / RH">Juridique / RH</option>
                    <option value="Soft Skills">Soft Skills</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block font-semibold text-gray-700 mb-1">Intitulé de la formation</label>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="Ex: Spring Boot 3 & Microservices"
                  className="w-full border rounded-lg p-2.5"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Volume Horaire (h)</label>
                  <input
                    type="number"
                    value={newDuration}
                    onChange={(e) => setNewDuration(parseInt(e.target.value, 10))}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Organisme / Formateur</label>
                  <input
                    type="text"
                    value={newProvider}
                    onChange={(e) => setNewProvider(e.target.value)}
                    placeholder="Ex: SupInfo Dakar"
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
              </div>
              <div>
                <label className="block font-semibold text-gray-700 mb-1">Description pédagogique</label>
                <textarea
                  rows={3}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="Objectifs pédagogiques et compétences visées..."
                  className="w-full border rounded-lg p-2.5"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                type="button"
                onClick={() => setShowNewTrainingModal(false)}
                className="btn btn-secondary text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={() => createTrainingMutation.mutate()}
                disabled={!newCode || !newTitle || createTrainingMutation.isPending}
                className="btn btn-primary text-xs"
              >
                {createTrainingMutation.isPending && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1" />
                )}
                Enregistrer la formation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODALE : PLANIFIER UNE SESSION (DRH) */}
      {showNewSessionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 space-y-4 shadow-xl">
            <h3 className="text-sm font-bold text-gray-900">Planifier une Session de Formation</h3>
            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-gray-700 mb-1">Module de formation</label>
                <select
                  value={selectedTrainingIdForSession}
                  onChange={(e) => setSelectedTrainingIdForSession(e.target.value)}
                  className="w-full border rounded-lg p-2.5 bg-white"
                >
                  {trainings.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.code} - {t.title}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Code Session</label>
                  <input
                    type="text"
                    value={newSessionCode}
                    onChange={(e) => setNewSessionCode(e.target.value)}
                    placeholder="Ex: SESS-2026-01"
                    className="w-full border rounded-lg p-2.5 font-mono"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Places max</label>
                  <input
                    type="number"
                    value={newMaxParticipants}
                    onChange={(e) => setNewMaxParticipants(parseInt(e.target.value, 10))}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Date Début</label>
                  <input
                    type="date"
                    value={newStartDate}
                    onChange={(e) => setNewStartDate(e.target.value)}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-gray-700 mb-1">Date Fin</label>
                  <input
                    type="date"
                    value={newEndDate}
                    onChange={(e) => setNewEndDate(e.target.value)}
                    className="w-full border rounded-lg p-2.5"
                  />
                </div>
              </div>
              <div>
                <label className="block font-semibold text-gray-700 mb-1">Lieu / Salle</label>
                <input
                  type="text"
                  value={newLocation}
                  onChange={(e) => setNewLocation(e.target.value)}
                  placeholder="Ex: Salle SupInfo Dakar ou distanciel"
                  className="w-full border rounded-lg p-2.5"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                type="button"
                onClick={() => setShowNewSessionModal(false)}
                className="btn btn-secondary text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={() => createSessionMutation.mutate()}
                disabled={!newSessionCode || !newStartDate || !newEndDate || createSessionMutation.isPending}
                className="btn btn-primary text-xs"
              >
                {createSessionMutation.isPending && (
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1" />
                )}
                Planifier la session
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TrainingListPage;
