import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { expectNoAxeViolations } from '../../../testing/axe';
import { ResetPassword } from './reset-password';
import { provideTestI18n } from '../../../testing/i18n';

function apiError(code: string, status: number, message = 'Nope'): ApiError {
  return { code, message, details: [], requestId: '', timestamp: '2026-01-01T00:00:00Z', status };
}

describe('ResetPassword', () => {
  function setup(redeemPasswordReset: ReturnType<typeof vi.fn> = vi.fn(), token: string | null = 'reset-token') {
    const navigateByUrl = vi.fn().mockResolvedValue(true);
    const notification = { success: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ResetPassword],
      providers: [...provideTestI18n(), 
        { provide: AuthApi, useValue: { redeemPasswordReset } },
        { provide: Router, useValue: { navigateByUrl } },
        { provide: Notification, useValue: notification }
      ]
    });

    const fixture = TestBed.createComponent(ResetPassword);
    fixture.componentRef.setInput('token', token ?? undefined);
    fixture.detectChanges();
    return { fixture, navigateByUrl, notification };
  }

  function fillAndSubmit(
    fixture: ReturnType<typeof TestBed.createComponent<ResetPassword>>,
    newPassword: string,
    confirmPassword: string
  ) {
    fixture.componentInstance['model'].set({ newPassword, confirmPassword });
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function errorTexts(fixture: ReturnType<typeof TestBed.createComponent<ResetPassword>>): string[] {
    return Array.from(fixture.nativeElement.querySelectorAll('mat-error') as NodeListOf<HTMLElement>).map((e) =>
      e.textContent!.trim()
    );
  }

  it('shows a mismatch error under the confirm field and sends nothing', () => {
    const redeem = vi.fn();
    const { fixture } = setup(redeem);

    fillAndSubmit(fixture, 'long-enough-1', 'long-enough-2');

    const fields = fixture.nativeElement.querySelectorAll('mat-form-field');
    expect(fields[1].textContent).toContain('Passwords do not match.');
    expect(fields[0].textContent).not.toContain('Passwords do not match.');
    expect(redeem).not.toHaveBeenCalled();
  });

  it('shows a length error for a password under 8 characters and sends nothing', () => {
    const redeem = vi.fn();
    const { fixture } = setup(redeem);

    fillAndSubmit(fixture, 'short', 'short');

    expect(errorTexts(fixture)).toContain('Use at least 8 characters.');
    expect(redeem).not.toHaveBeenCalled();
  });

  it('posts only the token and new password, then returns to login', () => {
    const redeem = vi.fn().mockReturnValue(of(undefined));
    const { fixture, navigateByUrl, notification } = setup(redeem);

    fillAndSubmit(fixture, 'brand-new-password', 'brand-new-password');

    expect(redeem).toHaveBeenCalledWith({
      token: 'reset-token',
      newPassword: 'brand-new-password'
    });
    expect(notification.success).toHaveBeenCalled();
    expect(navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('routes to the reset-link expired page when the token is rejected', () => {
    const redeem = vi.fn().mockReturnValue(throwError(() => apiError('INVALID_OR_EXPIRED_LINK', 401)));
    const { fixture, navigateByUrl } = setup(redeem);

    fillAndSubmit(fixture, 'brand-new-password', 'brand-new-password');

    expect(navigateByUrl).toHaveBeenCalledWith('/link-expired?kind=reset');
  });

  it('shows any other failure inline and stays on the page', () => {
    const redeem = vi.fn().mockReturnValue(throwError(() => apiError('UNKNOWN_ERROR', 0, 'Network error')));
    const { fixture, navigateByUrl } = setup(redeem);

    fillAndSubmit(fixture, 'brand-new-password', 'brand-new-password');

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent.trim()).toBe('Network error');
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('routes to the expired page without calling the API when the link has no token', () => {
    const redeem = vi.fn();
    const { fixture, navigateByUrl } = setup(redeem, null);

    fillAndSubmit(fixture, 'brand-new-password', 'brand-new-password');

    expect(navigateByUrl).toHaveBeenCalledWith('/link-expired?kind=reset');
    expect(redeem).not.toHaveBeenCalled();
  });

  it('has no axe violations', async () => {
    const { fixture } = setup();

    await expectNoAxeViolations(fixture.nativeElement);
  }, 15000);
});
