import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the cook/guest home screen (`GET /me/home`) ships in Phase 6. */
@Component({
  selector: 'app-participant-home',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header title="Home" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class ParticipantHome {}
