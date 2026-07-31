package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.application.dto.CreateAccountCommand;
import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateAccountService service;

    @Test
    void should_createAccount_when_emailIsNotTaken() {
        when(accountRepository.existsByEmail(new Email("host@example.com"))).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountView view = service.execute(
                new CreateAccountCommand("host@example.com", "Host", Set.of(SystemRole.ORGANIZER), null));

        assertEquals("host@example.com", view.email());
        assertEquals("Host", view.name());
        assertTrue(view.roles().contains(SystemRole.ORGANIZER));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(view.id(), captor.getValue().getId().toString());
    }

    @Test
    void should_throw_when_emailIsAlreadyTaken() {
        when(accountRepository.existsByEmail(new Email("host@example.com"))).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class, () -> service.execute(
                new CreateAccountCommand("host@example.com", "Host", Set.of(), null)));
    }
}
