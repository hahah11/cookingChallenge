import { Component, inject, input, signal } from '@angular/core';
import { email as emailValidator, form, FormField, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PublicApi, PublicRegistrationResult } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

interface RegisterFormModel {
  firstName: string;
  lastName: string;
  email: string;
}

/**
 * `/register?token=` — a scanned QR token, unauthenticated. `POST /public/registrations`
 * always creates the account; `result.message` is server-rendered display text, rendered
 * verbatim for both `joined` outcomes, see the frontend plan's Phase 6.
 */
@Component({
  selector: 'app-public-registration',
  imports: [
    ErrorState,
    FormField,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    PageHeader
  ],
  templateUrl: './public-registration.html',
  styleUrl: './public-registration.scss'
})
export class PublicRegistration {
  private readonly publicApi = inject(PublicApi);

  readonly token = input<string>();

  protected readonly model = signal<RegisterFormModel>({ firstName: '', lastName: '', email: '' });
  protected readonly registerForm = form(this.model, (path) => {
    required(path.firstName, { message: 'First name is required.' });
    required(path.lastName, { message: 'Last name is required.' });
    required(path.email, { message: 'Email is required.' });
    emailValidator(path.email, { message: 'Enter a valid email address.' });
  });

  protected readonly submitting = signal(false);
  protected readonly formErrorMessage = signal<string | null>(null);
  protected readonly linkExpired = signal(false);
  protected readonly result = signal<PublicRegistrationResult | null>(null);

  protected onSubmit(): void {
    this.registerForm().markAsTouched();
    if (this.registerForm().invalid() || this.submitting()) {
      return;
    }

    const token = this.token();
    if (!token) {
      this.linkExpired.set(true);
      return;
    }

    this.submitting.set(true);
    this.formErrorMessage.set(null);

    const { firstName, lastName, email } = this.model();
    this.publicApi.registerPublicly({ token, firstName, lastName, email }).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.result.set(response.data);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (error.code === 'INVALID_OR_EXPIRED_LINK') {
          this.linkExpired.set(true);
        } else if (error.code === 'ACCOUNT_ALREADY_EXISTS') {
          this.formErrorMessage.set('An account with this email already exists.');
        } else {
          this.formErrorMessage.set(error.message);
        }
      }
    });
  }
}
