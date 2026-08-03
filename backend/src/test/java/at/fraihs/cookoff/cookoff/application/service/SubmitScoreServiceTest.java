package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ScoreInput;
import at.fraihs.cookoff.cookoff.application.dto.SubmitScoreCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotOpenException;
import at.fraihs.cookoff.cookoff.application.exception.DuplicateSubmissionException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private List<ScoreInput> sixValidScores() {
        List<ScoreInput> scores = new java.util.ArrayList<>();
        for (String label : List.of("A", "B")) {
            for (String category : List.of("MUNDGEFUEHL", "TELLERSPRACHE", "GESCHMACK")) {
                scores.add(new ScoreInput(label, category, 3));
            }
        }
        return scores;
    }

    @Test
    void should_submitScores_when_guestIsAParticipantAndHasNotSubmittedYet() {
        Challenge challenge = openChallenge(List.of(guestId));
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(challenge.getId(), guestId))
                .thenReturn(false);

        service.execute(new SubmitScoreCommand(challenge.getId().toString(), guestId.toString(), sixValidScores()));

        verify(scoreSubmissionRepository).save(any(ScoreSubmission.class));
    }

    @Test
    void should_throw_when_accountIsACookRatherThanAGuestOrTheCreator() {
        Challenge challenge = openChallenge(List.of());
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(NotAParticipantException.class, () -> service.execute(
                new SubmitScoreCommand(challenge.getId().toString(), cookAId.toString(), sixValidScores())));
    }

    @Test
    void should_submitScores_when_accountIsTheCreatorRatherThanAPreAddedGuest() {
        Challenge challenge = openChallenge(List.of());
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(challenge.getId(), organizerId))
                .thenReturn(false);

        service.execute(new SubmitScoreCommand(challenge.getId().toString(), organizerId.toString(), sixValidScores()));

        verify(scoreSubmissionRepository).save(any(ScoreSubmission.class));
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        ChallengeId missingId = ChallengeId.generate();
        when(challengeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class, () -> service.execute(
                new SubmitScoreCommand(missingId.toString(), guestId.toString(), sixValidScores())));
    }

    @Test
    void should_throw_when_challengeIsAlreadyRevealed() {
        Challenge challenge = openChallenge(List.of(guestId));
        challenge.reveal(null);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ChallengeNotOpenException.class, () -> service.execute(
                new SubmitScoreCommand(challenge.getId().toString(), guestId.toString(), sixValidScores())));
    }

    @Test
    void should_throw_when_accountIsNotAParticipant() {
        Challenge challenge = openChallenge(List.of());
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(NotAParticipantException.class, () -> service.execute(
                new SubmitScoreCommand(challenge.getId().toString(), guestId.toString(), sixValidScores())));
    }

    @Test
    void should_throw_when_guestHasAlreadySubmitted() {
        Challenge challenge = openChallenge(List.of(guestId));
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));
        when(scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(challenge.getId(), guestId))
                .thenReturn(true);

        assertThrows(DuplicateSubmissionException.class, () -> service.execute(
                new SubmitScoreCommand(challenge.getId().toString(), guestId.toString(), sixValidScores())));
    }
}
