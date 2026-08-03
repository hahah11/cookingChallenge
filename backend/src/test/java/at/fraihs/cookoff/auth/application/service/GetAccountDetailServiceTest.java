package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountDetailServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private GetAccountDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetAccountDetailService(accountRepository, new AccountModelMapperImpl());
    }

    @Test
    void should_returnAccount_when_itExists() {
        Account account = Account.create(new Email("host@example.com"), "Host", SystemRole.ORGANIZER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        at.fraihs.cookoff.shared.web.openapi.model.Account result = service.execute(account.getId());

        assertEquals(account.getId().toString(), result.getId());
        assertEquals("host@example.com", result.getEmail());
        assertEquals("Host", result.getName());
        assertTrue(result.getRoles().contains(at.fraihs.cookoff.shared.web.openapi.model.SystemRole.ORGANIZER));
    }

    @Test
    void should_throw_when_accountDoesNotExist() {
        AccountId missingId = AccountId.generate();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.execute(missingId));
    }
}
