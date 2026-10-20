import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { isAxiosError } from 'axios';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { useEmployee } from '@/hooks/useEmployeeDetail';
import {
  useManagerOptions,
  useReferenceLists,
  useSaveEmployee,
  type EmployeeFormValues,
} from '@/hooks/useEmployeeForm';
import type { EmployeeStatus } from '@/types';

const STATUS_OPTIONS: { value: EmployeeStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Actif' },
  { value: 'PROBATION', label: "Période d'essai" },
  { value: 'ON_LEAVE', label: 'En congé' },
  { value: 'SUSPENDED', label: 'Suspendu' },
  { value: 'TERMINATED', label: 'Sorti' },
];

const EMPTY_VALUES: EmployeeFormValues = {
  employeeNumber: '',
  firstName: '',
  lastName: '',
  nationalId: '',
  email: '',
  phone: '',
  address: '',
  birthDate: '',
  hireDate: '',
  departmentId: '',
  positionId: '',
  managerId: '',
  status: 'ACTIVE',
};

const Field: React.FC<{ label: string; error?: string; children: React.ReactNode }> = ({
  label,
  error,
  children,
}) => (
  <label className="block space-y-1">
    <span className="text-[11px] uppercase tracking-wide text-gray-500">{label}</span>
    {children}
    {error && <span className="block text-[11px] text-red-600">{error}</span>}
  </label>
);

export const EmployeeFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const { data: employee, isLoading: loadingEmployee } = useEmployee(id);
  const { departments, positions } = useReferenceLists();
  const { data: managers } = useManagerOptions();
  const save = useSaveEmployee(id);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<EmployeeFormValues>({ defaultValues: EMPTY_VALUES });

  const departmentId = watch('departmentId');

  useEffect(() => {
    if (!employee) return;
    reset({
      employeeNumber: employee.employeeNumber ?? '',
      firstName: employee.firstName ?? '',
      lastName: employee.lastName ?? '',
      nationalId: employee.nationalId ?? '',
      email: employee.email ?? '',
      phone: employee.phone ?? '',
      address: employee.address ?? '',
      birthDate: employee.birthDate ?? '',
      hireDate: employee.hireDate ?? '',
      departmentId: employee.departmentId ?? '',
      positionId: employee.positionId ?? '',
      managerId: employee.managerId ?? '',
      status: employee.status,
    });
  }, [employee, reset]);

  const loading =
    departments.isLoading || positions.isLoading || (isEdit && loadingEmployee);

  if (loading) {
    return (
      <div className="p-8 text-center text-xs text-gray-500 flex items-center justify-center gap-2">
        <Loader2 className="w-4 h-4 animate-spin text-[#1A6B3C]" />
        Chargement du formulaire...
      </div>
    );
  }

  const availablePositions = (positions.data ?? []).filter(
    (p) => !departmentId || p.parentId === departmentId,
  );
  const availableManagers = (managers ?? []).filter((m) => m.id !== id);

  const errorMessage = save.isError
    ? (isAxiosError(save.error) ? (save.error.response?.data?.message as string | undefined) : undefined) ??
      'Enregistrement impossible. Vérifiez les champs et réessayez.'
    : null;

  const onSubmit = (values: EmployeeFormValues) => {
    save.mutate(values, { onSuccess: (saved) => navigate(`/employees/${saved.id}`) });
  };

  return (
    <div className="space-y-6">
      <Link
        to={isEdit ? `/employees/${id}` : '/employees'}
        className="inline-flex items-center gap-1 text-xs text-[#1A6B3C]"
      >
        <ArrowLeft className="w-4 h-4" />
        Retour
      </Link>

      <div className="page-header">
        <div className="page-header-info">
          <h1>{isEdit ? 'Modifier le dossier' : 'Nouveau salarié'}</h1>
          <p>Les champs marqués d'une étoile sont obligatoires.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
        <div className="card p-4 space-y-4">
          <h2 className="font-bold text-sm text-gray-900">Identité</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Prénom *" error={errors.firstName?.message}>
              <input className="form-control text-sm" {...register('firstName', { required: 'Le prénom est obligatoire' })} />
            </Field>
            <Field label="Nom *" error={errors.lastName?.message}>
              <input className="form-control text-sm" {...register('lastName', { required: 'Le nom est obligatoire' })} />
            </Field>
            <Field label="Numéro CNI *" error={errors.nationalId?.message}>
              <input className="form-control text-sm" {...register('nationalId', { required: 'Le numéro CNI est obligatoire' })} />
            </Field>
            <Field label="Date de naissance *" error={errors.birthDate?.message}>
              <input type="date" className="form-control text-sm" {...register('birthDate', { required: 'La date de naissance est obligatoire' })} />
            </Field>
            <Field label="E-mail *" error={errors.email?.message}>
              <input
                type="email"
                className="form-control text-sm"
                {...register('email', {
                  required: "L'adresse e-mail est obligatoire",
                  pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Adresse e-mail invalide' },
                })}
              />
            </Field>
            <Field label="Téléphone">
              <input className="form-control text-sm" {...register('phone')} />
            </Field>
            <div className="sm:col-span-2">
              <Field label="Adresse">
                <input className="form-control text-sm" {...register('address')} />
              </Field>
            </div>
          </div>
        </div>

        <div className="card p-4 space-y-4">
          <h2 className="font-bold text-sm text-gray-900">Situation professionnelle</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Matricule *" error={errors.employeeNumber?.message}>
              <input
                className="form-control text-sm font-medium"
                placeholder="Ex: EMP-2026-001"
                {...register('employeeNumber', { required: 'Le matricule est obligatoire' })}
              />
            </Field>
            <Field label="Date d'embauche *" error={errors.hireDate?.message}>
              <input type="date" className="form-control text-sm" {...register('hireDate', { required: "La date d'embauche est obligatoire" })} />
            </Field>
            <Field label="Département">
              <select className="form-control text-sm" {...register('departmentId')}>
                <option value="">Aucun</option>
                {(departments.data ?? []).map((d: any) => (
                  <option key={d.id} value={d.id}>
                    {d.label || d.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Poste">
              <select className="form-control text-sm" {...register('positionId')}>
                <option value="">Aucun</option>
                {availablePositions.map((p: any) => (
                  <option key={p.id} value={p.id}>
                    {p.label || p.title || p.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Manager">
              <select className="form-control text-sm" {...register('managerId')}>
                <option value="">Aucun</option>
                {availableManagers.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.fullName}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Statut">
              <select className="form-control text-sm" {...register('status')}>
                {STATUS_OPTIONS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </div>

        {errorMessage && (
          <div className="rounded-lg bg-red-50 border border-red-200 p-3 text-xs text-red-700">{errorMessage}</div>
        )}

        <div className="flex gap-2 justify-end">
          <Link to={isEdit ? `/employees/${id}` : '/employees'} className="btn btn-secondary text-xs">
            Annuler
          </Link>
          <button type="submit" disabled={save.isPending} className="btn btn-primary text-xs flex items-center gap-2">
            {save.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
            {isEdit ? 'Enregistrer' : 'Créer le salarié'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default EmployeeFormPage;