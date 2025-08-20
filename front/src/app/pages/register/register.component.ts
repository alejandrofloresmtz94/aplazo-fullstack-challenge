import { Component, inject } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AplazoButtonComponent } from '@apz/shared-ui/button';
import { AplazoLogoComponent } from '@apz/shared-ui/logo';
import { ROUTE_CONFIG } from '../../config/routes.config';
import { CustomerService } from '../../services/customer.service';
import { CustomerRequest, ErrorResponse } from '../../entities/customer';

// Validador personalizado para verificar edad entre 18 y 65 años según OpenAPI
function ageRangeValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  
  const today = new Date();
  const birthDate = new Date(control.value);
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  
  if (age < 18) {
    return { underage: true };
  }
  
  if (age > 65) {
    return { overage: true };
  }
  
  return null;
}

@Component({
  standalone: true,
  selector: 'app-register',
  templateUrl: './register.component.html',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, AplazoButtonComponent, AplazoLogoComponent],
})
export class RegisterComponent {
  readonly #router = inject(Router);
  readonly #customerService = inject(CustomerService);
  
  showErrors = false;
  isSubmitting = false;
  errorMessage = '';
  
  readonly firstName = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(2)],
  });

  readonly lastName = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(2)],
  });

  readonly secondLastName = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(2)],
  });

  readonly dateOfBirth = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, ageRangeValidator],
  });

  readonly form = new FormGroup({
    firstName: this.firstName,
    lastName: this.lastName,
    secondLastName: this.secondLastName,
    dateOfBirth: this.dateOfBirth,
  });

  getErrorMessage(controlName: string): string {
    const control = this.form.get(controlName);
    if (!control || !control.errors || !control.touched) return '';
    
    const errors = control.errors;
    if (errors['required']) return 'Este campo es requerido';
    if (errors['minlength']) return `Mínimo ${errors['minlength'].requiredLength} caracteres`;
    if (errors['underage']) return 'Debes tener al menos 18 años';
    if (errors['overage']) return 'La edad máxima permitida es 65 años';
    
    return 'Campo inválido';
  }

  register(): void {
    this.showErrors = true;
    this.errorMessage = '';
    
    if (this.form.invalid) {
      // Marcar todos los campos como tocados para mostrar errores
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
      return;
    }
    
    this.isSubmitting = true;
    
    const customerData: CustomerRequest = {
      firstName: this.form.value.firstName!,
      lastName: this.form.value.lastName!,
      secondLastName: this.form.value.secondLastName!,
      dateOfBirth: this.form.value.dateOfBirth!,
    };
    
    this.#customerService.createCustomer(customerData).subscribe({
      next: (response) => {
        console.log('Cliente creado exitosamente:', response);
        // Redirigir a la página de creación de préstamos
        this.#router.navigate([ROUTE_CONFIG.app, ROUTE_CONFIG.home]);
      },
      error: (error: ErrorResponse) => {
        console.error('Error al crear cliente:', error);
        this.isSubmitting = false;
        
        // Mostrar mensaje de error específico según el código
        if (error.code === 'APZ000002') {
          this.errorMessage = 'Datos inválidos. Por favor verifica la información.';
        } else if (error.code === 'APZ000003') {
          this.errorMessage = 'Demasiados intentos. Por favor intenta más tarde.';
        } else {
          this.errorMessage = error.message || 'Error al crear la cuenta. Por favor intenta nuevamente.';
        }
      }
    });
  }
}
