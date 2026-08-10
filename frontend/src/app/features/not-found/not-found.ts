import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { EmptyState } from '../../shared/components/empty-state/empty-state';

/** `**` wildcard route target — outside both shells, reachable by anyone. */
@Component({
  selector: 'app-not-found',
  imports: [EmptyState, MatButtonModule, RouterLink],
  template: `
    <app-empty-state icon="search_off" message="We can't find that page.">
      <a mat-stroked-button routerLink="/">Back to safety</a>
    </app-empty-state>
  `
})
export class NotFound {}
