package at.fraihs.cookoff.shared.mail;

import at.fraihs.cookoff.shared.config.AsyncRetryConfig;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The retry budget only exists inside the AOP proxy, so a plain Mockito test of
 * {@link MailTransport} would prove nothing about it. This runs the real annotation through a
 * minimal Spring context instead.
 */
@SpringJUnitConfig(MailTransportRetryTest.Config.class)
@TestPropertySource(properties = {
        "app.mail.enabled=true",
        "app.mail.max-retries=2",
        // Keep the suite fast: the production default is 5s with a doubling backoff.
        "app.mail.retry-delay=1ms",
})
class MailTransportRetryTest {

    @Configuration
    @Import({AsyncRetryConfig.class, MailTransport.class})
    static class Config {
        @Bean
        JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }
    }

    private final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

    @Autowired
    private MailTransport mailTransport;

    @Autowired
    private JavaMailSender javaMailSender;

    @Test
    void should_retryUpToTheConfiguredBudget_when_everyAttemptFails() {
        doThrow(new MailSendException("smtp is down")).when(javaMailSender).send(message);

        assertThrows(MailSendException.class, () -> mailTransport.send(message));

        // One initial attempt plus app.mail.max-retries.
        verify(javaMailSender, times(3)).send(message);
    }

    @Test
    void should_sendOnlyOnce_when_theFirstAttemptSucceeds() {
        mailTransport.send(message);

        verify(javaMailSender, times(1)).send(message);
    }

    @Test
    void should_stopRetrying_when_aLaterAttemptSucceeds() {
        doThrow(new MailSendException("transient")).doNothing().when(javaMailSender).send(message);

        mailTransport.send(message);

        verify(javaMailSender, times(2)).send(message);
    }

    @Test
    void should_failFastWithAReadableMessage_when_mailIsEnabledButNoSenderIsConfigured() {
        MailTransport withoutSender = new MailTransport(new ObjectProvider<JavaMailSender>() {
            @Override
            public JavaMailSender getObject() {
                return null;
            }
        });

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> withoutSender.send(message));
        assertTrue(thrown.getMessage().contains("spring.mail.host"));
    }
}
