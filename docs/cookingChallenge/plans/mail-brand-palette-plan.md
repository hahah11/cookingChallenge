# Mail Brand Palette Plan

## Status

**Implemented (2026-09-22).** Plan 3 of 3 from the 2026-09-22 feature request; it extracts the shared mail layout that [`cook-mail-phrasing-plan.md`](cook-mail-phrasing-plan.md) and [`admin-password-reset-plan.md`](admin-password-reset-plan.md) build on.

- `templates/mail/_layout.html` holds the chrome and palette as `page(title, heading, body, ctaLabel, footer)`.
  Callers pass markup through `th:ref` blocks (`~{::heading}`, `~{::body-copy}`, `~{::footer}`); the CTA
  label is a plain string, and `${link}` comes straight from the context. The footer is a parameter
  rather than fixed text because the password-reset mail needs different wording (2 hours, no login).
- Beyond the plan: a 1px `#c1bcbc` card border and a divider above the footer use `outline-variant`,
  because a `#ffffff` card on a `#f9f8f8` ground is otherwise nearly invisible.
- `MailTemplateRenderingTest` gained layout-resolution, palette and no-leaked-placeholder cases. Dropping
  the `.html` from the fragment reference was checked by hand to fail 5 of them.
- Full `./gradlew test` passes (330 tests). The Mailpit check under Verification has not been done yet.

## Context

`docs/cookingChallenge/plans/email-notifications-plan.md` shipped two HTML mails whose colors are
Google's **stock Material 3 baseline violet** — `#65558f` buttons on a `#f5f2f7` ground. The web
app's real theme (`frontend/src/styles/_theme.scss`) is generated in OKLCH from brand seed
`#ec3013` (hue 29°). So the mails currently look like a different product than the app.

Two things make this more than a find-and-replace:

1. **The theme is OKLCH; email is not.** `oklch()` has no support in Outlook, and patchy support
   in Gmail/Apple Mail. Every token must be converted to a literal hex at authoring time. There
   is no hex anywhere in `_theme.scss` to copy.
2. **There is no shared template chrome.** `access-link.html` and `results-available.html` each
   repeat the full wrapper, card, button and palette. Plans 1 and 2 add a third and a fourth
   template, so the palette would land in 4–8 files. Extract the layout first.

## Palette

Converted from `frontend/src/styles/_theme.scss` light-scheme roles (Ottosson OKLab→sRGB, verified
twice independently against the design-system source in
`docs/cookingChallenge/CookoffFrontendMockups/_ds/.../tokens/colors.css`):

| M3 role | Token | OKLCH | Hex | Replaces |
|---|---|---|---|---|
| `primary` | `primary-40` | `oklch(40% 0.1900 29)` | `#940000` | `#65558f` |
| `on-primary` | `primary-100` | `oklch(100% 0 29)` | `#ffffff` | `#ffffff` (unchanged) |
| `surface` / `background` | `neutral-98` | `oklch(98% 0.0003 29)` | `#f9f8f8` | `#f5f2f7` |
| `surface-container-lowest` | `neutral-100` | `oklch(100% 0 29)` | `#ffffff` | `#fffbff` (card) |
| `on-surface` | `neutral-10` | `oklch(10% 0.0023 29)` | `#040303` | `#1d1b20` |
| `on-surface-variant` | `neutral-variant-30` | `oklch(30% 0.0114 29)` | `#332c2b` | `#49454f` |
| `outline-variant` | `neutral-variant-80` | `oklch(80% 0.0052 29)` | `#c1bcbc` | — (new, divider) |

Light scheme only — an email renders on whatever ground the client paints, so the dark tokens are
not used. Contrast: `#ffffff` on `#940000` ≈ 11.8:1, `#040303` on `#f9f8f8` ≈ 19.6:1, and
`#332c2b` on `#ffffff` ≈ 12.5:1 — all well past WCAG AA.

## Approach

**Extract `templates/mail/_layout.html`** holding `<html>`/`<head>`, the outer table, the card,
the CTA button and the footer, exposing a Thymeleaf fragment:

```html
<body th:fragment="page(title, heading, body, ctaLabel, link, footer)" ...>
```

Each mail then becomes a thin caller:

```html
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{mail/_layout.html :: page(
        title=${challengeTitle},
        heading=...,
        ...)}"></html>
```

**Two gotchas, both in `shared/config/MailConfig.java`:**

- The resolvers set `setSuffix("")` and select by `resolvablePatterns` (`mail/*.html`). A
  fragment reference must therefore name the template **with its extension** —
  `~{mail/_layout.html :: page(...)}`, not `~{mail/_layout :: page(...)}`. Getting this wrong
  throws `TemplateInputException` at send time, which `MailDispatcher` **swallows and logs** —
  the mail silently never arrives. `MailTemplateRenderingTest` is what catches it; extend it.
- `_layout.html` matches `mail/*.html`, so it resolves with no config change.

**Leave the `.txt` templates standalone.** They carry no styling; the only shared content is the
two-line footer, which is not worth a fragment indirection. This is a deliberate asymmetry —
note it in a comment so the next person doesn't "fix" it.

## Files

| File | Change |
|---|---|
| `backend/src/main/resources/templates/mail/_layout.html` | **New.** Chrome + palette, one `page(...)` fragment |
| `.../mail/access-link.html` | Reduced to a `th:replace` call |
| `.../mail/results-available.html` | Reduced to a `th:replace` call |
| `.../mail/*.txt` | Untouched by this plan |

## Tests

- `shared/mail/MailTemplateRenderingTest` — add a case per HTML template asserting the rendered
  output contains `#940000` and **no** `#65558f`, and that fragment resolution actually happened
  (assert the card markup is present, which only the layout emits). This is the regression guard
  for the extension-in-fragment-name gotcha above.
- `MailDispatcherTest` needs no change — it asserts on substituted values, not markup.

## Verification

```bash
cd backend && ./gradlew test --tests '*Mail*'
# Then see them for real:
docker compose -f compose.yaml up -d mailpit
./gradlew bootRun --args='--app.mail.enabled=true'
```
Trigger a send, open Mailpit at <http://localhost:8025>, and check the HTML part renders with the
red CTA. Mailpit shows the raw source too, so confirm no `oklch(` survived.
