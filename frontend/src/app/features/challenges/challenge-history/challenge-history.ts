import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the challenge history grid ships in Phase 5. */
@Component({
  selector: 'app-challenge-history',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header kicker="Cook-off history" title="Challenges" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class ChallengeHistory {}
