import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  Lock,
  User,
  AlertCircle,
  Users,
  X,
  ShieldCheck,
  Briefcase,
  UserCheck,
  Eye,
} from 'lucide-react';

const DEMO_ACCOUNTS = [
  {
    role: 'DRH (Administration)',
    username: 'drh.ndiaye',
    password: 'password123',
    icon: ShieldCheck,
    iconBox: 'bg-emerald-50 text-emerald-600 border-emerald-200/80',
  },
  {
    role: 'Manager N+1',
    username: 'mgr.sow',
    password: 'password123',
    icon: Briefcase,
    iconBox: 'bg-sky-50 text-sky-600 border-sky-200/80',
  },
  {
    role: 'Collaborateur',
    username: 'emp.diallo',
    password: 'password123',
    icon: UserCheck,
    iconBox: 'bg-amber-50 text-amber-600 border-amber-200/80',
  },
  {
    role: 'Direction (Lecture seule)',
    username: 'dir.fall',
    password: 'password123',
    icon: Eye,
    iconBox: 'bg-indigo-50 text-indigo-600 border-indigo-200/80',
  },
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

  useEffect(() => {
    if (!showDemoAccounts) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setShowDemoAccounts(false);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [showDemoAccounts]);

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
      {/* Panneau gauche sombre avec vague organique */}
      <div className="relative bg-[#131B2E] text-white lg:w-[48%] lg:min-h-screen flex flex-col justify-center px-8 py-10 lg:px-14 xl:px-20 flex-shrink-0">
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

        {/* Illustration fusée stylisée 3D flottant sur la courbe (Desktop) */}
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

            <g>
              <ellipse cx="62" cy="182" rx="34" ry="18" fill="url(#ctCloud)" />
              <circle cx="48" cy="172" r="18" fill="url(#ctCloud)" />
              <circle cx="72" cy="166" r="22" fill="url(#ctCloud)" />
              <circle cx="92" cy="177" r="15" fill="url(#ctCloud)" />
            </g>

            <g transform="rotate(11 98 100)">
              <path
                d="M84 142 C82 162 94 178 98 184 C102 178 114 162 112 142 Z"
                fill="url(#ctFlame)"
              />
              <path d="M72 106 L52 138 C50 142 54 146 58 144 L78 134 Z" fill="url(#ctFin)" />
              <path d="M124 106 L144 138 C146 142 142 146 138 144 L118 134 Z" fill="url(#ctFin)" />
              <path
                d="M98 18 C122 44 128 84 122 134 C121 140 116 144 110 144 L86 144 C80 144 75 140 74 134 C68 84 74 44 98 18 Z"
                fill="url(#ctBody)"
              />
              <path d="M98 18 C109 30 116 42 119 52 L77 52 C80 42 87 30 98 18 Z" fill="url(#ctNose)" />
              <circle cx="98" cy="84" r="16" fill="#0F172A" opacity="0.12" />
              <circle cx="98" cy="82" r="14" fill="#1A6B3C" />
              <circle cx="98" cy="82" r="9.5" fill="#38BDF8" />
              <circle cx="94" cy="78" r="3" fill="#FFFFFF" opacity="0.85" />
              <path d="M98 114 C94 124 94 138 98 150 C102 138 102 124 98 114 Z" fill="#F5A623" />
            </g>
          </svg>
        </div>
      </div>

      {/* Panneau droit blanc : Formulaire épuré */}
      <div className="flex-1 flex flex-col justify-center items-center px-6 py-10 sm:px-10 lg:px-16 bg-white">
        <div className="w-full max-w-[380px]">
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

          {/* Bouton discret ouvrant la modale flottante des comptes de test */}
          <div className="mt-4 flex justify-center">
            <button
              type="button"
              onClick={() => setShowDemoAccounts(true)}
              className="inline-flex items-center justify-center gap-2 py-2 px-4 rounded-full text-xs font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200/80 hover:text-[#1A6B3C] transition-colors cursor-pointer"
            >
              <Users className="w-3.5 h-3.5 flex-shrink-0" />
              <span>Comptes de test</span>
            </button>
          </div>

          <p className="mt-7 text-center text-[11px] text-slate-400">
            Système RH conforme au Code du Travail de la République du Sénégal
          </p>
        </div>
      </div>

      {/* Modale popup centrée avec arrière-plan flouté (ne déforme jamais la page) */}
      {showDemoAccounts && (
        <div
          className="fixed inset-0 z-50 bg-slate-900/45 backdrop-blur-xs flex items-center justify-center p-4"
          onClick={() => setShowDemoAccounts(false)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="demo-accounts-title"
        >
          <div
            className="w-full max-w-[400px] bg-white rounded-2xl shadow-2xl border border-slate-100 p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 mb-4">
              <div>
                <h3 id="demo-accounts-title" className="text-base font-bold text-[#131B2E]">
                  Comptes de test
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Sélectionnez un profil pour préremplir les identifiants.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowDemoAccounts(false)}
                aria-label="Fermer"
                className="p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2.5">
              {DEMO_ACCOUNTS.map((acc) => {
                const Icon = acc.icon;
                return (
                  <button
                    key={acc.username}
                    type="button"
                    onClick={() => {
                      setUsername(acc.username);
                      setPassword(acc.password);
                      setError(null);
                      setShowDemoAccounts(false);
                    }}
                    className="w-full flex items-center justify-between gap-3 px-3.5 py-2.5 rounded-xl border border-slate-200 bg-white hover:border-[#1A6B3C]/50 hover:bg-slate-50/70 transition-all cursor-pointer text-left"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div
                        className={`w-9 h-9 rounded-xl border flex items-center justify-center flex-shrink-0 ${acc.iconBox}`}
                      >
                        <Icon className="w-4 h-4" />
                      </div>
                      <div className="min-w-0">
                        <div className="text-[13px] font-semibold text-slate-800 truncate">
                          {acc.role}
                        </div>
                        <div className="text-[11px] font-mono text-slate-500 truncate">
                          {acc.username}
                        </div>
                      </div>
                    </div>
                    <span className="text-xs font-semibold text-[#1A6B3C] flex-shrink-0">
                      Préremplir
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default LoginPage;
