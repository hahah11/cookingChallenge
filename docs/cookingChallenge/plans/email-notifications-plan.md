# Email Notifications Plan

## Status

**Implemented (2026-09-22).** Invitation access links and results-available notifications are
sent as real multipart email over SMTP. Disabled by default (`app.mail.enabled: false`), which
keeps the previous log-only behaviour for a fresh checkout and the whole test suite.

## Goal

The backend had a notification seam but no delivery: `NotificationPort` declared one method,
`sendAccessLink(Email, String)`, and its only implementation logged the link. Two shipped
behaviours quietly depended on mail that never arrived:

- The story map marked *"Get invitation email with unique link"* done, and
  `PublicRegistrationService` tells a walk-in guest *"You'll get an email once the organizer
  opens scoring for this cook-off."*
- Guests are `USER` accounts with `passwordHash == null`, so an access link is the **only** way
  they can log in. A guest added to a cook-off who was never shown a QR code had no route in.

The backlog item *"Notify guests and cooks that results are available"* was descoped on
2026-08-10 precisely because `NotificationPort` had no method for it. Both are now shipped.

## Design decisions (confirmed with the user)

| Question | Decision |
|---|---|
| Which mails | Invitation access link **and** results-available on reveal |
| Transport | SMTP via `spring-boot-starter-mail`, env-configured; logging stub as the default |
| Delivery | Asynchronous, after the DB commit, with a bounded retry count |
| Content | HTML + plain-text multipart, Thymeleaf templates |

**No REST/OpenAPI change.** `POST /challenges/{id}/invitations` still answers `{count}` and
`POST /challenges/{id}/reveal` is untouched, so `openapi/cookingchallenge-api.yaml`, the
generated Angular client, and `send-links-dialog` needed no changes.

## Flow

```
POST /challenges/{id}/invitations         POST /challenges/{id}/reveal
  SendChallengeInvitationsService           RevealChallengeService
    (@Transactional)                          (publishes ChallengeRevealed)
    issue access link per target                       │ AFTER_COMMIT
    notificationPort.sendAccessLink(…)                 ▼
               │                            ChallengeRevealedNotifier
               │                              issue a link per guest + both cooks
               │                              notificationPort.sendResultsAvailable(…)
               ▼                                       │
        EmailNotificationAdapter  ◀────────────────────┘
          publishes a MailRequest application event
               │
               ▼  @TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)
        MailDispatcher            @Async("mailTaskExecutor")
          renders mail/<name>.html + mail/<name>.txt
          MimeMessageHelper multipart
               │
               ▼
        MailTransport             @Retryable(MailException, maxRetries, ×2 backoff)
               │
               ▼  JavaMailSender
```

### Why an event rather than an inline send

The access link is written in the same transaction that triggers its mail. Sending before that
commit risks mailing a token a later rollback destroys. `MailDispatcher` therefore listens
`AFTER_COMMIT`; `fallbackExecution = true` keeps it working when there is no transaction.

The event is published by the *adapter*, not by `SendChallengeInvitationsService`. That matters:
`AccessLinkLoginFlowIntegrationTest` is `@SpringBootTest @Transactional` and reads the link back
off `CapturingNotificationPort`, which is registered `@Primary` and so replaces the adapter
entirely. Keeping the deferral behind the port boundary leaves that test — and its synchronous
expectations — untouched.

### Retry

Spring Framework 7 (which Boot 4.1 brings) ships `@Retryable` and `@EnableResilientMethods` in
`org.springframework.resilience.annotation`; no `spring-retry` dependency. `MailTransport` is a
separate bean from `MailDispatcher` purely so the retry interceptor wraps the real send rather
than an `@Async` submission.

Spring rethrows the *last original* exception on exhaustion, discarding the `RetryException`
that carries `getRetryCount()` — so the attempt count is not recoverable from the exception.
`MailDispatcher` logs the configured budget instead (`app.mail.max-retries` + 1), which is what
was actually spent. Retries are in-memory: a restart mid-retry loses the message.

### Why plain Thymeleaf instead of `spring-boot-starter-thymeleaf`

The starter resolves `thymeleaf-spring6` — a Spring Framework 6 artifact; there is no
`-spring7` release — and registers an MVC `ThymeleafViewResolver` that a REST-only app has no
use for. `MailConfig` builds a standalone `TemplateEngine` with two `ClassLoaderTemplateResolver`s
instead, selected by `resolvablePatterns` on the full template name with an **empty suffix**
(a `.txt` suffix plus a `mail/*.txt` pattern would resolve `mail/x.txt.txt`).

### `management.health.mail.enabled: false` is load-bearing

