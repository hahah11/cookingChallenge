package at.fraihs.cookoff.cookoff.application.event;

import at.fraihs.cookoff.cookoff.domain.event.ChallengeUnrevealed;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.repository.CookRivalryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeUnrevealedRivalryUpdater {

    private final CookRivalryRepository cookRivalryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ChallengeUnrevealed event) {
        CookRivalry rivalry = cookRivalryRepository.findByPair(event.cookAAccountId(), event.cookBAccountId())
                .orElseThrow(() -> new IllegalStateException(
                        "No CookRivalry found for pair (" + event.cookAAccountId() + ", "
                                + event.cookBAccountId() + ") while unrevealing challenge " + event.challengeId()));
        rivalry.reverseResult(event.previousOverallWinnerAccountId());
        cookRivalryRepository.save(rivalry);
        log.info("Cook rivalry reversed for pair ({}, {}) after unrevealing challenge {}",
                event.cookAAccountId(), event.cookBAccountId(), event.challengeId());
    }
}
