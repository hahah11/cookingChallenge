package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.mapper.AccountModelMapperImpl;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.Language;
import at.fraihs.cookoff.shared.web.openapi.model.LocaleRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateLocaleRequestRestDto;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeAccountLocaleServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private ChangeAccountLocaleService service;

    @BeforeEach
    void setUp() {
        service = new ChangeAccountLocaleService(accountRepository, new AccountModelMapperImpl());
    }

    @Test
    void should_storeTheLanguage_when_accountExists() {
        Account account = Account.create(new Email("guest@example.com"), "Guest", "One");
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        service.execute(account.getId(), new UpdateLocaleRequestRestDto(LocaleRestDto.DE));

        assertEquals(Language.DE, account.getLanguage());
        verify(accountRepository).save(account);
    }

    @Test
    void should_throw_when_accountDoesNotExist() {
        AccountId id = AccountId.generate();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.execute(id, new UpdateLocaleRequestRestDto(LocaleRestDto.DE)));
        verify(accountRepository, never()).save(any());
    }
}
