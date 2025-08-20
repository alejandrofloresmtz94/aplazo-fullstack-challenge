import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoginService } from '../services/login.service';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const loginService = inject(LoginService);
  const router = inject(Router);
  
  // No agregar token a peticiones de assets o archivos estáticos
  if (req.url.includes('/assets/') || req.url.endsWith('.json')) {
    return next(req);
  }

  // Obtener el token del servicio de login
  const token = loginService.getToken();

  // Si hay token, agregarlo al header
  let authReq = req;
  if (token) {
    authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
  }

  // Procesar la petición y manejar errores
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Si recibimos un 401 Unauthorized
      if (error.status === 401) {
        console.warn('Token inválido o expirado. Cerrando sesión...');
        
        // Hacer logout
        loginService.logout();
        
        // Opcional: Mostrar mensaje al usuario
        // Podrías inyectar un servicio de notificaciones aquí
      }
      
      // Propagar el error para que lo maneje el componente
      return throwError(() => error);
    })
  );
};
