# i18n: English + German (auto-detected)

## Context

The app is English-only. We want German as a second language, chosen automatically from the browser/device language (no manual switcher). Scope decided with the user:

- **Frontend UI**: Transloco (runtime, one build, one URL). English is the fallback for everything that isn't German.
- **Category labels**: translate for English users (German users keep Mundgefühl / Tellersprache / Geschmack).
- **Plate color names**: localized through a frontend map.
- **Backend emails** (access-link, password-reset, results-available): also localized. All of them are sent by an organizer/admin to someone else, so the recipient's language must be **stored on the account**, not read from the request.
- Locale is captured at public registration and admin account creation, and refreshed on login. Only `en` and `de` are accepted, default `en`.

Findings that shape the design:
- No i18n exists (no `@angular/localize`/Transloco, `<html lang="en">` hardcoded, no `LOCALE_ID`, `DatePipe` only with `'mediumDate'`).
- Text lives in ~34 templates (24 `.html` + 10 inline), snackbars (`core/notifications/notification.ts`), Signal Forms validation messages, confirm dialogs, route titles (`app.routes.ts`), aria-labels/placeholders/tooltips, `status-tag.ts` LABELS, category labels duplicated in `blind-scoring.ts` and `results-table.ts`, fallback in `core/errors/api-error.ts`.
- Backend errors are `{error:{code,message,...}}` with English `message`. `code` is stable, so the frontend maps codes to translated text and never shows `message`. No backend error localization is needed.
- Backend mail: a standalone Thymeleaf engine (`shared/config/MailConfig.java`) with **no** Spring integration or message resolver. Subjects are hardcoded in `EmailNotificationAdapter` and `EmailPasswordResetNotificationAdapter`. `MailRequest` has no locale. Recipients are `accounts` rows. Migrations are Liquibase (`resources/db/changelog/auth/NNN-*.yaml`).

## Part 1 — API contract (`openapi/cookingchallenge-api.yaml`)

Public API change; the user has approved the direction, and it must be additive/optional:
- New schema `Locale` (enum `en`, `de`).
- Add optional `locale` to `Account` (response), `CreateAccountRequest`, `UpdateAccountRequest`, `PublicRegistrationRequest`.
- New endpoint `PUT /api/v1/me/locale` (tag `Home`, next to `/me/home`; any authenticated role, body `{locale}`, 204). Needed because `PATCH /accounts/{id}` is organizer/admin only and guests must be able to refresh their own language on login. No "current account" endpoint exists (login returns only a token; guests get 403 on `/accounts`).
- Regenerate: `npm run generate:api` (frontend) and the backend Gradle OpenAPI generation. Never hand-edit generated code.

## Part 2 — Backend

**Persistence / domain (module `auth`)**
- Liquibase `auth/007-account-locale.yaml`: add `accounts.locale` (varchar, NOT NULL, default `'en'`); include it at the end of `db.changelog-master.yaml`. Follow the `auth/002-account-name-split.yaml` style (`author: fraihs`, comment).
- `Account` (`auth/domain/model/Account.java`): add a `Language` enum (`EN`, `DE`, `toLocale()`), the field in `create`/`reconstitute`, and `changeLanguage(...)`.
- `AccountJpaEntity` (all-args order), `AccountMapper` (`toDomain`/`toEntity`), `AccountModelMapper` (REST) and `AccountSummary` (used by `cookoff` via `AccountLookup`).
- Services: `CreateAccountService`, `UpdateAccountService`, `RegistrationInvites.register(...)` (port signature gains `locale`, called from `PublicRegistrationService`), and a new `ChangeAccountLocaleService` behind the new endpoint. Missing locale means `en`.

**Mail localization (`shared/mail`, `cookoff`, `auth`)**
- `MailRequest`: add `java.util.Locale locale` (plain type, keeps `shared` free of module dependencies).
- `InvitationNotification`, `ResultsAvailableNotification`, `PasswordResetNotification`: add `Locale`. Fill it from the recipient account (`AccountSummary` for `ChallengeRevealedNotifier` and `SendChallengeInvitationsService`; the target `Account` for `PasswordResetService`).
- Message bundles: `resources/mail/messages.properties` (English, fallback) and `messages_de.properties` (UTF-8). They hold subjects, headings, body copy, CTA labels and the "button doesn't work" text.
- A small `IMessageResolver` (in `MailConfig`) backed by a `ResourceBundleMessageSource` (`fallbackToSystemLocale=false`), registered on the standalone engine. This gives `#{key(args)}` in `.html` and `.txt`, including inside the `_layout.html` fragment, without adding `thymeleaf-spring6`.
- Templates (`resources/templates/mail/*`): replace hardcoded English (headings, body, `'Open the cook-off'`, `'Set a new password'`, `lang="en"` → `${#locale.language}`) with `#{...}`. Keep the `canRate`/`picksPlateColor` logic unchanged.
- `MailDispatcher.render`: `new Context(request.locale())`.
- A `MailMessages` helper (in `shared/mail`) used by both notification adapters for subjects, replacing the string literals.
- The "30 days" / "2 hours" copy stays in the bundles. Note in a bundle comment that it mirrors `LINK_VALIDITY` / `TOKEN_VALIDITY`.

