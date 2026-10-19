import React, { useEffect, useState } from 'react';
import { downloadFile } from '@/lib/downloadFile';
import {
  Search,
  UserPlus,
  FileDown,
  Building,
  Loader2,
  AlertTriangle,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { useEmployees } from '@/hooks/useEmployees';
import type { EmployeeStatus } from '@/types';

const PAGE_SIZE = 20;

const STATUS_BADGES: Record<EmployeeStatus, { label: string; className: string }> = {
  ACTIVE: { label: 'Actif', className: 'badge-success' },
  ON_LEAVE: { label: 'En congé', className: 'badge-warning' },
  PROBATION: { label: 'Essai', className: 'badge-info' },
  SUSPENDED: { label: 'Suspendu', className: 'badge-danger' },
  TERMINATED: { label: 'Sorti', className: 'badge-default' },
};

function initialsOf(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-');
  return year && month && day ? `${day}/${month}/${year}` : iso;
}

export const EmployeeListPage: React.FC = () => {
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [departmentId, setDepartmentId] = useState('ALL');
  const [page, setPage] = useState(0);
  const [knownDepartments, setKnownDepartments] = useState<Record<string, string>>({});

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const { data, isLoading, isError, isFetching, refetch } = useEmployees({
    page,
    size: PAGE_SIZE,
    search: search || undefined,
    departmentId: departmentId === 'ALL' ? undefined : departmentId,
  });

  useEffect(() => {
    if (!data) return;
    setKnownDepartments((previous) => {
      const next = { ...previous };
      let changed = false;
      data.content.forEach((emp) => {
        if (emp.departmentId && emp.departmentName && next[emp.departmentId] !== emp.departmentName) {
          next[emp.departmentId] = emp.departmentName;
          changed = true;
        }
      });
      return changed ? next : previous;
    });
  }, [data]);

  const employees = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;
  const departments = Object.entries(knownDepartments)
    .map(([id, name]) => ({ id, name }))
    .sort((a, b) => a.name.localeCompare(b.name));

  const attestation = (id: string, number: string) =>
    downloadFile(`/documents/attestation/${id}`, `attestation-${number || id}.pdf`);

  if (isError) {
    return (
      <div className="card p-12 text-center flex flex-col items-center justify-center gap-4 my-8">
        <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div className="space-y-1">
          <h2 className="text-lg font-bold text-gray-900">Échec de synchronisation API</h2>
          <p className="text-xs text-gray-500 max-w-md">
            Impossible d'accéder au registre des collaborateurs sur le serveur. Veuillez vérifier votre
            connexion ou relancer la requête.
          </p>
        </div>
        <button type="button" onClick={() => refetch()} className="btn btn-primary text-xs flex items-center gap-2">
          <RefreshCw className="w-4 h-4" />
          Réessayer
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>Gestion des Collaborateurs</h1>
          <p className="flex items-center gap-2">
            <span>Répertoire officiel et dossiers individuels</span>
            {!isLoading && <span className="badge badge-success text-[10px]">Connecté API Backend</span>}
          </p>
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <button type="button" className="btn btn-secondary text-xs flex-1 sm:flex-none">
            <FileDown className="w-4 h-4" />
            Exporter Registre
          </button>
          <button type="button" className="btn btn-primary text-xs flex-1 sm:flex-none">
            <UserPlus className="w-4 h-4" />
            Nouveau Salarié
          </button>
        </div>
      </div>

      <div className="card p-4">
        <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none z-10" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Rechercher un nom ou un matricule..."
              className="form-control has-icon-left text-xs"
              style={{ paddingLeft: '2.5rem' }}
              maxLength={100}
            />
          </div>

          <div className="flex gap-2 w-full sm:w-auto items-center">
            {isFetching && !isLoading && <Loader2 className="w-4 h-4 animate-spin text-[#1A6B3C]" />}
            <select
              value={departmentId}
              onChange={(e) => {
                setDepartmentId(e.target.value);
                setPage(0);
              }}
              className="form-control text-xs w-full sm:w-56"
            >
              <option value="ALL">Tous les départements</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {isLoading && (
        <div className="p-8 text-center text-xs text-gray-500 flex items-center justify-center gap-2">
          <Loader2 className="w-4 h-4 animate-spin text-[#1A6B3C]" />
          Chargement des collaborateurs depuis le serveur...
        </div>
      )}

      {!isLoading && employees.length === 0 && (
        <div className="card p-8 text-center text-sm text-gray-500">Aucun collaborateur trouvé.</div>
      )}

      {!isLoading && employees.length > 0 && (
        <>
          <div className="hidden md:block table-container bg-white">
            <table className="table">
              <thead>
                <tr>
                  <th>Salarié</th>
                  <th>Matricule</th>
                  <th>Département</th>
                  <th>Poste</th>
                  <th>Statut</th>
                  <th>Date d'embauche</th>
                  <th className="text-right">Attestation</th>
                </tr>
              </thead>
              <tbody>
                {employees.map((emp) => (
                  <tr key={emp.id}>
                    <td>
                      <div className="flex items-center gap-3">
                        <div className="avatar avatar-md avatar-primary font-bold">{initialsOf(emp.fullName)}</div>
                        <div className="font-semibold text-gray-900">{emp.fullName}</div>
                      </div>
                    </td>
                    <td>
                      <span className="font-mono text-xs font-semibold text-gray-700 bg-gray-100 px-2 py-0.5 rounded">
                        {emp.employeeNumber}
                      </span>
                    </td>
                    <td>
                      <div className="flex items-center gap-1.5 text-xs text-gray-700">
                        <Building className="w-3.5 h-3.5 text-gray-400" />
                        <span>{emp.departmentName || 'Général'}</span>
                      </div>
                    </td>
                    <td>
                      <div className="text-xs font-medium text-gray-900">{emp.positionTitle || 'Employé'}</div>
                    </td>
                    <td>
                      <span className={`badge ${STATUS_BADGES[emp.status].className}`}>
                        {STATUS_BADGES[emp.status].label}
                      </span>
                    </td>
                    <td className="text-xs text-gray-600">{formatDate(emp.hireDate)}</td>
                    <td className="text-right">
                      <button
                        type="button"
                        onClick={() => attestation(emp.id, emp.employeeNumber)}
                        className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5"
                      >
                        Attestation PDF
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="md:hidden space-y-3">
            {employees.map((emp) => (
              <div key={emp.id} className="card p-4 space-y-3 bg-white">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="avatar avatar-md avatar-primary font-bold">{initialsOf(emp.fullName)}</div>
                    <div>
                      <div className="font-bold text-sm text-gray-900">{emp.fullName}</div>
                      <div className="text-xs text-gray-400 font-mono">{emp.employeeNumber}</div>
                    </div>
                  </div>
                  <span className={`badge ${STATUS_BADGES[emp.status].className} text-[10px]`}>
                    {STATUS_BADGES[emp.status].label}
                  </span>
                </div>

                <div className="text-xs text-gray-600 bg-gray-50 p-2 rounded">
                  <div className="font-semibold text-gray-800">{emp.positionTitle || 'Employé'}</div>
                  <div className="text-[11px] text-gray-500">{emp.departmentName || 'Général'}</div>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-gray-100 text-xs">
                  <span className="text-gray-400 text-[11px]">Embauche : {formatDate(emp.hireDate)}</span>
                  <button
                    type="button"
                    onClick={() => attestation(emp.id, emp.employeeNumber)}
                    className="btn btn-secondary btn-sm text-[11px] py-1 px-2"
                  >
                    Attestation PDF
                  </button>
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-between text-xs text-gray-600">
              <span>
                {totalElements} collaborateurs · page {page + 1} sur {totalPages}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(p - 1, 0))}
                  disabled={page === 0}
                  className="btn btn-secondary btn-sm"
                  aria-label="Page précédente"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                  disabled={page >= totalPages - 1}
                  className="btn btn-secondary btn-sm"
                  aria-label="Page suivante"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default EmployeeListPage;