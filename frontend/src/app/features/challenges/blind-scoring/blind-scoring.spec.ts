import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import {
  Category,
  ChallengesApi,
  ChallengeStatus,
  Config,
  ConfigApi,
  DishLabel,
  ParticipantChallenge
} from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ApiError } from '../../../core/errors/api-error';
import { expectNoAxeViolations } from '../../../testing/axe';
import { BlindScoring } from './blind-scoring';

const challenge: ParticipantChallenge = {
  id: 'chal-1',
  date: '2026-08-01',
  title: 'Summer cook-off',
  dishName: 'Ramen',
  status: ChallengeStatus.OPEN,
  labels: [DishLabel.A, DishLabel.B],
  categories: [Category.MUNDGEFUEHL, Category.TELLERSPRACHE, Category.GESCHMACK],
  participantCookAssignments: [
    { label: DishLabel.A, accountId: null, name: null, colorId: 'red' },
    { label: DishLabel.B, accountId: null, name: null, colorId: 'yellow' }
  ],
  hasImage: false,
  submitted: false,
  mySubmission: null,
  myCookLabel: null,
  canScore: true,
  canPickColor: false
};

const fullSubmission = {
  submittedAt: '2026-08-01T12:00:00Z',
  scores: [
    { dishLabel: DishLabel.A, category: Category.MUNDGEFUEHL, points: 4 },
    { dishLabel: DishLabel.B, category: Category.MUNDGEFUEHL, points: 3 },
    { dishLabel: DishLabel.A, category: Category.TELLERSPRACHE, points: 5 },
    { dishLabel: DishLabel.B, category: Category.TELLERSPRACHE, points: 2 },
    { dishLabel: DishLabel.A, category: Category.GESCHMACK, points: 5 },
    { dishLabel: DishLabel.B, category: Category.GESCHMACK, points: 4 }
  ]
};

const config: Config = {
  availableRoles: [],
  plateColors: [
    { id: 'red', name: 'Red', hexCode: '#c0392b', sortOrder: 0 },
    { id: 'yellow', name: 'Yellow', hexCode: '#e0b400', sortOrder: 1 }
  ],
  featureFlags: {}
};
const meta = { requestId: 'req-1', timestamp: '2026-01-01T00:00:00Z' };