**Backend tests** (JUnit 5, `should_x_when_y`)
- Update constructors of the changed payload records in `MailWiringIntegrationTest`, `EmailNotificationAdapterTest`, `PasswordResetServiceTest`, the notifier tests and `CapturingNotificationPort`.
- `MailTemplateRenderingTest`: parameterize over `en`/`de` and assert German wording; build the engine via the `MailConfig` factory so the resolver is included.
- `MailDispatcherTest` and `EmailNotificationAdapterTest`: assert per-locale subjects.
- New `EmailPasswordResetNotificationAdapterTest` (currently untested).
- Parity test: `messages.properties` and `messages_de.properties` have identical key sets.
- `Account` domain tests (default, change language), a controller test for the new field/endpoint, and the existing integration tests confirming Hibernate `validate` accepts the migration.

## Part 3 — Frontend

**Infrastructure**
- Add `@jsverse/transloco` (v8, peer `@angular/core >=16`, compatible with Angular 22).
- `src/app/core/i18n/`:
  - `locale.ts`: `detectLocale(languages = navigator.languages): 'en' | 'de'`. It takes the first entry whose primary subtag is supported (`de-AT`, `de-CH` → `de`), else `en`. This is the single source of truth.
  - `transloco-loader.ts`: HTTP loader for `/i18n/{lang}.json`.
  - `provideI18n()`: `provideTransloco` (`availableLangs ['en','de']`, `defaultLang` from `detectLocale()`, `fallbackLang 'en'`), `{provide: LOCALE_ID, useFactory: detectLocale}`, `registerLocaleData(localeDe)`, and an app initializer that awaits `transloco.load(lang)` (no flash of keys) and sets `document.documentElement.lang`.
  - Wire `provideI18n()` into `app.config.ts`.
- Translation files: `public/i18n/en.json` and `public/i18n/de.json`, with dotted feature-scoped keys (e.g. `challengeDetail.closeScoring.title`). Params use `{{name}}`, never string concatenation (this fixes `'Open ' + a + ' vs ' + b` and `'Pick ' + color.name`).
- Title: a custom `TitleStrategy` that translates route `title` keys, plus `app.title`. Change `title:` strings in `app.routes.ts` to keys.
- `index.html`: `<title>` gets a proper name; `lang` is set at runtime.

**Conversion patterns** (follow the existing standalone/signal style from `frontend/AGENTS.md`)
- Templates: `{{ 'key' | transloco }}`, `[attr.aria-label]="'key' | transloco: {…}"`, and the same for `placeholder`, `matTooltip` and `alt`.
- TS: inject `TranslocoService` and call `translate()` for snackbars, confirm-dialog data, and Signal Forms `message:` values. The language is fixed after startup, so translating at creation time is safe.
- `NotificationService`: translate the "Dismiss" label.
- `api-error.ts`: map `code` → `errors.<CODE>`, falling back to `errors.unknown`. Add the missing codes (`DUPLICATE_SUBMISSION`, `INVALID_ARGUMENT`, `INTERNAL_ERROR`, `UNKNOWN_ERROR`) after checking `GlobalExceptionHandler`. Stop surfacing server `message` text, and `error.message` in the 9 snackbar call sites.
- `PublicRegistrationResult.message` is server-rendered English: the frontend renders its own text from the existing `joined` boolean and ignores `message`.
- Shared helpers to remove duplication: one category-label source (pipe or `categoryLabel` util) replacing the copies in `blind-scoring.ts` and `results-table.ts`, keyed `category.MUNDGEFUEHL|TELLERSPRACHE|GESCHMACK` (EN: Mouthfeel / Plating / Taste, to be reviewed by the user), `status-tag.ts` LABELS as keys, and a `plateColorName` pipe keyed by the color's stable id/code from `/config`, falling back to the server name for unknown colors.
- Dates: existing `| date: 'mediumDate'` follows `LOCALE_ID` automatically.
- Material built-ins: grep for paginator/datepicker/stepper. If any are used, provide localized `*Intl` providers. Otherwise nothing to do.
- Locale sync with the backend: after login / access-link login (in `core/auth/auth.ts`), unconditionally call `PUT /me/locale` with `detectLocale()` (idempotent; the frontend has no stored account locale to compare against, since login returns only a token). Registration (`public-registration`) and admin create/edit dialogs send `locale`. The admin create dialog defaults to `detectLocale()` and lets the admin choose it for the new account.

