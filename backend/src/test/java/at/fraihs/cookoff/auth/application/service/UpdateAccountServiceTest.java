package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.mapper.AccountModelMapperImpl;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateAccountRequestRestDto;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private UpdateAccountService service;

    @BeforeEach
    void setUp() {
        service = new UpdateAccountService(accountRepository, new AccountModelMapperImpl());
    }

    @Test
    void should_renameAccount_when_nameIsGiven() {
        Account account = Account.create(new Email("host@example.com"), "Host", "Original", SystemRole.ORGANIZER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        AccountRestDto result =
                service.execute(account.getId(), new UpdateAccountRequestRestDto().firstName("New").lastName("Name"));

        assertEquals("New Name", result.getName());
        assertEquals("host@example.com", result.getEmail());
        verify(accountRepository).save(account);
    }

    @Test
    void should_changeEmail_when_newEmailIsFree() {
        Account account = Account.create(new Email("host@example.com"), "Host", "Original", SystemRole.ORGANIZER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.existsByEmail(new Email("new@example.com"))).thenReturn(false);

        AccountRestDto result =
                service.execute(account.getId(), new UpdateAccountRequestRestDto().email("new@example.com"));

        assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void should_throw_when_newEmailIsAlreadyTaken() {
        Account account = Account.create(new Email("host@example.com"), "Host", "Original", SystemRole.ORGANIZER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.existsByEmail(new Email("taken@example.com"))).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class, () -> service.execute(
                account.getId(), new UpdateAccountRequestRestDto().email("taken@example.com")));
    }

    @Test
    void should_notCheckUniqueness_when_emailIsUnchanged() {
        Account account = Account.create(new Email("host@example.com"), "Host", "Original", SystemRole.ORGANIZER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        AccountRestDto result =
                service.execute(account.getId(), new UpdateAccountRequestRestDto().email("host@example.com"));

        assertEquals("host@example.com", result.getEmail());
    }

    @Test
    void should_replaceRoles_when_rolesAreGiven() {
        Account account = Account.create(new Email("host@example.com"), "Host", "Original", SystemRole.USER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        AccountRestDto result = service.execute(account.getId(),
                new UpdateAccountRequestRestDto().roles(List.of(
                        SystemRoleRestDto.ADMIN,
                        SystemRoleRestDto.ORGANIZER)));

        assertTrue(result.getRoles().contains(SystemRoleRestDto.ADMIN));
        assertTrue(result.getRoles().contains(SystemRoleRestDto.ORGANIZER));
        assertEquals(2, result.getRoles().size());
        assertTrue(account.hasRole(SystemRole.ADMIN));
        assertTrue(account.hasRole(SystemRole.ORGANIZER));
    }

    @Test
    void should_leaveRolesUnchanged_when_rolesListIsEmpty() {
        Account account = Account.create(new Email("host@example.com"), "Host", "Original", SystemRole.USER);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        service.execute(account.getId(), new UpdateAccountRequestRestDto().roles(List.of()));

        assertTrue(account.hasRole(SystemRole.USER));
        assertEquals(1, account.getRoles().size());
    }

    @Test
    void should_throw_when_accountDoesNotExist() {
        AccountId missingId = AccountId.generate();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.execute(
                missingId, new UpdateAccountRequestRestDto().firstName("New").lastName("Name")));
    }
}
