package at.fraihs.cookoff.auth.domain.model;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageTest {

    @ParameterizedTest
    @ValueSource(strings = {"de", "de-AT", "de-CH", "de-DE"})
    void should_matchGerman_when_regionalGermanVariantIsRequested(String tag) {
        assertEquals(Language.DE, Language.fromLocale(Locale.forLanguageTag(tag)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "en-US", "fr", "es-ES", "ja"})
    void should_fallBackToEnglish_when_languageIsEnglishOrUnsupported(String tag) {
        assertEquals(Language.EN, Language.fromLocale(Locale.forLanguageTag(tag)));
    }

    @Test
    void should_fallBackToEnglish_when_noLocaleGiven() {
        assertEquals(Language.EN, Language.fromLocale(null));
    }

    @Test
    void should_roundTripThroughLocale_when_convertingEveryLanguage() {
        for (Language language : Language.values()) {
            assertEquals(language, Language.fromLocale(language.toLocale()));
        }
    }
}
