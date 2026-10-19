import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import { downloadFile } from '@/lib/downloadFile';
import {
  ArrowLeft,
  User,
  Briefcase,
  Building,
  Calendar,
  CreditCard,
  FileText,
  History,
  Phone,
  MapPin,
  Mail,
  ShieldAlert,
  Edit,
  UserX,
  Download,
  AlertCircle,
  CheckCircle,
  X,
  Loader2,
  TrendingUp,
  Award
} from 'lucide-react';
import type {
  EmployeeDetailDto,
  EmployeeUpdateDto,
  EmployeeSelfUpdateDto,
  EmployeeDeactivateDto,
  Department,
  Position,
  ApiResponse
} from '@/types';

export const EmployeeDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'info' | 'contract' | 'history' | 'documents'>('info');
  const [showEditModal, setShowEditModal] = useState(false);
  const [showContactModal, setShowContactModal] = useState(false);
  const [showDeactivateModal, setShowDeactivateModal] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // 1. Récupération du dossier collaborateur complet
  const {
    data: employeeDetail,
    isLoading,
    isError,
    error,
    refetch
  } = useQuery<EmployeeDetailDto>({
    queryKey: ['employee', id],
    queryFn: async () => {
      const res = await api.get<ApiResponse<EmployeeDetailDto>>(`/employees/${id}`);
      return res.data.data;
    },
    enabled: !!id,
    retry: 1
  });

  // 2. Référentiels pour formulaires
  const { data: departments } = useQuery<Department[]>({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Department[]>>('/departments');
      return res.data.data;
    },
    enabled: showEditModal
  });

  const { data: positions } = useQuery<Position[]>({
    queryKey: ['positions'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Position[]>>('/positions');
      return res.data.data;
    },
    enabled: showEditModal
  });

  const { data: potentialManagers } = useQuery<{ id: string; fullName: string }[]>({
    queryKey: ['potentialManagers'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<{ content: { id: string; fullName: string }[] }>>('/employees?size=100');
      return res.data.data.content;
    },
    enabled: showEditModal
  });

  // Form states
  const [contactForm, setContactForm] = useState<EmployeeSelfUpdateDto>({ phone: '', address: '' });
  const [deactivateForm, setDeactivateForm] = useState<EmployeeDeactivateDto>({ reason: '', departureDate: '' });
  const [editForm, setEditForm] = useState<Partial<EmployeeUpdateDto>>({});

  // Mutations
  const contactMutation = useMutation({
    mutationFn: async (payload: EmployeeSelfUpdateDto) => {
      const res = await api.patch<ApiResponse<EmployeeDetailDto>>(`/employees/${id}/contact`, payload);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employee', id] });
      setShowContactModal(false);
      setSuccessMessage('Coordonnées mises à jour avec succès');
      setTimeout(() => setSuccessMessage(null), 4000);
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.message || 'Erreur lors de la mise à jour des coordonnées');
    }
  });

  const deactivateMutation = useMutation({
    mutationFn: async (payload: EmployeeDeactivateDto) => {
      await api.patch(`/employees/${id}/deactivate`, payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employee', id] });
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      setShowDeactivateModal(false);
      setSuccessMessage('Collaborateur désactivé avec succès');
      setTimeout(() => setSuccessMessage(null), 4000);
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.message || 'Erreur lors de la désactivation');
    }
  });

  const updateMutation = useMutation({
    mutationFn: async (payload: EmployeeUpdateDto) => {
      const res = await api.put<ApiResponse<EmployeeDetailDto>>(`/employees/${id}`, payload);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employee', id] });
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      setShowEditModal(false);
      setSuccessMessage('Dossier mis à jour avec succès');
      setTimeout(() => setSuccessMessage(null), 4000);
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.message || 'Erreur lors de la mise à jour du dossier');
    }
  });

  const openContactModal = () => {
    setErrorMessage(null);
    setContactForm({
      phone: employeeDetail?.personalData?.phone || '',
      address: employeeDetail?.personalData?.address || ''
    });
    setShowContactModal(true);
  };

  const openEditModal = () => {
    setErrorMessage(null);
    if (!employeeDetail) return;
    setEditForm({
      firstName: employeeDetail.identity.firstName,
      lastName: employeeDetail.identity.lastName,
      nationalId: employeeDetail.personalData?.nationalId || '',
      email: employeeDetail.identity.email,
      phone: employeeDetail.personalData?.phone || '',
      address: employeeDetail.personalData?.address || '',
      birthDate: employeeDetail.personalData?.birthDate || '',
      hireDate: employeeDetail.identity.hireDate,
      departmentId: employeeDetail.identity.departmentId,
      positionId: employeeDetail.identity.positionId,
      managerId: employeeDetail.identity.managerId || undefined,
      status: employeeDetail.identity.status,
      isCadre: employeeDetail.personalData?.isCadre ?? false,
      maritalStatus: employeeDetail.personalData?.maritalStatus || 'CELIBATAIRE',
      dependentsCount: employeeDetail.personalData?.dependentsCount ?? 0,
      grossSalary: employeeDetail.contract?.grossSalary,
      changeReason: ''
    });
    setShowEditModal(true);
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] gap-3">
        <div className="spinner spinner-lg text-[#1A6B3C]" />
        <span className="text-sm text-gray-500">Chargement du dossier collaborateur...</span>
      </div>
    );
  }

  if (isError || !employeeDetail) {
    const status = (error as any)?.response?.status;
    return (
      <div className="max-w-xl mx-auto my-12 p-6 bg-white rounded-2xl shadow-sm border border-gray-100 text-center space-y-4">
        <div className="w-14 h-14 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto">
          <ShieldAlert className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-gray-900">
          {status === 403 ? 'Accès non autorisé' : 'Dossier introuvable'}
        </h2>
        <p className="text-sm text-gray-600">
          {status === 403
            ? "Vous ne disposez pas des autorisations nécessaires pour consulter le dossier complet de ce collaborateur."
            : "Le dossier demandé n'existe pas ou a été supprimé."}
        </p>
        <div className="pt-2">
          <button
            onClick={() => navigate('/employees')}
            className="btn btn-secondary inline-flex items-center gap-2"
          >
            <ArrowLeft className="w-4 h-4" />
            Retour à l'annuaire
          </button>
        </div>
      </div>
    );
  }

  const { identity, personalData, contract, salaryHistory, positionHistory, documents, permissions } = employeeDetail;

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 py-4">
      {/* Messages d'alerte */}
      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex items-center justify-between text-sm shadow-sm animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage(null)} className="text-emerald-700 hover:text-emerald-900">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-xl flex items-center justify-between text-sm shadow-sm animate-fade-in">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-red-700 hover:text-red-900">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Navigation retour */}
      <div>
        <button
          onClick={() => navigate('/employees')}
          className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-[#1A6B3C] font-medium transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Retour à la liste des employés
        </button>
      </div>

      {/* En-tête profil */}
      <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex items-start sm:items-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#1A6B3C] to-[#15602f] text-white flex items-center justify-center font-bold text-xl shadow-md shrink-0">
            {identity.firstName[0]}
            {identity.lastName[0]}
          </div>
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-bold text-gray-900">{identity.fullName}</h1>
              <span className="badge badge-outline text-xs font-mono">{identity.employeeNumber}</span>
              <span
                className={`badge ${
                  identity.status === 'ACTIVE'
                    ? 'badge-success'
                    : identity.status === 'ON_LEAVE'
                    ? 'badge-warning'
                    : 'badge-danger'
                }`}
              >
                {identity.status === 'ACTIVE'
                  ? 'Actif'
                  : identity.status === 'ON_LEAVE'
                  ? 'En congé'
                  : 'Inactif / Sorti'}
              </span>
              {personalData?.isCadre && (
                <span className="badge bg-amber-50 text-amber-800 border border-amber-200 text-xs font-semibold">
                  Cadre
                </span>
              )}
            </div>
            <p className="text-sm font-medium text-[#1A6B3C] flex items-center gap-1.5">
              <Briefcase className="w-4 h-4" />
              {identity.positionTitle || 'Poste non attribué'}
              <span className="text-gray-300">•</span>
              <span className="text-gray-600 font-normal">{identity.departmentName || 'Département non assigné'}</span>
            </p>
          </div>
        </div>

        {/* Boutons d'action */}
        <div className="flex flex-wrap items-center gap-2.5">
          {permissions.canEditContact && !permissions.canEdit && (
            <button
              onClick={openContactModal}
              className="btn btn-secondary inline-flex items-center gap-2 text-xs font-medium min-h-[44px]"
            >
              <Phone className="w-4 h-4" />
              Modifier mes coordonnées
            </button>
          )}

          {permissions.canEdit && (
            <button
              onClick={openEditModal}
              className="btn btn-primary inline-flex items-center gap-2 text-xs font-semibold min-h-[44px]"
            >
              <Edit className="w-4 h-4" />
              Modifier le dossier
            </button>
          )}

          {permissions.canDeactivate && (
            <button
              onClick={() => {
                setErrorMessage(null);
                setDeactivateForm({ reason: '', departureDate: new Date().toISOString().split('T')[0] });
                setShowDeactivateModal(true);
              }}
              className="btn btn-danger inline-flex items-center gap-2 text-xs font-semibold min-h-[44px]"
            >
              <UserX className="w-4 h-4" />
              Désactiver
            </button>
          )}
        </div>
      </div>

      {/* Onglets Desktop / Mobile */}
      <div className="flex border-b border-gray-200 gap-2 overflow-x-auto pb-1 scrollbar-none">
        <button
          onClick={() => setActiveTab('info')}
          className={`px-4 py-2.5 text-sm font-medium border-b-2 whitespace-nowrap transition-colors min-h-[44px] flex items-center gap-2 ${
            activeTab === 'info'
              ? 'border-[#1A6B3C] text-[#1A6B3C]'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <User className="w-4 h-4" />
          Informations
        </button>

        <button
          onClick={() => setActiveTab('contract')}
          className={`px-4 py-2.5 text-sm font-medium border-b-2 whitespace-nowrap transition-colors min-h-[44px] flex items-center gap-2 ${
            activeTab === 'contract'
              ? 'border-[#1A6B3C] text-[#1A6B3C]'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <FileText className="w-4 h-4" />
          Contrat & Salaire
        </button>

        <button
          onClick={() => setActiveTab('history')}
          className={`px-4 py-2.5 text-sm font-medium border-b-2 whitespace-nowrap transition-colors min-h-[44px] flex items-center gap-2 ${
            activeTab === 'history'
              ? 'border-[#1A6B3C] text-[#1A6B3C]'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <History className="w-4 h-4" />
          Avancements & Historique
        </button>

        {documents && (
          <button
            onClick={() => setActiveTab('documents')}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 whitespace-nowrap transition-colors min-h-[44px] flex items-center gap-2 ${
              activeTab === 'documents'
                ? 'border-[#1A6B3C] text-[#1A6B3C]'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <Download className="w-4 h-4" />
            Documents officiels ({documents.length})
          </button>
        )}
      </div>

      {/* Contenu de l'onglet actif */}
      <div className="space-y-6">
        {/* Onglet 1 : Informations */}
        {activeTab === 'info' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Bloc Identité Professionnelle */}
            <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm space-y-4">
              <h2 className="text-base font-bold text-gray-900 flex items-center gap-2 border-b border-gray-100 pb-3">
                <Briefcase className="w-5 h-5 text-[#1A6B3C]" />
                Identité Professionnelle
              </h2>
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                <div>
                  <dt className="text-xs text-gray-500 uppercase font-medium">Matricule</dt>
                  <dd className="font-semibold text-gray-900 mt-0.5">{identity.employeeNumber}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 uppercase font-medium">Email professionnel</dt>
                  <dd className="font-medium text-gray-900 mt-0.5 truncate">{identity.email}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 uppercase font-medium">Date d'embauche</dt>
                  <dd className="font-medium text-gray-900 mt-0.5">{identity.hireDate}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 uppercase font-medium">Département</dt>
                  <dd className="font-medium text-gray-900 mt-0.5">{identity.departmentName || 'Non défini'}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 uppercase font-medium">Poste</dt>
                  <dd className="font-medium text-gray-900 mt-0.5">{identity.positionTitle || 'Non défini'}</dd>
                </div>
                <div>
                  <dt className="text-xs text-gray-500 uppercase font-medium">Responsable hiérarchique (N+1)</dt>
                  <dd className="font-medium text-gray-900 mt-0.5">
                    {identity.managerName ? (
                      <span className="text-[#1A6B3C] font-semibold">{identity.managerName}</span>
                    ) : (
                      <span className="text-gray-400 italic">Aucun (Direction Générale)</span>
                    )}
                  </dd>
                </div>
              </dl>
            </div>

            {/* Bloc Données Personnelles (si autorisé) */}
            <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm space-y-4">
              <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                <h2 className="text-base font-bold text-gray-900 flex items-center gap-2">
                  <User className="w-5 h-5 text-[#1A6B3C]" />
                  Données Personnelles
                </h2>
                {permissions.canEditContact && (
                  <button
                    onClick={openContactModal}
                    className="text-xs text-[#1A6B3C] hover:underline font-medium"
                  >
                    Modifier coordonnées
                  </button>
                )}
              </div>

              {personalData ? (
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                  <div>
                    <dt className="text-xs text-gray-500 uppercase font-medium">Numéro CNI / Passeport</dt>
                    <dd className="font-semibold text-gray-900 mt-0.5">{personalData.nationalId}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-gray-500 uppercase font-medium">Date de naissance</dt>
                    <dd className="font-medium text-gray-900 mt-0.5">{personalData.birthDate}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-gray-500 uppercase font-medium">Téléphone personnel</dt>
                    <dd className="font-medium text-gray-900 mt-0.5">{personalData.phone || 'Non renseigné'}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-gray-500 uppercase font-medium">Adresse</dt>
                    <dd className="font-medium text-gray-900 mt-0.5">{personalData.address || 'Non renseignée'}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-gray-500 uppercase font-medium">Situation familiale</dt>
                    <dd className="font-medium text-gray-900 mt-0.5">{personalData.maritalStatus || 'Non précisé'}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-gray-500 uppercase font-medium">Personnes à charge</dt>
                    <dd className="font-medium text-gray-900 mt-0.5">{personalData.dependentsCount ?? 0}</dd>
                  </div>
                </dl>
              ) : (
                <div className="p-4 bg-gray-50 rounded-xl text-center text-xs text-gray-500">
                  Les données personnelles et coordonnées privées sont strictement confidentielles et réservées à la DRH et au collaborateur.
                </div>
              )}
            </div>
          </div>
        )}

        {/* Onglet 2 : Contrat & Salaire */}
        {activeTab === 'contract' && (
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm space-y-6">
            <h2 className="text-base font-bold text-gray-900 flex items-center gap-2 border-b border-gray-100 pb-3">
              <CreditCard className="w-5 h-5 text-[#1A6B3C]" />
              Contrat de travail & Rémunération
            </h2>

            {contract ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
                <div className="p-4 bg-gray-50 rounded-xl">
                  <span className="text-xs text-gray-500 font-medium">Type de contrat</span>
                  <p className="text-lg font-bold text-gray-900 mt-1">{contract.contractType}</p>
                </div>

                <div className="p-4 bg-gray-50 rounded-xl">
                  <span className="text-xs text-gray-500 font-medium">Date de début</span>
                  <p className="text-lg font-bold text-gray-900 mt-1">{contract.startDate}</p>
                </div>

                <div className="p-4 bg-gray-50 rounded-xl">
                  <span className="text-xs text-gray-500 font-medium">Date de fin</span>
                  <p className="text-lg font-bold text-gray-900 mt-1">{contract.endDate || 'Indéterminée (CDI)'}</p>
                </div>

                <div className="p-4 bg-[#1A6B3C]/5 border border-[#1A6B3C]/20 rounded-xl">
                  <span className="text-xs text-[#1A6B3C] font-semibold">Salaire brut mensuel</span>
                  <p className="text-lg font-extrabold text-[#1A6B3C] mt-1">
                    {contract.grossSalary != null
                      ? `${new Intl.NumberFormat('fr-FR').format(contract.grossSalary)} FCFA`
                      : 'Confidentiel'}
                  </p>
                </div>
              </div>
            ) : (
              <p className="text-sm text-gray-500 italic">Aucun contrat actif enregistré pour ce collaborateur.</p>
            )}
          </div>
        )}

        {/* Onglet 3 : Historique des avancements */}
        {activeTab === 'history' && (
          <div className="space-y-6">
            {/* Historique des postes */}
            <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm space-y-4">
              <h2 className="text-base font-bold text-gray-900 flex items-center gap-2 border-b border-gray-100 pb-3">
                <Award className="w-5 h-5 text-[#1A6B3C]" />
                Historique des Postes & Mobilités
              </h2>

              {positionHistory && positionHistory.length > 0 ? (
                <div className="divide-y divide-gray-100">
                  {positionHistory.map((ph) => (
                    <div key={ph.id} className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <div className="space-y-0.5">
                        <div className="flex items-center gap-2 font-semibold text-sm text-gray-900">
                          <span>{ph.previousPositionTitle || 'Début de carrière'}</span>
                          <span className="text-gray-400">→</span>
                          <span className="text-[#1A6B3C]">{ph.newPositionTitle || 'Nouveau poste'}</span>
                        </div>
                        <p className="text-xs text-gray-500">
                          Motif : {ph.changeReason}
                          {ph.changedByName && ` • Par ${ph.changedByName}`}
                        </p>
                      </div>
                      <span className="text-xs font-mono text-gray-600 bg-gray-50 px-2.5 py-1 rounded-md shrink-0 self-start sm:self-center">
                        {ph.effectiveDate}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-500 italic py-2">Aucun changement de poste enregistré.</p>
              )}
            </div>

            {/* Historique salarial (si disponible) */}
            {salaryHistory && (
              <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm space-y-4">
                <h2 className="text-base font-bold text-gray-900 flex items-center gap-2 border-b border-gray-100 pb-3">
                  <TrendingUp className="w-5 h-5 text-[#1A6B3C]" />
                  Historique des Rémunérations (Confidentialité DRH & Salarié)
                </h2>

                {salaryHistory.length > 0 ? (
                  <div className="divide-y divide-gray-100">
                    {salaryHistory.map((sh) => {
                      const diff = sh.newSalary - sh.previousSalary;
                      return (
                        <div key={sh.id} className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                          <div className="space-y-0.5">
                            <div className="flex items-center gap-2 font-semibold text-sm text-gray-900">
                              <span>{new Intl.NumberFormat('fr-FR').format(sh.previousSalary)} FCFA</span>
                              <span className="text-gray-400">→</span>
                              <span className="text-[#1A6B3C]">
                                {new Intl.NumberFormat('fr-FR').format(sh.newSalary)} FCFA
                              </span>
                              {diff > 0 && (
                                <span className="badge badge-success text-[10px]">
                                  +{new Intl.NumberFormat('fr-FR').format(diff)} FCFA
                                </span>
                              )}
                            </div>
                            <p className="text-xs text-gray-500">
                              Motif : {sh.changeReason}
                              {sh.approvedByName && ` • Validé par ${sh.approvedByName}`}
                            </p>
                          </div>
                          <span className="text-xs font-mono text-gray-600 bg-gray-50 px-2.5 py-1 rounded-md shrink-0 self-start sm:self-center">
                            {sh.effectiveDate}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 italic py-2">Aucune révision salariale enregistrée.</p>
                )}
              </div>
            )}
          </div>
        )}

        {/* Onglet 4 : Documents liés (Lot 1 en lecture seule) */}
        {activeTab === 'documents' && documents && (
          <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm space-y-4">
            <h2 className="text-base font-bold text-gray-900 flex items-center gap-2 border-b border-gray-100 pb-3">
              <FileText className="w-5 h-5 text-[#1A6B3C]" />
              Documents RH du Collaborateur (Lecture seule)
            </h2>

            {documents.length > 0 ? (
              <div className="divide-y divide-gray-100">
                {documents.map((doc) => (
                  <div key={doc.id} className="py-3 flex items-center justify-between gap-4">
                    <div className="space-y-0.5 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="badge badge-outline text-[11px] font-semibold">{doc.documentType}</span>
                        <p className="text-sm font-semibold text-gray-900 truncate">{doc.title}</p>
                      </div>
                      <p className="text-xs text-gray-500 truncate">
                        {doc.fileName} • {(doc.fileSize / 1024).toFixed(1)} Ko • Déposé le {new Date(doc.uploadedAt).toLocaleDateString('fr-FR')}
                      </p>
                    </div>

                    <button
                      onClick={() => downloadFile(`/documents/${doc.id}/download`, doc.fileName)}
                      className="btn btn-secondary text-xs inline-flex items-center gap-1.5 shrink-0 min-h-[38px]"
                    >
                      <Download className="w-3.5 h-3.5" />
                      Télécharger
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-gray-500 italic py-2">Aucun document déposé pour le moment.</p>
            )}
          </div>
        )}
      </div>

      {/* Modal 1 : Mise à jour coordonnées (Employé / Manager / DRH) */}
      {showContactModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl animate-scale-up">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Phone className="w-5 h-5 text-[#1A6B3C]" />
                Modifier mes coordonnées
              </h3>
              <button onClick={() => setShowContactModal(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Téléphone</label>
                <input
                  type="text"
                  value={contactForm.phone || ''}
                  onChange={(e) => setContactForm({ ...contactForm, phone: e.target.value })}
                  placeholder="+221 77 000 00 00"
                  className="input w-full"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Adresse de résidence</label>
                <input
                  type="text"
                  value={contactForm.address || ''}
                  onChange={(e) => setContactForm({ ...contactForm, address: e.target.value })}
                  placeholder="Quartier, Ville"
                  className="input w-full"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowContactModal(false)}
                className="btn btn-secondary text-xs min-h-[44px]"
              >
                Annuler
              </button>
              <button
                type="button"
                disabled={contactMutation.isPending}
                onClick={() => contactMutation.mutate(contactForm)}
                className="btn btn-primary text-xs min-h-[44px]"
              >
                {contactMutation.isPending ? 'Enregistrement...' : 'Enregistrer'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 2 : Désactivation collaborateur (DRH uniquement) */}
      {showDeactivateModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl animate-scale-up">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-lg font-bold text-red-600 flex items-center gap-2">
                <UserX className="w-5 h-5" />
                Désactiver le collaborateur
              </h3>
              <button onClick={() => setShowDeactivateModal(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-gray-600">
              Cette action clôture le contrat actif, révoque l'accès au compte utilisateur lié et passe le statut en <strong className="text-red-700">Inactif (TERMINATED)</strong>.
            </p>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                  Motif de départ <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={deactivateForm.reason}
                  onChange={(e) => setDeactivateForm({ ...deactivateForm, reason: e.target.value })}
                  placeholder="Ex : Fin de contrat, Démission, Retraite..."
                  className="input w-full"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Date de sortie</label>
                <input
                  type="date"
                  value={deactivateForm.departureDate || ''}
                  onChange={(e) => setDeactivateForm({ ...deactivateForm, departureDate: e.target.value })}
                  className="input w-full"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowDeactivateModal(false)}
                className="btn btn-secondary text-xs min-h-[44px]"
              >
                Annuler
              </button>
              <button
                type="button"
                disabled={!deactivateForm.reason || deactivateMutation.isPending}
                onClick={() => deactivateMutation.mutate(deactivateForm)}
                className="btn btn-danger text-xs min-h-[44px]"
              >
                {deactivateMutation.isPending ? 'Désactivation...' : 'Confirmer la désactivation'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 3 : Modification administrative DRH */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 space-y-5 shadow-2xl my-8">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Edit className="w-5 h-5 text-[#1A6B3C]" />
                Mise à jour administrative du dossier
              </h3>
              <button onClick={() => setShowEditModal(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-h-[65vh] overflow-y-auto p-1">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Prénom</label>
                <input
                  type="text"
                  value={editForm.firstName || ''}
                  onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })}
                  className="input w-full"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Nom</label>
                <input
                  type="text"
                  value={editForm.lastName || ''}
                  onChange={(e) => setEditForm({ ...editForm, lastName: e.target.value })}
                  className="input w-full"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">CNI / Identifiant</label>
                <input
                  type="text"
                  value={editForm.nationalId || ''}
                  onChange={(e) => setEditForm({ ...editForm, nationalId: e.target.value })}
                  className="input w-full"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Email</label>
                <input
                  type="email"
                  value={editForm.email || ''}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                  className="input w-full"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Département</label>
                <select
                  value={editForm.departmentId || ''}
                  onChange={(e) => setEditForm({ ...editForm, departmentId: e.target.value || undefined })}
                  className="input w-full"
                >
                  <option value="">-- Aucun département --</option>
                  {departments?.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Poste</label>
                <select
                  value={editForm.positionId || ''}
                  onChange={(e) => setEditForm({ ...editForm, positionId: e.target.value || undefined })}
                  className="input w-full"
                >
                  <option value="">-- Aucun poste --</option>
                  {positions?.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Manager hiérarchique (N+1)</label>
                <select
                  value={editForm.managerId || ''}
                  onChange={(e) => setEditForm({ ...editForm, managerId: e.target.value || undefined })}
                  className="input w-full"
                >
                  <option value="">-- Aucun (Direction Générale) --</option>
                  {potentialManagers
                    ?.filter((m) => m.id !== id)
                    ?.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.fullName}
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Salaire brut mensuel (FCFA)</label>
                <input
                  type="number"
                  value={editForm.grossSalary ?? ''}
                  onChange={(e) => setEditForm({ ...editForm, grossSalary: e.target.value ? Number(e.target.value) : undefined })}
                  className="input w-full"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Situation familiale</label>
                <select
                  value={editForm.maritalStatus || 'CELIBATAIRE'}
                  onChange={(e) => setEditForm({ ...editForm, maritalStatus: e.target.value })}
                  className="input w-full"
                >
                  <option value="CELIBATAIRE">Célibataire</option>
                  <option value="MARIE_1">Marié(e) 1 conjoint</option>
                  <option value="MARIE_2">Marié(e) 2 conjoints</option>
                  <option value="DIVORCE">Divorcé(e)</option>
                  <option value="VEUF">Veuf / Veuve</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Nombre d'enfants à charge</label>
                <input
                  type="number"
                  min="0"
                  value={editForm.dependentsCount ?? 0}
                  onChange={(e) => setEditForm({ ...editForm, dependentsCount: Number(e.target.value) })}
                  className="input w-full"
                />
              </div>

              <div className="flex items-center gap-2 pt-4">
                <input
                  type="checkbox"
                  id="isCadreCheck"
                  checked={editForm.isCadre ?? false}
                  onChange={(e) => setEditForm({ ...editForm, isCadre: e.target.checked })}
                  className="w-4 h-4 rounded text-[#1A6B3C] focus:ring-[#1A6B3C]"
                />
                <label htmlFor="isCadreCheck" className="text-xs font-semibold text-gray-800">
                  Statut Cadre (IPRES régime complémentaire)
                </label>
              </div>

              <div className="sm:col-span-2">
                <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Motif de la modification</label>
                <input
                  type="text"
                  placeholder="Ex : Promotion interne, Changement d'équipe, Ajustement contractuel..."
                  value={editForm.changeReason || ''}
                  onChange={(e) => setEditForm({ ...editForm, changeReason: e.target.value })}
                  className="input w-full"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowEditModal(false)}
                className="btn btn-secondary text-xs min-h-[44px]"
              >
                Annuler
              </button>
              <button
                type="button"
                disabled={updateMutation.isPending}
                onClick={() => updateMutation.mutate(editForm as EmployeeUpdateDto)}
                className="btn btn-primary text-xs min-h-[44px]"
              >
                {updateMutation.isPending ? 'Enregistrement...' : 'Enregistrer les modifications'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EmployeeDetailPage;
