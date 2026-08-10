import { Overlay } from '@angular/cdk/overlay';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Account, AccountsApi, Challenge, ChallengeStatus, ChallengesApi, DishLabel, SystemRole } from '../../../core/api/generated';
import { NewChallengeDialog } from './new-challenge-dialog';

const accounts: Account[] = [
  { id: 'acc-1', email: 'a@example.com', firstName: 'Alice', lastName: 'A', name: 'Alice A', roles: [SystemRole.USER] },
  { id: 'acc-2', email: 'b@example.com', firstName: 'Bob', lastName: 'B', name: 'Bob B', roles: [SystemRole.USER] },
  { id: 'acc-3', email: 'c@example.com', firstName: 'Cara', lastName: 'C', name: 'Cara C', roles: [SystemRole.USER] }
];

const createdChallenge: Challenge = {
  id: 'chal-1',
  date: '2026-08-10',
  title: 'Summer cook-off',
  dishName: 'Ramen',
  status: ChallengeStatus.OPEN,
  cookAssignments: [
    { accountId: 'acc-1', name: 'Alice A', label: DishLabel.A, colorId: null },
    { accountId: 'acc-2', name: 'Bob B', label: DishLabel.B, colorId: null }
  ],
  guestAccountIds: ['acc-3'],
  createdByAccountId: 'organizer-1',
  submittedGuestCount: 0,
  totalGuestCount: 1,
  hasImage: false,
  overallWinnerAccountId: null
};

describe('NewChallengeDialog', () => {
  async function setup(createChallenge = vi.fn().mockReturnValue(of({ data: createdChallenge, meta: {} }))) {
    const dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [NewChallengeDialog, MatDialogModule],
      providers: [
        Overlay,
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AccountsApi, useValue: { listAccounts: () => of({ data: accounts, pagination: {}, meta: {} }) } },
        { provide: ChallengesApi, useValue: { createChallenge, updateChallengeImage: vi.fn() } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(NewChallengeDialog);
    fixture.detectChanges();
    await TestBed.inject(ApplicationRef).whenStable();
    return { fixture, dialogRef, createChallenge };
  }

  it('lists every account as a Cook A option and excludes the chosen Cook A from Cook B', async () => {
    const { fixture } = await setup();
    const component = fixture.componentInstance;
    component['model'].update((m) => ({ ...m, cookAAccountId: 'acc-1' }));
    fixture.detectChanges();

    expect(component['accounts']().map((a) => a.id)).toEqual(['acc-1', 'acc-2', 'acc-3']);
    expect(component['cookBOptions']().map((a) => a.id)).toEqual(['acc-2', 'acc-3']);
  });

  it('does not submit while required fields are missing', async () => {
    const { fixture, createChallenge } = await setup();

    fixture.componentInstance['onSubmit']();

    expect(createChallenge).not.toHaveBeenCalled();
  });

  it('creates the challenge with selected guests and closes with the result', async () => {
    const { fixture, dialogRef, createChallenge } = await setup();
    const component = fixture.componentInstance;

    component['model'].set({
      title: 'Summer cook-off',
      date: '2026-08-10',
      dishName: 'Ramen',
      cookAAccountId: 'acc-1',
      cookBAccountId: 'acc-2'
    });
    component['toggleGuest']('acc-3', true);
    component['onSubmit']();

    expect(createChallenge).toHaveBeenCalledWith({
      title: 'Summer cook-off',
      date: '2026-08-10',
      dishName: 'Ramen',
      cookAAccountId: 'acc-1',
      cookBAccountId: 'acc-2',
      guestAccountIds: ['acc-3']
    });
    expect(dialogRef.close).toHaveBeenCalledWith(createdChallenge);
  });

  it('closes with undefined on cancel', async () => {
    const { fixture, dialogRef } = await setup();
    fixture.componentInstance['onCancel']();
    expect(dialogRef.close).toHaveBeenCalledWith(undefined);
  });
});
