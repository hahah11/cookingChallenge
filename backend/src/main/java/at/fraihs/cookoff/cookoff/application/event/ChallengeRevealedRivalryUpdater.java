package at.fraihs.cookoff.cookoff.application.event;

import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeRevealedRivalryUpdater {

    private final CookRivalryRepository cookRivalryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ChallengeRevealed event) {
        CookRivalry rivalry = cookRivalryRepository.findByPair(event.cookAAccountId(), event.cookBAccountId())
                .orElseGet(() -> CookRivalry.start(event.cookAAccountId(), event.cookBAccountId()));
        rivalry.recordResult(event.overallWinnerAccountId());
        cookRivalryRepository.save(rivalry);
        log.info("Cook rivalry updated for pair ({}, {}) after challenge {}",
                event.cookAAccountId(), event.cookBAccountId(), event.challengeId());
    }
}
