package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatusRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequestRestDto;

import java.time.LocalDate;
import java.util.List;
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

    private CreateChallengeRequestRestDto request() {
        return new CreateChallengeRequestRestDto(LocalDate.now(), "Season Finale", "Schnitzel",
                cookAId.toString(), cookBId.toString()).guestAccountIds(List.of());
    }

    @Test
    void should_createChallenge_when_organizerCanOrganize() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(accountLookup.getById(any())).thenReturn(
                new AccountSummary(AccountId.generate(), new Email("cook@example.com"), "Cook", "Cook"));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChallengeRestDto result = service.execute(request(), organizerId);

        assertEquals("Schnitzel", result.getDishName());
        assertEquals(ChallengeStatusRestDto.OPEN, result.getStatus());
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
