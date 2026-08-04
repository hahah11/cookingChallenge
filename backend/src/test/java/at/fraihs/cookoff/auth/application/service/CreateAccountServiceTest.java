package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequest;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreateAccountService service;

    @BeforeEach
    void setUp() {
        service = new CreateAccountService(accountRepository, passwordEncoder, new AccountModelMapperImpl());
    }

    @Test
    void should_createAccount_when_emailIsNotTaken() {
        when(accountRepository.existsByEmail(new Email("host@example.com"))).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        at.fraihs.cookoff.shared.web.openapi.model.Account result = service.execute(
                new CreateAccountRequest("host@example.com", "Host").roles(List.of(SystemRole.ORGANIZER)));

        assertEquals("host@example.com", result.getEmail());
        assertEquals("Host", result.getName());
        assertTrue(result.getRoles().contains(SystemRole.ORGANIZER));
    }

    @Test
    void should_throw_when_emailIsAlreadyTaken() {
        when(accountRepository.existsByEmail(new Email("host@example.com"))).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class, () -> service.execute(
                new CreateAccountRequest("host@example.com", "Host")));
    }
}