**Conversion order** (each step keeps the app green): shell/nav/not-found → shared components (confirm-dialog, empty/error state, page-header, status-tag, qr) → auth (organizer login, reset password, link expired, public registration) → participant home/challenge card/rivalry → challenges (list, detail, dialogs, qr-dialog) → scoring (blind-scoring, results-table) → accounts admin.

**Frontend tests** (Vitest + jsdom)
- `src/app/testing/i18n.ts`: `provideTestI18n()` using `TranslocoTestingModule` with the real `en.json`/`de.json` preloaded. Existing specs that assert English text (~100 assertions) then keep passing; add the provider to their TestBed setups.
- New: `locale.spec.ts` (`de`, `de-AT`, `fr`, empty list, ordering like `['fr','de']` → `de`), an `en.json`/`de.json` parity test (same keys, no empty values, same `{{params}}`), an error-code mapping test, and one representative component rendered in German (also through the existing axe check).

**Docs**: add `docs/frontend/06-i18n.md` (how to add a key, the parity rule, no string concatenation, no server messages in the UI) and link it from `frontend/CLAUDE.md`. Add a short "mail localization" note to the backend docs.

## Critical files
- `openapi/cookingchallenge-api.yaml`
- Backend: `auth/domain/model/Account.java`, `auth/infrastructure/persistence/{entity/AccountJpaEntity,mapper/AccountMapper}`, `resources/db/changelog/auth/007-account-locale.yaml` + master, `shared/config/MailConfig.java`, `shared/mail/{MailDispatcher,MailRequest}`, `cookoff/infrastructure/notification/EmailNotificationAdapter`, `auth/infrastructure/notification/EmailPasswordResetNotificationAdapter`, `resources/templates/mail/*`, `resources/mail/messages*.properties`
- Frontend: `src/app/app.config.ts`, `app.routes.ts`, `core/i18n/*` (new), `core/errors/api-error.ts`, `core/notifications/notification.ts`, `core/auth/auth.ts`, all feature templates and components listed above, `public/i18n/{en,de}.json`

## Verification
1. `npm run generate:api`, then `npm run lint`, `npm test`, `npm run build` (frontend) and the backend Gradle `test` task. The IntelliJ MCP failed to connect this session, so tests fall back to the shell if it is still down.
2. Manual UI check (`npm start` with the backend on 8080), via Claude in Chrome: run with browser language `de-AT` → German text, German dates, `<html lang="de">`, German page titles, German validation and error messages. Repeat with `en-US` and with `fr-FR` (English fallback). Check aria-labels via the accessibility tree.
3. Manual mail check: set `app.mail.enabled=true` (Mailpit via `backend/compose.yaml`), register through a QR invite in a German browser, send invitations, reveal a challenge, trigger an admin password reset, and confirm German subject and body (HTML and text) in Mailpit. Confirm an `en` account still gets English mail.
4. Login refresh: log in with a German browser as an existing English account and confirm `accounts.locale` becomes `de`.

## As built (deviations and additions)

- **Server-rendered English text is not shown.** `RivalrySummary.headline`, `RivalryChallengeSummary.outcomeLabel`
  and `PublicRegistrationResult.message` are built from data the responses already carry, so the UI builds
  them itself in the user's language (`core/i18n/rivalry-text.ts`, and the `joined` flag for registration). The
  backend fields are unchanged. Alternative if ever wanted: resolve these in the backend from `Accept-Language`.
- **Error mapping happens in the interceptor.** `toApiError(httpError, translate)` puts the translated text in
  `ApiError.message` (original in `serverMessage`), so the ~30 existing `error.message` call sites needed no change.
- **`GlobalExceptionHandler` maps `HttpMessageNotReadableException` to 400 `VALIDATION_ERROR`.** Needed so an
  unsupported `locale` (or any malformed body) returns the 400 the spec declares instead of a 500. This is an
  existing gap that also affected every other endpoint.
- **`PUT /api/v1/me/locale` uses the `Auth` tag** (so it generates into `AuthController` in the `auth` module).
- **Also localized:** Material paginator labels (`TranslatedPaginatorIntl`), route titles (`TranslatedTitleStrategy`),
  role names, plate color names (`plateColorName`), and the account language select in the admin dialog.
- **Category names in English:** Mouthfeel / Plating / Taste (German unchanged), for review.
- **Docs:** `docs/frontend/06-i18n.md`, `docs/backend/04-mail-localization.md`.