describe('BlindScoring', () => {
  function setup(challengesApi: Record<string, unknown>) {
    TestBed.configureTestingModule({
      imports: [BlindScoring],
      providers: [
        provideRouter([]),
        { provide: ChallengesApi, useValue: challengesApi },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta }) } },
        AppConfig
      ]
    });
    TestBed.inject(AppConfig).load().subscribe();

    const fixture = TestBed.createComponent(BlindScoring);
    fixture.componentRef.setInput('id', 'chal-1');
    fixture.detectChanges();
    return { fixture };
  }

  it('renders a plate-color bar per dish, with a visible name — never color alone', () => {
    const getChallenge = vi.fn().mockReturnValue(of({ data: challenge, meta }));
    const { fixture } = setup({ getChallenge });

    const plates = fixture.nativeElement.querySelectorAll('.blind-scoring__plate');
    expect(plates.length).toBe(2);
    expect(plates[0].textContent.trim()).toBe('Red');
    expect(plates[1].textContent.trim()).toBe('Yellow');
  });

  it('gives every rating cell its category label and grid indexes for the stacked phone layout', () => {
    const getChallenge = vi.fn().mockReturnValue(of({ data: challenge, meta }));
    const { fixture } = setup({ getChallenge });

    const ratings: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('.blind-scoring__rating'));
    expect(ratings.length).toBe(challenge.categories.length * challenge.labels.length);
    expect(ratings[0].querySelector('.blind-scoring__cell-label')?.textContent?.trim()).toBe('Mundgefühl');
    expect(ratings.map((cell) => [cell.style.getPropertyValue('--label-index'), cell.style.getPropertyValue('--category-index')])).toEqual(
      challenge.categories.flatMap((_, categoryIndex) =>
        challenge.labels.map((_, labelIndex) => [String(labelIndex), String(categoryIndex)])
      )
    );
  });

  it('pre-fills stars from mySubmission for edit-until-reveal', () => {
    const submitted: ParticipantChallenge = {
      ...challenge,
      submitted: true,
      mySubmission: {
        submittedAt: '2026-08-01T12:00:00Z',
        scores: [
          { dishLabel: DishLabel.A, category: Category.MUNDGEFUEHL, points: 4 },
          { dishLabel: DishLabel.B, category: Category.MUNDGEFUEHL, points: 3 },
          { dishLabel: DishLabel.A, category: Category.TELLERSPRACHE, points: 5 },
          { dishLabel: DishLabel.B, category: Category.TELLERSPRACHE, points: 2 },
          { dishLabel: DishLabel.A, category: Category.GESCHMACK, points: 5 },
          { dishLabel: DishLabel.B, category: Category.GESCHMACK, points: 4 }
        ]
      }
    };
    const getChallenge = vi.fn().mockReturnValue(of({ data: submitted, meta }));
    const { fixture } = setup({ getChallenge });

    expect(fixture.componentInstance['scoreValues']()['MUNDGEFUEHL:A']).toBe(4);
    const submitButton = fixture.nativeElement.querySelector('.blind-scoring__submit');
    expect(submitButton.disabled).toBe(false);
  });

  it('disables submit until all 6 values are set', () => {
    const getChallenge = vi.fn().mockReturnValue(of({ data: challenge, meta }));
    const { fixture } = setup({ getChallenge });

    const submitButton = fixture.nativeElement.querySelector('.blind-scoring__submit');
    expect(submitButton.disabled).toBe(true);

    for (const category of challenge.categories) {
      for (const label of challenge.labels) {
        fixture.componentInstance['setScore'](category, label, 3);
      }
    }
    fixture.detectChanges();

    expect(submitButton.disabled).toBe(false);
  });

  it('shows the success screen on submit, and only navigates home once the user clicks through', () => {
    const getChallenge = vi.fn().mockReturnValue(of({ data: challenge, meta }));
    const submitScores = vi.fn().mockReturnValue(of({ data: {}, meta }));
    const { fixture } = setup({ getChallenge, submitScores });
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    for (const category of challenge.categories) {
      for (const label of challenge.labels) {
        fixture.componentInstance['setScore'](category, label, 3);
      }
    }
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.blind-scoring__submit').click();
    fixture.detectChanges();

    expect(submitScores).toHaveBeenCalledWith('chal-1', { scores: expect.arrayContaining([
      { dishLabel: DishLabel.A, category: Category.MUNDGEFUEHL, points: 3 }
    ]) });
    expect(fixture.nativeElement.textContent).toContain('scores submitted');
    expect(navigateSpy).not.toHaveBeenCalled();

    fixture.nativeElement.querySelector('.blind-scoring__success button').click();
    expect(navigateSpy).toHaveBeenCalledWith('/home');
  });

  it('labels the submit button "Save changes" once a submission already exists', () => {
    const submitted: ParticipantChallenge = {
      ...challenge,
      submitted: true,
      mySubmission: {
        submittedAt: '2026-08-01T12:00:00Z',
        scores: [{ dishLabel: DishLabel.A, category: Category.MUNDGEFUEHL, points: 4 }]
      }
    };
    const getChallenge = vi.fn().mockReturnValue(of({ data: submitted, meta }));
    const { fixture } = setup({ getChallenge });

    expect(fixture.nativeElement.querySelector('.blind-scoring__submit').textContent.trim()).toBe('Save changes');
  });

  it('reloads the challenge on a 409 submit response and shows the revealed message, without retrying', () => {
    const revealedChallenge: ParticipantChallenge = { ...challenge, status: ChallengeStatus.REVEALED };
    const getChallenge = vi
      .fn()
      .mockReturnValueOnce(of({ data: challenge, meta }))
      .mockReturnValueOnce(of({ data: revealedChallenge, meta }));
    const conflict: ApiError = {
      code: 'INVALID_STATE',
      message: 'Challenge revealed.',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 409
    };
    const submitScores = vi.fn().mockReturnValue(throwError(() => conflict));
    const { fixture } = setup({ getChallenge, submitScores });

    for (const category of challenge.categories) {
      for (const label of challenge.labels) {
        fixture.componentInstance['setScore'](category, label, 3);
      }
    }
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.blind-scoring__submit').click();
    fixture.detectChanges();

    expect(submitScores).toHaveBeenCalledTimes(1);
    expect(getChallenge).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('has been revealed');
    expect(fixture.nativeElement.querySelector('.blind-scoring__grid')).toBeNull();
  });

  it('reloads into the read-only closed view on a 409 when scoring was closed mid-edit', () => {
    const closedChallenge: ParticipantChallenge = {
      ...challenge,
      status: ChallengeStatus.CLOSED,
      submitted: true,
      mySubmission: fullSubmission
    };
    const getChallenge = vi
      .fn()
      .mockReturnValueOnce(of({ data: challenge, meta }))
      .mockReturnValueOnce(of({ data: closedChallenge, meta }));
    const conflict: ApiError = {
      code: 'CHALLENGE_NOT_OPEN',
      message: 'Scoring closed.',
      details: [],
      requestId: '',
      timestamp: '2026-01-01T00:00:00Z',
      status: 409
    };
    const submitScores = vi.fn().mockReturnValue(throwError(() => conflict));
    const { fixture } = setup({ getChallenge, submitScores });

    for (const category of challenge.categories) {
      for (const label of challenge.labels) {
        fixture.componentInstance['setScore'](category, label, 3);
      }
    }
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.blind-scoring__submit').click();
    fixture.detectChanges();

    expect(submitScores).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Scoring is closed');
    expect(fixture.nativeElement.querySelector('.blind-scoring__submit')).toBeNull();
  });

  it('renders submitted scores read-only with a closed banner and no submit button when CLOSED', () => {
    const closedChallenge: ParticipantChallenge = {
      ...challenge,
      status: ChallengeStatus.CLOSED,
      submitted: true,
      mySubmission: fullSubmission
    };
    const getChallenge = vi.fn().mockReturnValue(of({ data: closedChallenge, meta }));
    const { fixture } = setup({ getChallenge });

    expect(fixture.nativeElement.querySelector('.blind-scoring__closed-banner').textContent).toContain(
      'Scoring is closed — results coming soon'
    );
    expect(fixture.nativeElement.querySelector('.blind-scoring__grid')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.blind-scoring__submit')).toBeNull();
    const radios: HTMLInputElement[] = Array.from(fixture.nativeElement.querySelectorAll('app-star-rating input'));
    expect(radios.length).toBeGreaterThan(0);
    expect(radios.every((radio) => radio.disabled)).toBe(true);
  });

  it('shows the grid read-only, with no stars filled, when CLOSED and the guest never submitted', () => {
    const closedChallenge: ParticipantChallenge = { ...challenge, status: ChallengeStatus.CLOSED };
    const getChallenge = vi.fn().mockReturnValue(of({ data: closedChallenge, meta }));
    const { fixture } = setup({ getChallenge });

    expect(fixture.nativeElement.textContent).toContain('Scoring is closed');
    expect(fixture.nativeElement.querySelector('.blind-scoring__grid')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.blind-scoring__submit')).toBeNull();
    const radios: HTMLInputElement[] = Array.from(fixture.nativeElement.querySelectorAll('app-star-rating input'));
    expect(radios.length).toBeGreaterThan(0);
    expect(radios.every((radio) => radio.disabled && !radio.checked)).toBe(true);
  });

  it('shows the already-revealed message immediately when the challenge loads REVEALED', () => {
    const revealedChallenge: ParticipantChallenge = { ...challenge, status: ChallengeStatus.REVEALED };
    const getChallenge = vi.fn().mockReturnValue(of({ data: revealedChallenge, meta }));
    const { fixture } = setup({ getChallenge });

    expect(fixture.nativeElement.textContent).toContain('has been revealed');
    expect(fixture.nativeElement.querySelector('.blind-scoring__grid')).toBeNull();
  });

  it(
    'has no axe violations',
    async () => {
      const getChallenge = vi.fn().mockReturnValue(of({ data: challenge, meta }));
      const { fixture } = setup({ getChallenge });

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
