# Link-Login & QR-Registration Test Plan

## Status

**Implemented (2026-08-12).** Steps 1, 2, and 5 shipped as described below. Step 3
(`CapturingNotificationPort`) shipped but is only wired into `AccessLinkLoginFlowIntegrationTest`
so far — not yet used by any other test. Step 4 (the curl runbook) is documentation-only, no
code to ship; it's captured in this file as-is.

**Files added:**
- `backend/src/test/java/at/fraihs/cookoff/shared/testsupport/GuestOnboardingTestSupport.java`
- `backend/src/test/java/at/fraihs/cookoff/shared/testsupport/CapturingNotificationPort.java`
- `backend/src/test/java/at/fraihs/cookoff/shared/testsupport/CapturingNotificationPortConfig.java`
- `backend/src/test/java/at/fraihs/cookoff/shared/security/AccessLinkLoginFlowIntegrationTest.java`
- `backend/src/test/java/at/fraihs/cookoff/shared/security/QrRegistrationFlowIntegrationTest.java`

**Files changed:**
- `SecurityIntegrationTest` — refactored to call the shared helpers instead of its own private
  `exchangeAccessLinkForJwt`/`extractAccessToken` methods (deduped).
- `AccessLinkServiceTest`, `RegistrationInviteServiceTest` — added the exact-expiry-instant
  boundary test each.

All new/changed tests pass; full `./gradlew test` is green.

## Goal

Make the two guest-onboarding flows — **access-link login** ("link login") and **QR
self-registration** — fully exercisable by an automated test suite, and by a human/agent doing
a quick manual check, **without ever rendering a QR code image or clicking a real link in a
browser/email client.**

The key insight both flows share: at the protocol level each is just *"mint an opaque token
server-side, then redeem it over HTTP."* The QR image and the "email" are purely presentational
side-effects on top of that token. The backend already hands back the raw token as plain JSON
(QR case) or logs it via a swappable port (link case) — neither requires an image renderer, a
mailbox, or a browser to test.

## Current state

### Access-link login (`POST /api/v1/auth/access-link-login`)
- `AccessLinkService.issue(accountId, challengeId, validFor)` mints a `SecureRandom`
  Base64url token and saves it — no email involved at this layer.
- `SendChallengeInvitationsService` (the *real* production trigger, `POST
  /api/v1/challenges/{id}/invitations`, operationId `sendInvitations`) calls
  `AccessLinkService.issue(...)` per target,
  then hands the resulting URL to `NotificationPort.sendAccessLink(email, link)`.
- The **only** `NotificationPort` implementation is `LoggingNotificationAdapter`
  (`backend/.../infrastructure/notification/LoggingNotificationAdapter.java`) — a stub that
  just `log.info`s the link. No real email is ever sent, in any environment, today.
- `SecurityIntegrationTest` already has a working "no email" pattern:
  `issueLinkTokenForNewGuest()` calls `AccessLinkService.issue(...)` directly, then
  `exchangeAccessLinkForJwt()` POSTs the raw token to `/api/v1/auth/access-link-login` via
  MockMvc and extracts the JWT. This is exactly the pattern to generalize — it's just private
  to one test class today, and it bypasses `SendChallengeInvitationsService` entirely, so the
  actual `POST /send-invitations` HTTP path is never exercised end-to-end.

### QR self-registration (`POST /api/v1/challenges/{id}/registration-invites` → `POST /api/v1/public/registrations`)
- `CreateRegistrationInviteService` returns `RegistrationInviteRestDto(token, registrationUrl)`
  **directly in the HTTP response** to the organizer's authenticated call — the raw token is
  plain JSON, no image involved on the backend at all.
- The QR image is a pure frontend concern: `app-qr-code`
  (`frontend/.../shared/components/qr-code/qr-code.ts`) calls `QRCode.toCanvas(canvas, value,
  ...)` from the `qrcode` npm package to draw whatever `registrationUrl` it's given. The
  backend never generates, stores, or needs an image.
- `PublicRegistrationService` / `PublicController` (`POST /api/v1/public/registrations`,
  unauthenticated) redeems a raw token string — `RegistrationInviteService.verify(token)` — and
  creates the account (+ joins the challenge if still `OPEN`).
- Unit coverage already exists and is solid but fully mocked:
  `CreateRegistrationInviteServiceTest`, `RegistrationInviteServiceTest`,
  `RegistrationInvitesServiceTest`, `PublicRegistrationServiceTest`,
  `RegistrationInviteRepositoryImplTest`. None of them go over real HTTP, and none chain
  "organizer creates invite → guest redeems it" as one flow.

### The gap
No test (and no documented manual recipe) currently drives either flow **end-to-end over real
HTTP** the way a browser actually would — issue → redeem → use the result — while still
avoiding image rendering and real email delivery. That's what this plan adds.

