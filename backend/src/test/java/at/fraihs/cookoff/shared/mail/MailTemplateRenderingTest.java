package at.fraihs.cookoff.shared.mail;

import at.fraihs.cookoff.shared.config.MailConfig;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the one genuinely fiddly piece of {@link MailConfig}: two resolvers on one engine,
 * selected by pattern rather than suffix. Get the suffix wrong and Thymeleaf looks for
 * {@code mail/access-link.txt.txt}; get the mode wrong and the plain-text body comes back
 * HTML-escaped. Neither shows up anywhere else until a real mail looks broken.
 */
class MailTemplateRenderingTest {

    private final TemplateEngine engine = new MailConfig().mailTemplateEngine(new MailMessages());

    private static final String RATE_INVITE = "rate it on all three categories";
    private static final String COOK_INVITE = "choose your plate color";
    private static final String RATE_RESULTS = "who cooked which plate";
    private static final String COOK_RESULTS = "See how yours scored";

    private Context context() {
        return context(true, false);
    }

    private Context context(boolean canRate, boolean picksPlateColor) {
        return context(Locale.ENGLISH, "Schnitzel-Off", canRate, picksPlateColor);
    }

    /** The locale is always explicit: a bare {@code new Context()} would pick up the machine's default language. */
    private Context context(Locale locale, String dishName, boolean canRate, boolean picksPlateColor) {
        Context context = new Context(locale);
        context.setVariables(Map.of(
                "firstName", "Ada",
                "dishName", dishName,
                "link", "https://cookoff.test/home?token=abc",
                "canRate", canRate,
                "picksPlateColor", picksPlateColor));
        return context;
    }

    @Test
    void should_renderHtmlBody_when_templateEndsInHtml() {
        String body = engine.process("mail/access-link.html", context());

        assertTrue(body.contains("<html"), body);
    }

    @Test
    void should_substituteVariablesIntoHtmlBody_when_rendering() {
        String body = engine.process("mail/access-link.html", context());

        assertTrue(body.contains("Ada") && body.contains("Schnitzel-Off")
                && body.contains("https://cookoff.test/home?token=abc"), body);
    }

    @Test
    void should_renderPlainTextBody_when_templateEndsInTxt() {
        String body = engine.process("mail/access-link.txt", context());

        assertFalse(body.contains("<html"), body);
    }

    @Test
    void should_substituteVariablesIntoPlainTextBody_when_rendering() {
        String body = engine.process("mail/access-link.txt", context());

        assertTrue(body.contains("Ada") && body.contains("Schnitzel-Off")
                && body.contains("https://cookoff.test/home?token=abc"), body);
    }

    @Test
    void should_renderBothBodies_when_templateIsResultsAvailable() {
        assertTrue(engine.process("mail/results-available.html", context()).contains("Schnitzel-Off"));
        assertTrue(engine.process("mail/results-available.txt", context()).contains("Schnitzel-Off"));
    }

