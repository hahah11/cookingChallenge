package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.SubmissionStatusView;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.repository.ScoreSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetChallengeStatusServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @InjectMocks
    private GetChallengeStatusService service;

    @Test
    void should_countOnlyGuestSubmissions_notCookSubmissions() {
        AccountId cookA = AccountId.generate();
        AccountId cookB = AccountId.generate();
        AccountId organizer = AccountId.generate();
        AccountId guest1 = AccountId.generate();
        AccountId guest2 = AccountId.generate();
        Challenge challenge = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                cookA, cookB, List.of(guest1, guest2), organizer);
        when(challengeRepository.findById(challenge.getId())).thenReturn(Optional.of(challenge));

        List<Score> scores = List.of(new Score(DishLabel.A, Category.GESCHMACK, 5));
        ScoreSubmission guestSubmission = ScoreSubmission.reconstitute(
                ScoreSubmissionId.generate(), challenge.getId(), guest1, scores, Instant.now());
        ScoreSubmission cookSubmission = ScoreSubmission.reconstitute(
                ScoreSubmissionId.generate(), challenge.getId(), cookA, scores, Instant.now());
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId()))
                .thenReturn(List.of(guestSubmission, cookSubmission));

        SubmissionStatusView view = service.execute(challenge.getId().toString());

        assertEquals(2, view.totalGuestCount());
        assertEquals(1, view.submittedGuestCount());
        assertEquals(List.of(guest1.toString()), view.submittedGuestAccountIds());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        when(challengeRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class,
                () -> service.execute(AccountId.generate().toString()));
    }
}
