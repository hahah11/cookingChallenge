package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.domain.model.Language;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountLookupServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountLookupService service;

    @Test
    void should_returnTrue_when_accountHasAdminRole() {
        AccountId id = AccountId.generate();
        Account admin = Account.create(new Email("admin@example.com"), "Ada", "Min", SystemRole.ADMIN);
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.reconstitute(
                id, admin.getEmail(), admin.getFirstName(), admin.getLastName(), null, admin.getRoles(), Language.EN)));

        assertTrue(service.isAdmin(id));
    }

    @Test
    void should_returnFalse_when_accountDoesNotHaveAdminRole() {
        AccountId id = AccountId.generate();
        Account organizer = Account.create(new Email("org@example.com"), "Oli", "Ganizer", SystemRole.ORGANIZER);
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.reconstitute(
                id, organizer.getEmail(), organizer.getFirstName(), organizer.getLastName(), null, organizer.getRoles(), Language.EN)));

        assertFalse(service.isAdmin(id));
    }

    @Test
    void should_exposeTheAccountsLanguageAsLocale_when_summarizingAGermanAccount() {
        AccountId id = AccountId.generate();
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.reconstitute(
                id, new Email("ada@example.com"), "Ada", "Cook", null, Set.of(SystemRole.USER), Language.DE)));

        assertEquals(Locale.GERMAN, service.getById(id).locale());
    }
}
