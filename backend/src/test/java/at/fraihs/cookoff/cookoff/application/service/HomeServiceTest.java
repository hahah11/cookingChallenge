package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @InjectMocks
    private HomeService service;

    private final AccountId accountId = AccountId.generate();

    @Test
    void should_excludeChallenges_when_accountAlreadySubmitted() {
        Challenge notYetSubmitted = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(accountId), AccountId.generate());
        Challenge alreadySubmitted = Challenge.create(LocalDate.now(), null, new DishName("Goulash"),
                AccountId.generate(), AccountId.generate(), List.of(accountId), AccountId.generate());
        when(challengeRepository.findOpenByParticipant(accountId))
                .thenReturn(List.of(notYetSubmitted, alreadySubmitted));
        when(scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(notYetSubmitted.getId(), accountId))
                .thenReturn(false);
        when(scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(alreadySubmitted.getId(), accountId))
                .thenReturn(true);

        List<ChallengeParticipantView> home = service.execute(accountId);

        assertEquals(1, home.size());
        assertEquals(notYetSubmitted.getId().toString(), home.get(0).id());
    }

    @Test
    void should_hideCookMapping_when_challengeNotRevealed() {
        Challenge open = Challenge.create(LocalDate.now(), null, new DishName("Schnitzel"),
                AccountId.generate(), AccountId.generate(), List.of(accountId), AccountId.generate());
        when(challengeRepository.findOpenByParticipant(accountId)).thenReturn(List.of(open));
        when(scoreSubmissionRepository.existsByChallengeIdAndGuestAccountId(open.getId(), accountId))
                .thenReturn(false);

        List<ChallengeParticipantView> home = service.execute(accountId);

        assertNull(home.get(0).cookAssignments());
    }
}
