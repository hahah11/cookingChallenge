package at.fraihs.cookoff.shared.mail;

import java.util.Locale;
import java.util.Map;

/**
 * One rendered-on-demand mail. Deliberately built from plain types only — this package is the
 * shared, technology-facing half of mail delivery and must stay free of any {@code cookoff}/
 * {@code auth} domain type, or {@code ApplicationModules.verify()} would see {@code shared}
 * depend on a business module.
 *
 * <p>{@code template} is a base name under {@code templates/mail/} without an extension:
 * {@code MailDispatcher} renders both {@code <template>.html} and {@code <template>.txt} from it.
 *
 * <p>{@code locale} is the recipient's language: it selects the wording the templates pull from
 * {@code mail/messages*.properties}. Null means English.
 *
 * <p>Named {@code MailRequest} rather than {@code MailMessage} to leave that name to
 * {@link org.springframework.mail.MailMessage}, which the same classes have to import.
 */
public record MailRequest(String to, String subject, String template, Map<String, Object> model, Locale locale) {

    public MailRequest {
        model = Map.copyOf(model);
        locale = locale == null ? Locale.ENGLISH : locale;
    }
}
