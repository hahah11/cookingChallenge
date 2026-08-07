package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.mapper.AccountModelMapperImpl;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAccountsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private ListAccountsService service;

    @BeforeEach
    void setUp() {
        service = new ListAccountsService(accountRepository, new AccountModelMapperImpl());
    }

    @Test
    void should_returnMappedAccounts_when_accountsExist() {
        at.fraihs.cookoff.auth.domain.model.Account account =
                at.fraihs.cookoff.auth.domain.model.Account.create(new Email("host@example.com"), "Host", "Admin", SystemRole.ORGANIZER);
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(accountRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(account), pageRequest, 1));

        PagedResult<AccountRestDto> result = service.execute(0, 20);

        assertEquals(1, result.content().size());
        assertEquals("host@example.com", result.content().get(0).getEmail());
        assertEquals(1L, result.pagination().getTotalElements());
    }

    @Test
    void should_returnEmptyPage_when_noAccountsExist() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<at.fraihs.cookoff.auth.domain.model.Account> empty = new PageImpl<>(List.of(), pageRequest, 0);
        when(accountRepository.findAll(pageRequest)).thenReturn(empty);

        PagedResult<AccountRestDto> result = service.execute(0, 20);

        assertEquals(0, result.content().size());
        assertEquals(0L, result.pagination().getTotalElements());
    }
}
