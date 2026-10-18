import React from 'react';
import { Bell, Search, Globe, LogOut, Menu } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

interface NavbarProps {
  onMenuToggle?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onMenuToggle }) => {
  const { user, logout } = useAuth();

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button
          type="button"
          onClick={onMenuToggle}
          className="md:hidden p-1.5 -ml-1 text-gray-600 hover:bg-gray-100 rounded-lg"
          aria-label="Ouvrir le menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="relative hidden sm:block">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none z-10" />
          <input
            type="text"
            placeholder="Rechercher collaborateur, congé, offre..."
            style={{ paddingLeft: '2.5rem' }}
            className="pr-4 py-2 text-xs bg-gray-50 border border-gray-200 rounded-lg w-72 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#1A6B3C]/20 focus:border-[#1A6B3C] transition-all"
          />
        </div>
        <span className="text-xs text-gray-400 hidden md:inline">
          Fuseau horaire : Africa/Dakar (GMT)
        </span>
      </div>

      <div className="topbar-right">
        {/* Monnaie Sénégal */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 bg-amber-50 border border-amber-200 rounded-md text-amber-900 text-xs font-semibold">
          <Globe className="w-3.5 h-3.5 text-amber-600" />
          <span>Devise : FCFA (XOF)</span>
        </div>

        {/* Notifications */}
        <button
          type="button"
          aria-label="Notifications"
          onClick={async () => {
            try {
              const res = await (await import('@/lib/axios')).default.get('/notifications/me');
              const notifs = res.data?.data || res.data || [];
              const unread = notifs.filter((n: { read?: boolean }) => !n.read).length;
              alert(`Vous avez ${unread} notification(s) non lue(s).`);
            } catch {
              // ignore
            }
          }}
          className="relative p-2 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors"
        >
          <Bell className="w-5 h-5" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-[#C0392B] rounded-full ring-2 ring-white" />
        </button>

        {/* Utilisateur */}
        <div className="flex items-center gap-2 pl-3 border-l border-gray-200">
          <div className="avatar avatar-sm avatar-primary">
            {user?.username?.[0]?.toUpperCase() || 'U'}
          </div>
          <div className="hidden sm:flex flex-col text-left">
            <span className="text-xs font-semibold text-gray-800 leading-tight">
              {user?.username}
            </span>
            <span className="text-[10px] text-gray-500">
              {user?.roles?.[0]?.replace('ROLE_', '')}
            </span>
          </div>
        </div>

        {/* Déconnexion */}
        <button
          type="button"
          onClick={logout}
          title="Se déconnecter de CareerTrack"
          className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-red-700 bg-red-50 hover:bg-red-100 border border-red-200 rounded-lg transition-colors cursor-pointer shrink-0"
        >
          <LogOut className="w-3.5 h-3.5 text-red-600" />
          <span className="hidden sm:inline">Déconnexion</span>
        </button>
      </div>
    </header>
  );
};
export default Navbar;
