import { Component, inject } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AplazoButtonComponent } from '@apz/shared-ui/button';
import { AplazoLogoComponent } from '@apz/shared-ui/logo';
import { LoginService } from '../../services/login.service';

@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    AplazoButtonComponent,
    AplazoLogoComponent,
  ],
})
export class LoginComponent {
  readonly #loginService = inject(LoginService);

  isSubmitting = false;
  errorMessage = '';

  readonly username = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.email],
  });

  readonly password = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(6)],
  });

  readonly form = new FormGroup({
    username: this.username,
    password: this.password,
  });

  getErrorMessage(controlName: string): string {
    const control = this.form.get(controlName);
    if (!control?.errors || !control.touched) return '';

    const errors = control.errors;
    if (errors['required']) return 'Este campo es requerido';
    if (errors['email']) return 'Ingresa un correo electrónico válido';
    if (errors['minlength'])
      return `Mínimo ${errors['minlength'].requiredLength} caracteres`;

    return 'Campo inválido';
  }

  login(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach((key) => {
        this.form.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    this.#loginService
      .execute({
        username: this.username.value,
        password: this.password.value,
      })
      .subscribe({
        next: (token) => {
          // El servicio ya maneja la redirección
          this.isSubmitting = false;
        },
        error: (error) => {
          this.isSubmitting = false;
          this.errorMessage =
            error?.message ||
            'Credenciales inválidas. Por favor verifica tus datos.';
        },
      });
  }
}
