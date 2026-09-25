import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { ParticipantShell } from './participant-shell';
import { provideTestI18n } from '../../testing/i18n';

@Component({ template: 'Home content' })
class DummyHomeComponent {}

describe('ParticipantShell', () => {
  it('renders the matched child route inside its content container', async () => {
    TestBed.configureTestingModule({
      providers: [...provideTestI18n(), 
        provideRouter([
          {
            path: '',
            component: ParticipantShell,
            children: [{ path: 'home', component: DummyHomeComponent }]
          }
        ])
      ]
    });

    const harness = await RouterTestingHarness.create('/home');
    harness.detectChanges();

    expect(harness.routeNativeElement?.querySelector('.participant-shell__content')?.textContent).toContain(
      'Home content'
    );
  });
});
