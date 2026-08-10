import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the rivalry card grid ships in Phase 5. */
@Component({
  selector: 'app-rivalry-list',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header title="Rivalries" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class RivalryList {}
