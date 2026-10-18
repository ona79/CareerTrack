import { useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import type { ApiResponse, OrgNode } from '@/types';

interface OrgNodeDto {
  id: string;
  name: string;
  employeeNumber?: string;
  title: string;
  department: string;
  email?: string;
  phone?: string;
  subordinates?: OrgNodeDto[];
}

function toOrgNode(dto: OrgNodeDto): OrgNode {
  return {
    id: dto.id,
    name: dto.name,
    title: dto.title,
    department: dto.department,
    children: (dto.subordinates ?? []).map(toOrgNode),
  };
}

export function useOrgChart() {
  return useQuery({
    queryKey: ['orgchart', 'tree'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<OrgNodeDto[]>>('/orgchart/tree');
      const roots = res.data.data ?? [];
      return roots.map(toOrgNode);
    },
    staleTime: 60_000,
  });
}