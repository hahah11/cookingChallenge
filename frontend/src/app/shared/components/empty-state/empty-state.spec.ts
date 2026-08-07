import { TestBed } from '@angular/core/testing';

import { EmptyState } from './empty-state';

describe('EmptyState', () => {
  it('renders the message and a default icon', async () => {
    await TestBed.configureTestingModule({ imports: [EmptyState] }).compileComponents();
    const fixture = TestBed.createComponent(EmptyState);
    fixture.componentRef.setInput('message', 'Nothing open right now — check back after the next cook-off.');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty-state__message').textContent.trim()).toBe(
      'Nothing open right now — check back after the next cook-off.'
    );
    expect(fixture.nativeElement.querySelector('mat-icon').textContent.trim()).toBe('inbox');
  });
});
