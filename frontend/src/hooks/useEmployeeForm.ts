import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import type { ApiResponse, EmployeeStatus, EmployeeSummary, Page } from '@/types';
import type { EmployeeDetail } from '@/hooks/useEmployeeDetail';

export interface ReferenceItem {
  id: string;
  label: string;
  parentId?: string | null;
}

export interface EmployeeFormValues {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  nationalId: string;
  email: string;
  phone: string;
  address: string;
  birthDate: string;
  hireDate: string;
  departmentId: string;
  positionId: string;
  managerId: string;
  status: EmployeeStatus;
}

export function useReferenceLists() {
  const departments = useQuery({
    queryKey: ['reference', 'departments'],
    staleTime: 5 * 60_000,
    queryFn: async () => (await api.get<ApiResponse<ReferenceItem[]>>('/departments')).data.data,
  });
  const positions = useQuery({
    queryKey: ['reference', 'positions'],
    staleTime: 5 * 60_000,
    queryFn: async () => (await api.get<ApiResponse<ReferenceItem[]>>('/positions')).data.data,
  });
  return { departments, positions };
}

export function useManagerOptions() {
  return useQuery({
    queryKey: ['reference', 'managers'],
    staleTime: 60_000,
    queryFn: async () => {
      const res = await api.get<ApiResponse<Page<EmployeeSummary>>>('/employees', {
        params: { page: 0, size: 100, sortBy: 'lastName' },
      });
      return res.data.data.content;
    },
  });
}

function toPayload(values: EmployeeFormValues) {
  return {
    employeeNumber: values.employeeNumber.trim(),
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    nationalId: values.nationalId.trim(),
    email: values.email.trim(),
    phone: values.phone.trim() || null,
    address: values.address.trim() || null,
    birthDate: values.birthDate,
    hireDate: values.hireDate,
    departmentId: values.departmentId || null,
    positionId: values.positionId || null,
    managerId: values.managerId || null,
    status: values.status,
  };
}

export function useSaveEmployee(id?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (values: EmployeeFormValues) => {
      const payload = toPayload(values);
      const res = id
        ? await api.put<ApiResponse<EmployeeDetail>>(`/employees/${id}`, payload)
        : await api.post<ApiResponse<EmployeeDetail>>('/employees', payload);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      queryClient.invalidateQueries({ queryKey: ['employee'] });
      queryClient.invalidateQueries({ queryKey: ['orgchart'] });
    },
  });
}