import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Account, AccountsApi, SystemRole } from '../../../core/api/generated';
import { ApiError } from '../../../core/errors/api-error';
import { AccountsAdmin } from './accounts-admin';

const account: Account = {
  id: 'acc-1',
  email: 'alice@example.com',
  firstName: 'Alice',
  lastName: 'Anderson',
  name: 'Alice Anderson',
  roles: [SystemRole.USER]
};

describe('AccountsAdmin', () => {
  function setup(listAccounts: ReturnType<typeof vi.fn>, dialog: Record<string, unknown> = {}) {
    TestBed.configureTestingModule({
      imports: [AccountsAdmin],
      providers: [
        { provide: AccountsApi, useValue: { listAccounts } },
        { provide: MatDialog, useValue: dialog }
      ]
    });

    const fixture = TestBed.createComponent(AccountsAdmin);
    fixture.detectChanges();
    return { fixture };
  }

  it('renders a row per account with name, email, and roles', () => {
    const listAccounts = vi.fn().mockReturnValue(of({ data: [account], pagination: { totalElements: 1 }, meta: {} }));
    const { fixture } = setup(listAccounts);

    expect(listAccounts).toHaveBeenCalledWith(0, 20);
    const row = fixture.nativeElement.querySelector('tbody tr');
    expect(row.textContent).toContain('Alice Anderson');
    expect(row.textContent).toContain('alice@example.com');
    expect(row.textContent).toContain('USER');
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
});
