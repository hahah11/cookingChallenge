# Admin-Initiated Password Reset Plan

## Status

**Implemented (2026-09-22), not yet verified live.** Plan 1 of 3 from the 2026-09-22 feature
request, built on the `_layout.html` from [`mail-brand-palette-plan.md`](mail-brand-palette-plan.md).
Backend `./gradlew test` passes (384 tests). Frontend `ng test` has only the known
`error-interceptor.spec.ts` failure from before this work, and `ng lint` is clean. The manual walkthrough under Verification has not been run: there was no
Docker/Postgres on the machine at the time.

Deviations from the text below:
- **Generated client is not committed.** `src/app/core/api/generated/` is gitignored and rebuilt
  by the `prestart`/`prebuild`/`pretest` hooks, so the "commit the generated diff" step does not apply.
- **Port `claim` returns `Optional<PasswordResetToken>`**, not an `int`. The JPA repository still
  does the conditional UPDATE; the adapter then re-reads the claimed row, so the service never
  handles a raw row count.
- **`/link-expired` gained a `kind=reset` variant.** The `link` copy tells the reader to ask the
  organizer to resend a cook-off link, which is wrong for a reset link. The reset page routes to
  `/link-expired?kind=reset`.
- **`errorInterceptor` interplay.** Any 401 makes the interceptor log out and redirect to
  `/link-expired?kind=link`. On a dead reset token the page then navigates to `kind=reset`, which
  supersedes that navigation. Unit tests do not cover this path; confirm it in the live check.
- **Security and end-to-end coverage live in a new `PasswordResetFlowIntegrationTest`** instead of
  `SecurityIntegrationTest`. It mocks `PasswordResetNotificationPort` to recover the link, then
  drives trigger → redeem → login over HTTP. It covers ADMIN 202, ORGANIZER 403, anonymous 401,
  guest 409, single use, supersede, and redeem reachable with no `Authorization` header.
- **Organizers never see the button.** `/accounts` is already behind `adminGuard`, so the
  per-row guard only has to hide the button for guest (USER-only) rows.
- **Error codes.** `PASSWORD_RESET_NOT_ELIGIBLE` was added to the frontend `ApiErrorCode` union; a
  refused reset shows the server message in a snackbar.

## Context

Only ADMIN and ORGANIZER accounts hold a password; guests are `USER` accounts with
`passwordHash == null` who log in exclusively through access links. Today there is **no way to
recover a forgotten organizer password** — an admin would have to go into the database. The
`email-notifications-plan.md` follow-up list names this explicitly: *"Welcome mail on account
creation and password reset are not implemented. Both live in `auth`; `shared/mail` is already
placed so they can reuse it without crossing a module boundary."*

## Decisions (confirmed with the user)

| Question | Decision |
|---|---|
| Who may trigger | **ADMIN only** — an organizer must not be able to take over an admin account |
| Valid targets | Accounts that actually hold a password; `USER`-only accounts are rejected |
| Token lifetime | **2 hours**, strictly **single-use** |
| Self-service "forgot password?" | **No** — no anonymous endpoint that accepts an email address, so no user-enumeration surface and no rate limiting to build |
| Password rule | Minimum **8 characters**, no composition rules |

## Endpoints

Both names follow the established hyphenated action sub-resource style
(`/challenges/{id}/color-pick`, `/reveal`, `/registration-invites`):

| | Trigger | Redeem |
|---|---|---|
| Path | `POST /api/v1/accounts/{accountId}/password-reset` | `POST /api/v1/auth/password-reset` |
| Tag | Accounts | Auth |
| Security | `bearerAuth`, ADMIN | `security: []` (token in body) |
| Success | `202 Accepted`, no body | `204 No Content` |
| Errors | 403, 404, **409 `PASSWORD_RESET_NOT_ELIGIBLE`** | 400 validation, **401 `INVALID_OR_EXPIRED_LINK`** |

Splitting issue and redeem across two tags mirrors the existing access-link pair exactly
(`AccessLinkService.issue` in `auth`, called from `cookoff`; `AccessLinkLoginService` redeeming
under `/auth`). `202` rather than `200` because the mail send is async and best-effort — nothing
meaningful exists to return, and the account resource itself has not changed.

Request schema — **`token` and `newPassword` only**:

