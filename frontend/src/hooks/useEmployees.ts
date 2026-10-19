import { keepPreviousData, useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import type { ApiResponse, EmployeeSummary, Page } from '@/types';

export interface UseEmployeesParams {
  page?: number;
  size?: number;
  sortBy?: string;
  search?: string;
  departmentId?: string;
}

export function useEmployees({
  page = 0,
  size = 20,
  sortBy = 'lastName',
  search,
  departmentId,
}: UseEmployeesParams = {}) {
  return useQuery({
    queryKey: ['employees', page, size, sortBy, search ?? '', departmentId ?? ''],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Page<EmployeeSummary>>>('/employees', {
        params: { page, size, sortBy, search, departmentId },
      });
      return res.data.data;
    },
    placeholderData: keepPreviousData,
    staleTime: 30_000,
  });
}