import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the participant-facing results screen ships in Phase 6. */
@Component({
  selector: 'app-challenge-results',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header title="Results" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class ChallengeResults {}
