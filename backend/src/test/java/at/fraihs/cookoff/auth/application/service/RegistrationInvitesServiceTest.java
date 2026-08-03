package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.RegistrationResult;
import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationInvitesServiceTest {

    @Mock
    private RegistrationInviteService registrationInviteService;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private RegistrationInvitesService service;

    @Test
    void should_delegateToRegistrationInviteService_when_issuing() {
        AccountId organizerId = AccountId.generate();
        when(registrationInviteService.issue(organizerId, 42L, Duration.ofDays(30))).thenReturn("tok");

        String token = service.issue(organizerId, 42L, Duration.ofDays(30));

        assertEquals("tok", token);
    }

    @Test
    void should_registerNewAccount_and_returnItWithTheInvitedChallenge() {
        when(registrationInviteService.verify("tok")).thenReturn(42L);
        when(accountRepository.existsByEmail(new Email("walkin@example.com"))).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResult result = service.register("tok", "Walk", "In", "walkin@example.com");

        assertEquals(42L, result.challengeId());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(result.accountId(), captor.getValue().getId());
        assertEquals("Walk In", captor.getValue().getName());
        assertEquals(new Email("walkin@example.com"), captor.getValue().getEmail());
    }

    @Test
    void should_throw_when_emailIsAlreadyRegistered() {
        when(registrationInviteService.verify("tok")).thenReturn(42L);
        when(accountRepository.existsByEmail(new Email("walkin@example.com"))).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class,
                () -> service.register("tok", "Walk", "In", "walkin@example.com"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void should_throw_when_tokenIsInvalidOrExpired() {
        when(registrationInviteService.verify("bad-tok")).thenThrow(new InvalidOrExpiredLinkException());

        assertThrows(InvalidOrExpiredLinkException.class,
                () -> service.register("bad-tok", "Walk", "In", "walkin@example.com"));
        verify(accountRepository, never()).save(any());
    }
}
