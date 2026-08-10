import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
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
import { ChallengeDetail } from './challenge-detail';

const openChallenge = {
  id: 'chal-1',
  date: '2026-08-01',
  title: 'Summer cook-off',
  dishName: 'Ramen',
  status: ChallengeStatus.OPEN,
  cookAssignments: [
    { accountId: 'cook-a', name: 'Alice', label: DishLabel.A, colorId: null },
    { accountId: 'cook-b', name: 'Bob', label: DishLabel.B, colorId: null }
  ],
  guestAccountIds: ['guest-1'],
  createdByAccountId: 'organizer-1',
  submittedGuestCount: 0,
  totalGuestCount: 1,
  hasImage: false,
  overallWinnerAccountId: null
};

const revealedResult = {
  challengeId: 'chal-1',
  categoryWinners: { [Category.GESCHMACK]: 'cook-a' },
  categoryTotals: [],
  overallWinnerAccountId: 'cook-a',
  cookAssignments: openChallenge.cookAssignments,
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
  function setup(challengesApi: Record<string, unknown>, dialog: Record<string, unknown> = {}, navState: unknown = null) {
    TestBed.configureTestingModule({
      imports: [ChallengeDetail],
      providers: [
        { provide: ChallengesApi, useValue: challengesApi },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta }) } },
        AppConfig,
        { provide: MatDialog, useValue: dialog },
        { provide: Notification, useValue: { error: vi.fn(), success: vi.fn(), info: vi.fn() } },
        { provide: Location, useValue: { getState: () => navState } }
      ]
    });

    const fixture = TestBed.createComponent(ChallengeDetail);
    fixture.componentRef.setInput('id', 'chal-1');
    fixture.detectChanges();
    return { fixture };
  }

  it('uses the challenge handed off via router state and loads guest status for an OPEN challenge', () => {
    const getChallengeStatus = vi.fn().mockReturnValue(
      of({
        data: {
          challengeId: 'chal-1',
          totalGuestCount: 1,
          submittedGuestCount: 0,
          guests: [{ accountId: 'guest-1', name: 'Gina', email: 'gina@example.com', submitted: false }]
        },
        meta
      })
    );
    const { fixture } = setup({ getChallengeStatus }, {}, { challenge: openChallenge });

    expect(getChallengeStatus).toHaveBeenCalledWith('chal-1');
    expect(fixture.nativeElement.textContent).toContain('Pending');
  });

  it('falls back to scanning listChallenges when there is no router state', () => {
    const listChallenges = vi.fn().mockReturnValue(of({ data: [openChallenge], pagination: {}, meta }));
    const getChallengeStatus = vi.fn().mockReturnValue(
      of({ data: { challengeId: 'chal-1', totalGuestCount: 0, submittedGuestCount: 0, guests: [] }, meta })
    );
    const { fixture } = setup({ listChallenges, getChallengeStatus }, {}, null);

    expect(listChallenges).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Ramen');
  });

  it('reveals the challenge and switches to the results view after confirmation', () => {
    const revealChallenge = vi.fn().mockReturnValue(of({ data: revealedResult, meta }));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture } = setup(
      {
        getChallengeStatus: () =>
          of({ data: { challengeId: 'chal-1', totalGuestCount: 0, submittedGuestCount: 0, guests: [] }, meta }),
        revealChallenge
      },
      dialog,
      { challenge: openChallenge }
    );

    fixture.componentInstance['confirmReveal']();

    expect(dialog.open).toHaveBeenCalled();
    expect(revealChallenge).toHaveBeenCalledWith('chal-1');
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.REVEALED);
    expect(fixture.componentInstance['result']()).toEqual(revealedResult);
  });

  it('unreveals back to the open guest view after confirmation', () => {
    const revealedChallenge = { ...openChallenge, status: ChallengeStatus.REVEALED };
    const unrevealedChallenge = { ...openChallenge, status: ChallengeStatus.OPEN };
    const unrevealChallenge = vi.fn().mockReturnValue(of({ data: unrevealedChallenge, meta }));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture } = setup(
      {
        getChallengeResults: () => of({ data: revealedResult, meta }),
        getChallengeStatus: () =>
          of({ data: { challengeId: 'chal-1', totalGuestCount: 0, submittedGuestCount: 0, guests: [] }, meta }),
        unrevealChallenge
      },
      dialog,
      { challenge: revealedChallenge }
    );

    fixture.componentInstance['confirmUnreveal']();

    expect(unrevealChallenge).toHaveBeenCalledWith('chal-1');
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.OPEN);
  });
});
