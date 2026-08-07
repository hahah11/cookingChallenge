package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.dto.AccessLink;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.openapi.model.AccessLinkLoginRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenRestDto;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessLinkLoginServiceTest {

    @Mock
    private AccessLinkService accessLinkService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JwtEncoder jwtEncoder;

    private AccessLinkLoginService service;

    @BeforeEach
    void setUp() {
        service = new AccessLinkLoginService(accessLinkService, accountRepository, new JwtIssuer(jwtEncoder));
    }

    @Test
    void should_issueToken_when_linkTokenValid() {
        AccountId accountId = AccountId.generate();
        Account account = Account.create(new Email("guest@example.com"), "Guest", "One", SystemRole.USER);
        AccessLink accessLink = new AccessLink(1L, accountId, 42L, "tok",
                Instant.now().plus(Duration.ofDays(30)), null, Instant.now());
        when(accessLinkService.verify("tok")).thenReturn(accessLink);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        stubJwtEncoder("signed-jwt");

        AuthTokenRestDto result = service.execute(new AccessLinkLoginRequestRestDto("tok"));

        assertEquals("signed-jwt", result.getAccessToken());
    }

    @Test
    void should_capExpiry_when_linkOutlivesGuestExpirationCap() {
        AccountId accountId = AccountId.generate();
        Account account = Account.create(new Email("guest@example.com"), "Guest", "One", SystemRole.USER);
        Instant farFuture = Instant.now().plus(Duration.ofDays(30));
        AccessLink accessLink = new AccessLink(1L, accountId, 42L, "tok", farFuture, null, Instant.now());
        when(accessLinkService.verify("tok")).thenReturn(accessLink);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        stubJwtEncoder("signed-jwt");

        AuthTokenRestDto result = service.execute(new AccessLinkLoginRequestRestDto("tok"));

        assertTrue(result.getExpiresAt().toInstant().isBefore(farFuture));
    }

    @Test
    void should_throw_when_linkTokenInvalidOrExpired() {
        when(accessLinkService.verify("bad")).thenThrow(new InvalidOrExpiredLinkException());

        assertThrows(InvalidOrExpiredLinkException.class,
                () -> service.execute(new AccessLinkLoginRequestRestDto("bad")));
    }

    @Test
    void should_throw_when_linkedAccountNoLongerExists() {
        AccountId accountId = AccountId.generate();
        AccessLink accessLink = new AccessLink(1L, accountId, 42L, "tok",
                Instant.now().plus(Duration.ofDays(1)), null, Instant.now());
        when(accessLinkService.verify("tok")).thenReturn(accessLink);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredLinkException.class,
                () -> service.execute(new AccessLinkLoginRequestRestDto("tok")));
    }

    private void stubJwtEncoder(String tokenValue) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(jwtEncoder.encode(any())).thenReturn(jwt);
    }
}
