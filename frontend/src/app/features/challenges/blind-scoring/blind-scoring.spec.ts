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

  it('shows a revealed-mid-edit message on a 409 submit response, and does not retry', () => {
    const getChallenge = vi.fn().mockReturnValue(of({ data: challenge, meta }));
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
    expect(fixture.nativeElement.textContent).toContain('has been revealed');
    expect(fixture.nativeElement.querySelector('.blind-scoring__grid')).toBeNull();
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
