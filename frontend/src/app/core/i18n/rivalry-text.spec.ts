import { TestBed } from '@angular/core/testing';

import { ChallengeStatus } from '../api/generated';
import { provideTestI18n } from '../../testing/i18n';
import { RivalryText } from './rivalry-text';

const alice = { accountId: 'a', name: 'Alice' };
const bob = { accountId: 'b', name: 'Bob' };

function record(cookAWins: number, cookBWins: number, draws: number) {
  return { cookAName: 'Alice', cookBName: 'Bob', cookAWins, cookBWins, draws };
}

describe('RivalryText', () => {
  function setup(locale: 'en' | 'de') {
    TestBed.configureTestingModule({ providers: [...provideTestI18n(locale)] });
    return TestBed.inject(RivalryText);
  }

  describe('in English', () => {
    it('mirrors the backend headline wording', () => {
      const text = setup('en');

      expect(text.headline(record(0, 0, 0))).toBe("Alice and Bob haven't faced off yet.");
      expect(text.headline(record(3, 1, 0))).toBe('Alice leads Bob 3-1');
      expect(text.headline(record(1, 3, 0))).toBe('Bob leads Alice 3-1');
      expect(text.headline(record(2, 2, 0))).toBe('Alice and Bob are tied 2-2');
      expect(text.headline(record(3, 1, 1))).toBe('Alice leads Bob 3-1 (1 draw)');
      expect(text.headline(record(3, 1, 2))).toBe('Alice leads Bob 3-1 (2 draws)');
      expect(text.headline(record(0, 0, 2))).toBe('Alice and Bob are tied 0-0 (2 draws)');
    });

    it('mirrors the backend outcome wording', () => {
      const text = setup('en');

      expect(text.outcome(ChallengeStatus.OPEN, null, alice, bob)).toBe('Pending');
      expect(text.outcome(ChallengeStatus.CLOSED, 'a', alice, bob)).toBe('Pending');
      expect(text.outcome(ChallengeStatus.REVEALED, 'a', alice, bob)).toBe('Alice won');
      expect(text.outcome(ChallengeStatus.REVEALED, 'b', alice, bob)).toBe('Bob won');
      expect(text.outcome(ChallengeStatus.REVEALED, null, alice, bob)).toBe('Draw');
      expect(text.outcome(ChallengeStatus.REVEALED, 'someone-else', alice, bob)).toBe('Draw');
    });
  });

  describe('in German', () => {
    it('words the headline in German', () => {
      const text = setup('de');

      expect(text.headline(record(0, 0, 0))).toBe(
        'Alice und Bob sind noch nicht gegeneinander angetreten.',
      );
      expect(text.headline(record(3, 1, 0))).toBe('Alice führt gegen Bob mit 3:1');
      expect(text.headline(record(1, 3, 0))).toBe('Bob führt gegen Alice mit 3:1');
      expect(text.headline(record(2, 2, 1))).toBe(
        'Alice und Bob liegen gleichauf (2:2) (1 Unentschieden)',
      );
    });

    it('words the outcome in German', () => {
      const text = setup('de');

      expect(text.outcome(ChallengeStatus.OPEN, null, alice, bob)).toBe('Ausstehend');
      expect(text.outcome(ChallengeStatus.REVEALED, 'a', alice, bob)).toBe('Alice hat gewonnen');
      expect(text.outcome(ChallengeStatus.REVEALED, null, alice, bob)).toBe('Unentschieden');
    });
  });
});
