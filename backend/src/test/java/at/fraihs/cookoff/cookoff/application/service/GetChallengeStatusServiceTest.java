package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeDetailRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.GuestSubmissionStatusRestDto;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetChallengeStatusServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @Mock
    private AccountLookup accountLookup;

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
        when(accountLookup.getById(guest1)).thenReturn(new AccountSummary(guest1, new Email("g1@x.com"), "Guest One", "Guest"));
        when(accountLookup.getById(guest2)).thenReturn(new AccountSummary(guest2, new Email("g2@x.com"), "Guest Two", "Guest"));
        when(accountLookup.getById(cookA)).thenReturn(new AccountSummary(cookA, new Email("a@x.com"), "Cook A", "Cook"));
        when(accountLookup.getById(cookB)).thenReturn(new AccountSummary(cookB, new Email("b@x.com"), "Cook B", "Cook"));

        List<Score> scores = List.of(new Score(DishLabel.A, Category.GESCHMACK, 5));
        ScoreSubmission guestSubmission = ScoreSubmission.reconstitute(
                ScoreSubmissionId.generate(), challenge.getId(), guest1, scores, Instant.now());
        ScoreSubmission cookSubmission = ScoreSubmission.reconstitute(
                ScoreSubmissionId.generate(), challenge.getId(), cookA, scores, Instant.now());
        when(scoreSubmissionRepository.findByChallengeId(challenge.getId()))
                .thenReturn(List.of(guestSubmission, cookSubmission));

        ChallengeDetailRestDto status = service.execute(challenge.getId().toString());

        assertEquals(2, status.getTotalGuestCount());
        assertEquals(1, status.getSubmittedGuestCount());
        assertEquals(guest1.toString(), status.getGuests().stream()
                .filter(GuestSubmissionStatusRestDto::getSubmitted)
                .findFirst().orElseThrow().getAccountId());
        assertEquals("Schnitzel", status.getDishName());
        assertEquals(challenge.getDate(), status.getDate());
        assertEquals(2, status.getCookAssignments().size());
    }

    @Test
    void should_throw_when_challengeDoesNotExist() {
        when(challengeRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ChallengeNotFoundException.class,
                () -> service.execute(AccountId.generate().toString()));
    }
}
