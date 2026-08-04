package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatus;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateChallengeServiceTest {

    @Mock
    private AccountLookup accountLookup;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private CreateChallengeService service;

    private final AccountId organizerId = AccountId.generate();
    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();

    private CreateChallengeRequest request() {
        return new CreateChallengeRequest(LocalDate.now(), "Season Finale", "Schnitzel",
                cookAId.toString(), cookBId.toString()).guestAccountIds(List.of());
    }

    @Test
    void should_createChallenge_when_organizerCanOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        at.fraihs.cookoff.shared.web.openapi.model.Challenge result = service.execute(request(), organizerId);

        assertEquals("Schnitzel", result.getDishName());
        assertEquals(ChallengeStatus.OPEN, result.getStatus());
    }

    @Test
    void should_throw_when_organizerAccountDoesNotExist() {
        when(accountLookup.canOrganize(organizerId)).thenThrow(new AccountNotFoundException(organizerId.toString()));

        assertThrows(AccountNotFoundException.class, () -> service.execute(request(), organizerId));
    }

    @Test
    void should_throw_when_accountCannotOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.execute(request(), organizerId));
    }
}
