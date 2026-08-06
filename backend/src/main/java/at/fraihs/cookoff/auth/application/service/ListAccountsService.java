package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.shared.web.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAccountsService {

    private final AccountRepository accountRepository;
    private final AccountModelMapper accountModelMapper;

    @Transactional(readOnly = true)
    public PagedResult<AccountRestDto> execute(int page, int size) {
        Page<AccountRestDto> accounts = accountRepository.findAll(PageRequest.of(page, size))
                .map(accountModelMapper::toGenerated);
        return PagedResult.of(accounts);
    }
}
