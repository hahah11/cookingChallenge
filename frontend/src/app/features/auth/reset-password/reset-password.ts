import { Component, OnInit, inject, input, signal } from '@angular/core';
import { form, FormField, minLength, required, validate } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';

import { AuthApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { PageHeader } from '../../../shared/components/page-header/page-header';

interface ResetPasswordFormModel {
  newPassword: string;
  confirmPassword: string;
}

const MIN_PASSWORD_LENGTH = 8;
const EXPIRED_URL = '/link-expired?kind=reset';

/**
 * `/reset-password?token=` — the link from an admin-initiated password-reset mail,
 * unauthenticated. Asking twice is purely a typo guard: only `newPassword` goes on the wire.
 *
 * A dead token routes to `/link-expired` rather than rendering inline, like the other mailed
 * personal link (`/home?token=`). `errorInterceptor` also reacts to that 401 by heading to the
 * generic `kind=link` page; navigating here afterwards supersedes it, so the reset copy wins.
 */
@Component({
  selector: 'app-reset-password',
  imports: [
    FormField,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    PageHeader
  ],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss'
})
export class ResetPassword implements OnInit {
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly notification = inject(Notification);

  readonly token = input<string>();

  protected readonly model = signal<ResetPasswordFormModel>({
    newPassword: '',
    confirmPassword: ''
  });
  protected readonly resetForm = form(this.model, (path) => {
    required(path.newPassword, { message: 'New password is required.' });
    minLength(path.newPassword, MIN_PASSWORD_LENGTH, {
      message: `Use at least ${MIN_PASSWORD_LENGTH} characters.`
    });
    required(path.confirmPassword, { message: 'Please confirm your new password.' });
    // On the confirm field, not the first one: that is where the user is looking when it fails.
    validate(path.confirmPassword, ({ value, valueOf }) =>
      value() && value() !== valueOf(path.newPassword)
        ? { kind: 'passwordMismatch', message: 'Passwords do not match.' }
        : undefined
    );
  });

  protected readonly submitting = signal(false);
  protected readonly formErrorMessage = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.token()) {
      void this.router.navigateByUrl(EXPIRED_URL);
    }
  }

  protected onSubmit(): void {
    this.resetForm().markAsTouched();
    const token = this.token();
    if (this.resetForm().invalid() || this.submitting() || !token) {
      return;
    }

    this.submitting.set(true);
    this.formErrorMessage.set(null);

    this.authApi.redeemPasswordReset({ token, newPassword: this.model().newPassword }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.notification.success('Password changed. Log in with your new password.');
        void this.router.navigateByUrl('/login');
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (error.code === 'INVALID_OR_EXPIRED_LINK') {
          void this.router.navigateByUrl(EXPIRED_URL);
        } else {
          this.formErrorMessage.set(error.message);
        }
      }
    });
  }
}
