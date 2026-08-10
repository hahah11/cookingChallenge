import { Overlay } from '@angular/cdk/overlay';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { ChallengesApi, GuestSubmissionStatus } from '../../../core/api/generated';
import { SendLinksDialog, SendLinksDialogData } from './send-links-dialog';

const guests: GuestSubmissionStatus[] = [
  { accountId: 'guest-1', name: 'Gina', email: 'gina@example.com', submitted: false },
  { accountId: 'guest-2', name: 'Gus', email: 'gus@example.com', submitted: true }
];

describe('SendLinksDialog', () => {
  async function setup() {
    const dialogRef = { close: vi.fn() };
    const getChallengeStatus = vi.fn().mockReturnValue(
      of({ data: { challengeId: 'chal-1', totalGuestCount: 2, submittedGuestCount: 1, guests }, meta: {} })
    );
    const sendInvitations = vi.fn().mockReturnValue(of({ data: { count: 1 }, meta: {} }));

    await TestBed.configureTestingModule({
      imports: [SendLinksDialog, MatDialogModule],
      providers: [
        Overlay,
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { challengeId: 'chal-1' } satisfies SendLinksDialogData },
        { provide: ChallengesApi, useValue: { getChallengeStatus, sendInvitations } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(SendLinksDialog);
    fixture.detectChanges();
    await TestBed.inject(ApplicationRef).whenStable();
    return { fixture, dialogRef, getChallengeStatus, sendInvitations };
  }

  it('pre-selects only guests who have not submitted yet', async () => {
    const { fixture } = await setup();
    const selected = fixture.componentInstance['selectedIds']();
    expect(selected.has('guest-1')).toBe(true);
    expect(selected.has('guest-2')).toBe(false);
  });

  it('sends invitations to the selected guests and closes with the result', async () => {
    const { fixture, dialogRef, sendInvitations } = await setup();

    fixture.componentInstance['onSend']();

    expect(sendInvitations).toHaveBeenCalledWith('chal-1', { guestAccountIds: ['guest-1'] });
    expect(dialogRef.close).toHaveBeenCalledWith({ count: 1 });
  });
});
