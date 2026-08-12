import { Component, computed, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

interface ExpiredCopy {
  kicker: string;
  headline: string;
  body: string;
}

const COPY: Record<'link' | 'qr', ExpiredCopy> = {
  link: {
    kicker: 'Link expired',
    headline: 'This link is no longer valid',
    body: 'Your personalized access link has expired. Contact the organizer and ask them to resend your link for this challenge.'
  },
  qr: {
    kicker: 'QR code expired',
    headline: 'This QR code is no longer valid',
    body: 'The registration QR code for this cook-off has expired. Ask the organizer to show a fresh code or send you a personalized link by email.'
  }
};

/**
 * Reached when a guest's access-link session dies mid-visit (401 `UNAUTHENTICATED`) or their
 * QR/access-link token itself was already dead — see `errorInterceptor`. Guests never have
 * organizer credentials, so `/login` is a dead end for them; this is their landing spot instead.
 *
 * `withComponentInputBinding()` (app.config.ts) sets `kind` to `undefined` — not its declared
 * default — when the route has no matching `kind` query param, which is how `errorInterceptor`
 * always reaches this route. `copy` falls back to 'link' explicitly rather than trusting the
 * input's own default.
 */
@Component({
  selector: 'app-link-expired',
  imports: [MatButtonModule, MatCardModule, MatIconModule, RouterLink],
  templateUrl: './link-expired.html',
  styleUrl: './link-expired.scss'
})
export class LinkExpired {
  readonly kind = input<'link' | 'qr'>('link');

  protected readonly copy = computed(() => COPY[this.kind() ?? 'link']);
}
