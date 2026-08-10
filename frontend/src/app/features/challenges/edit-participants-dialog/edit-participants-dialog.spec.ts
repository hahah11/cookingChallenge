import { Overlay } from '@angular/cdk/overlay';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { Account, AccountsApi, Challenge, ChallengeStatus, ChallengesApi, DishLabel, SystemRole } from '../../../core/api/generated';
import { EditParticipantsDialog, EditParticipantsDialogData } from './edit-participants-dialog';

const accounts: Account[] = [
  { id: 'acc-1', email: 'a@example.com', firstName: 'Alice', lastName: 'A', name: 'Alice A', roles: [SystemRole.USER] },
  { id: 'acc-2', email: 'b@example.com', firstName: 'Bob', lastName: 'B', name: 'Bob B', roles: [SystemRole.USER] },
  { id: 'acc-3', email: 'c@example.com', firstName: 'Cara', lastName: 'C', name: 'Cara C', roles: [SystemRole.USER] },
  { id: 'acc-4', email: 'd@example.com', firstName: 'Dee', lastName: 'D', name: 'Dee D', roles: [SystemRole.USER] }
];

const updatedChallenge: Challenge = {
  id: 'chal-1',
  date: '2026-08-10',
  title: 'Summer cook-off',
  dishName: 'Ramen',
  status: ChallengeStatus.OPEN,
  cookAssignments: [
    { accountId: 'acc-1', name: 'Alice A', label: DishLabel.A, colorId: null },
    { accountId: 'acc-2', name: 'Bob B', label: DishLabel.B, colorId: null }
  ],
  guestAccountIds: ['acc-4'],
  createdByAccountId: 'organizer-1',
  submittedGuestCount: 0,
  totalGuestCount: 1,
  hasImage: false,
  overallWinnerAccountId: null
};

describe('EditParticipantsDialog', () => {
  async function setup(updateChallengeParticipants = vi.fn().mockReturnValue(of({ data: updatedChallenge, meta: {} }))) {
    const dialogRef = { close: vi.fn() };
    const data: EditParticipantsDialogData = {
      challengeId: 'chal-1',
      cookAAccountId: 'acc-1',
      cookBAccountId: 'acc-2',
      guestAccountIds: ['acc-3']
    };

    await TestBed.configureTestingModule({
      imports: [EditParticipantsDialog, MatDialogModule],
      providers: [
        Overlay,
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: AccountsApi, useValue: { listAccounts: () => of({ data: accounts, pagination: {}, meta: {} }) } },
        { provide: ChallengesApi, useValue: { updateChallengeParticipants } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(EditParticipantsDialog);
    fixture.detectChanges();
    await TestBed.inject(ApplicationRef).whenStable();
    return { fixture, dialogRef, updateChallengeParticipants };
  }

  it('seeds the current cooks and guests from dialog data', async () => {
    const { fixture } = await setup();
    const component = fixture.componentInstance;
    expect(component['cookAAccountId']()).toBe('acc-1');
    expect(component['cookBAccountId']()).toBe('acc-2');
    expect(component['guestIds']().has('acc-3')).toBe(true);
  });

  it('computes add/remove guest diffs and submits the update', async () => {
    const { fixture, dialogRef, updateChallengeParticipants } = await setup();
    const component = fixture.componentInstance;

    component['toggleGuest']('acc-3', false);
    component['toggleGuest']('acc-4', true);
    component['onSubmit']();

    expect(updateChallengeParticipants).toHaveBeenCalledWith('chal-1', {
      cookAAccountId: 'acc-1',
      cookBAccountId: 'acc-2',
      addGuestAccountIds: ['acc-4'],
      removeGuestAccountIds: ['acc-3']
    });
    expect(dialogRef.close).toHaveBeenCalledWith(updatedChallenge);
  });
});
