import { Component, computed, inject, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';

import {
  Category,
  CategoryScoreTotal,
  CookAssignment,
  DishLabel,
  RivalrySummary
} from '../../../core/api/generated';
import { RivalryText } from '../../../core/i18n/rivalry-text';

interface ResultsTableRow {
  category: Category;
  winnerAccountId: string | null;
  totalByLabel: Partial<Record<DishLabel, number>>;
}

/**
 * Shared by the organizer challenge detail and the participant results screen
 * — crown row, tinted columns, bold winner, Total row, head-to-head row.
 * The frontend sums `categoryTotals` for the Total row itself, deliberately,
 * per the frontend plan's Phase 6 (plain arithmetic, not business logic).
 */
@Component({
  selector: 'app-results-table',
  imports: [MatIconModule, TranslocoPipe],
  templateUrl: './results-table.html',
  styleUrl: './results-table.scss'
})
export class ResultsTable {
  readonly cookAssignments = input.required<CookAssignment[]>();
  readonly categoryTotals = input.required<CategoryScoreTotal[]>();
  readonly categoryWinners = input.required<Record<string, string>>();
  readonly overallWinnerAccountId = input.required<string | null>();
  readonly rivalry = input.required<RivalrySummary>();
  readonly plateColorHex = input.required<Record<string, string>>();

  private readonly rivalryText = inject(RivalryText);

  protected readonly headline = computed(() => {
    const rivalry = this.rivalry();
    const nameOf = (accountId: string) => this.cookAssignments().find((cook) => cook.accountId === accountId)?.name ?? '';
    return this.rivalryText.headline({
      cookAName: nameOf(rivalry.cookAAccountId),
      cookBName: nameOf(rivalry.cookBAccountId),
      cookAWins: rivalry.cookAWins,
      cookBWins: rivalry.cookBWins,
      draws: rivalry.draws
    });
  });

  protected readonly rows = computed<ResultsTableRow[]>(() =>
    this.categoryTotals().map((categoryTotal) => ({
      category: categoryTotal.category,
      winnerAccountId: this.categoryWinners()[categoryTotal.category] ?? null,
      totalByLabel: Object.fromEntries(
        categoryTotal.dishTotals.map((dishTotal) => [dishTotal.label, dishTotal.total])
      )
    }))
  );

  protected readonly totalByCook = computed<Partial<Record<DishLabel, number>>>(() => {
    const totals: Partial<Record<DishLabel, number>> = {};
    for (const row of this.rows()) {
      for (const [label, value] of Object.entries(row.totalByLabel)) {
        totals[label as DishLabel] = (totals[label as DishLabel] ?? 0) + value;
      }
    }
    return totals;
  });

  protected hexFor(cook: CookAssignment): string | null {
    return cook.colorId ? (this.plateColorHex()[cook.colorId] ?? null) : null;
  }

  protected winsFor(cook: CookAssignment): number {
    const rivalry = this.rivalry();
    if (cook.accountId === rivalry.cookAAccountId) return rivalry.cookAWins;
    if (cook.accountId === rivalry.cookBAccountId) return rivalry.cookBWins;
    return 0;
  }

  protected crownsFor(cook: CookAssignment): string {
    return '👑'.repeat(this.winsFor(cook));
  }
}
