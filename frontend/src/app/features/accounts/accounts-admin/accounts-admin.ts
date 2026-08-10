import { Component } from '@angular/core';

import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { PageHeader } from '../../../shared/components/page-header/page-header';

/** Placeholder — the accounts table and role editor ship in Phase 5. */
@Component({
  selector: 'app-accounts-admin',
  imports: [PageHeader, EmptyState],
  template: `
    <app-page-header title="Accounts" />
    <app-empty-state icon="construction" message="Coming soon." />
  `
})
export class AccountsAdmin {}
