import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import { AuthProvider, useAuth } from '@/context/AuthContext';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import AppLayout from '@/components/layout/AppLayout';

// Lazy loading des pages pour optimisation mobile, PWA et code splitting
const LoginPage = lazy(() => import('@/pages/auth/LoginPage'));
const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage'));
const EmployeeListPage = lazy(() => import('@/pages/employees/EmployeeListPage'));
const EmployeeDetailPage = lazy(() => import('@/pages/employees/EmployeeDetailPage'));
const LeaveListPage = lazy(() => import('@/pages/leaves/LeaveListPage'));
const PayrollListPage = lazy(() => import('@/pages/payroll/PayrollListPage'));
const RecruitmentKanbanPage = lazy(() => import('@/pages/recruitment/RecruitmentKanbanPage'));
const OrgChartPage = lazy(() => import('@/pages/orgchart/OrgChartPage'));
const EvaluationListPage = lazy(() => import('@/pages/evaluations/EvaluationListPage'));
const TrainingListPage = lazy(() => import('@/pages/trainings/TrainingListPage'));
const PromotionListPage = lazy(() => import('@/pages/promotions/PromotionListPage'));
const DocumentListPage = lazy(() => import('@/pages/documents/DocumentListPage'));

const PageLoader: React.FC = () => (
  <div className="flex flex-col items-center justify-center min-h-[50vh] gap-3">
    <div className="spinner spinner-lg" />
    <span className="text-xs text-gray-500 font-medium">Chargement du module...</span>
  </div>
);

const RootRedirect: React.FC = () => {
  const { hasRole } = useAuth();
  const canDashboard = hasRole('DRH') || hasRole('MANAGER') || hasRole('DIRECTION') || hasRole('ADMIN')
    || hasRole('ROLE_DRH') || hasRole('ROLE_MANAGER') || hasRole('ROLE_DIRECTION') || hasRole('ROLE_ADMIN');
  return <Navigate to={canDashboard ? '/dashboard' : '/leaves'} replace />;
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<PageLoader />}>
            <Routes>
              {/* Route Publique */}
              <Route path="/login" element={<LoginPage />} />

              {/* Routes Protégées sous le Layout Principal */}
              <Route
                element={
                  <ProtectedRoute>
                    <AppLayout />
                  </ProtectedRoute>
                }
              >
                <Route path="/" element={<RootRedirect />} />
                <Route
                  path="/dashboard"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_DIRECTION', 'ROLE_ADMIN']}>
                      <DashboardPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/employees"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER']}>
                      <EmployeeListPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/employees/:id"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE']}>
                      <EmployeeDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route path="/leaves" element={<LeaveListPage />} />
                <Route
                  path="/payroll"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE']}>
                      <PayrollListPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/recruitment"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER']}>
                      <RecruitmentKanbanPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/promotions"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER']}>
                      <PromotionListPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/documents"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE']}>
                      <DocumentListPage />
                    </ProtectedRoute>
                  }
                />
                <Route path="/orgchart" element={<OrgChartPage />} />
                <Route
                  path="/evaluations"
                  element={
                    <ProtectedRoute roles={['ROLE_DRH', 'ROLE_MANAGER', 'ROLE_EMPLOYE']}>
                      <EvaluationListPage />
                    </ProtectedRoute>
                  }
                />
                <Route path="/trainings" element={<TrainingListPage />} />
              </Route>

              {/* Fallback route */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
