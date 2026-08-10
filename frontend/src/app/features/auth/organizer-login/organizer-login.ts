import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — Signal Form login screen ships in Phase 5. */
@Component({
  selector: 'app-organizer-login',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header kicker="CookingChallenge" title="Organizer log in" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class OrganizerLogin {}
