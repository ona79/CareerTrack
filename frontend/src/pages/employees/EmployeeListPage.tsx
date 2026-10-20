import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import {
  Users,
  Search,
  UserPlus,
  Building,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Briefcase,
  X,
  AlertCircle,
  CheckCircle,
  ArrowRight,
  Download
} from 'lucide-react';
import type {
  EmployeeSummaryDto,
  EmployeeCreateDto,
  Department,
  Position,
  Page,
  ApiResponse
} from '@/types';

export const EmployeeListPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const isDrh = hasRole('ROLE_DRH');

  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sortBy, setSortBy] = useState('lastName');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // 1. Récupération des départements pour le filtre
  const { data: departments } = useQuery<Department[]>({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Department[]>>('/departments');
      return res.data.data;
    }
  });

  const { data: positions } = useQuery<Position[]>({
    queryKey: ['positions'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Position[]>>('/positions');
      return res.data.data;
    },
    enabled: showCreateModal
  });

  const { data: potentialManagers } = useQuery<{ id: string; fullName: string }[]>({
    queryKey: ['potentialManagers'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<{ content: { id: string; fullName: string }[] }>>('/employees?size=100');
      return res.data.data.content;
    },
    enabled: showCreateModal
  });

  // 2. Récupération de la liste paginée côté serveur (DTO réduit)
  const {
    data: employeesPage,
    isLoading,
    isError,
    refetch
  } = useQuery<Page<EmployeeSummaryDto>>({
    queryKey: ['employees', page, size, debouncedSearch, departmentFilter, statusFilter, sortBy, sortDir],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.append('page', String(page));
      params.append('size', String(size));
      params.append('sortBy', sortBy);
      params.append('sortDir', sortDir);
      if (debouncedSearch.trim()) params.append('search', debouncedSearch.trim());
      if (departmentFilter) params.append('departmentId', departmentFilter);
      if (statusFilter) params.append('status', statusFilter);

      const res = await api.get<ApiResponse<Page<EmployeeSummaryDto>>>(`/employees?${params.toString()}`);
      return res.data.data;
    }
  });

  // 3. Formulaire de création de collaborateur
  const [createForm, setCreateForm] = useState<EmployeeCreateDto>({
    employeeNumber: '',
    firstName: '',
    lastName: '',
    nationalId: '',
    email: '',
    phone: '',
    address: '',
    birthDate: '',
    hireDate: new Date().toISOString().split('T')[0],
    departmentId: undefined,
    positionId: undefined,
    managerId: undefined
  });

  const createMutation = useMutation({
    mutationFn: async (payload: EmployeeCreateDto) => {
      const res = await api.post<ApiResponse<any>>('/employees', payload);
      return res.data.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      setShowCreateModal(false);
      setSuccessMessage('Collaborateur créé avec succès');
      setTimeout(() => setSuccessMessage(null), 4000);
      navigate(`/employees/${data.id}`);
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.message || 'Erreur lors de la création du collaborateur');
    }
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setDebouncedSearch(searchTerm);
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setDebouncedSearch('');
    setDepartmentFilter('');
    setStatusFilter('');
    setPage(0);
  };

  const totalPages = employeesPage?.totalPages || 0;
  const employees = employeesPage?.content || [];

  const handleExportRegister = async () => {
    try {
      const res = await api.get<ApiResponse<Page<EmployeeSummaryDto>>>('/employees?size=500');
      const emps = res.data?.data?.content || employees;
      const headers = ['Matricule', 'Nom Complet', 'Departement', 'Poste', 'Statut'];
      const rows = emps.map(e => [
        `"${e.employeeNumber || ''}"`,
        `"${e.fullName || ''}"`,
        `"${e.departmentName || ''}"`,
        `"${e.positionTitle || ''}"`,
        `"${e.status || ''}"`
      ]);
      const csvContent = '\uFEFF' + [headers.join(';'), ...rows.map(r => r.join(';'))].join('\n');
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `registre_unique_personnel_${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      setSuccessMessage('Registre unique du personnel exporté avec succès');
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch {
      setErrorMessage('Erreur lors de l’export du registre du personnel');
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 py-4">
      {/* Alertes */}
      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex items-center justify-between text-sm shadow-sm">
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
        <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-xl flex items-center justify-between text-sm shadow-sm">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-red-700 hover:text-red-900">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* En-tête de la page */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2.5">
            <Users className="w-7 h-7 text-[#1A6B3C]" />
            Dossiers Collaborateurs
          </h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Annuaire d'entreprise et gestion des dossiers professionnels
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Bouton Exporter Registre */}
          <button
            type="button"
            onClick={handleExportRegister}
            className="btn btn-secondary inline-flex items-center gap-2 text-xs min-h-[44px]"
            title="Exporter le Registre Unique du Personnel officiel (CSV)"
          >
            <Download className="w-4 h-4 text-emerald-700" />
            Exporter Registre
          </button>

          <button
            onClick={() => refetch()}
            className="btn btn-secondary inline-flex items-center gap-2 text-xs min-h-[44px]"
            title="Actualiser la liste"
          >
            <RefreshCw className="w-4 h-4" />
            Actualiser
          </button>

          {isDrh && (
            <button
              onClick={() => {
                setErrorMessage(null);
                setCreateForm({
                  employeeNumber: `EMP-${Math.floor(100 + Math.random() * 900)}`,
                  firstName: '',
                  lastName: '',
                  nationalId: '',
                  email: '',
                  phone: '',
                  address: '',
                  birthDate: '',
                  hireDate: new Date().toISOString().split('T')[0]
                });
                setShowCreateModal(true);
              }}
              className="btn btn-primary inline-flex items-center gap-2 text-xs font-semibold min-h-[44px]"
            >
              <UserPlus className="w-4 h-4" />
              Nouveau salarié
            </button>
          )}
        </div>
      </div>

      {/* Barre de recherche et filtres côté serveur */}
      <div className="bg-white p-4 rounded-2xl border border-gray-100 shadow-sm space-y-3">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Nom, prénom ou matricule..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input pl-9 w-full text-xs"
            />
          </div>

          <div>
            <select
              value={departmentFilter}
              onChange={(e) => {
                setDepartmentFilter(e.target.value);
                setPage(0);
              }}
              className="input w-full text-xs"
            >
              <option value="">Tous les départements</option>
              {departments?.map((d: any) => (
                <option key={d.id} value={d.id}>
                  {d.label || d.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="input w-full text-xs"
            >
              <option value="">Actifs & En congé (par défaut)</option>
              <option value="ACTIVE">Actif uniquement</option>
              <option value="ON_LEAVE">En congé</option>
              {isDrh && <option value="TERMINATED">Inactifs / Sortis (DRH)</option>}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="submit"
              className="btn btn-primary text-xs flex-1 min-h-[40px] inline-flex items-center justify-center gap-1.5"
            >
              <Search className="w-3.5 h-3.5" />
              Rechercher
            </button>
            {(debouncedSearch || departmentFilter || statusFilter) && (
              <button
                type="button"
                onClick={handleClearFilters}
                className="btn btn-secondary text-xs min-h-[40px]"
                title="Effacer les filtres"
              >
                Effacer
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Contenu : Tableau Desktop (>= 640px) ou Cartes Mobile (< 640px) */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3">
          <div className="spinner spinner-lg text-[#1A6B3C]" />
          <span className="text-sm text-gray-500">Chargement des collaborateurs...</span>
        </div>
      ) : isError ? (
        <div className="p-8 bg-red-50 rounded-2xl border border-red-100 text-center space-y-3">
          <AlertCircle className="w-10 h-10 text-red-500 mx-auto" />
          <h3 className="text-base font-bold text-red-900">Erreur de chargement</h3>
          <p className="text-xs text-red-600">Impossible de charger la liste des employés.</p>
          <button onClick={() => refetch()} className="btn btn-secondary text-xs">
            Réessayer
          </button>
        </div>
      ) : employees.length === 0 ? (
        <div className="p-12 bg-white rounded-2xl border border-gray-100 text-center space-y-3 shadow-sm">
          <Users className="w-12 h-12 text-gray-300 mx-auto" />
          <h3 className="text-base font-bold text-gray-800">Aucun collaborateur trouvé</h3>
          <p className="text-xs text-gray-500">
            Aucun dossier ne correspond à vos critères de recherche ou à vos autorisations.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Vue 1 : Tableau Desktop (écrans >= 640px) */}
          <div className="hidden sm:block bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="table">
                <thead>
                  <tr>
                    <th>Matricule</th>
                    <th>Collaborateur</th>
                    <th>Poste</th>
                    <th>Département</th>
                    <th>Statut</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {employees.map((emp) => (
                    <tr
                      key={emp.id}
                      onClick={() => navigate(`/employees/${emp.id}`)}
                      className="cursor-pointer hover:bg-gray-50/80 transition-colors"
                    >
                      <td className="font-mono text-xs font-semibold text-gray-600">{emp.employeeNumber}</td>
                      <td>
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-xl bg-[#1A6B3C]/10 text-[#1A6B3C] font-bold text-xs flex items-center justify-center shrink-0">
                            {emp.fullName
                              .split(' ')
                              .map((n) => n[0])
                              .join('')
                              .slice(0, 2)}
                          </div>
                          <span className="font-semibold text-gray-900 text-sm">{emp.fullName}</span>
                        </div>
                      </td>
                      <td className="text-xs text-gray-700 font-medium">{emp.positionTitle || '—'}</td>
                      <td className="text-xs text-gray-600">{emp.departmentName || '—'}</td>
                      <td>
                        <span
                          className={`badge text-[11px] ${
                            emp.status === 'ACTIVE'
                              ? 'badge-success'
                              : emp.status === 'ON_LEAVE'
                              ? 'badge-warning'
                              : 'badge-danger'
                          }`}
                        >
                          {emp.status === 'ACTIVE'
                            ? 'Actif'
                            : emp.status === 'ON_LEAVE'
                            ? 'En congé'
                            : 'Inactif'}
                        </span>
                      </td>
                      <td className="text-right">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/employees/${emp.id}`);
                          }}
                          className="btn btn-secondary text-xs inline-flex items-center gap-1 py-1.5 px-3 min-h-[36px]"
                        >
                          Consulter
                          <ArrowRight className="w-3.5 h-3.5" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Vue 2 : Cartes Tactiles Mobiles (< 640px, spécialement 360px) */}
          <div className="sm:hidden space-y-3">
            {employees.map((emp) => (
              <div
                key={emp.id}
                onClick={() => navigate(`/employees/${emp.id}`)}
                className="bg-white p-4 rounded-2xl border border-gray-100 shadow-sm active:scale-[0.99] transition-all cursor-pointer flex flex-col gap-3 min-h-[88px]"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-[#1A6B3C]/10 text-[#1A6B3C] font-bold text-xs flex items-center justify-center shrink-0">
                      {emp.fullName
                        .split(' ')
                        .map((n) => n[0])
                        .join('')
                        .slice(0, 2)}
                    </div>
                    <div>
                      <h4 className="font-bold text-sm text-gray-900 leading-snug">{emp.fullName}</h4>
                      <span className="font-mono text-[11px] text-gray-500">{emp.employeeNumber}</span>
                    </div>
                  </div>
                  <span
                    className={`badge text-[10px] shrink-0 ${
                      emp.status === 'ACTIVE'
                        ? 'badge-success'
                        : emp.status === 'ON_LEAVE'
                        ? 'badge-warning'
                        : 'badge-danger'
                    }`}
                  >
                    {emp.status === 'ACTIVE' ? 'Actif' : emp.status === 'ON_LEAVE' ? 'En congé' : 'Inactif'}
                  </span>
                </div>

                <div className="text-xs text-gray-600 space-y-1 border-t border-gray-50 pt-2 flex items-center justify-between">
                  <div className="truncate">
                    <p className="font-medium text-gray-800 truncate">{emp.positionTitle || 'Poste non défini'}</p>
                    <p className="text-gray-400 text-[11px] truncate">{emp.departmentName || 'Sans département'}</p>
                  </div>
                  <button className="text-[#1A6B3C] text-xs font-semibold shrink-0 flex items-center gap-1 min-h-[44px] min-w-[44px] justify-end">
                    Voir
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination serveur */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between bg-white px-4 py-3 rounded-2xl border border-gray-100 shadow-sm">
              <span className="text-xs text-gray-500">
                Page <strong className="text-gray-800">{page + 1}</strong> sur{' '}
                <strong className="text-gray-800">{totalPages}</strong> ({employeesPage?.totalElements} collaborateurs)
              </span>

              <div className="flex items-center gap-2">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="btn btn-secondary text-xs p-2 min-h-[40px] min-w-[40px] inline-flex items-center justify-center disabled:opacity-40"
                  title="Page précédente"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="btn btn-secondary text-xs p-2 min-h-[40px] min-w-[40px] inline-flex items-center justify-center disabled:opacity-40"
                  title="Page suivante"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal Création Collaborateur (DRH uniquement) */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 overflow-hidden">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-2xl w-full shadow-2xl flex flex-col max-h-[92vh] sm:max-h-[90vh]">
            <div className="flex items-center justify-between border-b border-gray-100 p-4 sm:p-5 shrink-0">
              <h3 className="text-base font-bold text-gray-900 flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-emerald-100 text-[#1A6B3C] flex items-center justify-center font-bold">
                  <UserPlus className="w-5 h-5 text-[#1A6B3C]" />
                </div>
                Enregistrer un nouveau collaborateur
              </h3>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                createMutation.mutate(createForm);
              }}
              className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-4"
            >
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Matricule <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={createForm.employeeNumber}
                    onChange={(e) => setCreateForm({ ...createForm, employeeNumber: e.target.value })}
                    className="input w-full text-sm font-medium"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Numéro CNI <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="1 990 1990 00123"
                    value={createForm.nationalId}
                    onChange={(e) => setCreateForm({ ...createForm, nationalId: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Prénom <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={createForm.firstName}
                    onChange={(e) => setCreateForm({ ...createForm, firstName: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Nom de famille <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={createForm.lastName}
                    onChange={(e) => setCreateForm({ ...createForm, lastName: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Email professionnel <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="email"
                    required
                    placeholder="prenom.nom@careertrack.sn"
                    value={createForm.email}
                    onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Téléphone</label>
                  <input
                    type="text"
                    placeholder="+221 77 000 00 00"
                    value={createForm.phone || ''}
                    onChange={(e) => setCreateForm({ ...createForm, phone: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Date de naissance <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    required
                    value={createForm.birthDate}
                    onChange={(e) => setCreateForm({ ...createForm, birthDate: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Date d'embauche <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    required
                    value={createForm.hireDate}
                    onChange={(e) => setCreateForm({ ...createForm, hireDate: e.target.value })}
                    className="input w-full text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Département</label>
                  <select
                    value={createForm.departmentId || ''}
                    onChange={(e) => setCreateForm({ ...createForm, departmentId: e.target.value || undefined })}
                    className="input w-full text-sm"
                  >
                    <option value="">-- Choisir un département --</option>
                    {departments?.map((d: any) => (
                      <option key={d.id} value={d.id}>
                        {d.label || d.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Poste</label>
                  <select
                    value={createForm.positionId || ''}
                    onChange={(e) => setCreateForm({ ...createForm, positionId: e.target.value || undefined })}
                    className="input w-full text-sm"
                  >
                    <option value="">-- Choisir un poste --</option>
                    {positions?.map((p: any) => (
                      <option key={p.id} value={p.id}>
                        {p.label || p.title || p.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="sm:col-span-2">
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Responsable hiérarchique (N+1)
                  </label>
                  <select
                    value={createForm.managerId || ''}
                    onChange={(e) => setCreateForm({ ...createForm, managerId: e.target.value || undefined })}
                    className="input w-full text-sm"
                  >
                    <option value="">-- Aucun (Direction Générale) --</option>
                    {potentialManagers?.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.fullName}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-2.5 pt-4 border-t border-gray-100 shrink-0">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn btn-secondary px-5 text-sm min-h-[44px]"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="btn btn-primary px-5 text-sm min-h-[44px]"
                >
                  {createMutation.isPending ? 'Création en cours...' : 'Créer le collaborateur'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default EmployeeListPage;
