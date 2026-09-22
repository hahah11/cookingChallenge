package at.fraihs.cookoff.shared.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders and sends every {@link MailRequest} published as an application event.
 *
 * <p>Two annotations carry the delivery guarantees the feature was asked for:
 * <ul>
 *   <li>{@code AFTER_COMMIT} — an access link is written in the same transaction that triggers
 *       its mail. Sending before that commit risks mailing a token that a later rollback
 *       destroys, so nothing leaves the process until the transaction is durable.
 *       {@code fallbackExecution = true} keeps the listener working when there is no
 *       transaction at all.</li>
 *   <li>{@code @Async} — SMTP plus retries can take tens of seconds. The organizer's HTTP
 *       request must not wait for it, and a dedicated bounded executor means mail backpressure
 *       cannot starve the rest of the app.</li>
 * </ul>
 *
 * <p>Nothing here ever throws: a mail server being down is not a reason to fail a request, and
 * by the time this runs the caller has already been answered anyway.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailDispatcher {

    private final TemplateEngine mailTemplateEngine;
    private final MailTransport mailTransport;
    private final String from;
    private final int maxRetries;

    public MailDispatcher(
            TemplateEngine mailTemplateEngine,
            MailTransport mailTransport,
            @Value("${app.mail.from:CookOff <no-reply@cookoff.local>}") String from,
            @Value("${app.mail.max-retries:2}") int maxRetries) {
        this.mailTemplateEngine = mailTemplateEngine;
        this.mailTransport = mailTransport;
        this.from = from;
        this.maxRetries = maxRetries;
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MailRequest request) {
        try {
            mailTransport.send(render(request));
            log.info("Sent '{}' mail to {}", request.template(), request.to());
        } catch (MailException e) {
            // Spring's retry interceptor rethrows the last original failure, not a wrapper, so
            // the real attempt count is not on the exception — report the configured budget,
            // which is what was actually spent.
            log.error("Giving up on '{}' mail to {} after {} attempt(s): {}",
                    request.template(), request.to(), maxRetries + 1, e.getMessage());
        } catch (Exception e) {
            // A missing template or an unconfigured sender — a bug or a misconfiguration, not a
            // flaky mail server, so log the stack trace rather than just the message.
            log.error("Could not send '{}' mail to {}", request.template(), request.to(), e);
        }
    }

    private MimeMessage render(MailRequest request) throws MessagingException {
        Context context = new Context();
        context.setVariables(request.model());

        MimeMessage message = mailTransport.createMessage();
        // true = multipart: clients that refuse HTML still get the plain-text alternative.
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(request.to());
        helper.setSubject(request.subject());
        helper.setText(
                mailTemplateEngine.process("mail/" + request.template() + ".txt", context),
                mailTemplateEngine.process("mail/" + request.template() + ".html", context));
        return message;
    }
}
