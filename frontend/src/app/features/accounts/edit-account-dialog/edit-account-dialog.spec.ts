import { Overlay } from '@angular/cdk/overlay';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Account, AccountsApi, Config, ConfigApi, SystemRole } from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { EditAccountDialog, EditAccountDialogData } from './edit-account-dialog';

const account: Account = {
  id: 'acc-1',
  email: 'alice@example.com',
  firstName: 'Alice',
  lastName: 'Anderson',
  name: 'Alice Anderson',
  roles: [SystemRole.USER, SystemRole.ORGANIZER]
};

const config: Config = {
  availableRoles: [SystemRole.USER, SystemRole.ORGANIZER, SystemRole.ADMIN],
  plateColors: [],
  featureFlags: {}
};

describe('EditAccountDialog', () => {
  async function setup(
    accountId: string | null = 'acc-1',
    updateAccount = vi.fn().mockReturnValue(of({ data: account, meta: {} })),
    createAccount = vi.fn().mockReturnValue(of({ data: account, meta: {} }))
  ) {
    const dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [EditAccountDialog, MatDialogModule],
      providers: [
        Overlay,
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { accountId } satisfies EditAccountDialogData },
        {
          provide: AccountsApi,
          useValue: { getAccount: () => of({ data: account, meta: {} }), updateAccount, createAccount }
        },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta: {} }) } },
        AppConfig
      ]
    }).compileComponents();

    TestBed.inject(AppConfig).load().subscribe();
    const fixture = TestBed.createComponent(EditAccountDialog);
    fixture.detectChanges();
    await TestBed.inject(ApplicationRef).whenStable();
    return { fixture, dialogRef, updateAccount, createAccount };
  }

  it('pre-fills the form and roles from the freshly fetched account', async () => {
    const { fixture } = await setup();
    const component = fixture.componentInstance;
    expect(component['model']()).toEqual({
      firstName: 'Alice',
      lastName: 'Anderson',
      email: 'alice@example.com',
      password: ''
    });
    expect(component['roles']().has(SystemRole.ORGANIZER)).toBe(true);
  });

  it('cannot uncheck the USER role', async () => {
    const { fixture } = await setup();
    fixture.componentInstance['toggleRole'](SystemRole.USER, false);
    expect(fixture.componentInstance['roles']().has(SystemRole.USER)).toBe(true);
  });

  it('saves the edited fields and roles, closing with the updated account', async () => {
    const { fixture, dialogRef, updateAccount } = await setup();
    const component = fixture.componentInstance;

    component['model'].set({ firstName: 'Alice', lastName: 'Anderson', email: 'alice@example.com', password: '' });
    component['toggleRole'](SystemRole.ADMIN, true);
    component['onSubmit']();

    expect(updateAccount).toHaveBeenCalledWith('acc-1', {
      firstName: 'Alice',
      lastName: 'Anderson',
      email: 'alice@example.com',
      roles: [SystemRole.USER, SystemRole.ORGANIZER, SystemRole.ADMIN]
    });
    expect(dialogRef.close).toHaveBeenCalledWith(account);
  });

  it('opens in create mode with an empty form and no fetch, titled "New account"', async () => {
    const { fixture } = await setup(null);
    const component = fixture.componentInstance;

    expect(component['model']()).toEqual({ firstName: '', lastName: '', email: '', password: '' });
    expect(fixture.nativeElement.querySelector('h2').textContent.trim()).toBe('New account');
  });

  it('submits a create request with the entered password when accountId is null', async () => {
    const { fixture, dialogRef, createAccount } = await setup(null);
    const component = fixture.componentInstance;

    component['model'].set({ firstName: 'Nia', lastName: 'New', email: 'nia@example.com', password: 'secret123' });
    component['onSubmit']();

    expect(createAccount).toHaveBeenCalledWith({
      firstName: 'Nia',
      lastName: 'New',
      email: 'nia@example.com',
      roles: [SystemRole.USER],
      password: 'secret123'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(account);
  });
});
