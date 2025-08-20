import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { CustomerRequest, CustomerResponse, ErrorResponse } from '../entities/customer';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class CustomerService {
  readonly #http = inject(HttpClient);
  
  // URL base del backend - debe configurarse en environment
  private readonly baseUrl = environment.apiUrl || 'http://localhost:8080/v1';

  /**
   * Crea un nuevo cliente
   * @param customer Datos del cliente a crear
   * @returns Observable con la respuesta del servidor incluyendo el ID del cliente creado
   */
  createCustomer(customer: CustomerRequest): Observable<{ customer: CustomerResponse; token: string }> {
    return this.#http.post<CustomerResponse>(
      `${this.baseUrl}/customers`,
      customer,
      {
        observe: 'response',
      }
    ).pipe(
      map(response => {
        const customerData = response.body!;
        const token = response.headers.get('X-Auth-Token') || '';
        
        // Guardar el token en localStorage para usar en futuras peticiones
        if (token) {
          localStorage.setItem('authToken', token);
          localStorage.setItem('customerId', customerData.id);
        }
        
        return {
          customer: customerData,
          token,
        };
      }),
      catchError(error => {
        console.error('Error creando cliente:', error);
        
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
          path: '/customers',
        } as ErrorResponse));
      })
    );
  }

  /**
   * Obtiene la información de un cliente por su ID
   * @param customerId ID del cliente
   * @returns Observable con los datos del cliente
   */
  getCustomer(customerId: string): Observable<CustomerResponse> {
    return this.#http.get<CustomerResponse>(
      `${this.baseUrl}/customers/${customerId}`
    ).pipe(
      catchError(error => {
        console.error('Error obteniendo cliente:', error);
        
        if (error.error && error.error.code) {
          return throwError(() => error.error as ErrorResponse);
        }
        
        return throwError(() => ({
          code: 'CLIENT_ERROR',
          error: 'NETWORK_ERROR',
          timestamp: Date.now(),
          message: 'Error obteniendo información del cliente.',
          path: `/customers/${customerId}`,
        } as ErrorResponse));
      })
    );
  }
}
