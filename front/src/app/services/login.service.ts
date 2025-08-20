import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, map, Observable, take, tap, throwError } from 'rxjs';
import { ROUTE_CONFIG } from '../config/routes.config';
import { Credentials } from '../entities/credentials';

interface UserData {
  username: string;
  token: string;
  role?: string;
}

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  private readonly TOKEN_KEY = 'authToken';
  private readonly USER_KEY = 'user_data';
  
  readonly #router = inject(Router);
  readonly #http = inject(HttpClient);
  
  // Observable para el estado de autenticación
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  
  // Signal para el usuario actual
  private currentUserSignal = signal<UserData | null>(this.getUserFromStorage());
  public currentUser = this.currentUserSignal.asReadonly();

  #retrieveData(): Observable<{
    [key: string]: { password: string; token: string };
  }> {
    return this.#http.get<{
      [key: string]: { password: string; token: string };
    }>('/assets/auth.db.json');
  }

  execute(credentials: Credentials): Observable<string> {
    if (!credentials.username || !credentials.password) {
      return throwError(() => new Error('Por favor, ingrese usuario y contraseña'));
    }

    return this.#retrieveData().pipe(
      map((data) => {
        const user = data[credentials.username];

        if (!user) {
          throw new Error('Usuario o contraseña incorrectos');
        }

        if (user.password !== credentials.password) {
          throw new Error('Usuario o contraseña incorrectos');
        }

        return user.token;
      }),
      tap((token) => {
        // Guardar el token y los datos del usuario
        this.setSession(token, credentials.username);
        
        // Navegar a la página principal
        this.#router.navigate([ROUTE_CONFIG.app, ROUTE_CONFIG.home]);
      }),
      catchError((error) => {
        console.error('Error durante el login:', error);
        return throwError(() => error);
      }),
      take(1)
    );
  }

  logout(): void {
    // Limpiar el almacenamiento
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    
    // Actualizar el estado
    this.isAuthenticatedSubject.next(false);
    this.currentUserSignal.set(null);
    
    // Navegar al login
    this.#router.navigate([ROUTE_CONFIG.login]);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY) || sessionStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return this.hasValidToken();
  }

  private setSession(token: string, username: string): void {
    const userData: UserData = {
      username,
      token,
      // Podríamos decodificar el JWT para obtener más información
      // role: this.decodeToken(token).role
    };
    
    // Guardar en localStorage para persistencia
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(userData));
    
    // Actualizar el estado
    this.isAuthenticatedSubject.next(true);
    this.currentUserSignal.set(userData);
  }

  private hasValidToken(): boolean {
    const token = this.getToken();
    if (!token) return false;
    
    // Aquí podrías agregar validación adicional del token
    // Por ejemplo, verificar si no ha expirado
    return true;
  }

  private getUserFromStorage(): UserData | null {
    const userStr = localStorage.getItem(this.USER_KEY) || sessionStorage.getItem(this.USER_KEY);
    if (!userStr) return null;
    
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }
}
