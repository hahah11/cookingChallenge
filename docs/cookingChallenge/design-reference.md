# Design Reference

The UI/UX source of truth for `cookingChallenge-angular` lives in Claude Design (claude.ai/design), not in this repo.

- **Project**: [Cooking Challenge documentation review](https://claude.ai/design/p/6749cf81-ab80-4ad5-9c9c-043ad865b54f?file=CookingChallenge+Frontend.dc.html)
- **File**: `CookingChallenge Frontend.dc.html`
- **Design system**: "Modernist" (`_ds/modernist-8bb140f5-69c3-4d9c-96f6-ce9b7e72416a/`) — color/spacing/typography tokens and component classes (buttons, cards, tags, forms, tables, dialogs) in `styles.css`.

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

When building or updating `cookingChallenge-angular`, treat this prototype as the design spec: screen structure, component states (empty/loading/populated), copy, and the Modernist token values.
