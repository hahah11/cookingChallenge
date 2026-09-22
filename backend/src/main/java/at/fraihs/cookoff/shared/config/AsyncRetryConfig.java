package at.fraihs.cookoff.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Turns on the two method-level behaviours mail delivery depends on: {@code @Async} for
 * {@code MailDispatcher} and {@code @Retryable} for {@code MailTransport}.
 *
 * <p>{@code @Retryable} here is Spring Framework 7's own
 * {@link org.springframework.resilience.annotation.Retryable}, not Spring Retry — no extra
 * dependency. Nothing outside {@code shared.mail} uses either annotation today.
 */
@Configuration
@EnableAsync
@EnableResilientMethods
public class AsyncRetryConfig {
}
