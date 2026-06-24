import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login/login').then(m => m.Login) },
  { path: 'register', loadComponent: () => import('./auth/register/register').then(m => m.Register) },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];
