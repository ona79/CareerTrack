import React, { useState } from 'react';
import { downloadFile } from '@/lib/downloadFile';
import {
  Users,
  Search,
  UserPlus,
  FileDown,
  Mail,
  Phone,
  Building,
  Loader2,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import { useEmployees } from '@/hooks/useEmployees';

export const EmployeeListPage: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('ALL');

  const { data: employeesPage, isLoading, isError, refetch } = useEmployees({ page: 0, size: 50 });

  const employees = employeesPage?.content ?? [];

  // Construire la liste des départements dynamiquement depuis les données
  const departmentsMap = new Map<string, string>();
  employees.forEach((emp) => {
    if (emp.departmentId && emp.departmentName) {
      departmentsMap.set(emp.departmentId, emp.departmentName);
    }
  });
  const departments = Array.from(departmentsMap.entries()).map(([id, name]) => ({ id, name }));

  const filteredEmployees = employees.filter((emp) => {
    const matchesSearch =
      emp.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      emp.employeeNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (emp.positionTitle && emp.positionTitle.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesDept =
      departmentFilter === 'ALL' || emp.departmentId === departmentFilter;

    return matchesSearch && matchesDept;
  });

  if (isError) {
    return (
      <div className="card p-12 text-center flex flex-col items-center justify-center gap-4 my-8">
        <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div className="space-y-1">
          <h2 className="text-lg font-bold text-gray-900">Échec de synchronisation API</h2>
          <p className="text-xs text-gray-500 max-w-md">
            Impossible d'accéder au registre des collaborateurs sur le serveur. Veuillez vérifier votre connexion ou relancer la requête.
          </p>
        </div>
        <button
          type="button"
          onClick={() => refetch()}
          className="btn btn-primary text-xs flex items-center gap-2"
        >
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
            {!isLoading && !isError && (
              <span className="badge badge-success text-[10px]">Connecté API Backend</span>
            )}
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
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Rechercher collaborateur..."
              className="form-control has-icon-left text-xs"
              style={{ paddingLeft: '2.5rem' }}
            />
          </div>

          <div className="flex gap-2 w-full sm:w-auto">
            <select
              value={departmentFilter}
              onChange={(e) => setDepartmentFilter(e.target.value)}
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

      {!isLoading && !isError && filteredEmployees.length === 0 && (
        <div className="card p-8 text-center text-sm text-gray-500">
          Aucun collaborateur trouvé.
        </div>
      )}

      {!isLoading && !isError && filteredEmployees.length > 0 && (
        <>
          <div className="hidden md:block table-container bg-white">
            <table className="table">
              <thead>
                <tr>
                  <th>Salarié</th>
                  <th>Matricule</th>
                  <th>Département</th>
                  <th>Poste & Rôle</th>
                  <th>Statut</th>
                  <th>Date d'embauche</th>
                  <th>Contact</th>
                  <th className="text-right">Attestation</th>
                </tr>
              </thead>
              <tbody>
                {filteredEmployees.map((emp) => (
                  <tr key={emp.id}>
                    <td>
                      <div className="flex items-center gap-3">
                        <div className="avatar avatar-md avatar-primary font-bold">
                          {emp.firstName?.[0] || 'E'}
                          {emp.lastName?.[0] || 'M'}
                        </div>
                        <div>
                          <div className="font-semibold text-gray-900">{emp.fullName}</div>
                          <div className="text-xs text-gray-400">{emp.email}</div>
                        </div>
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
                      {emp.status === 'ACTIVE' && <span className="badge badge-success">Actif</span>}
                      {emp.status === 'ON_LEAVE' && <span className="badge badge-warning">En Congé</span>}
                      {emp.status === 'PROBATION' && <span className="badge badge-info">Essai</span>}
                      {emp.status === 'SUSPENDED' && <span className="badge badge-danger">Suspendu</span>}
                      {emp.status === 'TERMINATED' && <span className="badge badge-default">Sorti</span>}
                    </td>
                    <td className="text-xs text-gray-600">{emp.hireDate}</td>
                    <td>
                      <div className="flex items-center gap-2 text-gray-400">
                        {emp.phone && (
                          <span title={emp.phone}>
                            <Phone className="w-3.5 h-3.5" />
                          </span>
                        )}
                        {emp.email && (
                          <a href={`mailto:${emp.email}`} title={emp.email}>
                            <Mail className="w-3.5 h-3.5 text-[#1A6B3C]" />
                          </a>
                        )}
                      </div>
                    </td>
                    <td className="text-right">
                      <button
                        type="button"
                        onClick={() =>
                          downloadFile(
                            `/documents/attestation/${emp.id}`,
                            `attestation-${emp.employeeNumber || emp.id}.pdf`,
                          )
                        }
                        className="btn btn-secondary btn-sm text-[11px] py-1 px-2.5"
                      >
                        PDF iText
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="md:hidden space-y-3">
            {filteredEmployees.map((emp) => (
              <div key={emp.id} className="card p-4 space-y-3 bg-white">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="avatar avatar-md avatar-primary font-bold">
                      {emp.firstName?.[0]}
                      {emp.lastName?.[0]}
                    </div>
                    <div>
                      <div className="font-bold text-sm text-gray-900">{emp.fullName}</div>
                      <div className="text-xs text-gray-400 font-mono">{emp.employeeNumber}</div>
                    </div>
                  </div>
                  <span className="badge badge-success text-[10px]">{emp.status}</span>
                </div>

                <div className="text-xs text-gray-600 bg-gray-50 p-2 rounded">
                  <div className="font-semibold text-gray-800">{emp.positionTitle}</div>
                  <div className="text-[11px] text-gray-500">{emp.departmentName}</div>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-gray-100 text-xs">
                  <span className="text-gray-400 text-[11px]">Embauche : {emp.hireDate}</span>
                  <button
                    type="button"
                    onClick={() =>
                      downloadFile(
                        `/documents/attestation/${emp.id}`,
                        `attestation-${emp.employeeNumber || emp.id}.pdf`,
                      )
                    }
                    className="btn btn-secondary btn-sm text-[11px] py-1 px-2"
                  >
                    Attestation PDF
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default EmployeeListPage;