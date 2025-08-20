import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ROUTE_CONFIG } from '../config/routes.config';
import { LoginService } from '../services/login.service';

/**
 * Guard para proteger rutas que requieren autenticación
 * Redirige al login si no hay token de autenticación
 */
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const loginService = inject(LoginService);
  
  if (loginService.isLoggedIn()) {
    // Usuario autenticado, permitir acceso
    return true;
  } else {
    // Usuario no autenticado, redirigir al login
    router.navigate([ROUTE_CONFIG.login]);
    return false;
  }
};
