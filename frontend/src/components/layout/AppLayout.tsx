import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import { WifiOff } from 'lucide-react';

export const AppLayout: React.FC = () => {
  const [isOnline, setIsOnline] = useState<boolean>(navigator.onLine);
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return (
    <div className="app-layout">
      {/* Bandeau d'état hors-ligne */}
      {!isOnline && (
        <div
          role="alert"
          className="bg-amber-600 text-white px-4 py-2 text-xs font-medium flex items-center justify-center gap-2 fixed top-0 left-0 right-0 z-50 shadow-md"
        >
          <WifiOff className="w-4 h-4 shrink-0" />
          <span>
            Mode hors ligne : Données en cache local. Vos actions seront synchronisées dès le rétablissement de la connexion.
          </span>
        </div>
      )}

      {/* Backdrop sombre mobile quand le menu est ouvert */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 bg-black/40 z-40 md:hidden backdrop-blur-xs transition-opacity"
          onClick={() => setMobileMenuOpen(false)}
          aria-hidden="true"
        />
      )}

      <Sidebar isOpen={mobileMenuOpen} onClose={() => setMobileMenuOpen(false)} />

      <div className={`main-content ${!isOnline ? 'pt-8' : ''}`}>
        <Navbar onMenuToggle={() => setMobileMenuOpen((prev) => !prev)} />
        <main className="page-container flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
export default AppLayout;

