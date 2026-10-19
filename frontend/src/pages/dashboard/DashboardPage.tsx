import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Users,
  Calendar,
  Briefcase,
  TrendingUp,
  FileSpreadsheet,
  AlertTriangle,
  ArrowUpRight,
  ShieldCheck,
  CheckCircle2,
  Filter,
  RefreshCw,
  GraduationCap,
  DollarSign,
  Building2,
  PieChart as PieChartIcon,
  BarChart3,
  CalendarDays
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend
} from 'recharts';
import api from '@/lib/axios';
import { useAuth } from '@/context/AuthContext';
import type {
  DashboardStatsDto,
  DepartmentDto,
  ApiResponse
} from '@/types';

interface PositionOption {
  id: string;
  title: string;
}

const CONTRACT_COLORS: Record<string, string> = {
  CDI: '#1A6B3C',
  CDD: '#F5A623',
  STAGE: '#2980B9',
  PRESTATION: '#8E44AD',
  INTERIM: '#E67E22',
  AUTRE: '#7F8C8D'
};

const PYRAMID_COLORS = ['#3498DB', '#1A6B3C', '#27AE60', '#F39C12', '#E74C3C'];

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const currentYear = new Date().getFullYear();

  const [selectedYear, setSelectedYear] = useState<number>(currentYear);
  const [selectedDeptId, setSelectedDeptId] = useState<string>('');
  const [selectedPosId, setSelectedPosId] = useState<string>('');

  // Fetch départements
  const { data: departments = [] } = useQuery({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<DepartmentDto[]>>('/departments');
      return res.data.data || [];
    }
  });

  // Fetch postes
  const { data: positions = [] } = useQuery({
    queryKey: ['positions'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PositionOption[]>>('/positions');
      return res.data.data || [];
    }
  });

  // Fetch stats dashboard
  const {
    data: stats,
    isLoading,
    isError,
    error,
    refetch,
    isFetching
  } = useQuery({
    queryKey: ['dashboard-stats', selectedDeptId, selectedPosId, selectedYear],
    queryFn: async () => {
      const params: Record<string, string | number> = {
        periodYear: selectedYear
      };
      if (selectedDeptId) params.departmentId = selectedDeptId;
      if (selectedPosId) params.positionId = selectedPosId;

      const res = await api.get<ApiResponse<DashboardStatsDto>>('/dashboard/stats', { params });
      return res.data.data;
    }
  });

  const formatCFA = (amount?: number) => {
    if (amount === undefined || amount === null) return '0 FCFA';
    return new Intl.NumberFormat('fr-FR').format(Math.round(amount)) + ' FCFA';
  };

  // Préparation données contrats
  const contractChartData = (stats?.headcount?.contractBreakdown || []).map((c) => ({
    name: c.contractType,
    value: c.count,
    percentage: c.percentage,
    color: CONTRACT_COLORS[c.contractType] || '#95A5A6'
  }));

  // Préparation données pyramide des âges
  const agePyramidData = (stats?.agePyramid || []).map((p, idx) => ({
    name: p.bracket,
    count: p.count,
    percentage: p.percentage,
    color: PYRAMID_COLORS[idx % PYRAMID_COLORS.length]
  }));

  // Préparation données masse salariale mensuelle
  const payrollTrendData = (stats?.payroll?.monthlyTrend || []).map((m) => ({
    mois: m.monthLabel,
    brut: m.grossSalary,
    net: m.netSalary,
    patronal: m.employerContributions
  }));

  return (
    <div className="space-y-6">
      {/* En-tête de la page */}
      <div className="page-header flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="page-header-info">
          <div className="flex items-center gap-2">
            <h1>Tableau de Bord RH & Pilotage</h1>
            {user?.roles?.includes('MANAGER') && !user?.roles?.includes('DRH') && !user?.roles?.includes('DIRECTION') && (
              <span className="badge badge-info text-xs">Périmètre Équipe N+1</span>
            )}
          </div>
          <p>Supervision des effectifs, pyramide des âges, masse salariale, congés et formations</p>
        </div>
        <div className="flex flex-wrap gap-2 w-full sm:w-auto">
          <a href="/employees" className="btn btn-primary text-xs min-h-[40px] flex-1 sm:flex-none justify-center">
            <Users className="w-4 h-4 mr-1.5" />
            Collaborateurs
          </a>
          <a href="/leaves" className="btn btn-secondary text-xs min-h-[40px] flex-1 sm:flex-none justify-center">
            <Calendar className="w-4 h-4 mr-1.5" />
            Gestion Congés
          </a>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="btn btn-secondary text-xs min-h-[40px] px-3"
            title="Rafraîchir les données"
          >
            <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Barre de Filtres Dynamiques */}
      <div className="card p-4">
        <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
          <div className="flex items-center gap-2 text-xs font-semibold text-gray-700 whitespace-nowrap">
            <Filter className="w-4 h-4 text-[#1A6B3C]" />
            Filtres de pilotage :
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 flex-1">
            {/* Filtre Département */}
            <select
              value={selectedDeptId}
              onChange={(e) => setSelectedDeptId(e.target.value)}
              className="text-xs rounded-lg border border-gray-300 py-2 px-3 bg-white focus:outline-none focus:ring-1 focus:ring-[#1A6B3C]"
            >
              <option value="">Tous les départements</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>

            {/* Filtre Poste */}
            <select
              value={selectedPosId}
              onChange={(e) => setSelectedPosId(e.target.value)}
              className="text-xs rounded-lg border border-gray-300 py-2 px-3 bg-white focus:outline-none focus:ring-1 focus:ring-[#1A6B3C]"
            >
              <option value="">Tous les postes</option>
              {positions.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.title}
                </option>
              ))}
            </select>

            {/* Filtre Année */}
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(parseInt(e.target.value, 10))}
              className="text-xs rounded-lg border border-gray-300 py-2 px-3 bg-white focus:outline-none focus:ring-1 focus:ring-[#1A6B3C]"
            >
              <option value={currentYear}>Année {currentYear}</option>
              <option value={currentYear - 1}>Année {currentYear - 1}</option>
              <option value={currentYear - 2}>Année {currentYear - 2}</option>
            </select>
          </div>

          {(selectedDeptId || selectedPosId || selectedYear !== currentYear) && (
            <button
              onClick={() => {
                setSelectedDeptId('');
                setSelectedPosId('');
                setSelectedYear(currentYear);
              }}
              className="text-xs text-gray-500 hover:text-red-600 underline whitespace-nowrap self-center"
            >
              Réinitialiser
            </button>
          )}
        </div>
      </div>

      {/* État de chargement / Erreur */}
      {isLoading && (
        <div className="card p-12 text-center text-gray-500 flex flex-col items-center justify-center gap-3">
          <RefreshCw className="w-8 h-8 animate-spin text-[#1A6B3C]" />
          <p className="text-sm font-medium">Chargement des indicateurs RH...</p>
        </div>
      )}

      {isError && (
        <div className="card p-6 bg-red-50 border border-red-200 text-red-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertTriangle className="w-6 h-6 text-red-600" />
            <div>
              <p className="font-semibold text-sm">Impossible de charger les statistiques du tableau de bord</p>
              <p className="text-xs text-red-600">{(error as Error)?.message || 'Vérifiez vos droits d’accès.'}</p>
            </div>
          </div>
          <button onClick={() => refetch()} className="btn btn-secondary text-xs">
            Réessayer
          </button>
        </div>
      )}

      {stats && (
        <>
          {/* Cartes KPI Principales */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Effectif Actif */}
            <div className="stat-card primary">
              <div className="stat-icon primary">
                <Users className="w-6 h-6" />
              </div>
              <div>
                <div className="stat-label">Effectif Total Actif</div>
                <div className="stat-value">{stats.headcount?.activeEmployees ?? 0}</div>
                <div className="stat-trend up flex items-center gap-1">
                  <TrendingUp className="w-3.5 h-3.5" />
                  <span>
                    {stats.headcount?.totalEmployees ?? 0} au registre • {stats.headcount?.onLeaveEmployees ?? 0} en congé
                  </span>
                </div>
              </div>
            </div>

            {/* Congés en attente */}
            <div className="stat-card accent">
              <div className="stat-icon accent">
                <Calendar className="w-6 h-6" />
              </div>
              <div>
                <div className="stat-label">Congés en Attente N+1</div>
                <div className="stat-value">{stats.leaves?.pendingRequests ?? 0}</div>
                <div className="text-[11px] text-gray-500 mt-1 flex items-center justify-between">
                  <span>{stats.leaves?.approvedRequests ?? 0} validés</span>
                  <a href="/leaves" className="text-amber-700 font-semibold hover:underline flex items-center">
                    Gérer <ArrowUpRight className="w-3 h-3 ml-0.5" />
                  </a>
                </div>
              </div>
            </div>

            {/* Masse Salariale Brute */}
            <div className="stat-card info">
              <div className="stat-icon info">
                <DollarSign className="w-6 h-6" />
              </div>
              <div>
                <div className="stat-label">Masse Salariale Brute ({selectedYear})</div>
                <div className="stat-value text-base lg:text-lg">
                  {formatCFA(stats.payroll?.totalGrossSalary)}
                </div>
                <div className="text-[11px] text-gray-500 mt-1">
                  Charges patr. : {formatCFA(stats.payroll?.totalEmployerContributions)}
                </div>
              </div>
            </div>

            {/* Turnover & Mouvements */}
            <div className="stat-card danger">
              <div className="stat-icon danger">
                <TrendingUp className="w-6 h-6" />
              </div>
              <div>
                <div className="stat-label">Taux de Turnover</div>
                <div className="stat-value">
                  {stats.turnover?.turnoverRate !== undefined ? `${stats.turnover.turnoverRate.toFixed(1)}%` : '0%'}
                </div>
                <div className="text-[11px] text-gray-500 mt-1">
                  +{stats.turnover?.hiresCount ?? 0} entrées / -{stats.turnover?.departuresCount ?? 0} départs
                </div>
              </div>
            </div>
          </div>

          {/* Deuxième Ligne : Graphiques Contrats & Pyramide des âges */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Typologie des Contrats */}
            <div className="card">
              <div className="card-header flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <PieChartIcon className="w-4 h-4 text-[#1A6B3C]" />
                  <span className="card-title">Typologie des Contrats Actifs</span>
                </div>
                <span className="badge badge-success text-xs">
                  {stats.headcount?.activeEmployees ?? 0} contrats
                </span>
              </div>
              <div className="card-body flex flex-col items-center">
                {contractChartData.length > 0 ? (
                  <>
                    <div className="h-64 w-full">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={contractChartData}
                            cx="50%"
                            cy="50%"
                            innerRadius={55}
                            outerRadius={85}
                            paddingAngle={3}
                            dataKey="value"
                          >
                            {contractChartData.map((entry, index) => (
                              <Cell key={`contract-cell-${index}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip
                            formatter={(value: any, name: any, item: any) => [
                              `${value} (${item.payload.percentage.toFixed(1)}%)`,
                              name
                            ]}
                          />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 w-full mt-3 text-xs border-t pt-3">
                      {contractChartData.map((item) => (
                        <div key={item.name} className="flex items-center gap-2">
                          <span className="w-3 h-3 rounded-sm flex-shrink-0" style={{ backgroundColor: item.color }} />
                          <span className="text-gray-600 truncate">{item.name} :</span>
                          <span className="font-semibold text-gray-900">{item.value}</span>
                        </div>
                      ))}
                    </div>
                  </>
                ) : (
                  <div className="p-8 text-center text-xs text-gray-400">Aucun contrat actif répertorié</div>
                )}
              </div>
            </div>

            {/* Pyramide des Âges */}
            <div className="card">
              <div className="card-header flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <BarChart3 className="w-4 h-4 text-[#1A6B3C]" />
                  <span className="card-title">Pyramide des Âges</span>
                </div>
                <span className="text-xs text-gray-400">Distribution par tranche</span>
              </div>
              <div className="card-body">
                {agePyramidData.length > 0 ? (
                  <div className="h-64 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={agePyramidData} layout="vertical" margin={{ left: 20, right: 20 }}>
                        <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#E5E7EB" />
                        <XAxis type="number" stroke="#9CA3AF" fontSize={11} allowDecimals={false} />
                        <YAxis dataKey="name" type="category" stroke="#4B5563" fontSize={11} width={80} />
                        <Tooltip
                          formatter={(value: any, name: any, item: any) => [
                            `${value} collaborateur(s) (${item.payload.percentage.toFixed(1)}%)`,
                            'Effectif'
                          ]}
                        />
                        <Bar dataKey="count" name="Effectif" fill="#1A6B3C" radius={[0, 4, 4, 0]}>
                          {agePyramidData.map((entry, index) => (
                            <Cell key={`age-cell-${index}`} fill={entry.color} />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <div className="p-8 text-center text-xs text-gray-400">Aucune donnée d'âge disponible</div>
                )}
              </div>
            </div>
          </div>

          {/* Troisième Ligne : Masse Salariale Mensuelle */}
          <div className="card">
            <div className="card-header flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
              <div>
                <span className="card-title">Évolution Mensuelle de la Masse Salariale ({selectedYear})</span>
                <p className="text-xs text-gray-400 mt-0.5">
                  Salaires bruts déclarés et cotisations patronales associées
                </p>
              </div>
              <div className="flex items-center gap-4 text-xs">
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-sm bg-[#1A6B3C]" />
                  <span className="text-gray-600 font-medium">Brut</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-sm bg-[#F5A623]" />
                  <span className="text-gray-600 font-medium">Charges patr.</span>
                </div>
              </div>
            </div>
            <div className="card-body">
              {payrollTrendData.some((m) => m.brut > 0 || m.patronal > 0) ? (
                <div className="h-72 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={payrollTrendData}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                      <XAxis dataKey="mois" stroke="#9CA3AF" fontSize={12} />
                      <YAxis
                        stroke="#9CA3AF"
                        fontSize={11}
                        tickFormatter={(v) => `${(v / 1000000).toFixed(1)}M`}
                      />
                      <Tooltip
                        formatter={(val: any) => formatCFA(Number(val))}
                        labelFormatter={(label) => `Mois : ${label}`}
                      />
                      <Bar dataKey="brut" name="Salaire Brut" fill="#1A6B3C" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="patronal" name="Charges Patronales" fill="#F5A623" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <div className="p-8 text-center text-xs text-gray-400">
                  Aucun bulletin de paie généré pour l'année {selectedYear} dans ce périmètre.
                </div>
              )}
            </div>
          </div>

          {/* Quatrième Ligne : Plan de Formation & Alertes Réglementaires */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Indicateurs Formation */}
            <div className="card lg:col-span-1">
              <div className="card-header flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <GraduationCap className="w-4 h-4 text-[#1A6B3C]" />
                  <span className="card-title">Plan de Formation</span>
                </div>
                <span className="badge badge-info text-xs">Annuel</span>
              </div>
              <div className="card-body space-y-4">
                <div className="grid grid-cols-2 gap-3 text-center">
                  <div className="p-3 bg-gray-50 rounded-lg border border-gray-100">
                    <div className="text-xl font-bold text-gray-900">{stats.training?.totalTrainings ?? 0}</div>
                    <div className="text-[11px] text-gray-500 mt-0.5">Formations au catalogue</div>
                  </div>
                  <div className="p-3 bg-green-50 rounded-lg border border-green-100">
                    <div className="text-xl font-bold text-[#1A6B3C]">{stats.training?.activeSessions ?? 0}</div>
                    <div className="text-[11px] text-green-700 mt-0.5">Sessions en cours</div>
                  </div>
                </div>

                <div className="p-3.5 bg-blue-50/60 rounded-lg border border-blue-100 flex items-center justify-between text-xs">
                  <div>
                    <div className="font-semibold text-blue-900">Collaborateurs Formés</div>
                    <div className="text-blue-700 mt-0.5">
                      {stats.training?.totalParticipants ?? 0} participants enregistrés
                    </div>
                  </div>
                  <GraduationCap className="w-6 h-6 text-blue-600 opacity-80" />
                </div>
              </div>
            </div>

            {/* Conformité Légale Sénégal & CDD */}
            <div className="card lg:col-span-2">
              <div className="card-header flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-[#1A6B3C]" />
                  <span className="card-title">Conformité Légale & Sociale (Sénégal)</span>
                </div>
                <span className="text-xs text-green-700 bg-green-50 px-2.5 py-0.5 rounded font-semibold border border-green-200">
                  Règles Sénégal Actives
                </span>
              </div>
              <div className="card-body divide-y divide-gray-100 p-0 text-xs">
                <div className="p-3.5 flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-gray-800">Congés Payés Annuels (Art. L148)</div>
                    <div className="text-gray-500">
                      Acquisition de 2 jours ouvrables par mois effectif • Décompte du lundi au vendredi
                    </div>
                  </div>
                  <CheckCircle2 className="w-4 h-4 text-[#1A6B3C]" />
                </div>
                <div className="p-3.5 flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-gray-800">Cotisations Retraite IPRES</div>
                    <div className="text-gray-500">Régime général 5,6% sal. / 8,4% patr. + Régime cadres 2,4% / 3,6%</div>
                  </div>
                  <CheckCircle2 className="w-4 h-4 text-[#1A6B3C]" />
                </div>
                <div className="p-3.5 flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-gray-800">Caisse de Sécurité Sociale (CSS)</div>
                    <div className="text-gray-500">Accidents du travail & Prestations familiales plafonnées à 63 000 FCFA</div>
                  </div>
                  <CheckCircle2 className="w-4 h-4 text-[#1A6B3C]" />
                </div>
                <div className="p-3.5 flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-gray-800">Plafond CDD Légal (24 mois - Art. L44)</div>
                    <div className="text-gray-500">Contrôle strict des renouvellements et requalification CDI</div>
                  </div>
                  <span className="badge badge-warning text-[10px]">Surveillance active</span>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default DashboardPage;
