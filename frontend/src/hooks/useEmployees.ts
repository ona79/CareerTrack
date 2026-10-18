import { useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import type { ApiResponse, Employee, Page } from '@/types';

export interface UseEmployeesParams {
  page?: number;
  size?: number;
  sortBy?: string;
}

export function useEmployees({ page = 0, size = 50, sortBy = 'lastName' }: UseEmployeesParams = {}) {
  return useQuery({
    queryKey: ['employees', page, size, sortBy],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Page<Employee>>>('/employees', {
        params: { page, size, sortBy },
      });
      return res.data.data;
    },
    staleTime: 30_000,
  });
}