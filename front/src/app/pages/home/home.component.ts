import { Component, inject, OnInit } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AplazoButtonComponent } from '@apz/shared-ui/button';
import { LoanService } from '../../services/loan.service';
import { CustomerService } from '../../services/customer.service';
import { LoanRequest, LoanResponse } from '../../entities/loan';
import { CustomerResponse, ErrorResponse } from '../../entities/customer';

// Validador personalizado para UUID
function uuidValidator(
  control: AbstractControl
): { [key: string]: boolean } | null {
  if (!control.value) return null;

  const uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  if (!uuidRegex.test(control.value)) {
    return { invalidUuid: true };
  }

  return null;
}

@Component({
  standalone: true,
  selector: 'app-home',
  templateUrl: './home.component.html',
  imports: [CommonModule, ReactiveFormsModule, AplazoButtonComponent],
})
export class HomeComponent implements OnInit {
  readonly #loanService = inject(LoanService);
  readonly #customerService = inject(CustomerService);

  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  customerInfo: CustomerResponse | null = null;
  loanCreated: LoanResponse | null = null;

  readonly customerId = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, uuidValidator],
  });

  readonly amount = new FormControl<number | null>(null, {
    validators: [Validators.required, Validators.min(0.01)],
  });

  readonly loanForm = new FormGroup({
    customerId: this.customerId,
    amount: this.amount,
  });

  ngOnInit(): void {
    // Intentar obtener el customerId guardado
    const savedCustomerId = localStorage.getItem('customerId');
    if (savedCustomerId) {
      this.customerId.setValue(savedCustomerId);
      this.loadCustomerInfo(savedCustomerId);
    }
  }

  getErrorMessage(controlName: string): string {
    const control = this.loanForm.get(controlName);
    if (!control?.errors || !control.touched) return '';

    const errors = control.errors;
    if (errors['required']) return 'Este campo es requerido';
    if (errors['invalidUuid']) return 'El ID debe ser un UUID válido';
    if (errors['min']) return 'El monto debe ser mayor a 0';
    if (errors['max'])
      return (
        'El monto máximo es ' +
        (this.customerInfo?.availableCreditLineAmount || 1)
      );

    return 'Campo inválido';
  }

  loadCustomerInfo(customerId: string): void {
    this.#customerService.getCustomer(customerId).subscribe({
      next: (customer) => {
        this.customerInfo = customer;
        this.amount.addValidators(
          Validators.max(customer?.availableCreditLineAmount || 1000)
        );
      },
      error: (error) => {
        console.error('Error cargando información del cliente:', error);
      },
    });
  }

  createLoan(): void {
    if (this.loanForm.invalid) {
      Object.keys(this.loanForm.controls).forEach((key) => {
        this.loanForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const loanData: LoanRequest = {
      customerId: this.loanForm.value.customerId!,
      amount: this.loanForm.value.amount!,
    };

    this.#loanService.createLoan(loanData).subscribe({
      next: (response) => {
        console.log('Préstamo creado exitosamente:', response);
        this.isSubmitting = false;
        this.loanCreated = response;
        this.successMessage = '¡Préstamo creado exitosamente!';

        // Limpiar el formulario pero mantener el customerId
        this.amount.reset();
        this.amount.markAsUntouched();

        // Actualizar información del cliente
        if (this.customerInfo) {
          this.loadCustomerInfo(this.customerInfo.id);
        }
      },
      error: (error: ErrorResponse) => {
        console.error('Error al crear préstamo:', error);
        this.isSubmitting = false;

        // Mostrar mensaje de error específico según el código
        if (error.code === 'APZ000006') {
          this.errorMessage =
            'Solicitud de préstamo inválida. Verifica los datos.';
        } else if (error.code === 'APZ000007') {
          this.errorMessage =
            'No autorizado. Por favor inicia sesión nuevamente.';
        } else {
          this.errorMessage =
            error.message ||
            'Error al crear el préstamo. Por favor intenta nuevamente.';
        }
      },
    });
  }
}
