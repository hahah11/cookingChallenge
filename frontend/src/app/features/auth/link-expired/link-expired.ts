import { Component, computed, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

/**
 * Reached when a guest's access-link session dies mid-visit (401 `UNAUTHENTICATED`) or their
 * QR/access-link token itself was already dead — see `errorInterceptor`. Guests never have
 * organizer credentials, so `/login` is a dead end for them; this is their landing spot instead.
 *
 * `withComponentInputBinding()` (app.config.ts) sets `kind` to `undefined` — not its declared
 * default — when the route has no matching `kind` query param, which is how `errorInterceptor`
 * always reaches this route. `copyKey` falls back to 'link' explicitly rather than trusting the
 * input's own default.
 */
@Component({
  selector: 'app-link-expired',
  imports: [MatButtonModule, MatCardModule, MatIconModule, RouterLink, TranslocoPipe],
  templateUrl: './link-expired.html',
  styleUrl: './link-expired.scss'
})
export class LinkExpired {
  readonly kind = input<'link' | 'qr' | 'reset'>('link');

  /** Translation key prefix for the kicker/headline/body of this kind, e.g. `linkExpired.qr`. */
  protected readonly copyKey = computed(() => `linkExpired.${this.kind() ?? 'link'}`);
}
