import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { PageHeader } from './page-header';
import { provideTestI18n } from '../../../testing/i18n';

@Component({
  imports: [PageHeader],
  template: `
    <app-page-header kicker="Cook-off history" title="Challenges">
      <button actions type="button">New challenge</button>
    </app-page-header>
  `
})
class HostComponent {}

describe('PageHeader', () => {
  it('renders the kicker, title, and projected actions', async () => {
    await TestBed.configureTestingModule({
      providers: [...provideTestI18n()], imports: [HostComponent] }).compileComponents();
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const el = fixture.nativeElement;
    expect(el.querySelector('.page-header__kicker').textContent.trim()).toBe('Cook-off history');
    expect(el.querySelector('h1').textContent.trim()).toBe('Challenges');
    expect(el.querySelector('button').textContent.trim()).toBe('New challenge');
  });

  it('omits the kicker element when none is provided', async () => {
    await TestBed.configureTestingModule({
      providers: [...provideTestI18n()], imports: [PageHeader] }).compileComponents();
    const fixture = TestBed.createComponent(PageHeader);
    fixture.componentRef.setInput('title', 'Accounts');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.page-header__kicker')).toBeNull();
  });
});
