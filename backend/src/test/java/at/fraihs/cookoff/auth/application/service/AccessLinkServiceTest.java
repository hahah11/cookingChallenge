package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.port.AccessLink;
import at.fraihs.cookoff.auth.application.port.AccessLinkRepository;
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
class AccessLinkServiceTest {

    @Mock
    private AccessLinkRepository accessLinkRepository;

    @InjectMocks
    private AccessLinkService service;

    @Test
    void should_issueLink_and_verifyItBackToTheSameAccount() {
        AccountId accountId = AccountId.generate();
        ArgumentCaptor<AccessLink> captor = ArgumentCaptor.forClass(AccessLink.class);
        when(accessLinkRepository.save(any(AccessLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.issue(accountId, 42L, Duration.ofDays(7));

        verify(accessLinkRepository).save(captor.capture());
        AccessLink saved = captor.getValue();
        assertEquals(accountId, saved.accountId());
        assertEquals(42L, saved.challengeId());
        assertEquals(token, saved.token());
        assertTrue(saved.expiresAt().isAfter(Instant.now()));

        when(accessLinkRepository.findByToken(token)).thenReturn(Optional.of(saved));
        assertEquals(accountId, service.verify(token));
    }

    @Test
    void should_allowVerifyingTheSameLinkTwice_becauseLinksAreReusableUntilExpiry() {
        AccountId accountId = AccountId.generate();
        AccessLink link = new AccessLink(1L, accountId, 42L, "tok", Instant.now().plusSeconds(60), null, Instant.now());
        when(accessLinkRepository.findByToken("tok")).thenReturn(Optional.of(link));

        assertEquals(accountId, service.verify("tok"));
        assertEquals(accountId, service.verify("tok"));
    }

    @Test
    void should_throw_when_tokenIsUnknown() {
        when(accessLinkRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredLinkException.class, () -> service.verify("missing"));
    }

    @Test
    void should_throw_when_tokenHasExpired() {
        AccessLink expired = new AccessLink(1L, AccountId.generate(), 42L, "tok",
                Instant.now().minusSeconds(1), null, Instant.now().minusSeconds(120));
        when(accessLinkRepository.findByToken("tok")).thenReturn(Optional.of(expired));

        assertThrows(InvalidOrExpiredLinkException.class, () -> service.verify("tok"));
    }
}
