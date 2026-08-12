package at.fraihs.cookoff.shared.testsupport;

import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Test double for {@link NotificationPort} that records every "sent" access link instead of
 * just logging it (the production {@code LoggingNotificationAdapter} never sends real email —
 * see docs/cookingChallenge/plans/link-login-qr-registration-test-plan.md). Lets a test drive
 * the real {@code SendChallengeInvitationsService} HTTP path and recover the issued token from
 * the captured link, without a mailbox or log-scraping.
 */
public class CapturingNotificationPort implements NotificationPort {

    private final List<SentLink> sent = new ArrayList<>();

    @Override
    public void sendAccessLink(Email email, String link) {
        sent.add(new SentLink(email, link));
    }

    public String lastLinkFor(String email) {
        return sent.stream()
                .filter(s -> s.email.value().equals(email))
                .reduce((first, second) -> second)
                .map(SentLink::link)
                .orElseThrow(() -> new IllegalStateException("No access link captured for " + email));
    }

    /** Pulls the raw token out of a captured {@code .../home?token=...} link. */
    public static String extractToken(String link) {
        int idx = link.indexOf("token=");
        if (idx < 0) {
            throw new IllegalArgumentException("Link has no token= parameter: " + link);
        }
        return link.substring(idx + "token=".length());
    }

    private record SentLink(Email email, String link) {
    }
}