    /**
     * The card markup exists only in {@code _layout.html}, so finding it proves the fragment
     * reference resolved. A reference without the {@code .html} extension fails at send time,
     * where {@code MailDispatcher} logs and swallows it — this is the test that notices.
     */
    @ParameterizedTest
    @ValueSource(strings = {"mail/access-link.html", "mail/results-available.html"})
    void should_wrapBodyInSharedLayout_when_renderingHtmlTemplate(String template) {
        String body = engine.process(template, context());

        assertTrue(body.contains("data-mail-card"), body);
        assertTrue(body.contains("https://cookoff.test/home?token=abc"), body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mail/access-link.html", "mail/results-available.html"})
    void should_useAppBrandPalette_when_renderingHtmlTemplate(String template) {
        String body = engine.process(template, context());

        assertTrue(body.contains("#940000"), body);
        assertFalse(body.contains("#65558f"), body);
        assertFalse(body.contains("oklch("), body);
    }

    @Test
    void should_notLeakLayoutPlaceholders_when_renderingHtmlTemplate() {
        String body = engine.process("mail/access-link.html", context());

        assertFalse(body.contains("th:replace") || body.contains("th:ref") || body.contains("Heading") || body.contains("Footer"), body);
        assertTrue(body.contains("Open the cook-off") && body.contains("personal to you"), body);
    }

    /**
     * Guest vs cook wording, per template and body. Rows: template, canRate, picksPlateColor,
     * the rater sentence, the cook sentence. The only test that proves the {@code th:if}
     * conditions name the same variables {@code EmailNotificationAdapter} puts in the model.
     */
    @ParameterizedTest
    @CsvSource({
            "mail/access-link.html,       " + RATE_INVITE + "," + COOK_INVITE,
            "mail/access-link.txt,        " + RATE_INVITE + "," + COOK_INVITE,
            "mail/results-available.html, " + RATE_RESULTS + "," + COOK_RESULTS,
            "mail/results-available.txt,  " + RATE_RESULTS + "," + COOK_RESULTS})
    void should_tellCooksToPickAColorInsteadOfRating_when_recipientIsOnlyACook(
            String template, String raterSentence, String cookSentence) {
        String body = engine.process(template, context(false, true));

        assertTrue(body.contains(cookSentence), body);
        assertFalse(body.contains(raterSentence), body);
        assertFalse(body.toLowerCase().contains("rate it"), body);
    }

    @ParameterizedTest
    @CsvSource({
            "mail/access-link.html,       " + RATE_INVITE + "," + COOK_INVITE,
            "mail/access-link.txt,        " + RATE_INVITE + "," + COOK_INVITE,
            "mail/results-available.html, " + RATE_RESULTS + "," + COOK_RESULTS,
            "mail/results-available.txt,  " + RATE_RESULTS + "," + COOK_RESULTS})
    void should_keepRaterWording_when_recipientIsOnlyAGuest(
            String template, String raterSentence, String cookSentence) {
        String body = engine.process(template, context(true, false));

        assertTrue(body.contains(raterSentence), body);
        assertFalse(body.contains(cookSentence), body);
    }

    @ParameterizedTest
    @CsvSource({
            "mail/access-link.html,       " + RATE_INVITE + "," + COOK_INVITE,
            "mail/access-link.txt,        " + RATE_INVITE + "," + COOK_INVITE,
            "mail/results-available.html, " + RATE_RESULTS + "," + COOK_RESULTS,
            "mail/results-available.txt,  " + RATE_RESULTS + "," + COOK_RESULTS})
    void should_includeBothSentencesRaterFirst_when_cookIsAlsoAGuest(
            String template, String raterSentence, String cookSentence) {
        String body = engine.process(template, context(true, true));

        assertTrue(body.indexOf(raterSentence) >= 0
                && body.indexOf(raterSentence) < body.indexOf(cookSentence), body);
    }

    @Test
    void should_renderPasswordResetInBothBodiesOnTheSharedLayout_when_rendering() {
        String html = engine.process("mail/password-reset.html", context());
        String text = engine.process("mail/password-reset.txt", context());

        assertTrue(html.contains("data-mail-card") && html.contains("#940000"), html);
        assertTrue(html.contains("Ada") && html.contains("https://cookoff.test/home?token=abc")
                && html.contains("Set a new password") && html.contains("2 hours"), html);
        assertFalse(text.contains("<html"), text);
        assertTrue(text.contains("Ada") && text.contains("https://cookoff.test/home?token=abc")
                && text.contains("2 hours"), text);
    }

    /** Rows: template, heading, call-to-action label - phrases that only the German wording contains. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "mail/access-link.html       | Hallo Ada, du bist eingeladen!            | Zum Kochwettbewerb",
            "mail/access-link.txt        | Hallo Ada, du bist eingeladen!            | Zum Kochwettbewerb",
            "mail/results-available.html | Hallo Ada, die Ergebnisse sind da!        | Ergebnisse ansehen",
            "mail/results-available.txt  | Hallo Ada, die Ergebnisse sind da!        | Ergebnisse ansehen",
            "mail/password-reset.html    | Hallo Ada, setzen wir dein Passwort zur\u00fcck | Neues Passwort festlegen",
            "mail/password-reset.txt     | Hallo Ada, setzen wir dein Passwort zur\u00fcck | Neues Passwort festlegen"})
    void should_renderGermanWording_when_localeIsGerman(String template, String heading, String cta) {
        String body = engine.process(template, context(Locale.GERMAN, "Schnitzel-Off", true, false));

        assertTrue(body.contains(heading), body);
        assertTrue(body.contains(cta), body);
        assertFalse(body.contains("Open the cook-off") || body.contains("Set a new password")
                || body.contains("See the results"), body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mail/access-link.html", "mail/results-available.html", "mail/password-reset.html"})
    void should_declareTheRecipientsLanguage_when_renderingHtmlTemplate(String template) {
        assertTrue(engine.process(template, context(Locale.GERMAN, "T", true, false)).contains("lang=\"de\""));
        assertTrue(engine.process(template, context(Locale.ENGLISH, "T", true, false)).contains("lang=\"en\""));
    }

    @Test
    void should_keepRaterAndCookWordingApart_when_renderingGerman() {
        String cookOnly = engine.process("mail/access-link.txt", context(Locale.GERMAN, "T", false, true));
        String guestOnly = engine.process("mail/access-link.txt", context(Locale.GERMAN, "T", true, false));

        assertTrue(cookOnly.contains("Tellerfarbe") && !cookOnly.contains("probiere jeden Teller"), cookOnly);
        assertTrue(guestOnly.contains("probiere jeden Teller") && !guestOnly.contains("Tellerfarbe"), guestOnly);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fr", "es", "it"})
    void should_fallBackToEnglish_when_localeIsNotSupported(String language) {
        String body = engine.process("mail/access-link.html", context(Locale.forLanguageTag(language), "T", true, false));

        assertTrue(body.contains("Open the cook-off"), body);
    }

    @Test
    void should_escapeTheDishName_when_dishNameContainsMarkup() {
        Context context = context(Locale.ENGLISH, "<script>alert(1)</script>", true, false);

        String html = engine.process("mail/access-link.html", context);

        assertFalse(html.contains("<script>"), html);
        assertTrue(html.contains("&lt;script&gt;"), html);
    }

    @Test
    void should_keepApostrophesReadable_when_renderingEnglish() {
        String html = engine.process("mail/access-link.html", context());

        assertTrue(html.contains("You&#39;re invited!") || html.contains("you&#39;re invited!")
                || html.contains("you're invited!"), html);
        assertFalse(html.contains("''"), html);
    }
}
