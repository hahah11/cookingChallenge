import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Challenge, ChallengeStatus, ChallengesApi, Config, ConfigApi, DishLabel } from '../../../core/api/generated';
import { AppConfig } from '../../../core/config/app-config';
import { ChallengeCard } from './challenge-card';

const challenge: Challenge = {
  id: 'chal-1',
  date: '2026-08-01',
  title: 'Summer cook-off',
  dishName: 'Ramen',
  status: ChallengeStatus.REVEALED,
  cookAssignments: [
    { accountId: 'cook-a', name: 'Alice', label: DishLabel.A, colorId: 'red' },
    { accountId: 'cook-b', name: 'Bob', label: DishLabel.B, colorId: 'yellow' }
  ],
  guestAccountIds: ['guest-1'],
  createdByAccountId: 'organizer-1',
  submittedGuestCount: 1,
  totalGuestCount: 1,
  hasImage: false,
  overallWinnerAccountId: 'cook-a'
};

const config: Config = {
  availableRoles: [],
  plateColors: [
    { id: 'red', name: 'Red', hexCode: '#c0392b', sortOrder: 0 },
    { id: 'yellow', name: 'Yellow', hexCode: '#e0b400', sortOrder: 1 }
  ],
  featureFlags: {}
};

describe('ChallengeCard', () => {
  async function createComponent() {
    await TestBed.configureTestingModule({
      imports: [ChallengeCard],
      providers: [
        { provide: ChallengesApi, useValue: { getChallengeImage: () => of(new Blob()) } },
        { provide: ConfigApi, useValue: { getConfig: () => of({ data: config, meta: {} }) } },
        AppConfig
      ]
    }).compileComponents();
    TestBed.inject(AppConfig).load().subscribe();
    const fixture = TestBed.createComponent(ChallengeCard);
    fixture.componentRef.setInput('challenge', challenge);
    fixture.detectChanges();
    return fixture;
  }

  it('renders dish title, event title, and both cook names', async () => {
    const fixture = await createComponent();
    const el = fixture.nativeElement;
    expect(el.querySelector('.challenge-card__title').textContent.trim()).toBe('Ramen');
    expect(el.querySelector('.challenge-card__event').textContent.trim()).toBe('Summer cook-off');
    expect(el.querySelector('.challenge-card__cooks').textContent).toContain('Alice');
    expect(el.querySelector('.challenge-card__cooks').textContent).toContain('Bob');
  });

  it('tints each cook name with their plate color', async () => {
    const fixture = await createComponent();
    const cookEls = fixture.nativeElement.querySelectorAll('.challenge-card__cook');
    expect(cookEls[0].classList).toContain('plate-tint');
    expect(cookEls[0].style.getPropertyValue('--plate-color')).toBe('#c0392b');
    expect(cookEls[1].style.getPropertyValue('--plate-color')).toBe('#e0b400');
  });

  it('marks the winning cook with the trophy icon, not color alone', async () => {
    const fixture = await createComponent();
    const winnerSpan = fixture.nativeElement.querySelector('.challenge-card__cook--winner');
    expect(winnerSpan.textContent).toContain('Alice');
    expect(winnerSpan.querySelector('mat-icon').textContent.trim()).toBe('emoji_events');
  });

  it('shows "Results ready" instead of a progress bar once revealed', async () => {
    const fixture = await createComponent();
    expect(fixture.nativeElement.querySelector('mat-progress-bar')).toBeNull();
    expect(fixture.nativeElement.querySelector('.challenge-card__progress-text').textContent.trim()).toBe(
      'Results ready'
    );
  });

  it('emits open with the challenge id on click', async () => {
    const fixture = await createComponent();
    let openedId: string | undefined;
    fixture.componentInstance.open.subscribe((id) => (openedId = id));

    fixture.nativeElement.querySelector('mat-card').click();

    expect(openedId).toBe('chal-1');
  });
});
