import { Component, inject, signal } from '@angular/core';
import { email as emailValidator, form, FormField, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';

import { Auth } from '../../../core/auth/auth';
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
  imports: [FormField, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, PageHeader],
  templateUrl: './organizer-login.html',
  styleUrl: './organizer-login.scss'
})
export class OrganizerLogin {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly notification = inject(Notification);

  protected readonly model = signal<LoginFormModel>({ email: '', password: '' });
  protected readonly loginForm = form(this.model, (path) => {
    required(path.email, { message: 'Email is required.' });
    emailValidator(path.email, { message: 'Enter a valid email address.' });
    required(path.password, { message: 'Password is required.' });
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
          this.loginError.set('Incorrect email or password.');
        } else {
          this.notification.error(error.message);
        }
      }
    });
  }
}
