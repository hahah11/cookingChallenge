import { TestBed } from '@angular/core/testing';

import { ErrorState } from './error-state';
import { provideTestI18n } from '../../../testing/i18n';

describe('ErrorState', () => {
  it('hides the retry button by default', async () => {
    await TestBed.configureTestingModule({
      providers: [...provideTestI18n()], imports: [ErrorState] }).compileComponents();
    const fixture = TestBed.createComponent(ErrorState);
    fixture.componentRef.setInput('message', 'Something went wrong.');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('emits retry when the button is clicked', async () => {
    await TestBed.configureTestingModule({
      providers: [...provideTestI18n()], imports: [ErrorState] }).compileComponents();
    const fixture = TestBed.createComponent(ErrorState);
    fixture.componentRef.setInput('message', 'Something went wrong.');
    fixture.componentRef.setInput('retryable', true);
    fixture.detectChanges();

    let emitted = false;
    fixture.componentInstance.retry.subscribe(() => (emitted = true));
    fixture.nativeElement.querySelector('button').click();

    expect(emitted).toBe(true);
  });
});
