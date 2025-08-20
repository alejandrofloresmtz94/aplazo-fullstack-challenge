import { Routes } from '@angular/router';
import { ROUTE_CONFIG } from './config/routes.config';
import { LayoutComponent } from './layout/layout.component';
import { HistorialComponent } from './pages/historial/historial.component';
import { HomeComponent } from './pages/home/home.component';
import { RegisterComponent } from './pages/register/register.component';
import { LoginComponent } from './pages/login/login.component';
import { authGuard } from './guards/auth.guard';
import { noAuthGuard } from './guards/no-auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: ROUTE_CONFIG.register,
  },
  {
    path: ROUTE_CONFIG.register,
    component: RegisterComponent,
    canActivate: [noAuthGuard], // Solo usuarios no autenticados
  },
  {
    path: ROUTE_CONFIG.login,
    component: LoginComponent,
    canActivate: [noAuthGuard], // Solo usuarios no autenticados
  },
  {
    path: ROUTE_CONFIG.app,
    component: LayoutComponent,
    canActivate: [authGuard], // Proteger toda la sección de la app
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: ROUTE_CONFIG.home,
      },
      {
        path: ROUTE_CONFIG.home,
        component: HomeComponent,
      },
      {
        path: ROUTE_CONFIG.historial,
        component: HistorialComponent,
      },
    ],
  },
  {
    path: '**',
    pathMatch: 'full',
    redirectTo: ROUTE_CONFIG.register,
  },
];