```yaml
PasswordResetRedeemRequest:
  type: object
  required: [token, newPassword]
  properties:
    token: { type: string }
    newPassword: { type: string, minLength: 8 }
```

> **Deliberate deviation.** A `confirmPassword` field was considered for the wire contract and
> rejected. Confirmation is a UI affordance against typos — the server gains nothing by
> re-checking it, since if the two differ it cannot know which was intended and would just be
> refusing a request it could otherwise satisfy. It would also add a 400 path whose error code
> (`INVALID_ARGUMENT`, via `GlobalExceptionHandler.handleIllegalArgument`) differs from the
> `VALIDATION_ERROR` the generated Bean Validation path produces, for no benefit. The second
> field lives purely in the Angular form.

`minLength: 8` in the schema means the generator annotates the DTO with `@Size(min = 8)`, which
fires through the controller's `@Valid @RequestBody` into the existing `MethodArgumentNotValidException`
→ 400 `VALIDATION_ERROR` handler. No new error wiring for the length rule.

## Liquibase

`backend/src/main/resources/db/changelog/auth/006-password-reset-tokens.yaml`, changeset
`006-01-create-password-reset-tokens`, author `fraihs` — the `access_links` shape minus
`challenge_id`:

`id` BIGINT PK (TSID) · `account_id` BIGINT NOT NULL · `token` VARCHAR(255) NOT NULL UNIQUE ·
`expires_at` TIMESTAMP NOT NULL · `used_at` TIMESTAMP NULL · `created_at` TIMESTAMP NOT NULL
`defaultValueComputed: CURRENT_TIMESTAMP`, plus `addForeignKeyConstraint`
`fk_password_reset_tokens_account` → `accounts(id)` `onDelete: CASCADE`. Append the `- include:`
to `db.changelog-master.yaml` after the `005` entry.

## Single-use must be enforced in the database, not in Java

This is the one place where copying `AccessLinkService` would produce a real bug. Access links
are **deliberately not single-use**, so no existing code in this repo solves the claim race.

A naive `find → validate → save(markUsed)` inside one `@Transactional` method is not safe at
READ_COMMITTED: two concurrent submits of the same token both read `used_at IS NULL`, both write
a password hash, and both report success — with the surviving password decided by commit order.

So the token is **claimed atomically before the account is touched**:

```java
// PasswordResetTokenJpaRepository
@Modifying
@Query("update PasswordResetTokenJpaEntity t set t.usedAt = :now "
     + "where t.token = :token and t.usedAt is null and t.expiresAt > :now")
int claim(@Param("token") String token, @Param("now") Instant now);
```

`claim` returning `0` means unknown, expired, or already claimed — all three throw
`InvalidOrExpiredLinkException`. Only a return of `1` proceeds to change the password. Expiry is
folded into the same `WHERE` so there is no second read to disagree with it.

## Backend files

All in `auth` — the module must never depend on `cookoff`.

| File | Role |
|---|---|
| `auth/application/dto/PasswordResetToken.java` | Record `(id, accountId, token, expiresAt, usedAt, createdAt)` with `isExpired`/`isUsed`; sibling of `AccessLink` |
| `auth/application/port/PasswordResetTokenRepository.java` | `save`, `findByToken`, `claim(token, now)`, `deleteAllByAccountId` |
| `auth/infrastructure/passwordreset/{entity/PasswordResetTokenJpaEntity, PasswordResetTokenJpaRepository, PasswordResetTokenRepositoryImpl}.java` | Mirrors `infrastructure/accesslink/`; impl package-private |
| `auth/application/port/PasswordResetNotificationPort.java` | `sendPasswordReset(PasswordResetNotification)` |
| `auth/application/dto/PasswordResetNotification.java` | Record `(Email recipient, String firstName, String link)` |
| `auth/infrastructure/notification/EmailPasswordResetNotificationAdapter.java` | `@ConditionalOnProperty(app.mail.enabled=true)`; publishes `shared.mail.MailRequest` |
| `auth/infrastructure/notification/LoggingPasswordResetNotificationAdapter.java` | `havingValue="false", matchIfMissing=true`; logs the link |
| `auth/application/service/PasswordResetService.java` | Issues: eligibility check, supersede, persist, notify |
| `auth/application/service/PasswordResetRedeemService.java` | Redeems: `claim` then re-hash |
| `auth/application/exception/PasswordResetNotEligibleException.java` | → 409 |
| `templates/mail/password-reset.{html,txt}` | Built on the `_layout.html` from `mail-brand-palette-plan.md` |

