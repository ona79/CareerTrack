import React from 'react';
import {
  Users,
  Calendar,
  Briefcase,
  TrendingUp,
  FileSpreadsheet,
  AlertTriangle,
  ArrowUpRight,
  ShieldCheck,
  CheckCircle2
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
  Cell
} from 'recharts';

const contractData = [
  { name: 'CDI', value: 85, color: '#1A6B3C' },
  { name: 'CDD', value: 25, color: '#F5A623' },
  { name: 'Stage', value: 10, color: '#2980B9' },
  { name: 'Prestation', value: 5, color: '#8E44AD' },
];

const leaveMonthlyData = [
  { mois: 'Jan', demandes: 4, approuves: 4 },
  { mois: 'Fév', demandes: 6, approuves: 5 },
  { mois: 'Mar', demandes: 8, approuves: 7 },
  { mois: 'Avr', demandes: 5, approuves: 5 },
  { mois: 'Mai', demandes: 9, approuves: 8 },
  { mois: 'Juin', demandes: 14, approuves: 12 },
  { mois: 'Juil', demandes: 22, approuves: 20 },
  { mois: 'Août', demandes: 28, approuves: 26 },
];

export const DashboardPage: React.FC = () => {
  return (
    <div className="space-y-6">
      {/* En-tête de la page */}
      <div className="page-header">
        <div className="page-header-info">
          <h1>Tableau de Bord RH & Carrières</h1>
          <p>Supervision globale des effectifs, contrats, paie et demandes de congés</p>
        </div>
        <div className="flex gap-2">
          <a href="/employees" className="btn btn-primary text-xs">
            <Users className="w-4 h-4" />
            Nouveau Collaborateur
          </a>
          <a href="/leaves" className="btn btn-secondary text-xs">
            <Calendar className="w-4 h-4" />
            Poser un Congé
          </a>
        </div>
      </div>

      {/* Cartes KPI */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="stat-card primary">
          <div className="stat-icon primary">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <div className="stat-label">Effectif Total Actif</div>
            <div className="stat-value">125</div>
            <div className="stat-trend up flex items-center gap-1">
              <TrendingUp className="w-3.5 h-3.5" />
              <span>+3 recrues ce mois</span>
            </div>
          </div>
        </div>

        <div className="stat-card accent">
          <div className="stat-icon accent">
            <Calendar className="w-6 h-6" />
          </div>
          <div>
            <div className="stat-label">Congés en attente</div>
            <div className="stat-value">7</div>
            <div className="text-[11px] text-gray-400 mt-1">À valider par managers N+1</div>
          </div>
        </div>

        <div className="stat-card info">
          <div className="stat-icon info">
            <Briefcase className="w-6 h-6" />
          </div>
          <div>
            <div className="stat-label">Postes Ouverts</div>
            <div className="stat-value">4</div>
            <div className="text-[11px] text-blue-600 font-medium mt-1">18 candidatures reçues</div>
          </div>
        </div>

        <div className="stat-card danger">
          <div className="stat-icon danger">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <div className="stat-label">Alertes CDD (&lt; 60j)</div>
            <div className="stat-value text-red-600">3</div>
            <div className="text-[11px] text-red-600 font-medium mt-1">Limite 24 mois (Art. L44)</div>
          </div>
        </div>
      </div>

      {/* Graphiques */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Répartition des contrats */}
        <div className="card lg:col-span-1">
          <div className="card-header">
            <span className="card-title">Typologie des Contrats</span>
            <span className="text-xs text-gray-400">Total : 125</span>
          </div>
          <div className="card-body flex flex-col items-center">
            <div className="h-56 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={contractData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {contractData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="grid grid-cols-2 gap-3 w-full mt-2 text-xs">
              {contractData.map((item) => (
                <div key={item.name} className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-sm" style={{ backgroundColor: item.color }} />
                  <span className="text-gray-600">{item.name} :</span>
                  <span className="font-semibold text-gray-900">{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Évolution mensuelle des congés */}
        <div className="card lg:col-span-2">
          <div className="card-header">
            <div>
              <span className="card-title">Flux Mensuel des Demandes de Congés</span>
              <p className="text-xs text-gray-400 mt-0.5">
                Acquisition légale : 2 jours ouvrables par mois effectif
              </p>
            </div>
            <span className="badge badge-success">Année 2026</span>
          </div>
          <div className="card-body">
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={leaveMonthlyData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                  <XAxis dataKey="mois" stroke="#9CA3AF" fontSize={12} />
                  <YAxis stroke="#9CA3AF" fontSize={12} />
                  <Tooltip />
                  <Bar dataKey="demandes" name="Demandes" fill="#F5A623" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="approuves" name="Approuvés" fill="#1A6B3C" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      {/* Alertes Réglementaires Sénégal & Prochaines échéances */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="card-header">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-[#1A6B3C]" />
              <span className="card-title">Conformité Légale & Sociale (Sénégal)</span>
            </div>
            <span className="text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded font-semibold">
              À jour
            </span>
          </div>
          <div className="card-body divide-y divide-gray-100 p-0 text-xs">
            <div className="p-3.5 flex items-center justify-between">
              <div>
                <div className="font-semibold text-gray-800">Cotisations Retraite IPRES</div>
                <div className="text-gray-500">Taux employé 5,6% | Taux patronal 8,4%</div>
              </div>
              <CheckCircle2 className="w-4 h-4 text-[#1A6B3C]" />
            </div>
            <div className="p-3.5 flex items-center justify-between">
              <div>
                <div className="font-semibold text-gray-800">Caisse de Sécurité Sociale (CSS)</div>
                <div className="text-gray-500">Accidents du travail & Prestations familiales (7%)</div>
              </div>
              <CheckCircle2 className="w-4 h-4 text-[#1A6B3C]" />
            </div>
            <div className="p-3.5 flex items-center justify-between">
              <div>
                <div className="font-semibold text-gray-800">Plafond CDD légal (24 mois)</div>
                <div className="text-gray-500">Surveillance continue des renouvellements</div>
              </div>
              <span className="badge badge-warning">3 à surveiller</span>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div className="flex items-center gap-2">
              <FileSpreadsheet className="w-4 h-4 text-amber-600" />
              <span className="card-title">Dernières Actions Rapides</span>
            </div>
          </div>
          <div className="card-body p-4 space-y-2 text-xs">
            <a
              href="/payroll"
              className="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:border-gray-200 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-green-100 text-green-800 flex items-center justify-center font-bold">
                  PDF
                </div>
                <div>
                  <div className="font-semibold text-gray-800">Télécharger les Bulletins de Paie</div>
                  <div className="text-gray-400">Génération par lot avec déductions IPRES & CSS</div>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-gray-400" />
            </a>

            <a
              href="/recruitment"
              className="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:border-gray-200 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center font-bold">
                  KB
                </div>
                <div>
                  <div className="font-semibold text-gray-800">Suivi du Pipeline de Recrutement</div>
                  <div className="text-gray-400">Tableau Kanban glisser-déposer des candidatures</div>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-gray-400" />
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
export default DashboardPage;
