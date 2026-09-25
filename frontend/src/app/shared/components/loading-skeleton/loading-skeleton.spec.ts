import { TestBed } from '@angular/core/testing';

import { LoadingSkeleton } from './loading-skeleton';
import { provideTestI18n } from '../../../testing/i18n';

describe('LoadingSkeleton', () => {
  it('renders the requested number of lines and an accessible status', async () => {
    await TestBed.configureTestingModule({
      providers: [...provideTestI18n()], imports: [LoadingSkeleton] }).compileComponents();
    const fixture = TestBed.createComponent(LoadingSkeleton);
    fixture.componentRef.setInput('lines', 4);
    fixture.detectChanges();

    const el = fixture.nativeElement;
    expect(el.querySelectorAll('.loading-skeleton__line').length).toBe(4);
    expect(el.querySelector('[role="status"]')).not.toBeNull();
    expect(el.querySelector('.cc-visually-hidden').textContent.trim()).toBe('Loading…');
  });
});
