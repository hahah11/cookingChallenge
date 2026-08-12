import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import {
  ChallengeStatus,
  ChallengesApi,
  DishLabel,
  ParticipantChallenge,
  ParticipantChallengeMyCookLabelEnum,
  PlateColor
} from '../../../core/api/generated';
import { ParticipantChallengeCard } from './participant-challenge-card';

const RED: PlateColor = { id: 'red', name: 'Red', hexCode: '#c0392b', sortOrder: 0 };
const YELLOW: PlateColor = { id: 'yellow', name: 'Yellow', hexCode: '#e0b400', sortOrder: 1 };

function baseChallenge(overrides: Partial<ParticipantChallenge> = {}): ParticipantChallenge {
  return {
    id: 'chal-1',
    date: '2026-08-01',
    title: 'Summer cook-off',
    dishName: 'Ramen',
    status: ChallengeStatus.OPEN,
    labels: [DishLabel.A, DishLabel.B],
    categories: [],
    participantCookAssignments: [
      { label: DishLabel.A, accountId: null, name: null, colorId: null },
      { label: DishLabel.B, accountId: null, name: null, colorId: null }
    ],
    hasImage: false,
    submitted: false,
    mySubmission: null,
    myCookLabel: null,
    canScore: false,
    canPickColor: false,
    ...overrides
  };
}

describe('ParticipantChallengeCard', () => {
  async function createComponent(props: Record<string, unknown> = {}) {
    await TestBed.configureTestingModule({
      imports: [ParticipantChallengeCard],
      providers: [{ provide: ChallengesApi, useValue: { getChallengeImage: () => of(new Blob()) } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(ParticipantChallengeCard);
    for (const [key, value] of Object.entries(props)) {
      fixture.componentRef.setInput(key, value);
    }
    fixture.detectChanges();
    return fixture;
  }

  it('shows "Score now" when the requester can score and has not submitted', async () => {
    const fixture = await createComponent({ challenge: baseChallenge({ canScore: true, submitted: false }) });
    const button = fixture.nativeElement.querySelector('.participant-challenge-card__action button');
    expect(button.textContent.trim()).toBe('Score now');
  });

  it('shows "Edit scores" plus the submitted tag once already submitted', async () => {
    const fixture = await createComponent({ challenge: baseChallenge({ canScore: true, submitted: true }) });
    const el = fixture.nativeElement;
    expect(el.querySelector('.participant-challenge-card__edit-score button').textContent.trim()).toBe(
      'Edit scores'
    );
    expect(el.querySelector('.participant-challenge-card__submitted-tag').textContent.trim()).toBe(
      'Submitted — editable until reveal'
    );
  });

  it('shows accessible plate-color swatches when the cook can pick', async () => {
    const fixture = await createComponent({
      challenge: baseChallenge({ myCookLabel: ParticipantChallengeMyCookLabelEnum.A, canPickColor: true }),
      pickableColors: [RED, YELLOW]
    });
    const swatches = fixture.nativeElement.querySelectorAll('.participant-challenge-card__swatch');
    expect(swatches.length).toBe(2);
    expect(swatches[0].getAttribute('aria-label')).toBe('Pick Red');
    expect(swatches[1].getAttribute('aria-label')).toBe('Pick Yellow');
  });

  it('emits pickColor with the chosen color on swatch click', async () => {
    const fixture = await createComponent({
      challenge: baseChallenge({ myCookLabel: ParticipantChallengeMyCookLabelEnum.A, canPickColor: true }),
      pickableColors: [RED, YELLOW]
    });
    let picked: PlateColor | undefined;
    fixture.componentInstance.pickColor.subscribe((color) => (picked = color));

    fixture.nativeElement.querySelector('.participant-challenge-card__swatch').click();

    expect(picked).toEqual(RED);
  });

  it('shows the already-picked color, not the picker, once a color is locked in', async () => {
    const fixture = await createComponent({
      challenge: baseChallenge({ myCookLabel: ParticipantChallengeMyCookLabelEnum.A, canPickColor: false }),
      ownColor: RED
    });
    const el = fixture.nativeElement;
    expect(el.querySelector('.participant-challenge-card__swatches')).toBeNull();
    expect(el.querySelector('.participant-challenge-card__own-color').textContent).toContain('Red');
  });

  it('renders the cook-facing card style (outlined, no photo) when the cook can still pick a color', async () => {
    const fixture = await createComponent({
      challenge: baseChallenge({ myCookLabel: ParticipantChallengeMyCookLabelEnum.A, canPickColor: true }),
      pickableColors: [RED, YELLOW]
    });
    const el = fixture.nativeElement;
    expect(el.querySelector('.participant-challenge-card--cook')).not.toBeNull();
    expect(el.querySelector('app-challenge-photo')).toBeNull();
  });

  it('keeps the cook-facing card style once the color is already picked', async () => {
    const fixture = await createComponent({
      challenge: baseChallenge({ myCookLabel: ParticipantChallengeMyCookLabelEnum.A, canPickColor: false }),
      ownColor: RED
    });
    const el = fixture.nativeElement;
    expect(el.querySelector('.participant-challenge-card--cook')).not.toBeNull();
    expect(el.querySelector('app-challenge-photo')).toBeNull();
  });

  it('renders the guest-facing card style (filled, with photo) for a non-cook card', async () => {
    const fixture = await createComponent({ challenge: baseChallenge({ canScore: true, submitted: false }) });
    const el = fixture.nativeElement;
    expect(el.querySelector('.participant-challenge-card--cook')).toBeNull();
    expect(el.querySelector('app-challenge-photo')).not.toBeNull();
  });
});
