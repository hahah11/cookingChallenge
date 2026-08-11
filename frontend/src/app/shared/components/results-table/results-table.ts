import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import {
  Category,
  CategoryScoreTotal,
  CookAssignment,
  DishLabel,
  RivalrySummary
} from '../../../core/api/generated';

const CATEGORY_LABELS: Record<Category, string> = {
  [Category.MUNDGEFUEHL]: 'Mundgefühl',
  [Category.TELLERSPRACHE]: 'Tellersprache',
  [Category.GESCHMACK]: 'Geschmack'
};

interface ResultsTableRow {
  category: Category;
  label: string;
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
  imports: [MatIconModule],
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

  protected readonly rows = computed<ResultsTableRow[]>(() =>
    this.categoryTotals().map((categoryTotal) => ({
      category: categoryTotal.category,
      label: CATEGORY_LABELS[categoryTotal.category],
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
