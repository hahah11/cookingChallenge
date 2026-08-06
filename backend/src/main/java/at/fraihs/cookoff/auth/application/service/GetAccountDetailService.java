package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.mapper.AccountModelMapper;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Backs the edit-account dialog's on-open fetch - always fresh, never reused from the accounts list. */
@Service
@RequiredArgsConstructor
public class GetAccountDetailService {

    private final AccountRepository accountRepository;
    private final AccountModelMapper accountModelMapper;

    @Transactional(readOnly = true)
    public AccountRestDto execute(AccountId id) {
        at.fraihs.cookoff.auth.domain.model.Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
        return accountModelMapper.toGenerated(account);
    }
}
