import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { provideTestI18n } from './testing/i18n';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [...provideTestI18n(), provideRouter([])]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('renders a router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });
});
