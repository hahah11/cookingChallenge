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

const challengeDetail = {
  challengeId: 'chal-1',
  totalGuestCount: 1,
  submittedGuestCount: 0,
  guests: [{ accountId: 'guest-1', name: 'Gina', email: 'gina@example.com', submitted: false }],
  title: 'Summer cook-off',
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
    TestBed.configureTestingModule({
      imports: [ChallengeDetail],
      providers: [
        { provide: ChallengesApi, useValue: challengesApi },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta }) } },
        AppConfig,
        { provide: MatDialog, useValue: dialog },
        { provide: Notification, useValue: { error: vi.fn(), success: vi.fn(), info: vi.fn() } }
      ]
    });

    const fixture = TestBed.createComponent(ChallengeDetail);
    fixture.componentRef.setInput('id', 'chal-1');
    fixture.detectChanges();
    return { fixture };
  }

  it('fetches challenge detail fresh and renders metadata plus guest status for an OPEN challenge', () => {
    const getChallengeStatus = vi.fn().mockReturnValue(of({ data: challengeDetail, meta }));
    const { fixture } = setup({ getChallengeStatus });

    expect(getChallengeStatus).toHaveBeenCalledWith('chal-1');
    expect(fixture.nativeElement.textContent).toContain('Ramen');
    expect(fixture.nativeElement.textContent).toContain('Pending');
  });

  it('reveals the challenge and switches to the results view after confirmation', () => {
    const revealChallenge = vi.fn().mockReturnValue(of({ data: revealedResult, meta }));
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const { fixture } = setup(
      {
        getChallengeStatus: () => of({ data: challengeDetail, meta }),
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

  it('unreveals back to the open guest view after confirmation, re-fetching the challenge', () => {
    const revealedChallenge = { ...challengeDetail, status: ChallengeStatus.REVEALED };
    const unrevealedChallenge = { ...challengeDetail, status: ChallengeStatus.OPEN };
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
    expect(fixture.componentInstance['challenge']()?.status).toBe(ChallengeStatus.OPEN);
  });
});
