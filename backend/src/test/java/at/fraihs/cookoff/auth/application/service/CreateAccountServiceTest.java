package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

        AccountRestDto result = service.execute(
                new CreateAccountRequestRestDto("host@example.com", "Host").roles(List.of(SystemRoleRestDto.ORGANIZER)));

        assertEquals("host@example.com", result.getEmail());
        assertEquals("Host", result.getName());
        assertTrue(result.getRoles().contains(SystemRoleRestDto.ORGANIZER));
    }

    @Test
    void should_throw_when_emailIsAlreadyTaken() {
        when(accountRepository.existsByEmail(new Email("host@example.com"))).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class, () -> service.execute(
                new CreateAccountRequestRestDto("host@example.com", "Host")));
    }
}
