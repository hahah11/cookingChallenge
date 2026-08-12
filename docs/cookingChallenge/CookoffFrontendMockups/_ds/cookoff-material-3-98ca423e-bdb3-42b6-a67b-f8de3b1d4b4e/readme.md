# CookOff — Material 3 design system

CookOff is a cook-off app: two cooks blind-prepare the same dish under a plate color (red or yellow) so tasters don't know who made what, guests score 1–5 stars across three categories, and an organizer reveals the results once everyone has scored. Roles: **Organizer** (creates challenges, sends links, reveals results), **Cook** (picks a plate color, is judged blind), **Guest** (scores, views results), and **New participant** (self-registers via a QR code).

This design system exists to give CookOff an **Angular Material 3** foundation — the same brand colors as the existing prototype, restructured around Material 3's actual token architecture (reference/system color roles, the M3 type scale, M3 shape and elevation, Material Symbols iconography) instead of the flat, zero-radius "Modernist" system the prototype currently borrows.

## Sources

- **Prototype & PRD**: `CookingChallenge Frontend.dc.html` and `CookingChallenge Frontend PRD.md`, from a companion project (`https://claude.ai/design/p/6749cf81-ab80-4ad5-9c9c-043ad865b54f`). This is the ground truth for the product's screens, roles, copy and interaction rules — see the PRD for the full screen-by-screen spec.
- **Prior visual system**: that prototype's linked design system, `_ds/modernist-…/` in the same project — the source of the brand's colors (`--color-bg #f3f2f2`, `--color-text #201e1d`, `--color-accent #ec3013`) and voice, which this system carries forward while replacing everything structural (type, shape, elevation, components) with Material 3.
- No Figma file or engineering codebase was attached — this system is derived entirely from the two files above.

## What changed vs. the source, and why

The brief was "same colors, but aligned with Angular Material 3." Concretely:

- **Kept**: the warm off-white ground, near-black ink, and the #ec3013 red-orange as the brand color — now the seed for the M3 primary tonal palette.
- **Added** (M3 requires them, the mono source didn't have them): a desaturated secondary role (same hue, low chroma) and a complementary herb-green tertiary (hue 100) — themed for a cooking app, used for lower-emphasis accents and category tags. Flagged here as an intentional addition; nothing in the source dictated tertiary's hue, so revisit it if you want a different accent.
- **Realigned to M3 spec** (deliberately different from the source): shape (source was 0-radius everywhere; this system uses the M3 rounded shape scale, full-round buttons/chips, 12dp cards), typeface (source used Archivo; this system uses **Roboto**, M3's reference typeface — no Roboto files were supplied, so it's loaded from Google Fonts), and iconography (source used inline Lucide SVGs; this system uses **Material Symbols Outlined**, M3's canonical icon font, loaded from Google Fonts).
- **No logo** was supplied anywhere in the source materials. The brand mark is the wordmark "CookOff" set in Roboto Medium — see `foundations/brand-mark.html`. Replace with a real mark if one exists.

## Content fundamentals

- **Tone**: plain and functional, not marketing copy. Sentences describe what a control does ("This locks your plate color for this challenge and assigns the other cook the remaining color automatically. This can't be changed afterward.") rather than selling the feature.
- **Person**: mostly third-person/descriptive UI labels ("Guest list", "Send links"); second person appears only in direct address on personal-link pages ("Hi, {name}", "You're plating **Red**").
- **Casing**: sentence case everywhere — button labels ("New challenge", "Send links"), dialog titles, tags. No title case, no all-caps except tiny kicker labels (10-11px, letter-spaced) and M3 button labels' inherited type scale.
- **Playfulness in small doses**: a full-screen "REVEALED!" animated banner on reveal, 🎉 on registration/scoring success, 👑 crown emoji marking a head-to-head win. These are the only decorative emoji in the product — used as celebratory punctuation, never as icon replacements or inline in body copy.
- **Confirmations are explicit and consequence-first**: every destructive/irreversible action's dialog states the consequence before asking ("Revealing exposes the cook↔dish mapping … This can't be undone."), not just "Are you sure?".
- **Names**: the sample data is German/Austrian (Anna Novak, Marco Huber, Sommerküche, Grillabend) — the product is written for a specific friend group, not a generic global audience. Keep sample data flavor consistent if extending it.

## Visual foundations

- **Color**: Material 3 tonal-palette system. One seed hue (29°, the brand red-orange) drives primary; secondary is the same hue at low chroma; tertiary is an added herb-green (hue 100) for variety. Neutral and neutral-variant carry a faint warm tint at the primary hue, matching the source's warm-gray ground rather than a cold gray. All roles are full M3 reference palettes (13+ tones each) mapped to system roles (`--md-sys-color-primary`, `-on-primary`, `-primary-container`, etc.) for both a light scheme (default) and a `[data-theme="dark"]` scheme. See `foundations/color-*.html`.
- **Type**: Roboto, M3's full 15-step type scale (display/headline/title/body/label × large/medium/small), loaded via Google Fonts. See `foundations/type-*.html` and `tokens/typography.css`.
- **Shape**: M3's 7-step corner scale, none (0) through full (999px) — buttons and chips are fully rounded (pill), FABs and cards use medium/large radii, dialogs use extra-large. See `foundations/shape.html`.
- **Elevation**: 5 M3 elevation levels, each a two-layer ink shadow (key + ambient) tuned for the light ground, plus a parallel surface-tint-opacity scale for tinted surfaces. No inner shadows, no glassmorphism/blur — M3 elevation is shadow + surface-container tone, not blur.
- **Spacing**: a plain 4dp grid (4/8/12/16/20/24/32/40/48/64).
- **Backgrounds**: flat color fields only — no gradients, no photographic full-bleed treatments, no illustration or texture. Photography (challenge/dish photos) sits in fixed 4:3 or 16:9 card media slots, never as a page background.
- **Motion**: M3 standard/emphasized easing curves and short/medium/long durations are defined as tokens (`tokens/elevation.css`) for consumers to animate with (dialog/menu enter-exit, state-layer fades). The system itself doesn't ship a signature bounce or custom easing — this is standard M3 motion, not a brand quirk.
- **Interaction states**: M3 state layers — a flat `currentColor` wash at 8% (hover), 10% (focus/pressed) or 16% (dragged) opacity over the interactive element, not a color swap or darken/lighten shift. Disabled controls drop to 38% content opacity / 12% container opacity per M3 spec. Focus rings are a 2px primary-color outline, offset 2px.
- **Borders**: hairline (1px) outlines in `--md-sys-color-outline` / `-outline-variant` for outlined buttons, chips, text fields and outlined cards — never a colored/thick border for emphasis (that's the filled/tonal button's job).
- **Cards**: default `elevated` (surface-container-low background, level-1 shadow, no border), with `filled` (surface-container-highest, no shadow) and `outlined` (surface + hairline border) alternates — all 12dp corner radius, per M3.
- **Imagery**: no color treatment specified by the source beyond "prints in grayscale," which was specific to the old flat system's editorial look; this M3 system uses full-color photography in card media slots (no grayscale filter, no duotone).

## Iconography

- **Material Symbols Outlined** (Google's variable icon font — M3's canonical icon set) via Google Fonts CDN, 24px default, `FILL 0` (outline) with a `.filled` modifier class for toggled/selected states (e.g. a filled heart). This replaces the source prototype's inline Lucide SVGs — flagged substitution, since Lucide isn't part of Material 3's spec.
- No emoji-as-icon usage anywhere in the system; the product's few emoji (🎉 👑) are celebratory copy, not interface icons, and stay out of buttons, nav, and tags.
- No PNG icon assets. All icons are the single variable webfont; weight/fill/grade/optical-size are exposed as font-variation-settings if a consumer needs to match a heavier weight.

## Components

Full Material 3 component inventory (Material 3 spec is the source of truth here, since neither the PRD nor prototype defined a component library of its own — see "What changed" above):

- **Button** (`components/button/`) — filled, tonal, elevated, outlined, text
- **IconButton** (`components/icon-button/`) — standard, filled, tonal, outlined
- **Fab** (`components/fab/`) — small, regular, large, extended, surface
- **Card** (`components/card/`) — elevated, filled, outlined
- **Chip** (`components/chip/`) — assist, filter, input, suggestion, elevated
- **Badge** (`components/badge/`) — numeric and dot
- **Divider** (`components/divider/`) — full-width and inset
- **TextField** (`components/text-field/`) — outlined and filled, with error state
- **Checkbox** (`components/checkbox/`)
- **RadioButton** (`components/radio-button/`)
- **Switch** (`components/switch/`)
- **TopAppBar** (`components/top-app-bar/`) — small and center-aligned
- **NavigationBar** (`components/navigation-bar/`) — bottom nav for mobile
- **Tabs** (`components/tabs/`) — primary tabs
- **Dialog** (`components/dialog/`) — modal confirmation
- **Snackbar** (`components/snackbar/`)
- **ProgressIndicator** (`components/progress-indicator/`) — linear and circular

Each lives in `components/<name>/` as `<Name>.jsx` + `.d.ts` + `.prompt.md`, with a `*.card.html` demonstrating every variant/state.

## Index

- `styles.css` — the single stylesheet consumers link; imports everything below.
- `tokens/colors.css` — M3 reference palettes + light/dark system color roles.
- `tokens/typography.css` — Roboto + the M3 type scale (tokens and `.md-typescale-*` classes).
- `tokens/shape.css` — the 7-step corner radius scale.
- `tokens/elevation.css` — 5 elevation levels, state-layer opacities, motion tokens.
- `tokens/spacing.css` — the 4dp spacing scale.
- `tokens/icons.css` — Material Symbols Outlined font-face + base class.
- `components.css` — every `.md-*` component class, consuming the tokens above.
- `foundations/` — specimen cards for the Design System tab (Colors, Type, Shape, Spacing, Brand groups).
- `components/` — the 17 component families listed above.
- `ui_kits/cookoff/` — click-through recreation of the CookingChallenge product (organizer + cook/guest flows).
- `thumbnail.html` — this project's homepage tile.
- `SKILL.md` — portable skill file for using this system in Claude Code.

## Caveats — please help me iterate

- **Typeface and iconography are substitutions, not sourced choices**: I switched Archivo→Roboto and Lucide→Material Symbols because that's what "aligned with Angular Material 3" calls for, but if you'd rather keep Archivo as a display font layered on M3 structure (many real M3 apps do custom type), say so and I'll adjust `tokens/typography.css` without touching color/shape/elevation.
- **Secondary and tertiary hues are my inference**, not given anywhere in the source (the source was a deliberately mono red-on-white system). If CookOff has a real secondary/tertiary brand color in mind, tell me and I'll regenerate the palettes from it instead.
- **No logo exists in any source I could access** — the brand mark is currently just type. If you have a mark, drop it in and I'll wire it into `foundations/brand-mark.html` and the thumbnail.
- I have not built a dark-theme UI kit screen, only the dark color-role swatches in `foundations/color-dark-scheme.html` — say the word if you want the UI kit itself shown in dark mode too.
