package at.fraihs.cookoff.auth.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void should_createEmail_when_valueIsValid() {
        Email email = new Email("cook@example.com");

        assertEquals("cook@example.com", email.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing-domain@", "@missing-local.com", "no-at-sign.com"})
    void should_throw_when_valueIsInvalid(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Email(invalid));
    }

    @Test
    void should_throw_when_valueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }
}
