import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Account, AccountsApi, SystemRole } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { Notification } from '../../../core/notifications/notification';
import { expectNoAxeViolations } from '../../../testing/axe';
import { AccountsAdmin } from './accounts-admin';
import { provideTestI18n } from '../../../testing/i18n';

const account: Account = {
  id: 'acc-1',
  email: 'alice@example.com',
  firstName: 'Alice',
  lastName: 'Anderson',
  name: 'Alice Anderson',
  roles: [SystemRole.USER]
};

describe('AccountsAdmin', () => {
  function setup(
    listAccounts: ReturnType<typeof vi.fn>,
    dialog: Record<string, unknown> = {},
    triggerPasswordReset: ReturnType<typeof vi.fn> = vi.fn()
  ) {
    const notification = { success: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AccountsAdmin],
      providers: [...provideTestI18n(), 
        { provide: AccountsApi, useValue: { listAccounts, triggerPasswordReset } },
        { provide: MatDialog, useValue: dialog },
        { provide: Notification, useValue: notification }
      ]
    });

    const fixture = TestBed.createComponent(AccountsAdmin);
    fixture.detectChanges();
    return { fixture, notification };
  }

  const organizer: Account = {
    id: 'acc-2',
    email: 'olga@example.com',
    firstName: 'Olga',
    lastName: 'Organizer',
    name: 'Olga Organizer',
    roles: [SystemRole.ORGANIZER]
  };
  const admin: Account = { ...organizer, id: 'acc-3', email: 'ada@example.com', name: 'Ada Admin', roles: [SystemRole.ADMIN] };

  function listOf(...accounts: Account[]) {
    return vi.fn().mockReturnValue(of({ data: accounts, pagination: { totalElements: accounts.length }, meta: {} }));
  }

  function resetButtonIn(row: HTMLElement): HTMLButtonElement | undefined {
    return Array.from(row.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Reset password');
  }

  it('renders a row per account with name, email, and roles', () => {
    const listAccounts = vi.fn().mockReturnValue(of({ data: [account], pagination: { totalElements: 1 }, meta: {} }));
    const { fixture } = setup(listAccounts);

    expect(listAccounts).toHaveBeenCalledWith(0, 20);
    const row = fixture.nativeElement.querySelector('tbody tr');
    expect(row.textContent).toContain('Alice Anderson');
    expect(row.textContent).toContain('alice@example.com');
    expect(row.textContent).toContain('User');
  });

  it('shows an empty state when there are no accounts', () => {
    const listAccounts = vi.fn().mockReturnValue(of({ data: [], pagination: { totalElements: 0 }, meta: {} }));
    const { fixture } = setup(listAccounts);

    expect(fixture.nativeElement.querySelector('app-empty-state')).not.toBeNull();
  });

  it('shows a retryable error state on failure', () => {
    const apiError: ApiError = {
      code: 'UNKNOWN_ERROR',
      message: 'Network error',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 0
    };
    const listAccounts = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture } = setup(listAccounts);

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
  });

  it('opens the edit dialog and reloads once it closes with an updated account', () => {
    const listAccounts = vi.fn().mockReturnValue(of({ data: [account], pagination: { totalElements: 1 }, meta: {} }));
    const dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(account) });
    const { fixture } = setup(listAccounts, { open: dialogOpen });

    fixture.componentInstance['openEditDialog']('acc-1');

    expect(dialogOpen).toHaveBeenCalled();
    expect(listAccounts).toHaveBeenCalledTimes(2);
  });

  it('opens the create dialog in create mode and reloads once it closes with a new account', () => {
    const listAccounts = vi.fn().mockReturnValue(of({ data: [account], pagination: { totalElements: 1 }, meta: {} }));
    const dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(account) });
    const { fixture } = setup(listAccounts, { open: dialogOpen });

    fixture.componentInstance['openCreateDialog']();

    expect(dialogOpen).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ data: { accountId: null } })
    );
    expect(listAccounts).toHaveBeenCalledTimes(2);
  });

  it('offers a password reset on organizer and admin rows but not on guest rows', () => {
    const { fixture } = setup(listOf(account, organizer, admin));

    const rows: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('tbody tr'));
    expect(resetButtonIn(rows[0])).toBeUndefined();
    expect(resetButtonIn(rows[1])).toBeDefined();
    expect(resetButtonIn(rows[2])).toBeDefined();
  });

  it('sends the reset and confirms it in a snackbar once the dialog is confirmed', () => {
    const dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(true) });
    const triggerPasswordReset = vi.fn().mockReturnValue(of(undefined));
    const { fixture, notification } = setup(listOf(organizer), { open: dialogOpen }, triggerPasswordReset);

    resetButtonIn(fixture.nativeElement.querySelector('tbody tr'))!.click();

    expect(dialogOpen).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: expect.objectContaining({ message: expect.stringContaining('replaces any earlier reset link') })
      })
    );
    expect(triggerPasswordReset).toHaveBeenCalledWith('acc-2');
    expect(notification.success).toHaveBeenCalledWith('Reset link sent to olga@example.com.');
  });

  it('sends nothing when the confirm dialog is dismissed', () => {
    const dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(false) });
    const triggerPasswordReset = vi.fn();
    const { fixture } = setup(listOf(organizer), { open: dialogOpen }, triggerPasswordReset);

    resetButtonIn(fixture.nativeElement.querySelector('tbody tr'))!.click();

    expect(triggerPasswordReset).not.toHaveBeenCalled();
  });

  it('shows the server message when the reset is refused', () => {
    const apiError: ApiError = {
      code: 'PASSWORD_RESET_NOT_ELIGIBLE',
      message: 'Account has no password to reset: acc-2',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 409
    };
    const dialogOpen = vi.fn().mockReturnValue({ afterClosed: () => of(true) });
    const triggerPasswordReset = vi.fn().mockReturnValue(throwError(() => apiError));
    const { fixture, notification } = setup(listOf(organizer), { open: dialogOpen }, triggerPasswordReset);

    resetButtonIn(fixture.nativeElement.querySelector('tbody tr'))!.click();

    expect(notification.error).toHaveBeenCalledWith('Account has no password to reset: acc-2');
    expect(notification.success).not.toHaveBeenCalled();
  });

  it(
    'has no axe violations',
    async () => {
      const listAccounts = vi
        .fn()
        .mockReturnValue(of({ data: [account, organizer], pagination: { totalElements: 2 }, meta: {} }));
      const { fixture } = setup(listAccounts);

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
