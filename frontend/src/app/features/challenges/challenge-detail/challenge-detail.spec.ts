import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import {
  Category,
  ChallengesApi,
  ChallengeStatus,
  Config,
  ConfigApi,
  DishLabel
} from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { Notification } from '../../../core/notifications/notification';
import { expectNoAxeViolations } from '../../../testing/axe';
import { ChallengeDetail } from './challenge-detail';
import { provideTestI18n } from '../../../testing/i18n';

const challengeDetail = {
  challengeId: 'chal-1',
  totalGuestCount: 1,
  submittedGuestCount: 0,
  guests: [{ accountId: 'guest-1', name: 'Gina', email: 'gina@example.com', submitted: false }],
  dishName: 'Ramen',
  date: '2026-08-01',
  status: ChallengeStatus.OPEN,
  hasImage: false,
  cookAssignments: [
    { accountId: 'cook-a', name: 'Alice', label: DishLabel.A, colorId: null },
    { accountId: 'cook-b', name: 'Bob', label: DishLabel.B, colorId: null }
  ]
};

const revealedResult = {
  challengeId: 'chal-1',
  categoryWinners: { [Category.GESCHMACK]: 'cook-a' },
  categoryTotals: [],
  overallWinnerAccountId: 'cook-a',
  cookAssignments: challengeDetail.cookAssignments,
  rivalry: {
    cookAAccountId: 'cook-a',
    cookBAccountId: 'cook-b',
    cookAWins: 1,
    cookBWins: 0,
    draws: 0,
    totalChallenges: 1,
    headline: 'Alice leads Bob 1-0'
  }
};

const config: Config = { availableRoles: [], plateColors: [], featureFlags: {} };
const meta = { requestId: 'req-1', timestamp: '2026-01-01T00:00:00Z' };

