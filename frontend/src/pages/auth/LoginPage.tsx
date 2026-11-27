import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Lock, User, AlertCircle, Users, ChevronDown, ChevronUp } from 'lucide-react';

const DEMO_ACCOUNTS = [
  { role: 'DRH', username: 'drh.ndiaye', password: 'password123', hint: 'Accès complet' },
  { role: 'Manager N+1', username: 'mgr.sow', password: 'password123', hint: 'Équipe & congés' },
  { role: 'Collaborateur', username: 'emp.diallo', password: 'password123', hint: 'Self-service' },
  { role: 'Direction', username: 'dir.fall', password: 'password123', hint: 'Lecture seule' },
] as const;

export const LoginPage: React.FC = () => {
  const [username, setUsername] = useState(import.meta.env.DEV ? 'drh.ndiaye' : '');
  const [password, setPassword] = useState(import.meta.env.DEV ? 'password123' : '');
  const [showDemoAccounts, setShowDemoAccounts] = useState(false);
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
      const isMgmt = u?.roles?.some((r: string) =>
        r.includes('DRH') || r.includes('MANAGER') || r.includes('DIRECTION') || r.includes('ADMIN')
      );
      const target = from && from !== '/dashboard' ? from : isMgmt ? '/dashboard' : '/leaves';
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
    <div className="min-h-screen w-full bg-white flex flex-col lg:flex-row overflow-x-hidden">
      {/* Panneau gauche sombre avec vague organique (desktop) / bandeau compact (mobile) */}
      <div className="relative bg-[#131B2E] text-white lg:w-[48%] lg:min-h-screen flex flex-col justify-center px-8 py-10 lg:px-14 xl:px-20 flex-shrink-0">
        {/* Halo subtil d'ambiance */}
        <div
          className="absolute -top-24 -left-24 w-72 h-72 rounded-full opacity-20 blur-3xl pointer-events-none"
          style={{ background: 'radial-gradient(circle, #1A6B3C 0%, transparent 70%)' }}
        />

        <div className="relative z-10 max-w-sm mx-auto lg:mx-0 text-center lg:text-left">
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight leading-[1.12] text-white">
            Accès à
            <span className="block mt-1 text-[#22C55E]">CAREERTRACK</span>
          </h1>
          <p className="mt-4 lg:mt-6 text-sm sm:text-[15px] leading-relaxed text-slate-300">
            Débloquez la gestion de vos collaborateurs, le suivi des congés et de la paie
            sénégalaise (IPRES/CSS) et analysez vos indicateurs RH en temps réel sur notre
            plateforme.
          </p>
        </div>

        {/* Séparateur vague organique verticale sur Desktop */}
        <svg
          className="hidden lg:block absolute top-0 -right-[1px] h-full w-24 xl:w-32 text-white pointer-events-none z-10"
          viewBox="0 0 120 800"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <path
            d="M45,0 C115,180 5,320 55,500 C95,640 15,730 45,800 L120,800 L120,0 Z"
            fill="currentColor"
          />
        </svg>

        {/* Illustration fusée stylisée 3D flottant sur la courbe (Desktop uniquement) */}
        <div
          className="hidden lg:flex items-center justify-center absolute right-0 top-1/2 -translate-y-1/2 translate-x-[38%] z-20 pointer-events-none select-none"
          aria-hidden="true"
        >
          <svg
            width="176"
            height="210"
            viewBox="0 0 180 220"
            fill="none"
            className="drop-shadow-[0_18px_28px_rgba(15,23,42,0.22)]"
          >
            <defs>
              <linearGradient id="ctBody" x1="65" y1="30" x2="125" y2="140" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#FFFFFF" />
                <stop offset="70%" stopColor="#E2E8F0" />
                <stop offset="100%" stopColor="#CBD5E1" />
              </linearGradient>
              <linearGradient id="ctNose" x1="88" y1="12" x2="120" y2="55" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#22C55E" />
                <stop offset="100%" stopColor="#1A6B3C" />
              </linearGradient>
              <linearGradient id="ctFin" x1="50" y1="95" x2="135" y2="150" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#1A6B3C" />
                <stop offset="100%" stopColor="#14532D" />
              </linearGradient>
              <linearGradient id="ctFlame" x1="88" y1="145" x2="80" y2="188" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#FDE047" />
                <stop offset="55%" stopColor="#F59E0B" />
                <stop offset="100%" stopColor="#EA580C" />
              </linearGradient>
              <linearGradient id="ctCloud" x1="30" y1="145" x2="110" y2="205" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#FFFFFF" />
                <stop offset="100%" stopColor="#E2E8F0" />
              </linearGradient>
            </defs>

            {/* Nuage de décollage */}
            <g>
              <ellipse cx="62" cy="182" rx="34" ry="18" fill="url(#ctCloud)" />
              <circle cx="48" cy="172" r="18" fill="url(#ctCloud)" />
              <circle cx="72" cy="166" r="22" fill="url(#ctCloud)" />
              <circle cx="92" cy="177" r="15" fill="url(#ctCloud)" />
            </g>

            {/* Fusée inclinée */}
            <g transform="rotate(11 98 100)">
              {/* Flamme */}
              <path
                d="M84 142 C82 162 94 178 98 184 C102 178 114 162 112 142 Z"
                fill="url(#ctFlame)"
              />
              {/* Aileron gauche */}
              <path d="M72 106 L52 138 C50 142 54 146 58 144 L78 134 Z" fill="url(#ctFin)" />
              {/* Aileron droit */}
              <path d="M124 106 L144 138 C146 142 142 146 138 144 L118 134 Z" fill="url(#ctFin)" />
              {/* Corps */}
              <path
                d="M98 18 C122 44 128 84 122 134 C121 140 116 144 110 144 L86 144 C80 144 75 140 74 134 C68 84 74 44 98 18 Z"
                fill="url(#ctBody)"
              />
              {/* Coiffe verte */}
              <path d="M98 18 C109 30 116 42 119 52 L77 52 C80 42 87 30 98 18 Z" fill="url(#ctNose)" />
              {/* Hublot */}
              <circle cx="98" cy="84" r="16" fill="#0F172A" opacity="0.12" />
              <circle cx="98" cy="82" r="14" fill="#1A6B3C" />
              <circle cx="98" cy="82" r="9.5" fill="#38BDF8" />
              <circle cx="94" cy="78" r="3" fill="#FFFFFF" opacity="0.85" />
              {/* Aileron central */}
              <path d="M98 114 C94 124 94 138 98 150 C102 138 102 124 98 114 Z" fill="#F5A623" />
            </g>
          </svg>
        </div>
      </div>

      {/* Panneau droit blanc : Formulaire épuré */}
      <div className="flex-1 flex flex-col justify-center items-center px-6 py-10 sm:px-10 lg:px-16 bg-white">
        <div className="w-full max-w-[380px]">
          {/* Logo centré */}
          <div className="flex flex-col items-center mb-7">
            <div className="w-16 h-16 rounded-full bg-[#1A6B3C]/10 border border-[#1A6B3C]/20 p-2 shadow-sm flex items-center justify-center mb-4">
              <img
                src="/logo.png"
                alt="Logo CareerTrack Sénégal"
                className="w-full h-full object-contain rounded-full"
              />
            </div>
            <h2 className="text-2xl sm:text-[28px] font-extrabold tracking-tight text-[#131B2E]">
              Espace RH
            </h2>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-2xl flex items-center gap-2 text-xs text-red-700">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="relative">
              <User className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
              <input
                id="username"
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                aria-label="Identifiant ou nom d'utilisateur"
                className="w-full rounded-full bg-[#F1F5F9] text-slate-800 placeholder:text-slate-400 text-sm py-3.5 pl-11 pr-5 border border-transparent focus:border-[#1A6B3C] focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#1A6B3C]/20 transition-all"
                placeholder="Identifiant ou nom d'utilisateur"
              />
            </div>

            <div className="relative">
              <Lock className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
              <input
                id="password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                aria-label="Mot de passe"
                className="w-full rounded-full bg-[#F1F5F9] text-slate-800 placeholder:text-slate-400 text-sm py-3.5 pl-11 pr-5 border border-transparent focus:border-[#1A6B3C] focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#1A6B3C]/20 transition-all"
                placeholder="Mot de passe"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-full py-3.5 text-sm font-bold text-white bg-gradient-to-r from-[#1A6B3C] to-[#14532D] hover:from-[#15803D] hover:to-[#166534] shadow-lg shadow-[#1A6B3C]/30 active:scale-[0.99] transition-all cursor-pointer disabled:opacity-60 mt-2"
            >
              {isSubmitting ? 'Connexion en cours...' : 'Connexion'}
            </button>
          </form>

          {/* Bouton discret pour afficher / masquer les comptes de démonstration */}
          <div className="mt-5">
            <button
              type="button"
              onClick={() => setShowDemoAccounts((prev) => !prev)}
              aria-expanded={showDemoAccounts}
              className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-full text-xs font-semibold text-[#1A6B3C] bg-[#1A6B3C]/5 hover:bg-[#1A6B3C]/10 border border-[#1A6B3C]/20 transition-colors cursor-pointer"
            >
              <Users className="w-3.5 h-3.5 flex-shrink-0" />
              <span>
                {showDemoAccounts
                  ? 'Masquer les comptes de test'
                  : 'Comptes de démonstration (Test)'}
              </span>
              {showDemoAccounts ? (
                <ChevronUp className="w-3.5 h-3.5 flex-shrink-0" />
              ) : (
                <ChevronDown className="w-3.5 h-3.5 flex-shrink-0" />
              )}
            </button>

            {showDemoAccounts && (
              <div className="mt-3 space-y-2">
                <p className="text-[11px] text-slate-500 text-center">
                  Cliquez sur un profil pour remplir automatiquement :
                </p>
                <div className="grid grid-cols-2 gap-2">
                  {DEMO_ACCOUNTS.map((acc) => {
                    const isSelected = username === acc.username;
                    return (
                      <button
                        key={acc.username}
                        type="button"
                        onClick={() => {
                          setUsername(acc.username);
                          setPassword(acc.password);
                          setError(null);
                        }}
                        className={`text-left p-2.5 rounded-xl border transition-all cursor-pointer min-w-0 ${
                          isSelected
                            ? 'border-[#1A6B3C] bg-[#1A6B3C]/10 ring-1 ring-[#1A6B3C]/30'
                            : 'border-slate-200 bg-slate-50/80 hover:border-[#1A6B3C]/50 hover:bg-white'
                        }`}
                      >
                        <div className="text-xs font-bold text-slate-800 truncate">
                          {acc.role}
                        </div>
                        <div className="text-[11px] font-mono text-[#1A6B3C] truncate">
                          {acc.username}
                        </div>
                        <div className="text-[10px] text-slate-400 truncate">
                          {acc.hint}
                        </div>
                      </button>
                    );
                  })}
                </div>
                <p className="text-[10px] text-slate-400 text-center">
                  Mot de passe commun : <span className="font-mono text-slate-600">password123</span>
                </p>
              </div>
            )}
          </div>

          <p className="mt-7 text-center text-[11px] text-slate-400">
            Système RH conforme au Code du Travail de la République du Sénégal
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
