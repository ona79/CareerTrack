import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  Users,
  Calendar,
  Award,
  Briefcase,
  GraduationCap,
  FileSpreadsheet,
  Network,
  LogOut,
  LayoutDashboard,
  X,
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

interface NavItemConfig {
  label: string;
  to: string;
  icon: React.ComponentType<{ className?: string }>;
  roles?: string[];
  badge?: string;
}

const navItems: NavItemConfig[] = [
  { label: 'Tableau de bord', to: '/dashboard', icon: LayoutDashboard, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_DIRECTION', 'ROLE_ADMIN'] },
  { label: 'Employés & Contrats', to: '/employees', icon: Users, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_ADMIN'] },
  { label: 'Congés & Absences', to: '/leaves', icon: Calendar },
  { label: 'Paie & Fiches de paie', to: '/payroll', icon: FileSpreadsheet, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE', 'ROLE_DIRECTION', 'ROLE_ADMIN'] },
  { label: 'Recrutement (Kanban)', to: '/recruitment', icon: Briefcase, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_DIRECTION', 'ROLE_ADMIN'] },
  { label: 'Évaluations Annuelles', to: '/evaluations', icon: Award, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE', 'ROLE_DIRECTION', 'ROLE_ADMIN'] },
  { label: 'Promotions & Carrières', to: '/promotions', icon: Award, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_DIRECTION', 'ROLE_ADMIN'] },
  { label: 'Plans de Formation', to: '/trainings', icon: GraduationCap },
  { label: 'Organigramme', to: '/orgchart', icon: Network },
  { label: 'Documents & PDF', to: '/documents', icon: FileSpreadsheet, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE', 'ROLE_ADMIN'] },
];

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { user, logout, hasRole } = useAuth();

  return (
    <aside className={`sidebar ${isOpen ? 'sidebar-open' : ''}`}>
      {/* En-tête compacte sur fond sombre #131B2E */}
      <div className="sidebar-logo justify-between">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="w-8 h-8 rounded-lg bg-white p-1 flex items-center justify-center shrink-0 shadow-xs">
            <img
              src="/logo.png"
              alt="CareerTrack"
              className="w-full h-full object-contain"
            />
          </div>
          <div className="flex flex-col min-w-0">
            <span className="sidebar-logo-text truncate">
              Career<span>Track</span>
            </span>
            <span className="text-[9px] text-slate-400 font-semibold tracking-wider uppercase truncate">
              RH PME Sénégal
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="md:hidden p-1 text-slate-400 hover:text-white rounded-lg cursor-pointer"
          aria-label="Fermer le menu"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Navigation compacte */}
      <nav className="sidebar-nav">
        <div className="nav-section-label">Modules Principaux</div>
        {navItems
          .filter((item) => !item.roles || item.roles.some((r) => hasRole(r)))
          .map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => onClose?.()}
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              >
                <Icon className="w-4 h-4" />
                <span className="truncate">{item.label}</span>
                {item.badge && <span className="nav-badge">{item.badge}</span>}
              </NavLink>
            );
          })}
      </nav>

      {/* Profil connecté & Déconnexion */}
      <div className="sidebar-footer">
        <div className="flex items-center gap-2 p-1.5 rounded-lg bg-white/5 border border-white/10 hover:border-white/15 transition-colors">
          <div className="avatar avatar-sm bg-[#22C55E]/20 text-[#4ADE80] font-bold">
            {user?.username?.substring(0, 2).toUpperCase() || 'CT'}
          </div>
          <div className="flex flex-col min-w-0 flex-1">
            <span className="text-xs font-semibold text-white truncate">
              {user?.username}
            </span>
            <span className="text-[10px] text-slate-400 truncate">
              {user?.roles?.[0]?.replace('ROLE_', '') || 'UTILISATEUR'}
            </span>
          </div>
          <button
            onClick={logout}
            title="Se déconnecter"
            aria-label="Se déconnecter"
            className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors cursor-pointer"
            type="button"
          >
            <LogOut className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </aside>
  );
};
export default Sidebar;
