import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the `?token=` registration form ships in Phase 6. */
@Component({
  selector: 'app-public-registration',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header title="Register" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class PublicRegistration {}
