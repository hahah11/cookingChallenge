package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotOpenException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.CategoryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.DishLabelRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ScoreEntryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SubmitScoresRequestRestDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitScoreServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @InjectMocks
    private SubmitScoreService service;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();
    private final AccountId guestId = AccountId.generate();

    private Challenge openChallenge(List<AccountId> guests) {
        return Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookAId, cookBId, guests, organizerId);
    }

    private SubmitScoresRequestRestDto sixValidScores() {
        List<ScoreEntryRestDto> scores = new ArrayList<>();
        for (DishLabelRestDto label : DishLabelRestDto.values()) {
            for (CategoryRestDto category : CategoryRestDto.values()) {
                scores.add(new ScoreEntryRestDto(label, category, 3));
            }
        }
        return new SubmitScoresRequestRestDto(scores);
    }

    private List<Score> sixDomainScores() {
        List<Score> scores = new ArrayList<>();
        for (at.fraihs.cookoff.cookoff.domain.model.DishLabel label : at.fraihs.cookoff.cookoff.domain.model.DishLabel.values()) {
            for (at.fraihs.cookoff.cookoff.domain.model.Category category : at.fraihs.cookoff.cookoff.domain.model.Category.values()) {
                scores.add(new Score(label, category, 3));
            }
        }
        return scores;
    }

    @Test
    void should_submitScores_when_guestIsAParticipantAndHasNotSubmittedYet() {
        Challenge challenge = openChallenge(List.of(guestId));
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(challenge.getId(), guestId))
                .thenReturn(Optional.empty());

        SubmitScoreService.Result result = service.execute(challenge.getId().toString(), guestId, sixValidScores());

        assertTrue(result.created());
        verify(scoreSubmissionRepository).save(any(ScoreSubmission.class));
    }

    @Test
    void should_updateExistingSubmission_when_guestResubmitsBeforeReveal() {
        Challenge challenge = openChallenge(List.of(guestId));
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        ScoreSubmission existing = ScoreSubmission.submit(challenge.getId(), guestId, sixDomainScores(), Instant.now());
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(challenge.getId(), guestId))
                .thenReturn(Optional.of(existing));

        SubmitScoreService.Result result = service.execute(challenge.getId().toString(), guestId, sixValidScores());

        assertFalse(result.created());
        verify(scoreSubmissionRepository).save(existing);
    }

    @Test
    void should_throw_when_accountIsACookRatherThanAGuestOrTheCreator() {
        Challenge challenge = openChallenge(List.of());
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ForbiddenException.class, () -> service.execute(
                challenge.getId().toString(), cookAId, sixValidScores()));
    }

    @Test
    void should_submitScores_when_accountIsTheCreatorRatherThanAPreAddedGuest() {
        Challenge challenge = openChallenge(List.of());
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(challenge.getId(), organizerId))
                .thenReturn(Optional.empty());

        service.execute(challenge.getId().toString(), organizerId, sixValidScores());

        verify(scoreSubmissionRepository).save(any(ScoreSubmission.class));
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(
                missingId.toString(), guestId, sixValidScores()));
    }

    @Test
    void should_throw_when_challengeIsAlreadyRevealed() {
        Challenge challenge = openChallenge(List.of(guestId));
        challenge.reveal(null);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ChallengeNotOpenException.class, () -> service.execute(
                challenge.getId().toString(), guestId, sixValidScores()));
    }

    @Test
    void should_throw_when_accountIsNotAParticipant() {
        Challenge challenge = openChallenge(List.of());
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ForbiddenException.class, () -> service.execute(
                challenge.getId().toString(), guestId, sixValidScores()));
    }
}
