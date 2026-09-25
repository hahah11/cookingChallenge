import { TestBed } from '@angular/core/testing';

import { Category, CookAssignment, DishLabel, RivalrySummary } from '../../../core/api/generated';
import { ResultsTable } from './results-table';
import { provideTestI18n } from '../../../testing/i18n';

const cookAssignments: CookAssignment[] = [
  { accountId: 'cook-a', name: 'Alice', label: DishLabel.A, colorId: 'red' },
  { accountId: 'cook-b', name: 'Bob', label: DishLabel.B, colorId: 'yellow' }
];

const rivalry: RivalrySummary = {
  cookAAccountId: 'cook-a',
  cookBAccountId: 'cook-b',
  cookAWins: 3,
  cookBWins: 1,
  draws: 0,
  totalChallenges: 4,
  headline: 'Alice leads Bob 3-1'
};

describe('ResultsTable', () => {
  async function createComponent() {
    await TestBed.configureTestingModule({
      providers: [...provideTestI18n()], imports: [ResultsTable] }).compileComponents();
    const fixture = TestBed.createComponent(ResultsTable);
    fixture.componentRef.setInput('cookAssignments', cookAssignments);
    fixture.componentRef.setInput('categoryTotals', [
      {
        category: Category.MUNDGEFUEHL,
        dishTotals: [
          { label: DishLabel.A, total: 12 },
          { label: DishLabel.B, total: 9 }
        ]
      },
      {
        category: Category.GESCHMACK,
        dishTotals: [
          { label: DishLabel.A, total: 10 },
          { label: DishLabel.B, total: 14 }
        ]
      }
    ]);
    fixture.componentRef.setInput('categoryWinners', {
      MUNDGEFUEHL: 'cook-a',
      GESCHMACK: 'cook-b'
    });
    fixture.componentRef.setInput('overallWinnerAccountId', 'cook-a');
    fixture.componentRef.setInput('rivalry', rivalry);
    fixture.componentRef.setInput('plateColorHex', { red: '#c0392b', yellow: '#e0b400' });
    fixture.detectChanges();
    return fixture;
  }

  it('sums category totals into a Total row per cook', async () => {
    const fixture = await createComponent();
    const totals = fixture.componentInstance['totalByCook']();
    expect(totals[DishLabel.A]).toBe(22);
    expect(totals[DishLabel.B]).toBe(23);
  });

  it('resolves the category and winner for each row', async () => {
    const fixture = await createComponent();
    const [mundgefuehl, geschmack] = fixture.componentInstance['rows']();
    expect(mundgefuehl.category).toBe(Category.MUNDGEFUEHL);
    expect(mundgefuehl.winnerAccountId).toBe('cook-a');
    expect(geschmack.winnerAccountId).toBe('cook-b');
  });

  it('renders the category names in the active language', async () => {
    const fixture = await createComponent();
    const rowHeaders = Array.from<Element>(fixture.nativeElement.querySelectorAll('tbody th[scope="row"]')).map((th) =>
      th.textContent?.trim()
    );
    expect(rowHeaders).toContain('Mouthfeel');
  });

  it('no longer renders the head-to-head row or the headline sentence in the score table', async () => {
    const fixture = await createComponent();
    expect(fixture.nativeElement.querySelector('.results-table tfoot').querySelectorAll('tr').length).toBe(1);
    expect(fixture.nativeElement.textContent).not.toContain('Alice leads Bob');
  });

  it('renders a headerless Rivalry table with one row per cook plus a draws row', async () => {
    const fixture = await createComponent();
    expect(fixture.nativeElement.querySelector('.rivalry__title').textContent.trim()).toBe('Rivalry');
    const table = fixture.nativeElement.querySelector('.rivalry__table');
    expect(table.querySelector('thead')).toBeNull();
    const labels = Array.from<Element>(table.querySelectorAll('tr th')).map((th) => th.textContent?.trim());
    expect(labels).toEqual(['Alice', 'Bob', 'Draws']);
  });

  it('shows one crown per win in the Rivalry table', async () => {
    const fixture = await createComponent();
    const crowns = Array.from<Element>(fixture.nativeElement.querySelectorAll('.rivalry__crowns')).map((el) =>
      el.textContent?.trim()
    );
    expect(crowns).toEqual(['👑👑👑', '👑']);
  });

  it('shows one ghost per draw and none when there are no draws', async () => {
    const fixture = await createComponent();
    expect(fixture.nativeElement.querySelector('.rivalry__draws').textContent.trim()).toBe('');

    fixture.componentRef.setInput('rivalry', { ...rivalry, draws: 2 });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.rivalry__draws').textContent.trim()).toBe('👻👻');
  });

  it('resolves plate color hex from colorId for column tinting', async () => {
    const fixture = await createComponent();
    expect(fixture.componentInstance['hexFor'](cookAssignments[0])).toBe('#c0392b');
    expect(fixture.componentInstance['hexFor']({ ...cookAssignments[0], colorId: null })).toBeNull();
  });

  it('renders one crown per head-to-head win, matched by rivalry cookA/cookB accountId', async () => {
    const fixture = await createComponent();
    expect(fixture.componentInstance['crownsFor'](cookAssignments[0])).toBe('👑👑👑');
    expect(fixture.componentInstance['crownsFor'](cookAssignments[1])).toBe('👑');
    expect(fixture.componentInstance['winsFor'](cookAssignments[0])).toBe(3);
  });

  it('marks the overall winner column header with the same crown as the head-to-head row', async () => {
    const fixture = await createComponent();
    const headerCrowns = fixture.nativeElement.querySelectorAll('thead .results-table__crown');
    expect(headerCrowns.length).toBe(1);
    expect(headerCrowns[0].textContent.trim()).toBe('👑');
    expect(headerCrowns[0].closest('th').textContent).toContain('Alice');
    expect(fixture.nativeElement.querySelector('thead mat-icon')).toBeNull();
  });
});
