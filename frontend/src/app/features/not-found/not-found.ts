import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { EmptyState } from '../../shared/components/empty-state/empty-state';

/** `**` wildcard route target — outside both shells, reachable by anyone. */
@Component({
  selector: 'app-not-found',
  imports: [EmptyState, MatButtonModule, RouterLink, TranslocoPipe],
  template: `
    <app-empty-state icon="search_off" [message]="'notFound.message' | transloco">
      <a mat-stroked-button routerLink="/">{{ 'notFound.back' | transloco }}</a>
    </app-empty-state>
  `
})
export class NotFound {}
