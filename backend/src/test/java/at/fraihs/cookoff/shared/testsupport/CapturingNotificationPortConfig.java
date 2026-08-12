package at.fraihs.cookoff.shared.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Import into a {@code @SpringBootTest} to replace the real {@code NotificationPort} bean with
 * {@link CapturingNotificationPort}, so a test can drive the production
 * "send invitations" HTTP path and recover the issued access-link token without a mailbox.
 */
@TestConfiguration
public class CapturingNotificationPortConfig {

    @Bean
    @Primary
    public CapturingNotificationPort capturingNotificationPort() {
        return new CapturingNotificationPort();
    }
}
