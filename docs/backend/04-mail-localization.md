# Mail Localization (English + German)

Every email is sent by an organizer or admin **to someone else** (invitations, results on reveal,
admin-triggered password reset), so a request header can't say what language the recipient reads.
The language is stored on the recipient's account instead.

## Where the language lives

- `accounts.locale` (Liquibase `auth/007-account-locale.yaml`), stored as the `Language` enum name
  (`EN`, `DE`), default `EN`; the API/OpenAPI value is `en` / `de` (`Locale` schema).
- `Account.language` (`auth.domain.model.Language`, `changeLanguage(...)`). `Language.fromLocale(Locale)`
  matches on the language part only (`de-AT` → `DE`); anything else, or `null`, is `EN`.
- Set by: public registration (`PublicRegistrationRequest.locale`), admin create/update account
  (`locale`), and `PUT /api/v1/me/locale`, which the frontend calls after every login.
- Cross-module: `AccountSummary.locale()` carries it to `cookoff` (invitations, reveal); the
  notification payloads (`InvitationNotification`, `ResultsAvailableNotification`,
  `PasswordResetNotification`) and `MailRequest` carry a `java.util.Locale`.

## How mails are rendered

- Wording lives in `src/main/resources/mail/messages.properties` (English, the default and the
  fallback) and `messages_de.properties`. Values are `MessageFormat` patterns: a literal apostrophe is
  `''`, arguments are `{0}`. Keys ending `.html` may contain markup and receive HTML-escaped
  arguments; `.text` keys are plain text.
- `MailMessages` looks messages up (never falling back to the JVM's default locale);
  `MailMessageResolver` feeds `#{key(args)}` expressions in the Thymeleaf templates. The engine is
  standalone (see `MailConfig`), so this replaces the Spring `MessageSource` integration we don't have.
- Subjects come from the same bundle via `MailMessages` in the notification adapters; the template
  context's locale (`MailDispatcher` → `new Context(request.locale())`) selects the language, and
  `<html lang>` follows it.
- User-entered text that lands inside a markup message (the dish name) is
  `#strings.escapeXml`-ed before it goes in.

## Adding or changing text

1. Add the key to **both** `messages.properties` and `messages_de.properties`
   (`MailMessagesTest` fails if the keys or `{n}` placeholders differ).
2. Use `#{key(${arg})}` in the `.html` / `.txt` templates, or `MailMessages.get(...)` for subjects.
3. Add a language to `Language`, the `Locale` schema in `openapi/cookingchallenge-api.yaml`, a
   `messages_<lang>.properties`, and the frontend's `SUPPORTED_LOCALES` together.

## Not localized

API error `message` text stays English; the frontend shows its own translation of the stable
`error.code` (see `docs/frontend/06-i18n.md`).
