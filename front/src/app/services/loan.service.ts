import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { LoanRequest, LoanResponse } from '../entities/loan';
import { ErrorResponse } from '../entities/customer';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class LoanService {
  readonly #http = inject(HttpClient);
  
  private readonly baseUrl = environment.apiUrl || 'http://localhost:8080/v1';

  /**
   * Crea un nuevo préstamo
   * @param loan Datos del préstamo a crear
   * @returns Observable con la respuesta del servidor incluyendo el ID del préstamo creado
   */
  createLoan(loan: LoanRequest): Observable<LoanResponse> {
    return this.#http.post<LoanResponse>(
      `${this.baseUrl}/loans`,
      loan
    ).pipe(
      catchError(error => {
        console.error('Error creando préstamo:', error);
        
        if (error.error && error.error.code) {
          // Error estructurado del backend
          return throwError(() => error.error as ErrorResponse);
        }
        
        // Error genérico
        return throwError(() => ({
          code: 'CLIENT_ERROR',
          error: 'NETWORK_ERROR',
          timestamp: Date.now(),
          message: 'Error de conexión. Por favor intente nuevamente.',
          path: '/loans',
        } as ErrorResponse));
      })
    );
  }

  /**
   * Obtiene la información de un préstamo por su ID
   * @param loanId ID del préstamo
   * @returns Observable con los datos del préstamo
   */
  getLoan(loanId: string): Observable<LoanResponse> {
    return this.#http.get<LoanResponse>(
      `${this.baseUrl}/loans/${loanId}`
    ).pipe(
      catchError(error => {
        console.error('Error obteniendo préstamo:', error);
        
        if (error.error && error.error.code) {
          return throwError(() => error.error as ErrorResponse);
        }
        
        return throwError(() => ({
          code: 'CLIENT_ERROR',
          error: 'NETWORK_ERROR',
          timestamp: Date.now(),
          message: 'Error obteniendo información del préstamo.',
          path: `/loans/${loanId}`,
        } as ErrorResponse));
      })
    );
  }
}