## Plan

### 1. Backend: reusable HTTP-level test helpers

Extract a small shared test-support class,
`backend/src/test/java/at/fraihs/cookoff/shared/testsupport/GuestOnboardingTestSupport.java`
(plain static helpers taking `MockMvc`/`ObjectMapper` + whatever services are needed), covering:

- `issueAccessLinkToken(AccessLinkService, AccountId guest, long challengeId, Duration validFor)`
  → raw token, direct call (fastest path, use when the login exchange itself is what's under test).
- `exchangeAccessLinkForJwt(MockMvc, ObjectMapper, String token)` → POSTs
  `/api/v1/auth/access-link-login`, returns the JWT string.
- `createRegistrationInviteToken(MockMvc, ObjectMapper, String organizerJwt, String challengeId)`
  → POSTs `/api/v1/challenges/{id}/registration-invites`, extracts `.data.token` from the JSON
  body (never touches `registrationUrl` / no QR rendering).
- `selfRegisterViaInvite(MockMvc, ObjectMapper, String token, String firstName, String lastName, String email)`
  → POSTs `/api/v1/public/registrations`, returns the parsed
  `PublicRegistrationResultRestDto`.

Move `SecurityIntegrationTest`'s existing private `issueLinkTokenForNewGuest()` /
`exchangeAccessLinkForJwt()` there and have it call the shared helpers, so there's one
canonical "no email, no QR" pattern instead of one test class's private methods.

### 2. New end-to-end integration tests (`@SpringBootTest` + `MockMvc`, real transactions)

**`AccessLinkLoginFlowIntegrationTest`**
- Happy path: issue → exchange → use the JWT on a protected guest endpoint (`GET
  /api/v1/me/home`). (Largely already covered in `SecurityIntegrationTest`; relocate or keep,
  just dedupe via the shared helper.)
- Full production path: call `POST /api/v1/challenges/{id}/send-invitations` for real (see
  step 3 for how to capture the token this generates), then exchange + use — this is currently
  untested end-to-end since existing tests skip straight to `AccessLinkService.issue(...)`.
- Invalid / expired token → `401 INVALID_OR_EXPIRED_LINK` (already covered, keep).

**`QrRegistrationFlowIntegrationTest`**
- Happy path: organizer creates an `OPEN` challenge → `POST .../registration-invites` →
  extract `token` from JSON → `POST /api/v1/public/registrations` → assert `200`, account
  persisted, account added as a guest on the challenge.
- Challenge no longer `OPEN` at redemption time → account still created, `joined: false`.
- Redeeming with an email that already exists → `409`/`AccountAlreadyExistsException` mapping.
- Invalid/expired invite token → `401`.
- Non-organizer / non-owner calling `createRegistrationInvite` → `403` (unit-covered already;
  optional to duplicate at HTTP level).

### 3. Capture the "sent" link without sending it

Add a test-only fake `NotificationPort` (e.g. `@TestConfiguration`-provided `@Primary` bean
overriding `LoggingNotificationAdapter` in the relevant `@SpringBootTest`) that stores the last
`(email, link)` pair in memory instead of just logging it:

```java
@TestConfiguration
class CapturingNotificationPortConfig {
    @Bean @Primary
    NotificationPort capturingNotificationPort() {
        return new CapturingNotificationPort(); // records sendAccessLink(email, link) calls
    }
}
```

The test then calls the real `POST /send-invitations` endpoint, pulls the captured `link`
string, and parses out the `token=` query parameter — proving the *actual* production code path
(`SendChallengeInvitationsService` → `NotificationPort`) works, not just `AccessLinkService`
in isolation. This step only applies to the access-link flow; QR registration needs nothing
equivalent since the invite token is already returned as JSON, not routed through
`NotificationPort` at all.

### 4. Manual / agent verification recipe (curl only, no browser, no test framework)

Worth documenting as a short runbook (either appended to this plan or as a
`docs/backend/`-side note) for quickly sanity-checking either flow against a locally running
instance:

```bash
# Setup: organizer login, create a challenge -> $CHALLENGE_ID
ORG_JWT=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' -d '{"email":"...","password":"..."}' | jq -r .data.accessToken)

# --- QR self-registration ---
TOKEN=$(curl -s -X POST localhost:8080/api/v1/challenges/$CHALLENGE_ID/registration-invites \
  -H "Authorization: Bearer $ORG_JWT" | jq -r .data.token)
curl -s -X POST localhost:8080/api/v1/public/registrations \
  -H 'Content-Type: application/json' \
  -d "{\"token\":\"$TOKEN\",\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"a@b.com\"}"
# -> expect 200, joined:true

# --- Access-link login ---
curl -s -X POST localhost:8080/api/v1/challenges/$CHALLENGE_ID/invitations \
  -H "Authorization: Bearer $ORG_JWT" -H 'Content-Type: application/json' -d '{}'
# LINK_TOKEN isn't in this response (it only goes to LoggingNotificationAdapter) —
# grab it from the app log line "Access link for ...: http://.../home?token=..."
LINK_TOKEN="<paste from log>"
GUEST_JWT=$(curl -s -X POST localhost:8080/api/v1/auth/access-link-login \
  -H 'Content-Type: application/json' -d "{\"token\":\"$LINK_TOKEN\"}" | jq -r .data.accessToken)
curl -s localhost:8080/api/v1/me/home -H "Authorization: Bearer $GUEST_JWT"
# -> expect 200
```

This is the practical fallback whenever a full integration test is overkill — e.g. Claude
verifying a change by hand — and it never touches a QR renderer or a real mailbox.

### 5. Two extra regression-guard cases worth adding while in this code

- **Expiry boundary**: `AccessLink.isExpired`/`RegistrationInvite.isExpired` both use `!now.isBefore(expiresAt)` — a token expiring *exactly now* already counts as expired. Worth an explicit boundary-value unit test on both records (currently only "clearly expired" and "clearly valid" are tested).
- **Reusability is intentional, not accidental**: both tokens are explicitly multi-use — `AccessLink.verify()`/`RegistrationInviteService.verify()` can be called repeatedly (a link "serves the same guest across multiple open challenges"; a QR code "serves many walk-ins"). `AccessLink` carries an unused `usedAt`/`markUsed(Instant)` that no code path ever calls — if the plan's new integration tests touch this record, add one assertion that `usedAt` stays `null` after a successful exchange, so a future "make it single-use" change doesn't silently break the current multi-use guarantee without a test failing.

### 6. Frontend manual verification (click-through, no QR scan, no real email)

The frontend already unit-tests both flows the same way the backend does — by substituting the
token/JSON layer instead of the presentation layer (`participant-home.spec.ts` mocks
`Auth.accessLinkLogin` directly; `public-registration.spec.ts` mocks `PublicApi.registerPublicly`
directly — neither ever touches a QR image or triggers a real network call). What's missing is a
recipe for checking the two flows by hand against the real running app — still without scanning
an actual QR code or receiving an actual email, by reading the token straight out of the network
response / server log instead.

**Setup** (one-time per session):
```bash
cd backend && ./gradlew bootRun   # auto-starts Postgres via compose.yaml (spring-boot-docker-compose)
cd frontend && npm start          # ng serve --proxy-config proxy.conf.json, proxies /api -> :8080
```
Log in at `http://localhost:4200/login` with an existing organizer/admin account.

**QR self-registration** (`/challenges/:id` → **Registration QR code** button, opens
`QrDialog`, `challenge-detail.html:82-85`):
1. Open a challenge's detail page, click **Registration QR code**. This fires `POST
   /api/v1/challenges/{id}/registration-invites` and renders the response as a QR canvas — no
   scanner needed, just read the request instead: open DevTools → Network → the
   `registration-invites` request → Response tab → copy `data.registrationUrl` (or `data.token`
   and build the URL yourself: `http://localhost:4200/register?token=<token>`).
2. Paste that URL into a new tab (or the same tab's address bar) instead of scanning it.
   Lands on `PublicRegistration` (`features/register/public-registration/public-registration.ts`),
   fill the form, submit.
3. Confirm the server message renders verbatim and, back on the challenge detail page (refresh),
   the new account shows up as a guest.

**Access-link login** (`/challenges/:id` → **Send links** button, opens `SendLinksDialog`):
1. Click **Send links** (targets guests who haven't submitted yet, or pick specific
   guests/cooks in the dialog — e.g. the account you just self-registered via QR above). This
   fires `POST /api/v1/challenges/{id}/invitations`.
2. The link never appears in the UI or the HTTP response — `LoggingNotificationAdapter` only
   logs it. Read it from the backend terminal: `Access link for <email>: http://localhost:4200
   /home?token=<token>`.
3. Paste that URL into a new tab instead of clicking an email link. Lands on `ParticipantHome`
   (`features/home/participant-home/participant-home.ts`), which exchanges the token for a JWT,
   strips the query param, and loads `GET /me/home` — confirm the guest's open challenges render.

### Non-goals

- Testing actual QR pixel/image rendering — that's `qr-code.spec.ts` / `qr-dialog.spec.ts`'s
  job on the frontend (asserting the `value` input reaches `QRCode.toCanvas`, not decoding
  pixels).
- Testing real email delivery/SMTP — there is no real provider wired up yet
  (`LoggingNotificationAdapter` is a stub); out of scope until that's built.
