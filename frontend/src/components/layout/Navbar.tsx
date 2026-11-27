import React, { useState, useEffect } from 'react';
import { Bell, Globe, Clock, Menu } from 'lucide-react';

interface NavbarProps {
  onMenuToggle?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onMenuToggle }) => {
  const [timeZone, setTimeZone] = useState<string>('Africa/Dakar');

  useEffect(() => {
    try {
      const detected = Intl.DateTimeFormat().resolvedOptions().timeZone;
      if (detected) {
        setTimeZone(detected);
      }
    } catch {
      setTimeZone('Africa/Dakar');
    }
  }, []);

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button
          type="button"
          onClick={onMenuToggle}
          className="md:hidden p-1.5 -ml-1 text-slate-600 hover:bg-slate-100 rounded-lg cursor-pointer"
          aria-label="Ouvrir le menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Fuseau horaire compact en pilule */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 bg-slate-50 border border-slate-200 rounded-full text-[11px] font-medium text-slate-600">
          <Clock className="w-3 h-3 text-slate-400 shrink-0" />
          <span className="hidden sm:inline">Fuseau :</span>
          <strong className="text-slate-800">{timeZone}</strong>
        </div>
      </div>

      <div className="topbar-right">
        {/* Monnaie Sénégal compacte en pilule */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 bg-amber-50 border border-amber-200/80 rounded-full text-amber-900 text-[11px] font-semibold">
          <Globe className="w-3 h-3 text-amber-600 shrink-0" />
          <span>FCFA (XOF)</span>
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
          className="relative p-1.5 rounded-full text-slate-500 hover:bg-slate-100 transition-colors cursor-pointer"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-1 right-1 w-1.5 h-1.5 bg-[#C0392B] rounded-full ring-2 ring-white" />
        </button>
      </div>
    </header>
  );
};

export default Navbar;
