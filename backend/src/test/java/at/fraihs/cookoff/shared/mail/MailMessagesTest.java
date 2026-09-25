package at.fraihs.cookoff.shared.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailMessagesTest {

    private final MailMessages messages = new MailMessages();

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = MailMessagesTest.class.getResourceAsStream(resource);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    @Test
    void should_defineTheSameKeysInEnglishAndGerman_when_bundlesAreLoaded() throws IOException {
        Properties english = load("/mail/messages.properties");
        Properties german = load("/mail/messages_de.properties");

        assertEquals(new TreeSet<>(english.stringPropertyNames()), new TreeSet<>(german.stringPropertyNames()));
    }

    @Test
    void should_useTheSamePlaceholdersInEnglishAndGerman_when_bundlesAreLoaded() throws IOException {
        Properties english = load("/mail/messages.properties");
        Properties german = load("/mail/messages_de.properties");

        for (String key : english.stringPropertyNames()) {
            assertEquals(placeholders(english.getProperty(key)), placeholders(german.getProperty(key)), key);
        }
    }

    @Test
    void should_haveNoBlankMessages_when_bundlesAreLoaded() throws IOException {
        for (String resource : new String[] {"/mail/messages.properties", "/mail/messages_de.properties"}) {
            Properties bundle = load(resource);
            for (String key : bundle.stringPropertyNames()) {
                assertFalse(bundle.getProperty(key).isBlank(), resource + " " + key);
            }
        }
    }

    @Test
    void should_returnTheGermanText_when_localeIsGerman() {
        assertEquals("Du bist eingeladen: Schnitzel-Off",
                messages.get("mail.accessLink.subject", Locale.GERMAN, "Schnitzel-Off"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"de-AT", "de-CH"})
    void should_returnTheGermanText_when_localeIsARegionalGermanVariant(String tag) {
        assertEquals("Passwort zurücksetzen",
                messages.get("mail.passwordReset.title", Locale.forLanguageTag(tag)));
    }

    @Test
    void should_returnTheEnglishText_when_localeIsEnglish() {
        assertEquals("You're invited: Schnitzel-Off",
                messages.get("mail.accessLink.subject", Locale.ENGLISH, "Schnitzel-Off"));
    }

    @Test
    void should_fallBackToEnglishNotToTheJvmDefault_when_localeIsUnsupported() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
        try {
            assertEquals("Reset your password",
                    messages.get("mail.passwordReset.title", Locale.FRENCH));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void should_returnNullFromFind_when_keyIsUnknown() {
        assertNull(messages.find("mail.no.such.key", Locale.ENGLISH));
    }

    @Test
    void should_throwFromGet_when_keyIsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> messages.get("mail.no.such.key", Locale.ENGLISH));
    }

    @Test
    void should_notLeakBraceOrQuoteEscapes_when_formattingArguments() {
        String subject = messages.get("mail.accessLink.subject", Locale.ENGLISH, "Ben & Jerry's {Bake-Off}");

        assertTrue(subject.contains("Ben & Jerry's {Bake-Off}"), subject);
        assertFalse(subject.contains("''"), subject);
    }

    /** Distinct {n} argument indexes used by a message, e.g. "{0}{1}" -> "01". */
    private static String placeholders(String message) {
        StringBuilder found = new StringBuilder();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{(\\d+)}").matcher(message);
        java.util.TreeSet<String> indexes = new java.util.TreeSet<>();
        while (matcher.find()) {
            indexes.add(matcher.group(1));
        }
        indexes.forEach(found::append);
        return found.toString();
    }
}
