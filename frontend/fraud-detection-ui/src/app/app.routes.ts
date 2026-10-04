import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Dashboard } from './pages/dashboard/dashboard';
import { Customers } from './pages/customers/customers';
import { TransactionsComponent } from './pages/transactions/transactions';
import { FraudAlertComponent } from './pages/fraud-alert/fraud-alert';
import { AnalyticsComponent } from './pages/analytics/analytics';
import { LiveTracking } from './pages/live-tracking/live-tracking';
import { Settings } from './pages/settings/settings';
import { Profile } from './pages/profile/profile';
import { VerifyComponent } from './pages/verify/verify';

// User / Cardholder components
import { UserDashboard } from './pages/user/user-dashboard/user-dashboard';
import { UserCards } from './pages/user/user-cards/user-cards';
import { UserTransactions } from './pages/user/user-transactions/user-transactions';
import { UserAlerts } from './pages/user/user-alerts/user-alerts';
import { UserHistory } from './pages/user/user-history/user-history';

// Guards
import { AuthGuard } from './services/auth.guard';
import { RoleGuard } from './services/role.guard';

export const routes: Routes = [
  // Public Routes
  {
    path: '',
    component: Login
  },
  {
    path: 'register',
    component: Register
  },
  {
    path: 'verify/:id',
    component: VerifyComponent
  },
  {
    path: 'verify',
    component: VerifyComponent
  },

  // ADMIN-ONLY ROUTES (Protected by RoleGuard for ADMIN role)
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'transactions',
    component: TransactionsComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'fraud-alert',
    component: FraudAlertComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'customers',
    component: Customers,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'live-tracking',
    component: LiveTracking,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'analytics',
    component: AnalyticsComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'settings',
    component: Settings,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
  },

  // USER / CARDHOLDER ROUTES (Protected by RoleGuard for USER role)
  {
    path: 'user/dashboard',
    component: UserDashboard,
    canActivate: [RoleGuard],
    data: { roles: ['USER'] }
  },
  {
    path: 'user/cards',
    component: UserCards,
    canActivate: [RoleGuard],
    data: { roles: ['USER'] }
  },
  {
    path: 'user/transactions',
    component: UserTransactions,
    canActivate: [RoleGuard],
    data: { roles: ['USER'] }
  },
  {
    path: 'user/alerts',
    component: UserAlerts,
    canActivate: [RoleGuard],
    data: { roles: ['USER'] }
  },
  {
    path: 'user/history',
    component: UserHistory,
    canActivate: [RoleGuard],
    data: { roles: ['USER'] }
  },

  // Shared Authenticated Routes
  {
    path: 'profile',
    component: Profile,
    canActivate: [AuthGuard]
  },

  // Catch-all Fallback
  {
    path: '**',
    redirectTo: ''
  }
];