package at.fraihs.cookoff.auth.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Locale;

/**
 * Languages an account can be addressed in (notification emails). English is the default and
 * the fallback for everything else; the API only accepts the codes listed here.
 */
@ValueObject
public enum Language {
    EN(Locale.ENGLISH),
    DE(Locale.GERMAN);

    private final Locale locale;

    Language(Locale locale) {
        this.locale = locale;
    }

    public Locale toLocale() {
        return locale;
    }

    /** Matches on the language part only (de-AT -> DE); anything unsupported or null falls back to English. */
    public static Language fromLocale(Locale requested) {
        if (requested == null) {
            return EN;
        }
        for (Language language : values()) {
            if (language.locale.getLanguage().equals(requested.getLanguage())) {
                return language;
            }
        }
        return EN;
    }
}
