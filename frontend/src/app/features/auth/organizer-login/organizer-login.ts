import { Component, inject, signal } from '@angular/core';
import { email as emailValidator, form, FormField, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { Auth } from '../../../core/auth/auth';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { PageHeader } from '../../../shared/components/page-header/page-header';

interface LoginFormModel {
  email: string;
  password: string;
}

/**
 * Cooks and guests never see this screen — they authenticate through an
 * emailed access link (`/home?token=`), see the frontend plan's Phase 5/6.
 */
@Component({
  selector: 'app-organizer-login',
  imports: [
    FormField,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    PageHeader,
    TranslocoPipe
  ],
  templateUrl: './organizer-login.html',
  styleUrl: './organizer-login.scss'
})
export class OrganizerLogin {
  private readonly transloco = inject(TranslocoService);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly notification = inject(Notification);

  /** Which build is running. Shown here so it can be read without logging in first. */
  protected readonly version = inject(AppConfig).version;

  protected readonly model = signal<LoginFormModel>({ email: '', password: '' });
  protected readonly loginForm = form(this.model, (path) => {
    required(path.email, { message: this.transloco.translate('validation.emailRequired') });
    emailValidator(path.email, { message: this.transloco.translate('validation.emailInvalid') });
    required(path.password, { message: this.transloco.translate('validation.passwordRequired') });
  });

  protected readonly submitting = signal(false);
  protected readonly loginError = signal<string | null>(null);

  protected onSubmit(): void {
    this.loginForm().markAsTouched();
    if (this.loginForm().invalid() || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.loginError.set(null);

    this.auth.login(this.model()).subscribe({
      next: () => void this.router.navigateByUrl('/challenges'),
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (error.code === 'INVALID_CREDENTIALS') {
          this.loginError.set(this.transloco.translate('login.incorrectCredentials'));
        } else {
          this.notification.error(error.message);
        }
      }
    });
  }
}
