package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import at.fraihs.cookoff.shared.web.openapi.model.GuestHomeRestDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @InjectMocks
    private HomeService service;

    private final AccountId accountId = AccountId.generate();

    private List<Score> sixScores() {
        return List.of(
                new Score(DishLabel.A, Category.MUNDGEFUEHL, 3), new Score(DishLabel.A, Category.TELLERSPRACHE, 3),
                new Score(DishLabel.A, Category.GESCHMACK, 3), new Score(DishLabel.B, Category.MUNDGEFUEHL, 3),
                new Score(DishLabel.B, Category.TELLERSPRACHE, 3), new Score(DishLabel.B, Category.GESCHMACK, 3));
    }

    @Test
    void should_bucketAllOpenChallengesAsOpen_regardlessOfSubmission() {
        Challenge notYetSubmitted = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(accountId), AccountId.generate());
        Challenge alreadySubmitted = Challenge.create(LocalDate.now(), null, new DishName("Goulash"),
                AccountId.generate(), AccountId.generate(), List.of(accountId), AccountId.generate());
        when(accountLookup.getById(any())).thenReturn(
                new AccountSummary(AccountId.generate(), new Email("guest@example.com"), "Guest", "Guest"));
        when(challengeRepository.findByParticipant(accountId)).thenReturn(List.of(notYetSubmitted, alreadySubmitted));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(notYetSubmitted.getId(), accountId))
                .thenReturn(Optional.empty());
        ScoreSubmission submission = ScoreSubmission.reconstitute(
                ScoreSubmissionId.generate(), alreadySubmitted.getId(), accountId, sixScores(), Instant.now());
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(alreadySubmitted.getId(), accountId))
                .thenReturn(Optional.of(submission));

        GuestHomeRestDto home = service.execute(accountId);

        assertEquals(2, home.getOpen().size());
        assertTrue(home.getOpen().stream().anyMatch(c -> c.getId().equals(notYetSubmitted.getId().toString())));
        assertTrue(home.getOpen().stream().anyMatch(c -> c.getId().equals(alreadySubmitted.getId().toString())));
        assertTrue(home.getPast().isEmpty());
    }

    @Test
    void should_bucketOpenChallengeAsOpen_evenAfterCookAlreadyPickedColor() {
        AccountId cookAccountId = accountId;
        Challenge challenge = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                cookAccountId, AccountId.generate(), List.of(), AccountId.generate());
        challenge.pickColor(cookAccountId, PlateColorId.generate(), PlateColorId.generate());
        when(accountLookup.getById(any())).thenReturn(
                new AccountSummary(AccountId.generate(), new Email("cook@example.com"), "Cook", "Cook"));
        when(challengeRepository.findByParticipant(accountId)).thenReturn(List.of(challenge));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(challenge.getId(), accountId))
                .thenReturn(Optional.empty());

        GuestHomeRestDto home = service.execute(accountId);

        assertEquals(1, home.getOpen().size());
        assertTrue(home.getPast().isEmpty());
    }

    @Test
    void should_bucketRevealedChallengeAsPast_evenWithoutASubmission() {
        Challenge revealed = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(accountId), AccountId.generate());
        revealed.reveal(null);
        when(accountLookup.getById(any())).thenReturn(
                new AccountSummary(AccountId.generate(), new Email("guest@example.com"), "Guest", "Guest"));
        when(challengeRepository.findByParticipant(accountId)).thenReturn(List.of(revealed));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(revealed.getId(), accountId))
                .thenReturn(Optional.empty());

        GuestHomeRestDto home = service.execute(accountId);

        assertTrue(home.getOpen().isEmpty());
        assertEquals(1, home.getPast().size());
    }

    @Test
    void should_hideCookMapping_when_challengeNotRevealed() {
        Challenge open = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(accountId), AccountId.generate());
        when(accountLookup.getById(any())).thenReturn(
                new AccountSummary(AccountId.generate(), new Email("guest@example.com"), "Guest", "Guest"));
        when(challengeRepository.findByParticipant(accountId)).thenReturn(List.of(open));
        when(scoreSubmissionRepository.findByChallengeIdAndGuestAccountId(open.getId(), accountId))
                .thenReturn(Optional.empty());

        GuestHomeRestDto home = service.execute(accountId);

        assertTrue(home.getOpen().get(0).getParticipantCookAssignments().stream()
                .allMatch(assignment -> assignment.getAccountId().get() == null));
    }
}
