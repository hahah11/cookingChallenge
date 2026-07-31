package at.fraihs.cookoff.cookoff.application.event;

import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.repository.CookRivalryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChallengeRevealedRivalryUpdater {

    private final CookRivalryRepository cookRivalryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ChallengeRevealed event) {
        CookRivalry rivalry = cookRivalryRepository.findByPair(event.cookAAccountId(), event.cookBAccountId())
                .orElseGet(() -> CookRivalry.start(event.cookAAccountId(), event.cookBAccountId()));
        rivalry.recordResult(event.overallWinnerAccountId());
        cookRivalryRepository.save(rivalry);
    }
}
