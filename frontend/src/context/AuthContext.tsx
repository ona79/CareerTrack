import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { UserDto, JwtResponse, LoginRequest } from '@/types';
import api from '@/lib/axios';

interface AuthContextType {
  user: UserDto | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Restaurer la session depuis le localStorage au démarrage
  useEffect(() => {
    const stored = localStorage.getItem('user');
    const token  = localStorage.getItem('accessToken');
    if (stored && token) {
      try {
        setUser(JSON.parse(stored) as UserDto);
      } catch {
        localStorage.clear();
      }
    }
    setIsLoading(false);
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    const res = await api.post('/auth/login', credentials);
    // Supporte le format enveloppé { success: true, data: { ... } } et direct
    const payload = ((res.data as Record<string, unknown>)?.data || res.data) as Record<string, unknown>;
    const user: UserDto = {
      id: (payload.userId || (payload.user as Record<string, unknown>)?.id || '') as string,
      username: (payload.username || (payload.user as Record<string, unknown>)?.username || '') as string,
      email: (payload.email || (payload.user as Record<string, unknown>)?.email || '') as string,
      roles: (payload.roles || (payload.user as Record<string, unknown>)?.roles || []) as string[],
      employeeId: (payload.employeeId || (payload.user as Record<string, unknown>)?.employeeId) as string | undefined,
    };
    localStorage.setItem('accessToken', payload.accessToken as string);
    localStorage.setItem('refreshToken', payload.refreshToken as string);
    localStorage.setItem('user', JSON.stringify(user));
    setUser(user);
  }, []);

  const logout = useCallback(() => {
    const refreshToken = localStorage.getItem('refreshToken');
    localStorage.clear();
    setUser(null);
    // Vider le cache de données API hors-ligne à la déconnexion pour protéger la confidentialité
    if (typeof window !== 'undefined' && 'caches' in window) {
      caches.delete('careertrack-api-cache').catch(() => {});
    }
    if (refreshToken) {
      api.post('/auth/logout', { refreshToken }).catch(() => {/* ignorer les erreurs réseau */});
    }
  }, []);

  const hasRole = useCallback(
    (role: string) => user?.roles.includes(role) ?? false,
    [user],
  );

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans <AuthProvider>');
  return ctx;
}
