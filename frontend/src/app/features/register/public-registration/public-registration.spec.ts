import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { PublicApi } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { expectNoAxeViolations } from '../../../testing/axe';
import { PublicRegistration } from './public-registration';

const meta = { requestId: 'req-1', timestamp: '2026-01-01T00:00:00Z' };

describe('PublicRegistration', () => {
  function setup(registerPublicly: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      imports: [PublicRegistration],
      providers: [{ provide: PublicApi, useValue: { registerPublicly } }]
    });

    const fixture = TestBed.createComponent(PublicRegistration);
    fixture.componentRef.setInput('token', 'qr-token');
    fixture.detectChanges();
    return { fixture };
  }

  function fillAndSubmit(fixture: ReturnType<typeof TestBed.createComponent<PublicRegistration>>) {
    fixture.componentInstance['model'].set({ firstName: 'Gina', lastName: 'Guest', email: 'gina@example.com' });
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('renders the server message verbatim when the challenge is still open', () => {
    const registerPublicly = vi.fn().mockReturnValue(
      of({ data: { accountId: 'acc-1', joined: true, message: "You're registered and joined!" }, meta })
    );
    const { fixture } = setup(registerPublicly);

    fillAndSubmit(fixture);

    expect(registerPublicly).toHaveBeenCalledWith({
      token: 'qr-token',
      firstName: 'Gina',
      lastName: 'Guest',
      email: 'gina@example.com'
    });
    expect(fixture.nativeElement.querySelector('.public-registration__message').textContent.trim()).toBe(
      "You're registered and joined!"
    );
  });

  it('renders the server message verbatim when the event already closed', () => {
    const registerPublicly = vi.fn().mockReturnValue(
      of({
        data: { accountId: 'acc-1', joined: false, message: "You're registered, but this event has already closed." },
        meta
      })
    );
    const { fixture } = setup(registerPublicly);

    fillAndSubmit(fixture);

    expect(fixture.nativeElement.querySelector('.public-registration__message').textContent.trim()).toBe(
      "You're registered, but this event has already closed."
    );
  });

  it('shows an expired-code state on INVALID_OR_EXPIRED_LINK', () => {
    const apiError: ApiError = {
      code: 'INVALID_OR_EXPIRED_LINK',
      message: 'Token invalid.',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 401
    };
    const registerPublicly = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture } = setup(registerPublicly);

    fillAndSubmit(fixture);

    expect(fixture.nativeElement.textContent).toContain('This code has expired');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('shows a duplicate-email message on ACCOUNT_ALREADY_EXISTS, keeping the form', () => {
    const apiError: ApiError = {
      code: 'ACCOUNT_ALREADY_EXISTS',
      message: 'Account exists.',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 409
    };
    const registerPublicly = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture } = setup(registerPublicly);

    fillAndSubmit(fixture);

    expect(fixture.nativeElement.querySelector('.public-registration__error').textContent).toContain(
      'already exists'
    );
    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
  });

  it(
    'has no axe violations',
    async () => {
      const { fixture } = setup(vi.fn());

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
