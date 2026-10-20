import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Lock, User, AlertCircle } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [username, setUsername] = useState(import.meta.env.DEV ? 'drh.ndiaye' : '');
  const [password, setPassword] = useState(import.meta.env.DEV ? 'password123' : '');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/dashboard';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await login({ username, password });
      const stored = localStorage.getItem('user');
      const u = stored ? JSON.parse(stored) : null;
      const isMgmt = u?.roles?.some((r: string) => r.includes('DRH') || r.includes('MANAGER') || r.includes('DIRECTION') || r.includes('ADMIN'));
      const target = (from && from !== '/dashboard') ? from : (isMgmt ? '/dashboard' : '/leaves');
      navigate(target, { replace: true });
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      console.error('Erreur lors de la tentative de connexion:', { status, error: err });
      if (status === 401) {
        setError('Identifiants incorrects');
      } else {
        setError('Serveur inaccessible, réessayez plus tard');
      }
    } finally {
      setIsSubmitting(false);
    }
  };


  return (
    <div className="min-h-screen bg-gradient-to-br from-[#1A6B3C]/10 via-white to-[#F5A623]/10 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden">
        {/* En-tête avec bandeau sénégalais */}
        <div className="bg-[#1A6B3C] p-6 text-white text-center relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-[#F5A623]/20 rounded-full blur-2xl pointer-events-none" />
          
          {/* Logo CareerTrack officiel */}
          <div className="flex justify-center mb-3">
            <div className="w-16 h-16 bg-white rounded-2xl p-1.5 shadow-md flex items-center justify-center">
              <img
                src="/logo.png"
                alt="Logo CareerTrack Sénégal"
                className="w-full h-full object-contain rounded-xl"
              />
            </div>
          </div>
          
          <h1 className="text-xl font-bold tracking-tight text-white mb-1">CareerTrack Sénégal</h1>
          <p className="text-xs font-medium" style={{ color: '#E8F5EE' }}>
            Plateforme RH &amp; Gestion des Carrières pour PME
          </p>
        </div>

        {/* Formulaire */}
        <div className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-xs text-red-700">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="form-group">
              <label className="form-label" htmlFor="username">
                Identifiant / Nom d'utilisateur
              </label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                <input
                  id="username"
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="form-control has-icon-left text-sm"
                  style={{ paddingLeft: '2.75rem' }}
                  placeholder="Ex : prenom.nom"
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="password">
                Mot de passe
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                <input
                  id="password"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="form-control has-icon-left text-sm"
                  style={{ paddingLeft: '2.75rem' }}
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="btn btn-primary w-full py-2.5 text-sm font-semibold shadow-md mt-2 cursor-pointer"
            >
              {isSubmitting ? 'Connexion en cours...' : 'Se connecter à l\'espace RH'}
            </button>
          </form>
        </div>

        <div className="px-6 py-3 bg-gray-50 border-t border-gray-100 text-center text-[11px] text-gray-400">
          Système RH conforme au Code du Travail de la République du Sénégal
        </div>
      </div>
    </div>
  );
};
export default LoginPage;
