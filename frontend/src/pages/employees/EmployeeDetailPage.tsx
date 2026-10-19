import React from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { AlertTriangle, ArrowLeft, Loader2, RefreshCw, Pencil } from 'lucide-react'; // <-- Pencil ajouté
import { useEmployee, useSalaryHistory } from '@/hooks/useEmployeeDetail';
import { useAuth } from '@/context/AuthContext'; // <-- NOUVEL IMPORT
import type { EmployeeStatus } from '@/types';

const STATUS_LABELS: Record<EmployeeStatus, string> = {
  ACTIVE: 'Actif',
  ON_LEAVE: 'En congé',
  PROBATION: 'Période d\'essai',
  SUSPENDED: 'Suspendu',
  TERMINATED: 'Sorti',
};

function formatDate(iso?: string | null): string {
  if (!iso) return '—';
  const [year, month, day] = iso.split('-');
  return year && month && day ? `${day}/${month}/${year}` : iso;
}

function formatFcfa(amount: number): string {
  return `${new Intl.NumberFormat('fr-FR').format(amount)} FCFA`;
}

const Field: React.FC<{ label: string; value?: string | null }> = ({ label, value }) => (
  <div>
    <div className="text-[11px] uppercase tracking-wide text-gray-400">{label}</div>
    <div className="text-sm text-gray-900 break-words">{value || '—'}</div>
  </div>
);

export const EmployeeDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate(); // <-- NOUVEAU
  const { hasRole } = useAuth(); // <-- NOUVEAU
  const isDrh = hasRole('ROLE_DRH') || hasRole('DRH'); // <-- NOUVEAU
  
  const { data: employee, isLoading, isError, error, refetch } = useEmployee(id);
  const { data: salaryHistory } = useSalaryHistory(id);

  if (isLoading) {
    return (
      <div className="p-8 text-center text-xs text-gray-500 flex items-center justify-center gap-2">
        <Loader2 className="w-4 h-4 animate-spin text-[#1A6B3C]" />
        Chargement du dossier...
      </div>
    );
  }

  if (isError || !employee) {
    const forbidden = isAxiosError(error) && error.response?.status === 403;
    return (
      <div className="card p-10 text-center flex flex-col items-center gap-4 my-8">
        <AlertTriangle className="w-8 h-8 text-red-500" />
        <p className="text-sm text-gray-700">
          {forbidden ? 'Vous n\'avez pas accès à ce dossier.' : 'Impossible de charger le dossier.'}
        </p>
        {!forbidden && (
          <button type="button" onClick={() => refetch()} className="btn btn-primary text-xs flex items-center gap-2">
            <RefreshCw className="w-4 h-4" />
            Réessayer
          </button>
        )}
        <Link to="/employees" className="text-xs text-[#1A6B3C] underline">
          Retour à la liste
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Link to="/employees" className="inline-flex items-center gap-1 text-xs text-[#1A6B3C]">
        <ArrowLeft className="w-4 h-4" />
        Retour à la liste
      </Link>

      <div className="page-header flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="page-header-info">
          <h1>{employee.fullName}</h1>
          <p>
            {employee.positionTitle || 'Poste non défini'} · {employee.departmentName || 'Département non défini'}
          </p>
        </div>
        {/* NOUVEAU : Bouton Modifier visible par le DRH */}
        {isDrh && (
          <button
            type="button"
            onClick={() => navigate(`/employees/${employee.id}/edit`)}
            className="btn btn-primary text-xs flex items-center gap-2"
          >
            <Pencil className="w-4 h-4" />
            Modifier
          </button>
        )}
      </div>

      <div className="card p-4 space-y-4">
        <h2 className="font-bold text-sm text-gray-900">Informations professionnelles</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Field label="Matricule" value={employee.employeeNumber} />
          <Field label="Statut" value={STATUS_LABELS[employee.status]} />
          <Field label="Date d'embauche" value={formatDate(employee.hireDate)} />
          <Field label="Manager" value={employee.managerName} />
          <Field label="Département" value={employee.departmentName} />
          <Field label="Poste" value={employee.positionTitle} />
        </div>
      </div>

      <div className="card p-4 space-y-4">
        <h2 className="font-bold text-sm text-gray-900">Informations personnelles</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Field label="E-mail" value={employee.email} />
          <Field label="Téléphone" value={employee.phone} />
          <Field label="Adresse" value={employee.address} />
          <Field label="Date de naissance" value={formatDate(employee.birthDate)} />
          <Field label="Numéro CNI" value={employee.nationalId} />
        </div>
        {!employee.nationalId && (
          <p className="text-[11px] text-gray-400">
            Certaines informations personnelles ne sont visibles que par le DRH et par l'employé concerné.
          </p>
        )}
      </div>

      {salaryHistory && (
        <div className="card p-4 space-y-3">
          <h2 className="font-bold text-sm text-gray-900">Historique des salaires</h2>
          {salaryHistory.length === 0 ? (
            <p className="text-xs text-gray-500">Aucune évolution de salaire enregistrée.</p>
          ) : (
            <ul className="divide-y divide-gray-100">
              {salaryHistory.map((item) => (
                <li key={item.id} className="py-2 text-xs">
                  <div className="font-semibold text-gray-900">
                    {formatFcfa(item.previousSalary)} → {formatFcfa(item.newSalary)}
                  </div>
                  <div className="text-gray-500">
                    {formatDate(item.effectiveDate)} · {item.changeReason}
                    {item.approvedByName ? ` · approuvé par ${item.approvedByName}` : ''}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
};

export default EmployeeDetailPage;