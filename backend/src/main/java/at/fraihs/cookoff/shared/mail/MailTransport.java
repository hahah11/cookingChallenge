package at.fraihs.cookoff.shared.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * The one place that actually talks SMTP, isolated purely so {@link Retryable} has a bean of its
 * own to proxy: the retry interceptor must wrap the real send, and keeping it off
 * {@link MailDispatcher}'s {@code @Async} method makes that ordering obvious instead of relying
 * on the (correct, but invisible) interplay of the two bean post-processors' default orders.
 *
 * <p>Retries cover transient trouble only — a refused connection, a dropped socket, a 4xx
 * greeting. They are in-memory, so a restart mid-retry loses the message; that is an accepted
 * trade-off for not adding an event-publication table.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailTransport {

    private final ObjectProvider<JavaMailSender> javaMailSender;

    public MailTransport(ObjectProvider<JavaMailSender> javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * @throws MailException when every attempt failed; Spring rethrows the last original
     *         exception rather than a wrapper, so the attempt count is not recoverable here —
     *         {@link MailDispatcher} logs it from the configured maximum instead.
     */
    @Retryable(
            includes = MailException.class,
            maxRetriesString = "${app.mail.max-retries:2}",
            delayString = "${app.mail.retry-delay:5s}",
            multiplier = 2)
    public void send(MimeMessage message) {
        sender().send(message);
    }

    public MimeMessage createMessage() {
        return sender().createMimeMessage();
    }

    /**
     * Boot only auto-configures a {@link JavaMailSender} when {@code spring.mail.host} (or
     * {@code jndi-name}) is set, so enabling mail without a host would otherwise fail context
     * refresh with an opaque {@code NoSuchBeanDefinitionException} nested three levels down.
     */
    private JavaMailSender sender() {
        return javaMailSender.getIfAvailable(() -> {
            throw new IllegalStateException(
                    "app.mail.enabled=true but no JavaMailSender is configured — set spring.mail.host "
                            + "(plus port/username/password as needed), or set app.mail.enabled=false.");
        });
    }
}
