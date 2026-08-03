package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAccountsService {

    private final AccountRepository accountRepository;

    /**
     * Unpaginated for now - this use case's own rewrite (to the generated, paginated
     * AccountListResponse) is still open, see openapi-first-api-plan.md's Phase 4 status.
     * {@link Pageable#unpaged()} keeps today's "return everything" behavior while letting
     * the port itself already speak Spring Data pagination end to end.
     */
    @Transactional(readOnly = true)
    public List<AccountView> execute() {
        return accountRepository.findAll(Pageable.unpaged()).map(AccountView::from).toList();
    }
}
