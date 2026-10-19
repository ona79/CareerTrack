import { useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import type { ApiResponse, EmployeeStatus } from '@/types';

export interface EmployeeDetail {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  fullName: string;
  nationalId?: string | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  birthDate?: string | null;
  hireDate: string;
  departmentId?: string;
  departmentName?: string;
  positionId?: string;
  positionTitle?: string;
  managerId?: string;
  managerName?: string;
  status: EmployeeStatus;
}

export interface SalaryHistoryItem {
  id: string;
  previousSalary: number;
  newSalary: number;
  changeReason: string;
  effectiveDate: string;
  approvedByName?: string | null;
}

export function useEmployee(id: string | undefined) {
  return useQuery({
    queryKey: ['employee', id],
    enabled: Boolean(id),
    retry: false,
    queryFn: async () => {
      const res = await api.get<ApiResponse<EmployeeDetail>>(`/employees/${id}`);
      return res.data.data;
    },
  });
}

export function useSalaryHistory(id: string | undefined) {
  return useQuery({
    queryKey: ['employee', id, 'salary-history'],
    enabled: Boolean(id),
    retry: false,
    queryFn: async () => {
      const res = await api.get<ApiResponse<SalaryHistoryItem[]>>(`/employees/${id}/salary-history`);
      return res.data.data;
    },
  });
}