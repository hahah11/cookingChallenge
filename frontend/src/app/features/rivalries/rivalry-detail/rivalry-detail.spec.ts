import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import {
  ChallengesApi,
  ChallengeStatus,
  RivalriesApi,
  RivalryDetail as RivalryDetailModel
} from '../../../core/api/generated';
import { expectNoAxeViolations } from '../../../testing/axe';
import { RivalryDetail } from './rivalry-detail';

const rivalryDetail: RivalryDetailModel = {
  cookAAccountId: 'cook-a',
  cookAName: 'Alice',
  cookBAccountId: 'cook-b',
  cookBName: 'Bob',
  cookAWins: 2,
  cookBWins: 1,
  draws: 0,
  totalChallenges: 3,
  headline: 'Alice leads Bob 2-1',
  challenges: [
    {
      id: 'chal-1',
      date: '2026-08-01',
      title: 'Summer cook-off',
      dishName: 'Ramen',
      status: ChallengeStatus.REVEALED,
      hasImage: false,
      overallWinnerAccountId: 'cook-a',
      outcomeLabel: 'Alice won'
    },
    {
      id: 'chal-2',
      date: '2026-07-01',
      title: 'Spring cook-off',
      dishName: 'Curry',
      status: ChallengeStatus.OPEN,
      hasImage: false,
      overallWinnerAccountId: null,
      outcomeLabel: 'Pending'
    }
  ]
};

describe('RivalryDetail', () => {
  function setup(getRivalryDetail: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      imports: [RivalryDetail],
      providers: [
        provideRouter([]),
        { provide: RivalriesApi, useValue: { getRivalryDetail } },
        { provide: ChallengesApi, useValue: { getChallengeImage: () => of(new Blob()) } }
      ]
    });

    const fixture = TestBed.createComponent(RivalryDetail);
    fixture.componentRef.setInput('cookA', 'cook-a');
    fixture.componentRef.setInput('cookB', 'cook-b');
    fixture.detectChanges();
    return { fixture };
  }

  it('fetches the pair detail and renders the server headline verbatim', () => {
    const getRivalryDetail = vi.fn().mockReturnValue(of({ data: rivalryDetail, meta: {} }));
    const { fixture } = setup(getRivalryDetail);

    expect(getRivalryDetail).toHaveBeenCalledWith('cook-a', 'cook-b');
    expect(fixture.nativeElement.querySelector('.rivalry-detail__headline').textContent.trim()).toBe(
      'Alice leads Bob 2-1'
    );
  });

  it('renders a card per shared challenge with dish name and the server outcome label', () => {
    const getRivalryDetail = vi.fn().mockReturnValue(of({ data: rivalryDetail, meta: {} }));
    const { fixture } = setup(getRivalryDetail);

    const titles = fixture.nativeElement.querySelectorAll('.rivalry-detail__challenge-title');
    expect(titles[0].textContent.trim()).toBe('Ramen');
    expect(titles[1].textContent.trim()).toBe('Curry');

    const outcomes = fixture.nativeElement.querySelectorAll('.rivalry-detail__challenge-outcome');
    expect(outcomes[0].textContent.trim()).toBe('Alice won');
    expect(outcomes[1].textContent.trim()).toBe('Pending');
  });

  it('links back to the rivalry list', () => {
    const getRivalryDetail = vi.fn().mockReturnValue(of({ data: rivalryDetail, meta: {} }));
    const { fixture } = setup(getRivalryDetail);

    const back = fixture.nativeElement.querySelector('.rivalry-detail__back');
    expect(back.getAttribute('href')).toBe('/rivalries');
  });

  it(
    'has no axe violations',
    async () => {
      const getRivalryDetail = vi.fn().mockReturnValue(of({ data: rivalryDetail, meta: {} }));
      const { fixture } = setup(getRivalryDetail);

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