describe('ChallengeDetail', () => {
  function setup(challengesApi: Record<string, unknown>, dialog: Record<string, unknown> = {}) {
    const notification = { error: vi.fn(), success: vi.fn(), info: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ChallengeDetail],
      providers: [...provideTestI18n(), 
        provideRouter([]),
        { provide: ChallengesApi, useValue: challengesApi },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta }) } },
        AppConfig,
        { provide: MatDialog, useValue: dialog },
        { provide: Notification, useValue: notification }
      ]
    });

    const fixture = TestBed.createComponent(ChallengeDetail);
    fixture.componentRef.setInput('id', 'chal-1');
    fixture.detectChanges();
    return { fixture, notification };
  }

  it('fetches challenge detail fresh and renders metadata plus guest status for an OPEN challenge', () => {
    const getChallengeStatus = vi.fn().mockReturnValue(of({ data: challengeDetail, meta }));
    const { fixture } = setup({ getChallengeStatus });

    expect(getChallengeStatus).toHaveBeenCalledWith('chal-1');
    expect(fixture.nativeElement.textContent).toContain('Ramen');
    expect(fixture.nativeElement.textContent).toContain('Pending');
  });

  it('renders a back link to the challenge history', () => {
    const getChallengeStatus = vi.fn().mockReturnValue(of({ data: challengeDetail, meta }));
    const { fixture } = setup({ getChallengeStatus });

    const back = fixture.nativeElement.querySelector('.challenge-detail__back');
    expect(back.getAttribute('href')).toBe('/challenges');
  });

  function buttonLabels(fixture: { nativeElement: HTMLElement }): string[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.challenge-detail__actions button')).map((button) =>
      (button as HTMLElement).textContent!.replace(/\s+/g, ' ').trim()
    );
  }

  it('offers edit, QR and close scoring (but not reveal) while OPEN', () => {
    const { fixture } = setup({ getChallengeStatus: () => of({ data: challengeDetail, meta }) });

    const labels = buttonLabels(fixture);
    expect(labels.some((label) => label.includes('Edit cooks & guests'))).toBe(true);
    expect(labels.some((label) => label.includes('Registration QR code'))).toBe(true);
    expect(labels.some((label) => label.includes('Close scoring'))).toBe(true);
    expect(labels.some((label) => label.includes('Reveal results'))).toBe(false);
  });

  it('offers send links, reopen and reveal (but not edit or QR) while CLOSED, keeping the guest list', () => {
    const closedChallenge = { ...challengeDetail, status: ChallengeStatus.CLOSED };
    const { fixture } = setup({ getChallengeStatus: () => of({ data: closedChallenge, meta }) });

    const labels = buttonLabels(fixture);
    expect(labels.some((label) => label.includes('Send links'))).toBe(true);
    expect(labels.some((label) => label.includes('Reopen scoring'))).toBe(true);
    expect(labels.some((label) => label.includes('Reveal results'))).toBe(true);
    expect(labels.some((label) => label.includes('Edit cooks & guests'))).toBe(false);
    expect(labels.some((label) => label.includes('Registration QR code'))).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Gina');
  });

  it('closes scoring after confirmation and switches to the CLOSED actions', () => {
    const closeChallenge = vi.fn().mockReturnValue(of({ data: null, meta }));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture } = setup({ getChallengeStatus: () => of({ data: challengeDetail, meta }), closeChallenge }, dialog);

    fixture.componentInstance['confirmCloseScoring']();
    fixture.detectChanges();

    expect(dialog.open).toHaveBeenCalled();
    expect(closeChallenge).toHaveBeenCalledWith('chal-1');
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.CLOSED);
    expect(buttonLabels(fixture).some((label) => label.includes('Reopen scoring'))).toBe(true);
  });

  it('does not close scoring when the confirmation is cancelled', () => {
    const closeChallenge = vi.fn();
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(false) }) };
    const { fixture } = setup({ getChallengeStatus: () => of({ data: challengeDetail, meta }), closeChallenge }, dialog);

    fixture.componentInstance['confirmCloseScoring']();

    expect(closeChallenge).not.toHaveBeenCalled();
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.OPEN);
  });

  it('reopens scoring back to OPEN', () => {
    const closedChallenge = { ...challengeDetail, status: ChallengeStatus.CLOSED };
    const reopenChallenge = vi.fn().mockReturnValue(of({ data: null, meta }));
    const { fixture } = setup({ getChallengeStatus: () => of({ data: closedChallenge, meta }), reopenChallenge });

    fixture.componentInstance['reopenScoring']();

    expect(reopenChallenge).toHaveBeenCalledWith('chal-1');
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.OPEN);
  });

  it('reveals the challenge and switches to the results view after confirmation', () => {
    const revealChallenge = vi.fn().mockReturnValue(of({ data: revealedResult, meta }));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture } = setup(
      {
        getChallengeStatus: () => of({ data: { ...challengeDetail, status: ChallengeStatus.CLOSED }, meta }),
        revealChallenge
      },
      dialog
    );

    fixture.componentInstance['confirmReveal']();

    expect(dialog.open).toHaveBeenCalled();
    expect(revealChallenge).toHaveBeenCalledWith('chal-1');
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.REVEALED);
    expect(fixture.componentInstance['result']()).toEqual(revealedResult);
  });

  it('unreveals back to the closed guest view after confirmation, re-fetching the challenge', () => {
    const revealedChallenge = { ...challengeDetail, status: ChallengeStatus.REVEALED };
    const unrevealedChallenge = { ...challengeDetail, status: ChallengeStatus.CLOSED };
    const getChallengeStatus = vi
      .fn()
      .mockReturnValueOnce(of({ data: revealedChallenge, meta }))
      .mockReturnValueOnce(of({ data: unrevealedChallenge, meta }));
    const unrevealChallenge = vi.fn().mockReturnValue(of({ data: null, meta }));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture } = setup(
      {
        getChallengeResults: () => of({ data: revealedResult, meta }),
        getChallengeStatus,
        unrevealChallenge
      },
      dialog
    );

    fixture.componentInstance['confirmUnreveal']();

    expect(unrevealChallenge).toHaveBeenCalledWith('chal-1');
    expect(getChallengeStatus).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.CLOSED);
  });

  it.each([ChallengeStatus.OPEN, ChallengeStatus.CLOSED, ChallengeStatus.REVEALED])(
    'shows the delete button for a %s challenge',
    (status) => {
      const { fixture } = setup({
        getChallengeStatus: () => of({ data: { ...challengeDetail, status }, meta }),
        getChallengeResults: () => of({ data: revealedResult, meta })
      });

      expect(fixture.nativeElement.textContent).toContain('Delete challenge');
    }
  );

  it('deletes the challenge after confirmation in danger style and returns to the list', () => {
    const deleteChallenge = vi.fn().mockReturnValue(of(undefined));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture } = setup(
      { getChallengeStatus: () => of({ data: challengeDetail, meta }), deleteChallenge },
      dialog
    );
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.componentInstance['confirmDelete']();

    expect(dialog.open.mock.calls[0][1].data.danger).toBe(true);
    expect(deleteChallenge).toHaveBeenCalledWith('chal-1');
    expect(navigate).toHaveBeenCalledWith(['/challenges']);
  });

  it('does not delete when the confirmation is cancelled', () => {
    const deleteChallenge = vi.fn();
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(false) }) };
    const { fixture } = setup(
      { getChallengeStatus: () => of({ data: challengeDetail, meta }), deleteChallenge },
      dialog
    );

    fixture.componentInstance['confirmDelete']();

    expect(deleteChallenge).not.toHaveBeenCalled();
  });

  it('shows the error and stays on the page when deleting fails', () => {
    const deleteChallenge = vi.fn().mockReturnValue(throwError(() => ({ code: 'NOT_FOUND', message: 'Gone' })));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture, notification } = setup(
      { getChallengeStatus: () => of({ data: challengeDetail, meta }), deleteChallenge },
      dialog
    );
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');

    fixture.componentInstance['confirmDelete']();

    expect(notification.error).toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance['deleteBusy']()).toBe(false);
  });

  it(
    'has no axe violations',
    async () => {
      const getChallengeStatus = vi.fn().mockReturnValue(of({ data: challengeDetail, meta }));
      const { fixture } = setup({ getChallengeStatus });

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
