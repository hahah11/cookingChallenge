package at.fraihs.cookoff.cookoff.application.event;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ResultsAvailableNotification;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.CookAssignment;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tells every participant that a cook-off they took part in has been revealed — the story map's
 * "Notify guests and cooks that results are available", which sat in the backlog only because
 * {@code NotificationPort} had no method for it.
 *
 * <p>Each recipient gets a <em>freshly issued</em> access link rather than a bare URL: guests are
 * {@code USER} accounts with no password, so a link is the only thing they can act on, and the
 * one they were invited with may have expired.
 *
 * <p>Known behaviour: unrevealing and revealing again notifies everyone a second time.
 * {@code Challenge.reveal(...)} publishes unconditionally, and re-revealing means the results
 * genuinely changed, so a repeat mail is defensible — but it is logged so it is never a surprise.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeRevealedNotifier {

    private static final Duration LINK_VALIDITY = Duration.ofDays(30);

    private final ChallengeRepository challengeRepository;
    private final AccountLookup accountLookup;
    private final AccessLinkService accessLinkService;
    private final NotificationPort notificationPort;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ChallengeRevealed event) {
        Optional<Challenge> found = challengeRepository.findById(event.challengeId());
        if (found.isEmpty()) {
            log.warn("Challenge {} vanished before results notifications could be sent", event.challengeId());
            return;
        }
        Challenge challenge = found.get();

        for (AccountId accountId : recipientsOf(challenge)) {
            AccountSummary account = accountLookup.getById(accountId);
            String token = accessLinkService.issue(accountId, challenge.getId().value(), LINK_VALIDITY);
            notificationPort.sendResultsAvailable(new ResultsAvailableNotification(
                    account.email(), account.firstName(), challenge.getTitle(),
                    frontendBaseUrl + "/home?token=" + token,
                    challenge.canScore(accountId), challenge.isCook(accountId), account.locale()));
        }
        log.info("Notified participants that challenge {} was revealed", event.challengeId());
    }

    /** Guests and both cooks, de-duplicated — a cook can also be a guest on the same cook-off. */
    private static Set<AccountId> recipientsOf(Challenge challenge) {
        Set<AccountId> recipients = new LinkedHashSet<>(challenge.getGuestAccountIds());
        challenge.getCookAssignments().stream().map(CookAssignment::accountId).forEach(recipients::add);
        return recipients;
    }
}
