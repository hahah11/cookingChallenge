# Access-Link JWT Unification Plan

## Status

**Planned (2026-08-04), not yet implemented.**

## Goal

Today there are two independent authentication mechanisms (see
`backend/src/main/java/at/fraihs/cookoff/shared/config/SecurityConfig.java`'s javadoc):

1. **JWT** (`Authorization: Bearer ...`) — issued by `POST /api/v1/auth/login` (password),
   validated by Spring's OAuth2 resource server.
2. **Access-link token** — a raw opaque token sent as `?token=...` on every request to a
   fixed set of guest endpoints, re-verified per request against the `access_links` table by
   `AccessLinkAuthenticationFilter`, which manually populates the `SecurityContext`.

This plan unifies both onto JWT: using an access link becomes a one-time **exchange** (link
token → JWT), after which a guest authenticates exactly like an organizer
(`Authorization: Bearer`) for the rest of the session. The per-request query-param filter is
removed entirely — single mechanism, per the user's explicit goal.

Frontend is currently an empty `ng new` skeleton with no auth code at all (confirmed by
exploration 2026-08-04) — this plan is backend-only. The frontend work (calling the new
endpoint on `?token=` landing, storing the JWT, dropping query-param auth) is future work once
the Angular app has an actual home/auth feature to build it into.

## Design decisions (confirmed with user)

- **New endpoint**: `POST /api/v1/auth/access-link-login`, mirroring `POST /api/v1/auth/login`
  — public (`security: []`), request `{ token }`, response the same `AuthTokenResponse` shape.
- **Token lifetime**: the issued JWT expires at `min(accessLink.expiresAt(), now + guest cap)`
  — tied to the access link's own expiry (currently 30 days, `SendChallengeInvitationsService.
  LINK_VALIDITY`) but capped (`app.jwt.guest-expiration-cap`, default `P1D`) since an
  unrevocable bearer token living up to 30 days is a bigger blast radius than a DB-backed
  token re-verified every request. Organizer JWTs are unaffected (still `app.jwt.expiration`,
  default `PT12H`).
- **No hard per-challenge JWT claim.** The originally-discussed "scope JWT to one challenge"
  turned out to be redundant: every guest endpoint already authorizes via a domain-level check
  on the resolved `AccountId` against the specific challenge in the path/body —
  `Challenge.isParticipant(...)` (`GetChallengeForParticipantService`), `Challenge.canScore(...)`
  (`SubmitScoreService`), `Challenge.getCookAssignments()`-based cook check (`PickColorService`).
  This is *unaffected* by which access link the caller originally used to authenticate, and
  is in fact more correct than a hard claim would be: `GET /api/v1/me/home` is intentionally
  cross-challenge (surfaces every challenge the account participates in), so a token scoped to
  a single challenge would break it. Adding a claim would either under- or over-restrict
  relative to the existing, already-correct domain checks. Roles claim is populated the same
  way as organizer JWTs — `account.getRoles()` — since self-registered guest accounts default
  to `SystemRole.USER` with no password hash, they can never reach organizer-only endpoints
  regardless.
- **Legacy `?token=` mechanism removed entirely.** `AccessLinkAuthenticationFilter` is deleted,
  along with the `accessLinkToken` OpenAPI security scheme. Every previously
  `accessLinkToken`-secured (or dual `bearerAuth`/`accessLinkToken`) operation becomes
  `bearerAuth`-only. `CurrentAccount` simplifies to only resolve a `Jwt` principal (the
  `AccountId`-principal branch existed solely for the filter's `UsernamePasswordAuthenticationToken`).

## Backend changes

1. **`openapi/cookingchallenge-api.yaml`**
   - Add `AccessLinkLoginRequest` schema (`required: [token]`, `token: string`) next to
     `LoginRequest`.
   - Add `POST /api/v1/auth/access-link-login` path (`operationId: accessLinkLogin`, tag
     `Auth`, `security: []`, request `AccessLinkLoginRequest`, response `AuthTokenResponse`,
     `401` on invalid/expired token — reuses `InvalidOrExpiredLinkException` →
     `INVALID_OR_EXPIRED_LINK` via the existing `GlobalExceptionHandler` mapping).
   - Remove the `accessLinkToken` security scheme.
   - Switch every operation currently listing `accessLinkToken` (alone or alongside
     `bearerAuth`) to `security: [{ bearerAuth: [] }]` only: `getChallenge`,
     `pickChallengeColor`, `getChallengeImage`, `getChallengeResults`, `submitScores`,
     `getMyHome`.

2. **`JwtIssuer`** (new, `auth.application.service`) — extracts the claim-building/signing
   logic currently inline in `LoginService.execute` into a small reusable component (both
   `LoginService` and the new `AccessLinkLoginService` need it — avoids duplicating the
   `JwtClaimsSet`/`JwsHeader`/`JwtEncoder` wiring). `issueUntil(Account, Instant expiresAt):
   AuthToken`.

3. **`AccessLinkService.verify(String token)`** changes return type from `AccountId` to the
   full `AccessLink` (needed for `expiresAt` in the new service). Its only remaining caller
   after the filter is deleted is the new `AccessLinkLoginService`. Update
   `AccessLinkServiceTest` accordingly (trivial — tests already hold the `AccessLink`, just
   assert on `.accountId()`).

4. **`AccessLinkLoginService`** (new, `auth.application.service`) — `execute(AccessLinkLoginRequest)`:
   `accessLinkService.verify(token)` → `AccessLink`; load the `Account` via
   `AccountRepository.findById(link.accountId())`; compute capped expiry; delegate to
   `JwtIssuer`.

5. **`AuthController`** — add `accessLinkLogin` operation, delegating to the new service,
   wrapped in `AuthTokenResponse` (same pattern as `login`).

6. **`SecurityConfig`**
   - Delete the `AccessLinkAuthenticationFilter` bean and its `.addFilterBefore(...)` wiring.
   - Add `.requestMatchers(HttpMethod.POST, "/api/v1/auth/access-link-login").permitAll()`.
   - Change the six previously link/dual-secured `.authenticated()` rules — no change needed
     to the rule itself (`.authenticated()` already just means "any valid principal"), only
     to the fact that a JWT is now the sole way to satisfy it.
   - Update javadoc to describe the single-mechanism model.

7. **Delete** `shared/security/AccessLinkAuthenticationFilter.java`.

8. **`CurrentAccount`** — drop the `AccountId`-principal branch, keep only the `Jwt` branch
   (throw if principal isn't a `Jwt`).

9. **`application.yaml`** — add `app.jwt.guest-expiration-cap: P1D` alongside the existing
   `app.jwt.issuer`/`app.jwt.expiration`.

## Test changes

- `LoginServiceTest` — update to mock `JwtIssuer` instead of `JwtEncoder` directly (or keep
  mocking `JwtEncoder` if `JwtIssuer` is constructed inline in tests — decide at
  implementation time based on what's less brittle).
- `AccessLinkServiceTest` — update the two `verify(...)` assertions to `.accountId()`.
- New `AccessLinkLoginServiceTest` (Mockito) — happy path issues a JWT for the linked
  account; invalid/expired token throws `InvalidOrExpiredLinkException`; expiry is capped
  by `guest-expiration-cap` when the link outlives it.
- `SecurityIntegrationTest` — replace the three `?token=` query-param tests
  (`should_return200_when_validLinkTokenHitsGuestEndpoint`,
  `should_return401_when_linkTokenInvalid`, `should_return401_when_linkTokenExpired`) with
  equivalents against `POST /api/v1/auth/access-link-login` + a follow-up bearer-token request
  to `GET /api/v1/me/home`.

## Follow-up (out of scope here)

- Frontend: on landing at `/home?token=...`, call the new endpoint, store the returned JWT
  (see `docs/frontend/03-services-state.md`'s `AuthService`/interceptor template), strip the
  query param, and use the JWT like an organizer session from then on.