**Changed:** `AccountsController` (+`triggerPasswordReset`), `AuthController` (+`redeemPasswordReset`),
`GlobalExceptionHandler` (+409 `PASSWORD_RESET_NOT_ELIGIBLE`), `SecurityConfig` (two matchers),
`openapi/cookingchallenge-api.yaml`, `db.changelog-master.yaml`.

The auth module gets its **own** notification port and adapter pair rather than reusing
`cookoff.application.port.NotificationPort`. That duplicates a small `@ConditionalOnProperty`
pattern, but the alternative is `auth` depending on `cookoff`, which `ApplicationModules.verify()`
would reject. Everything expensive — `MailDispatcher`, `MailTransport`, `MailConfig` — is shared.

### Eligibility

Check `account.getPasswordHash() == null`, **not** the role set. The two should always agree, but
the hash is the thing that actually matters, so it cannot disagree with reality if a future bug
produces an ORGANIZER without a password.

### Security matchers

```java
.requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/password-reset").hasRole("ADMIN")
.requestMatchers(HttpMethod.POST, "/api/v1/auth/password-reset").permitAll()
```

Note `SecurityConfig` ends in `.anyRequest().denyAll()` — a new endpoint without a matcher returns
403 rather than falling open. Keep the ADMIN line with `POST /api/v1/accounts` and away from the
`hasAnyRole("ORGANIZER","ADMIN")` block just below it.

## Edge cases

| Case | Behaviour |
|---|---|
| Target is a guest/`USER` | 409 `PASSWORD_RESET_NOT_ELIGIBLE`. The button is also hidden for those rows — defense in depth, not the only guard |
| Unknown account | 404 via existing `AccountNotFoundException` |
| Unknown / expired / already-used token | All three → 401 `INVALID_OR_EXPIRED_LINK`, deliberately indistinguishable (matches the `InvalidCredentialsException` anti-enumeration posture) and handled by the frontend's existing single error branch |
| Concurrent double-submit | The atomic `claim` above; the loser gets 401 |
| Second reset issued before the first is used | `deleteAllByAccountId` runs before issuing, so the older link dies immediately. The confirm dialog says so |
| `app.mail.enabled=false` (the default) | Token is still issued and persisted; the logging adapter prints the link at INFO instead of mailing it — same as access links today |

### Known limitation: existing sessions survive a reset

JWTs here are self-contained and stateless — `JwtIssuer` embeds only `sub`/`roles`/`iat`/`exp`,
and there is no revocation list. **Resetting a password does not log out anyone already holding a
bearer token for that account**; an organizer JWT lives up to 12h (`app.jwt.expiration`).

That is fine for the stated use case (someone forgot their password). It is *not* sufficient if
the reset is a response to a compromised account. Closing it means a `passwordChangedAt` column
checked against the JWT's `iat` on every authenticated request — a DB read or cache per request,
and a real change to the stateless model. **Out of scope here**; raised so the choice is
deliberate rather than accidental.

## Frontend

**Row action** — `features/accounts/accounts-admin/accounts-admin.{ts,html}`. A second
`mat-button` next to the existing Edit button, guarded by
`account.roles.includes('ADMIN') || account.roles.includes('ORGANIZER')`. No `MatMenuModule` —
the app has no menu component anywhere yet, and a second inline button matches the row's existing
shape. Wiring copies `confirmReveal`/`reveal` in
`features/challenges/challenge-detail/challenge-detail.ts`: open the reusable `ConfirmDialog`
(`shared/components/confirm-dialog/`), and on `true` call
`accountsApi.triggerPasswordReset(id)`. Dialog message uses the derived `account.name` field and
must mention that any earlier reset link stops working.

This is the **first use of `Notification.success()`** in the app — appropriate, because unlike
reveal/unreveal nothing on the page changes, so the snackbar carries all the feedback.

**New route** — `{ path: 'reset-password', title: 'Reset password', loadComponent: ... }` in
`app.routes.ts`, top-level and unguarded, beside `register` and `link-expired`.

