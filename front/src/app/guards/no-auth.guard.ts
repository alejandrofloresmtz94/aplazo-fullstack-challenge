import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ROUTE_CONFIG } from '../config/routes.config';
import { LoginService } from '../services/login.service';

/**
 * Guard para proteger rutas que NO deben ser accesibles por usuarios autenticados
 * Redirige al home si hay token de autenticación
 */
export const noAuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const loginService = inject(LoginService);
  
  if (loginService.isLoggedIn()) {
    // Usuario autenticado, redirigir al home
    router.navigate([`/${ROUTE_CONFIG.app}`, ROUTE_CONFIG.home]);
    return false;
  } else {
    // Usuario no autenticado, permitir acceso
    return true;
  }
};
