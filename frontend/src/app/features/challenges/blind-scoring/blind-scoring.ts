import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the blind scoring form ships in Phase 6. */
@Component({
  selector: 'app-blind-scoring',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header title="Score" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class BlindScoring {}
