package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.port.RegistrationInvite;
import at.fraihs.cookoff.auth.application.port.RegistrationInviteRepository;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationInviteServiceTest {

    @Mock
    private RegistrationInviteRepository registrationInviteRepository;

    @InjectMocks
    private RegistrationInviteService service;

    @Test
    void should_issueInvite_and_verifyItBackToTheSameChallenge() {
        AccountId organizerId = AccountId.generate();
        ArgumentCaptor<RegistrationInvite> captor = ArgumentCaptor.forClass(RegistrationInvite.class);
        when(registrationInviteRepository.save(any(RegistrationInvite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.issue(organizerId, 42L, Duration.ofDays(30));

        verify(registrationInviteRepository).save(captor.capture());
        RegistrationInvite saved = captor.getValue();
        assertEquals(organizerId, saved.issuedByAccountId());
        assertEquals(42L, saved.challengeId());
        assertEquals(token, saved.token());
        assertTrue(saved.expiresAt().isAfter(Instant.now()));

        when(registrationInviteRepository.findByToken(token)).thenReturn(Optional.of(saved));
        assertEquals(42L, service.verify(token));
    }

    @Test
    void should_allowVerifyingTheSameInviteMultipleTimes_becauseOneQrServesManyWalkIns() {
        RegistrationInvite invite = new RegistrationInvite(
                1L, AccountId.generate(), 42L, "tok", Instant.now().plusSeconds(60));
        when(registrationInviteRepository.findByToken("tok")).thenReturn(Optional.of(invite));

        assertEquals(42L, service.verify("tok"));
        assertEquals(42L, service.verify("tok"));
    }

    @Test
    void should_throw_when_tokenIsUnknown() {
        when(registrationInviteRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredLinkException.class, () -> service.verify("missing"));
    }

    @Test
    void should_throw_when_tokenHasExpired() {
        RegistrationInvite expired = new RegistrationInvite(
                1L, AccountId.generate(), 42L, "tok", Instant.now().minusSeconds(1));
        when(registrationInviteRepository.findByToken("tok")).thenReturn(Optional.of(expired));

        assertThrows(InvalidOrExpiredLinkException.class, () -> service.verify("tok"));
    }
}
