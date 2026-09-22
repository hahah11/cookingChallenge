package at.fraihs.cookoff.shared.mail;

import at.fraihs.cookoff.shared.config.MailConfig;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

    private final TemplateEngine engine = new MailConfig().mailTemplateEngine();

    private Context context() {
        Context context = new Context();
        context.setVariables(Map.of(
                "firstName", "Ada",
                "challengeTitle", "Schnitzel-Off",
                "link", "https://cookoff.test/home?token=abc"));
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
}
