# Design Reference

The UI/UX source of truth for `cookingChallenge-angular` lives in Claude Design (claude.ai/design), not in this repo. Two projects feed it — one for product content, one for the visual system.

## Product content (screens, copy, flows) — unchanged

- **Project**: [Cooking Challenge documentation review](https://claude.ai/design/p/6749cf81-ab80-4ad5-9c9c-043ad865b54f?file=CookingChallenge+Frontend.dc.html&via=share)
- **File**: `CookingChallenge Frontend.dc.html`
- Still the ground truth for screen structure, component states (empty/loading/populated), copy, and interaction rules — see [`frontend-prd.md`](frontend-prd.md), which is derived from it.

## Visual design system — Angular Material 3 (current)

- **Project**: [Cookoff Frontend Rebuild](https://claude.ai/design/p/eddd583d-a944-4319-a1a6-c853a5f2fe57?file=CookingChallenge.dc.html&via=share)
- **File**: `CookingChallenge.dc.html`
- **Design system**: "CookOff Material 3" (`_ds/cookoff-material-3-98ca423e-bdb3-42b6-a67b-f8de3b1d4b4e/`) — **replaces** the earlier "Modernist" system (`_ds/modernist-8bb140f5-69c3-4d9c-96f6-ce9b7e72416a/`, now superseded, kept only inside the content project above for history). Same brand seed color (`#ec3013`), restructured onto Material 3's real token architecture: M3 color roles for light + dark (`tokens/colors.css`), the 15-step M3 type scale in Roboto (`tokens/typography.css`), the 7-step M3 shape scale — rounded, not zero-radius (`tokens/shape.css`), 5 M3 elevation levels (`tokens/elevation.css`), a 4dp spacing grid (`tokens/spacing.css`), and Material Symbols Outlined iconography (`tokens/icons.css`). Full M3 component inventory (Button, IconButton, Fab, Card, Chip, Badge, Divider, TextField, Checkbox, RadioButton, Switch, TopAppBar, NavigationBar, Tabs, Dialog, Snackbar, ProgressIndicator) plus a click-through `ui_kits/cookoff/` recreation of the product screens.
- See [`plans/frontend-implementation-plan.md`](plans/frontend-implementation-plan.md) Phase 1 for how these tokens map onto `@angular/material`'s M3 theming API.

## What it covers

An interactive prototype of the full app flow:

- Organizer login
- Challenge history (list + create)
- Challenge detail: guest list, send links, reveal/unreveal, category results
- Accounts admin (create/edit, roles)
- Rivalries (list + head-to-head detail)
- Guest-facing personalized home (no login — link-based access)
- Blind category scoring (per dish, 0–5 per category)
- Guest results view
- QR code registration

When building or updating `frontend`, treat the content project as the spec for screen structure, component states (empty/loading/populated), copy, and flows — and the Material 3 project as the spec for visual tokens and components.
