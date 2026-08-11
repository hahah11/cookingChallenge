import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatRippleModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { SystemRole } from '../../core/api/generated';
import { Auth } from '../../core/auth/auth';

interface OrganizerNavLink {
  label: string;
  route: string;
}

/**
 * Top app bar shell for `/challenges`, `/accounts`, `/rivalries` and their
 * detail routes — see the frontend plan's Phase 4. "Accounts" is only shown
 * to admins since the route itself is admin-gated (`adminGuard`), stricter
 * than the organizer-wide shell.
 *
 * The organizer's display name isn't shown: the JWT carries only
 * `sub`/`roles` (see `jwt-claims.ts`), and there's no `/me` endpoint —
 * resolving a name would mean an extra `/accounts/{id}` call this phase
 * doesn't otherwise need.
 */
@Component({
  selector: 'app-organizer-shell',
  imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatRippleModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './organizer-shell.html',
  styleUrl: './organizer-shell.scss'
})
export class OrganizerShell {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected readonly links = computed<OrganizerNavLink[]>(() => {
    const links: OrganizerNavLink[] = [{ label: 'History', route: '/challenges' }];
    if (this.auth.hasAnyRole(SystemRole.ADMIN)) {
      links.push({ label: 'Accounts', route: '/accounts' });
    }
    links.push({ label: 'Rivalries', route: '/rivalries' });
    return links;
  });

  protected logOut(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
