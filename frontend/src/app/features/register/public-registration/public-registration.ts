import { Component, inject, input, signal } from '@angular/core';
import { email as emailValidator, form, FormField, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { PublicApi, PublicRegistrationResult } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { detectApiLocale } from '../../../core/i18n/api-locale';
import { PageHeader } from '../../../shared/components/page-header/page-header';

interface RegisterFormModel {
  firstName: string;
  lastName: string;
  email: string;
}

/**
 * `/register?token=` — a scanned QR token, unauthenticated. `POST /public/registrations`
 * always creates the account. The landing text is chosen here from `result.joined`; the
 * server's own `result.message` is English-only and is deliberately not shown.
 */
@Component({
  selector: 'app-public-registration',
  imports: [
    FormField,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    PageHeader,
    TranslocoPipe
  ],
  templateUrl: './public-registration.html',
  styleUrl: './public-registration.scss'
})
export class PublicRegistration {
  private readonly transloco = inject(TranslocoService);
  private readonly publicApi = inject(PublicApi);
  private readonly router = inject(Router);

  readonly token = input<string>();

  protected readonly model = signal<RegisterFormModel>({ firstName: '', lastName: '', email: '' });
  protected readonly registerForm = form(this.model, (path) => {
    required(path.firstName, { message: this.transloco.translate('validation.firstNameRequired') });
    required(path.lastName, { message: this.transloco.translate('validation.lastNameRequired') });
    required(path.email, { message: this.transloco.translate('validation.emailRequired') });
    emailValidator(path.email, { message: this.transloco.translate('validation.emailInvalid') });
  });

  protected readonly submitting = signal(false);
  protected readonly formErrorMessage = signal<string | null>(null);
  protected readonly result = signal<PublicRegistrationResult | null>(null);

  protected onSubmit(): void {
    this.registerForm().markAsTouched();
    if (this.registerForm().invalid() || this.submitting()) {
      return;
    }

    const token = this.token();
    if (!token) {
      this.showLinkExpired();
      return;
    }

    this.submitting.set(true);
    this.formErrorMessage.set(null);

    const { firstName, lastName, email } = this.model();
    this.publicApi.registerPublicly({ token, firstName, lastName, email, locale: detectApiLocale() }).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.result.set(response.data);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (error.code === 'INVALID_OR_EXPIRED_LINK') {
          this.showLinkExpired();
        } else {
          this.formErrorMessage.set(error.message);
        }
      }
    });
  }

  private showLinkExpired(): void {
    void this.router.navigate(['/link-expired'], { queryParams: { kind: 'qr' }, replaceUrl: true });
  }
}
