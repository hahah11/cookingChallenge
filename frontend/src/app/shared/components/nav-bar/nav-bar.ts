import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatRippleModule } from '@angular/material/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

export interface NavBarItem {
  icon: string;
  label: string;
  route: string;
}

/**
 * Hand-built bottom navigation — Angular Material has no stock
 * NavigationBar component, see the frontend plan's Phase 1 "Gap:
 * NavigationBar". Styled to the M3 nav-bar spec: indicator pill, 3–5
 * destinations, active-state color.
 */
@Component({
  selector: 'app-nav-bar',
  imports: [MatIconModule, MatRippleModule, RouterLink, RouterLinkActive],
  templateUrl: './nav-bar.html',
  styleUrl: './nav-bar.scss'
})
export class NavBar {
  readonly items = input.required<NavBarItem[]>();
}
