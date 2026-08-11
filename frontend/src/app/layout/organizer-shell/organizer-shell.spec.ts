import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { SystemRole } from '../../core/api/generated';
import { Auth } from '../../core/auth/auth';
import { OrganizerShell } from './organizer-shell';

describe('OrganizerShell', () => {
  function setup(roles: SystemRole[]) {
    const auth = {
      hasAnyRole: (...check: SystemRole[]) => check.some((role) => roles.includes(role)),
      logout: vi.fn()
    };

    TestBed.configureTestingModule({
      imports: [OrganizerShell],
      providers: [provideRouter([]), { provide: Auth, useValue: auth }]
    });

    const fixture = TestBed.createComponent(OrganizerShell);
    fixture.detectChanges();
    return { fixture, auth };
  }

  it('shows History and Rivalries, but hides Accounts for a non-admin organizer', () => {
    const { fixture } = setup([SystemRole.ORGANIZER]);

    const links = Array.from(fixture.nativeElement.querySelectorAll('[mat-tab-link]')) as HTMLElement[];
    expect(links.map((link) => link.textContent?.trim())).toEqual(['History', 'Rivalries']);
  });

  it('shows Accounts between History and Rivalries for an admin', () => {
    const { fixture } = setup([SystemRole.ORGANIZER, SystemRole.ADMIN]);

    const links = Array.from(fixture.nativeElement.querySelectorAll('[mat-tab-link]')) as HTMLElement[];
    expect(links.map((link) => link.textContent?.trim())).toEqual(['History', 'Accounts', 'Rivalries']);
  });

  it('logs out and navigates to /login when Log out is clicked', () => {
    const { fixture, auth } = setup([SystemRole.ORGANIZER]);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    const logoutButton = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    logoutButton.click();

    expect(auth.logout).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith('/login');
  });
});