`spring-boot-starter-actuator` is on the classpath and `compose.prod.yaml` gates the `frontend`
service on `backend`'s `condition: service_healthy`. Boot's mail health indicator opens an SMTP
connection on every probe, so leaving it enabled would take the entire site offline whenever the
mail server is merely unreachable. Delivery failures are logged by `MailDispatcher` instead.

It is set in **both** `src/main/resources/application.yaml` and `src/test/resources/application.yaml`:
the test file shadows the main one on the classpath (which is also why every `@Value` in this
codebase carries an inline default), and without it any test setting `app.mail.enabled=true`
fails context refresh on the mail health contributor.

## Files

### Added

| File | Role |
|---|---|
| `shared/mail/MailRequest.java` | The event/payload: `to`, `subject`, `template`, `model`. Plain types only, so `shared` gains no dependency on a business module |
| `shared/mail/MailDispatcher.java` | AFTER_COMMIT + `@Async` listener: renders both bodies, builds the multipart, logs and swallows failures |
| `shared/mail/MailTransport.java` | `@Retryable` wrapper around `JavaMailSender`; fails fast with a readable message when mail is enabled but no sender is configured |
| `shared/config/MailConfig.java` | Standalone dual-mode `TemplateEngine` + bounded `mailTaskExecutor` |
| `shared/config/AsyncRetryConfig.java` | `@EnableAsync` + `@EnableResilientMethods` |
| `cookoff/application/dto/InvitationNotification.java` | Port payload (recipient, firstName, challengeTitle, link) |
| `cookoff/application/dto/ResultsAvailableNotification.java` | Port payload |
| `cookoff/infrastructure/notification/EmailNotificationAdapter.java` | Subject + template + model, then publishes the event |
| `cookoff/application/event/ChallengeRevealedNotifier.java` | AFTER_COMMIT listener on `ChallengeRevealed` |
| `resources/templates/mail/{access-link,results-available}.{html,txt}` | The four bodies |

### Changed

- `NotificationPort` — widened to take the two payload records, and gained `sendResultsAvailable`.
  The records live in `application/dto` because `PackageConventionArchitectureTests` forbids
  top-level records in `..application.port..`. This is an internal port, not a public REST API.
- `LoggingNotificationAdapter` — implements both methods; `@ConditionalOnProperty` makes it the
  default. Note the two conditions are exhaustive only for literal `true`/`false`; any other
  value leaves no `NotificationPort` bean and fails startup, which beats silently logging
  invitations in production.
- `SendChallengeInvitationsService` — one line, building the payload. Control flow untouched.
- `CapturingNotificationPort` — implements both methods; public helpers unchanged.
- `backend/compose.yaml` — added Mailpit (SMTP 1025, UI 8025), started automatically in dev by
  `spring-boot-docker-compose`. Boot ships no docker-compose service-connection for mail, so
  `spring.mail.host/port` point at it explicitly.
- `compose.prod.yaml` + `.env.example` — `APP_MAIL_ENABLED`, `APP_MAIL_FROM`, `SPRING_MAIL_*`.

## Both mails link to `/home?token=…`

Not a deep link to the results page: `ParticipantHome` is the only component that performs the
access-link → JWT exchange, so `/home` is the only entry point a passwordless guest can use.
The reveal mail mints a fresh 30-day link per recipient rather than assuming the invitation link
is still valid.

## Tests

`MailTemplateRenderingTest` (dual-mode resolver — the one genuinely fiddly piece of config),
`MailDispatcherTest` (headers, both body parts, failures swallowed), `MailTransportRetryTest`
(real AOP proxy: exactly `max-retries + 1` attempts, stops early on success),
`EmailNotificationAdapterTest` (subject/template/model per notification type),
`ChallengeRevealedNotifierTest` (guests + both cooks, de-duplicated, fresh link each),
`MailWiringIntegrationTest` (the only coverage of `app.mail.enabled=true`: the conditional bean
swap and real event delivery on the async executor).

Verified manually against a real SMTP conversation during implementation: correct `From`/`To`/
`Subject`, `multipart/alternative` with a clean plain-text part and a rendered HTML part.

## Known behaviour and follow-ups

- **Unreveal → reveal re-notifies everyone.** `Challenge.reveal(...)` publishes unconditionally,
  and the results genuinely changed, so a repeat mail is defensible. Logged, not suppressed.
  Suppressing it would need a `resultsNotifiedAt` column and a Liquibase changeset.
- **Retries are in-memory.** Surviving a restart means `spring-modulith-starter-events-jpa` and
  its event-publication table.
- **The organizer never learns a send failed.** The dialog reports links issued, not mail
  delivered; surfacing failures would need an OpenAPI change.
- **Welcome mail on account creation and password reset are not implemented.** Both live in
  `auth`; `shared/mail` is already placed so they can reuse it without crossing a module boundary.
