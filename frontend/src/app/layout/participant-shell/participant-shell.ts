import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Bare shell for participant-facing routes (`/home`, `/challenges/:id/score`,
 * `/challenges/:id/results`) — no toolbar or nav chrome, per the frontend
 * plan's Phase 4. Cooks and guests never switch between sibling screens;
 * each arrives at one destination per emailed link.
 */
@Component({
  selector: 'app-participant-shell',
  imports: [RouterOutlet],
  template: `
    <main class="participant-shell__content">
      <router-outlet />
    </main>
  `,
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
    }

    .participant-shell__content {
      padding: var(--md-sys-spacing-4);
      max-width: 640px;
      margin-inline: auto;
    }
  `
})
export class ParticipantShell {}
