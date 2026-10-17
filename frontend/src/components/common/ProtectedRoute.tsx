import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactElement;
  roles?: string[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, roles }) => {
  const { user, isAuthenticated, isLoading, hasRole } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="flex flex-col items-center gap-3">
          <div className="spinner spinner-lg" />
          <span className="text-xs text-gray-500 font-medium">Chargement de CareerTrack...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && roles.length > 0) {
    const hasRequiredRole = roles.some((role) => hasRole(role));
    if (!hasRequiredRole) {
      return (
        <div className="p-8 max-w-xl mx-auto my-12 bg-white rounded-xl border border-red-200 shadow-sm text-center">
          <h2 className="text-lg font-bold text-red-700 mb-2">Accès restreint</h2>
          <p className="text-sm text-gray-600 mb-4">
            Votre profil actuel n'a pas les permissions requises pour consulter cette ressource.
          </p>
          <a href="/dashboard" className="btn btn-primary inline-flex text-xs">
            Retour au tableau de bord
          </a>
        </div>
      );
    }
  }

  return children;
};
export default ProtectedRoute;
