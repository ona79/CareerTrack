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
      <div className="topbar-left flex items-center gap-3">
        <button
          type="button"
          onClick={onMenuToggle}
          className="md:hidden p-1.5 -ml-1 text-gray-600 hover:bg-gray-100 rounded-lg cursor-pointer"
          aria-label="Ouvrir le menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Fuseau horaire détecté automatiquement */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 bg-gray-50 border border-gray-200 rounded-md text-xs font-medium text-gray-600 shadow-2xs">
          <Clock className="w-3.5 h-3.5 text-gray-400" />
          <span>Fuseau horaire : <strong className="text-gray-800">{timeZone}</strong></span>
        </div>
      </div>

      <div className="topbar-right flex items-center gap-3">
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
          className="relative p-2 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors cursor-pointer"
        >
          <Bell className="w-5 h-5" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-[#C0392B] rounded-full ring-2 ring-white" />
        </button>
      </div>
    </header>
  );
};

export default Navbar;
