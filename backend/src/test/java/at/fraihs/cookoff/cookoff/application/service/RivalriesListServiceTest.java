package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryRestDto;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RivalriesListServiceTest {

    @Mock
    private CookRivalryRepository cookRivalryRepository;

    @Mock
    private AccountLookup accountLookup;

    @InjectMocks
    private RivalriesListService service;

    private final AccountId aliceId = AccountId.generate();
    private final AccountId bobId = AccountId.generate();

    @Test
    void should_returnRivalryWithHeadline_when_onePairExists() {
        CookRivalry rivalry = CookRivalry.reconstitute(
                at.fraihs.cookoff.cookoff.domain.model.CookRivalryId.generate(), aliceId, bobId, 3, 1, 1, 5);
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(cookRivalryRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(rivalry), pageRequest, 1));
        when(accountLookup.getById(aliceId)).thenReturn(new AccountSummary(aliceId, new Email("alice@example.com"), "Alice", "Alice"));
        when(accountLookup.getById(bobId)).thenReturn(new AccountSummary(bobId, new Email("bob@example.com"), "Bob", "Bob"));

        PagedResult<RivalryRestDto> result = service.execute(0, 20);

        assertEquals(1, result.content().size());
        RivalryRestDto view = result.content().get(0);
        assertEquals("Alice", view.getCookAName());
        assertEquals("Bob", view.getCookBName());
        assertEquals("Alice leads Bob 3-1 (1 draw)", view.getHeadline());
        assertEquals(1L, result.pagination().getTotalElements());
    }

    @Test
    void should_returnEmptyPage_when_noRivalriesExist() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<CookRivalry> empty = new PageImpl<>(List.of(), pageRequest, 0);
        when(cookRivalryRepository.findAll(pageRequest)).thenReturn(empty);

        PagedResult<RivalryRestDto> result = service.execute(0, 20);

        assertEquals(0, result.content().size());
        assertEquals(0L, result.pagination().getTotalElements());
    }
}
