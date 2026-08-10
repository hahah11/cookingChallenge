import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { expectNoAxeViolations } from '../../testing/axe';
import { NotFound } from './not-found';

describe('NotFound', () => {
  it('renders a message and a link back to the root', async () => {
    await TestBed.configureTestingModule({
      imports: [NotFound],
      providers: [provideRouter([])]
    }).compileComponents();

    const fixture = TestBed.createComponent(NotFound);
    fixture.detectChanges();

    const el = fixture.nativeElement;
    expect(el.querySelector('.empty-state__message').textContent).toContain("can't find that page");
    expect(el.querySelector('a').getAttribute('href')).toBe('/');
  });

  it(
    'has no axe violations',
    async () => {
      await TestBed.configureTestingModule({
        imports: [NotFound],
        providers: [provideRouter([])]
      }).compileComponents();

      const fixture = TestBed.createComponent(NotFound);
      fixture.detectChanges();

      await expectNoAxeViolations(fixture.nativeElement);
    },
    15000
  );
});
