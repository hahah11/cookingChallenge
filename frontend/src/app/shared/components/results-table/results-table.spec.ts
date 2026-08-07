import { TestBed } from '@angular/core/testing';

import { Category, CookAssignment, DishLabel, RivalrySummary } from '../../../core/api/generated';
import { ResultsTable } from './results-table';

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
    await TestBed.configureTestingModule({ imports: [ResultsTable] }).compileComponents();
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

  it('resolves the category label and winner for each row', async () => {
    const fixture = await createComponent();
    const [mundgefuehl, geschmack] = fixture.componentInstance['rows']();
    expect(mundgefuehl.label).toBe('Mundgefühl');
    expect(mundgefuehl.winnerAccountId).toBe('cook-a');
    expect(geschmack.winnerAccountId).toBe('cook-b');
  });

  it('renders the server-rendered rivalry headline verbatim', async () => {
    const fixture = await createComponent();
    const headline = fixture.nativeElement.querySelector('.results-table__headline');
    expect(headline.textContent.trim()).toBe('Alice leads Bob 3-1');
  });

  it('resolves plate color hex from colorId for column tinting', async () => {
    const fixture = await createComponent();
    expect(fixture.componentInstance['hexFor'](cookAssignments[0])).toBe('#c0392b');
    expect(fixture.componentInstance['hexFor']({ ...cookAssignments[0], colorId: null })).toBeNull();
  });
});
