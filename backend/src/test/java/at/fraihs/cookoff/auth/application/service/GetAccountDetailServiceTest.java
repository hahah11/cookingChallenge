package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.mapper.AccountModelMapperImpl;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        Account account = Account.create(new Email("host@example.com"), "Host", "Person", SystemRole.ORGANIZER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        AccountRestDto result = service.execute(account.getId());

        assertEquals(account.getId().toString(), result.getId());
        assertEquals("host@example.com", result.getEmail());
        assertEquals("Host Person", result.getName());
        assertTrue(result.getRoles().contains(SystemRoleRestDto.ORGANIZER));
    }

    @Test
    void should_throw_when_accountDoesNotExist() {
        AccountId missingId = AccountId.generate();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.execute(missingId));
    }
}
