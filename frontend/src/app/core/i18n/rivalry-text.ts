import { Injectable, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

import { ChallengeStatus } from '../api/generated';

export interface RivalryRecord {
  cookAName: string;
  cookBName: string;
  cookAWins: number;
  cookBWins: number;
  draws: number;
}

export interface OutcomeCook {
  accountId: string;
  name: string;
}

/**
 * The backend also sends English `headline` / `outcomeLabel` prose for these, built from exactly
 * the numbers and names the response already carries. The UI builds them from that structured data
 * instead, in the user's language, and never shows the server's wording. Mirrors the backend's
 * `RivalryHeadline` and `ChallengeOutcomeLabel`.
 */
@Injectable({ providedIn: 'root' })
export class RivalryText {
  private readonly transloco = inject(TranslocoService);

  headline(record: RivalryRecord): string {
    const { cookAName: a, cookBName: b, cookAWins, cookBWins, draws } = record;
    if (cookAWins === 0 && cookBWins === 0 && draws === 0) {
      return this.transloco.translate('rivalry.headline.none', { a, b });
    }

    let base: string;
    if (cookAWins === cookBWins) {
      base = this.transloco.translate('rivalry.headline.tied', { a, b, wins: cookAWins });
    } else if (cookAWins > cookBWins) {
      base = this.transloco.translate('rivalry.headline.leads', {
        leader: a,
        trailer: b,
        high: cookAWins,
        low: cookBWins,
      });
    } else {
      base = this.transloco.translate('rivalry.headline.leads', {
        leader: b,
        trailer: a,
        high: cookBWins,
        low: cookAWins,
      });
    }

    if (draws === 0) {
      return base;
    }
    const drawsText = this.transloco.translate(
      draws === 1 ? 'rivalry.draws.one' : 'rivalry.draws.other',
      { count: draws },
    );
    return `${base} (${drawsText})`;
  }

  /** "Alice won" / "Draw" / "Pending" for one challenge of a rivalry. */
  outcome(
    status: ChallengeStatus,
    overallWinnerAccountId: string | null,
    cookA: OutcomeCook,
    cookB: OutcomeCook,
  ): string {
    if (status !== ChallengeStatus.REVEALED) {
      return this.transloco.translate('rivalry.outcome.pending');
    }
    if (overallWinnerAccountId === cookA.accountId) {
      return this.transloco.translate('rivalry.outcome.won', { name: cookA.name });
    }
    if (overallWinnerAccountId === cookB.accountId) {
      return this.transloco.translate('rivalry.outcome.won', { name: cookB.name });
    }
    return this.transloco.translate('rivalry.outcome.draw');
  }
}
