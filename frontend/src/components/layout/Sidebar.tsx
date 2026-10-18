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
  ShieldAlert
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
  { label: 'Tableau de bord', to: '/dashboard', icon: LayoutDashboard },
  { label: 'Employés & Contrats', to: '/employees', icon: Users, roles: ['ROLE_DRH', 'ROLE_MANAGER'] },
  { label: 'Congés & Absences', to: '/leaves', icon: Calendar },
  { label: 'Paie & Fiches de paie', to: '/payroll', icon: FileSpreadsheet, roles: ['ROLE_DRH'] },
  { label: 'Recrutement (Kanban)', to: '/recruitment', icon: Briefcase, roles: ['ROLE_DRH', 'ROLE_MANAGER'] },
  { label: 'Évaluations Annuelles', to: '/evaluations', icon: Award, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE'] },
  { label: 'Promotions & Carrières', to: '/promotions', icon: Award, roles: ['ROLE_DRH', 'ROLE_MANAGER'] },
  { label: 'Plans de Formation', to: '/trainings', icon: GraduationCap },
  { label: 'Organigramme', to: '/orgchart', icon: Network },
  { label: 'Documents & PDF', to: '/documents', icon: FileSpreadsheet, roles: ['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE'] },
];

import { X } from 'lucide-react';

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { user, logout, hasRole } = useAuth();

  return (
    <aside className={`sidebar ${isOpen ? 'sidebar-open' : ''}`}>
      {/* En-tête / Logo */}
      <div className="sidebar-logo justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-white border border-gray-100 p-1 flex items-center justify-center shadow-xs shrink-0">
            <img
              src="/logo.png"
              alt="CareerTrack"
              className="w-full h-full object-contain"
            />
          </div>
          <div className="flex flex-col">
            <span className="sidebar-logo-text">
              Career<span>Track</span>
            </span>
            <span className="text-[10px] text-gray-500 font-medium tracking-wider uppercase">
              RH PME Sénégal
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="md:hidden p-1 text-gray-400 hover:text-gray-600 rounded-lg"
          aria-label="Fermer le menu"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Navigation */}
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
                <Icon className="w-5 h-5 text-gray-500" />
                <span>{item.label}</span>
                {item.badge && <span className="nav-badge">{item.badge}</span>}
              </NavLink>
            );
          })}
      </nav>

      {/* Profil connecté & Déconnexion */}
      <div className="sidebar-footer">
        <div className="flex items-center gap-3 mb-3 p-2 rounded-lg bg-gray-50 border border-gray-100">
          <div className="avatar avatar-sm avatar-primary font-bold">
            {user?.username?.substring(0, 2).toUpperCase() || 'CT'}
          </div>
          <div className="flex flex-col min-w-0 flex-1">
            <span className="text-xs font-semibold text-gray-900 truncate">
              {user?.username}
            </span>
            <span className="text-[10px] text-gray-500 truncate">
              {user?.roles?.[0]?.replace('ROLE_', '') || 'UTILISATEUR'}
            </span>
          </div>
        </div>

        <button
          onClick={logout}
          className="btn btn-secondary w-full text-xs text-red-700 hover:bg-red-50 hover:border-red-200"
          type="button"
        >
          <LogOut className="w-4 h-4 text-red-600" />
          Se déconnecter
        </button>
      </div>
    </aside>
  );
};
export default Sidebar;
