import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { ChallengeStatus, RivalriesApi, RivalryDetail as RivalryDetailModel } from '../../../core/api/generated';
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
    { id: 'chal-1', date: '2026-08-01', title: 'Summer cook-off', status: ChallengeStatus.REVEALED, overallWinnerAccountId: 'cook-a' },
    { id: 'chal-2', date: '2026-07-01', title: 'Spring cook-off', status: ChallengeStatus.OPEN, overallWinnerAccountId: null }
  ]
};

describe('RivalryDetail', () => {
  function setup(getRivalryDetail: ReturnType<typeof vi.fn>) {
    TestBed.configureTestingModule({
      imports: [RivalryDetail],
      providers: [provideRouter([]), { provide: RivalriesApi, useValue: { getRivalryDetail } }]
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

  it('shows the winner name (not color alone) on a revealed shared challenge', () => {
    const getRivalryDetail = vi.fn().mockReturnValue(of({ data: rivalryDetail, meta: {} }));
    const { fixture } = setup(getRivalryDetail);

    const winnerEls = fixture.nativeElement.querySelectorAll('.rivalry-detail__winner');
    expect(winnerEls.length).toBe(1);
    expect(winnerEls[0].textContent).toContain('Alice');
  });
});
