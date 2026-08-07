package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.InvalidCredentialsException;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.LoginRequestRestDto;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private LoginService service;

    @BeforeEach
    void setUp() {
        JwtIssuer jwtIssuer = new JwtIssuer(jwtEncoder);
        service = new LoginService(accountRepository, passwordEncoder, jwtIssuer);
    }

    @Test
    void should_issueToken_when_credentialsValid() {
        Account account = Account.create(new Email("a@b.com"), "Alice", "Cook", SystemRole.ORGANIZER);
        account.changePasswordHash("hashed");
        when(accountRepository.findByEmail(new Email("a@b.com"))).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("signed-jwt");
        when(jwtEncoder.encode(any())).thenReturn(jwt);

        AuthTokenRestDto result = service.execute(new LoginRequestRestDto("a@b.com", "secret"));

        assertEquals("signed-jwt", result.getAccessToken());
    }

    @Test
    void should_throw_when_emailUnknown() {
        when(accountRepository.findByEmail(new Email("a@b.com"))).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> service.execute(new LoginRequestRestDto("a@b.com", "secret")));
    }

    @Test
    void should_throw_when_passwordWrong() {
        Account account = Account.create(new Email("a@b.com"), "Alice", "Cook", SystemRole.ORGANIZER);
        account.changePasswordHash("hashed");
        when(accountRepository.findByEmail(new Email("a@b.com"))).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> service.execute(new LoginRequestRestDto("a@b.com", "wrong")));
    }

    @Test
    void should_throw_when_accountHasNoPasswordSet() {
        Account account = Account.create(new Email("a@b.com"), "Alice", "Cook", SystemRole.USER);
        when(accountRepository.findByEmail(new Email("a@b.com"))).thenReturn(Optional.of(account));

        assertThrows(InvalidCredentialsException.class,
                () -> service.execute(new LoginRequestRestDto("a@b.com", "anything")));
    }
}
