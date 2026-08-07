package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResultRestDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevealChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @Mock
    private CookRivalryRepository cookRivalryRepository;

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RevealChallengeService service;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();
    private final AccountId guestId = AccountId.generate();

    private Challenge openChallenge() {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(guestId), organizerId);
    }

    private Score score(DishLabel label, Category category, int points) {
        return new Score(label, category, points);
    }

    @Test
    void should_revealChallenge_andPublishEvent_when_challengeExists() {
        Challenge challenge = openChallenge();
        ScoreSubmission submission = ScoreSubmission.submit(challenge.getId(), guestId, List.of(
                score(DishLabel.A, Category.MUNDGEFUEHL, 5),
                score(DishLabel.A, Category.TELLERSPRACHE, 5),
                score(DishLabel.A, Category.GESCHMACK, 5),
                score(DishLabel.B, Category.MUNDGEFUEHL, 1),
                score(DishLabel.B, Category.TELLERSPRACHE, 1),
                score(DishLabel.B, Category.GESCHMACK, 1)
        ), Instant.now());
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId())).thenReturn(List.of(submission));
        when(cookRivalryRepository.findByPair(cookAId, cookBId)).thenReturn(Optional.empty());
        when(accountLookup.getById(cookAId)).thenReturn(new AccountSummary(cookAId, new Email("a@x.com"), "Cook A", "Cook"));
        when(accountLookup.getById(cookBId)).thenReturn(new AccountSummary(cookBId, new Email("b@x.com"), "Cook B", "Cook"));

        ChallengeResultRestDto result = service.execute(challenge.getId().toString());

        assertEquals(cookAId.toString(), result.getOverallWinnerAccountId().get());
        assertEquals(ChallengeStatus.REVEALED, challenge.getStatus());
        ArgumentCaptor<ChallengeRevealed> captor = ArgumentCaptor.forClass(ChallengeRevealed.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(cookAId, captor.getValue().overallWinnerAccountId());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(missingId.toString()));
    }
}
