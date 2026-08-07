import { TestBed } from '@angular/core/testing';

import { LoadingSkeleton } from './loading-skeleton';

describe('LoadingSkeleton', () => {
  it('renders the requested number of lines and an accessible status', async () => {
    await TestBed.configureTestingModule({ imports: [LoadingSkeleton] }).compileComponents();
    const fixture = TestBed.createComponent(LoadingSkeleton);
    fixture.componentRef.setInput('lines', 4);
    fixture.detectChanges();

    const el = fixture.nativeElement;
    expect(el.querySelectorAll('.loading-skeleton__line').length).toBe(4);
    expect(el.querySelector('[role="status"]')).not.toBeNull();
    expect(el.querySelector('.cc-visually-hidden').textContent.trim()).toBe('Loading…');
  });
});