**New component** — `features/auth/reset-password/reset-password.{ts,html,scss}`, modeled on
`features/register/public-registration/public-registration.ts`. `withComponentInputBinding()` is
already configured, so the token binds with no `ActivatedRoute` boilerplate:

```ts
readonly token = input<string>();
```

Signal Forms (`@angular/forms/signals` — **not** `ReactiveFormsModule`). `minLength` and
`validate` are both confirmed present in the installed package. No cross-field validator exists
anywhere in the codebase to copy, so:

```ts
protected readonly model = signal({ newPassword: '', confirmPassword: '' });
protected readonly resetForm = form(this.model, (path) => {
  required(path.newPassword, { message: 'New password is required.' });
  minLength(path.newPassword, 8, { message: 'Use at least 8 characters.' });
  required(path.confirmPassword, { message: 'Please confirm your new password.' });
  validate(path.confirmPassword, ({ value, valueOf }) =>
    value() !== valueOf(path.newPassword)
      ? { kind: 'passwordMismatch', message: 'Passwords do not match.' }
      : undefined);
});
```

Attach the match check to `confirmPassword`, not `newPassword` — that is where the user is
looking when it fails. Only `newPassword` goes on the wire.

On success `router.navigateByUrl('/login')`. On `INVALID_OR_EXPIRED_LINK`, or when the route
carries no token at all, route to `/link-expired` with `kind: 'link'` — `ParticipantHome` (the
other mailed-personal-link landing page) does exactly this, whereas `PublicRegistration` renders
inline because a QR code is shared rather than personal. This page belongs with the former.

**Generated client** — run `npm run generate:api` and commit the
`frontend/src/app/core/api/generated/` diff in the same change; `generate:api:check` fails CI on
drift. Never hand-edit those files.

## Tests

**Backend** — `PasswordResetServiceTest` (issues + supersedes; rejects a passwordless account;
404 on unknown; captures the notification's link), `PasswordResetRedeemServiceTest` (happy path
re-hashes and claims; a `claim` returning 0 throws `InvalidOrExpiredLinkException` — the test that
stands in for the race), `PasswordResetTokenRepositoryImplTest` (`@DataJpaTest` on H2:
round-trip, token uniqueness, `claim` returns 1 then 0 on a second call, `claim` returns 0 past
`expires_at`, `deleteAllByAccountId` scoping), controller slices in `AccountsControllerTest`
(202/404/409) and `AuthControllerTest` (204/401/400-too-short), `SecurityIntegrationTest`
(ADMIN 202, ORGANIZER 403, anonymous 401, and redeem reachable with no `Authorization` header),
and a `password-reset` case in `MailTemplateRenderingTest`.

**Frontend (Vitest)** — `accounts-admin.spec.ts`: button present for ADMIN/ORGANIZER rows and
absent for USER; confirming calls the API and shows the success snackbar; dismissing calls
nothing. New `reset-password.spec.ts`: mismatch error under `confirmPassword`; under-8 error;
successful submit posts `{token, newPassword}` and navigates to `/login`;
`INVALID_OR_EXPIRED_LINK` routes to `/link-expired`; a missing token never calls the API; plus
`expectNoAxeViolations`.

## Verification

```bash
cd backend && ./gradlew test
cd ../frontend && npm run generate:api:check && npm test
```

Then end to end, with `APP_MAIL_ENABLED=true` so Mailpit actually receives the mail:

1. Log in as the seeded admin (`005-add-claude-user.yaml`), open `/accounts`.
2. Confirm "Reset password" shows on an ORGANIZER row and is absent on a guest row.
3. Trigger it; read the mail at <http://localhost:8025> (or lift the link from the log with mail
   disabled) and open it.
4. Mismatched passwords → inline error, no request sent. Under 8 characters → length error.
5. A valid pair → redirect to `/login`; the new password works and the old one returns
   `INVALID_CREDENTIALS`.
6. Reopen the same link → `/link-expired`, proving single-use.
7. Trigger a second reset, then open the *first* link → also rejected, proving supersede.
8. `curl` the trigger endpoint with an ORGANIZER JWT → 403; against a guest account as ADMIN → 409.

## Follow-ups (not in scope)

- `CreateAccountRequest.password` has no `minLength` in the spec, so `POST /accounts` still
  accepts a one-character password while this flow demands eight. Worth aligning separately.
- Session invalidation on password change, as described above.
