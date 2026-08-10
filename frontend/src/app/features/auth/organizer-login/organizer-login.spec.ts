import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { throwError, of } from 'rxjs';
import { vi } from 'vitest';

import { Auth } from '../../../core/auth/auth';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { OrganizerLogin } from './organizer-login';

describe('OrganizerLogin', () => {
  function setup(auth: Partial<Auth>) {
    const notification = { error: vi.fn(), success: vi.fn(), info: vi.fn() };

    TestBed.configureTestingModule({
      imports: [OrganizerLogin],
      providers: [
        provideRouter([]),
        { provide: Auth, useValue: auth },
        { provide: Notification, useValue: notification }
      ]
    });

    const fixture = TestBed.createComponent(OrganizerLogin);
    fixture.detectChanges();
    return { fixture, notification };
  }

  function fillForm(fixture: ReturnType<typeof setup>['fixture'], email: string, password: string): void {
    const emailInput: HTMLInputElement = fixture.nativeElement.querySelector('input[type=email]');
    const passwordInput: HTMLInputElement = fixture.nativeElement.querySelector('input[type=password]');
    emailInput.value = email;
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = password;
    passwordInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('does not submit and shows field errors when the form is empty', () => {
    const login = vi.fn();
    const { fixture } = setup({ login });

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(login).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelectorAll('mat-error').length).toBeGreaterThan(0);
  });

  it('logs in and navigates to /challenges on success', async () => {
    const login = vi.fn().mockReturnValue(of({ accessToken: 'token', expiresAt: '2026-01-01T00:00:00Z' }));
    const { fixture } = setup({ login });
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    fillForm(fixture, 'organizer@example.com', 'hunter2');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(login).toHaveBeenCalledWith({ email: 'organizer@example.com', password: 'hunter2' });
    expect(navigateSpy).toHaveBeenCalledWith('/challenges');
  });

  it('shows an inline error for INVALID_CREDENTIALS', () => {
    const apiError: ApiError = {
      code: 'INVALID_CREDENTIALS',
      message: 'Invalid credentials',
      details: [],
      requestId: 'req-1',
      timestamp: '2026-01-01T00:00:00Z',
      status: 401
    };
    const login = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture, notification } = setup({ login });

    fillForm(fixture, 'organizer@example.com', 'wrong');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.organizer-login__error').textContent).toContain(
      'Incorrect email or password.'
    );
    expect(notification.error).not.toHaveBeenCalled();
  });
});
