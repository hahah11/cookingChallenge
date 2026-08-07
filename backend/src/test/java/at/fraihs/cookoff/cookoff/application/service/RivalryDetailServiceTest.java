package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.exception.RivalryNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalryId;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryDetailRestDto;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RivalryDetailServiceTest {

    @Mock
    private CookRivalryRepository cookRivalryRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private AccountLookup accountLookup;

    @InjectMocks
    private RivalryDetailService service;

    private final AccountId aliceId = AccountId.generate();
    private final AccountId bobId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();

    private void stubNames() {
        when(accountLookup.getById(aliceId)).thenReturn(new AccountSummary(aliceId, new Email("alice@example.com"), "Alice", "Alice"));
        when(accountLookup.getById(bobId)).thenReturn(new AccountSummary(bobId, new Email("bob@example.com"), "Bob", "Bob"));
    }

    @Test
    void should_returnDetail_when_rivalryRecordExists() {
        Challenge challenge = Challenge.create(LocalDate.of(2026, 1, 1), "Finale", new DishName("Schnitzel"),
                aliceId, bobId, List.of(), organizerId);
        CookRivalry rivalry = CookRivalry.reconstitute(CookRivalryId.generate(), aliceId, bobId, 2, 0, 0, 2);
        when(challengeRepository.findByCookPair(aliceId, bobId)).thenReturn(List.of(challenge));
        when(cookRivalryRepository.findByPair(aliceId, bobId)).thenReturn(Optional.of(rivalry));
        stubNames();

        RivalryDetailRestDto detail = service.execute(aliceId, bobId);

        assertEquals("Alice", detail.getCookAName());
        assertEquals(2, detail.getCookAWins());
        assertEquals("Alice leads Bob 2-0", detail.getHeadline());
        assertEquals(1, detail.getChallenges().size());
        assertEquals(challenge.getId().toString(), detail.getChallenges().get(0).getId());
    }

    @Test
    void should_returnZeroedDetail_when_challengesExistButNeverRevealed() {
        Challenge challenge = Challenge.create(LocalDate.of(2026, 1, 1), "Opener", new DishName("Goulash"),
                bobId, aliceId, List.of(), organizerId);
        when(challengeRepository.findByCookPair(aliceId, bobId)).thenReturn(List.of(challenge));
        when(cookRivalryRepository.findByPair(aliceId, bobId)).thenReturn(Optional.empty());
        stubNames();

        RivalryDetailRestDto detail = service.execute(aliceId, bobId);

        assertEquals(0, detail.getCookAWins());
        assertEquals(0, detail.getTotalChallenges());
        assertEquals("Alice and Bob haven't faced off yet.", detail.getHeadline());
    }

    @Test
    void should_canonicalizeOrder_when_calledWithReversedPair() {
        when(challengeRepository.findByCookPair(aliceId, bobId)).thenReturn(List.of());
        when(cookRivalryRepository.findByPair(aliceId, bobId))
                .thenReturn(Optional.of(CookRivalry.reconstitute(CookRivalryId.generate(), aliceId, bobId, 1, 0, 0, 1)));
        stubNames();

        RivalryDetailRestDto detail = service.execute(bobId, aliceId);

        assertEquals(aliceId.toString(), detail.getCookAAccountId());
        assertEquals(bobId.toString(), detail.getCookBAccountId());
    }

    @Test
    void should_throw_when_pairHasNoSharedChallenges() {
        when(challengeRepository.findByCookPair(aliceId, bobId)).thenReturn(List.of());
        when(cookRivalryRepository.findByPair(aliceId, bobId)).thenReturn(Optional.empty());

        assertThrows(RivalryNotFoundException.class, () -> service.execute(aliceId, bobId));
    }
}
