import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the open/revealed challenge detail screen ships in Phase 5. */
@Component({
  selector: 'app-challenge-detail',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header title="Challenge" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class ChallengeDetail {}
