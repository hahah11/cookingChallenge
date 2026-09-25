package at.fraihs.cookoff.shared.mail;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.springframework.stereotype.Component;

/**
 * Looks up mail wording in {@code mail/messages*.properties}: {@code messages.properties} is the
 * English default and fallback, {@code messages_de.properties} the German translation.
 *
 * <p>Used for subjects (by the notification adapters) and, through {@link MailMessageResolver},
 * for the {@code #{...}} expressions in the templates, so a subject and the body it belongs to
 * always come from the same bundle. {@code MessageFormat} is applied even to messages without
 * arguments so that {@code ''} means a literal apostrophe everywhere.
 */
@Component
public class MailMessages {

    private static final String BASE_NAME = "mail.messages";

    /** Without this, a missing key in {@code _fr} would fall back to the JVM's default locale, not to English. */
    private static final ResourceBundle.Control NO_SYSTEM_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    /** @return the formatted message, or {@code null} if neither the locale's bundle nor the default has the key */
    public String find(String key, Locale locale, Object... args) {
        try {
            String pattern = ResourceBundle.getBundle(BASE_NAME, locale, NO_SYSTEM_FALLBACK).getString(key);
            return new MessageFormat(pattern, locale).format(args);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public String get(String key, Locale locale, Object... args) {
        String message = find(key, locale, args);
        if (message == null) {
            throw new IllegalArgumentException("Missing mail message '" + key + "' for " + locale);
        }
        return message;
    }
}
