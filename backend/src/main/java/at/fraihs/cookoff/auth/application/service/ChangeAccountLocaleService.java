package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.mapper.AccountModelMapper;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateLocaleRequestRestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets any authenticated account set its own preferred language (the frontend does this after login). */
@Service
@RequiredArgsConstructor
public class ChangeAccountLocaleService {

    private final AccountRepository accountRepository;
    private final AccountModelMapper accountModelMapper;

    @Transactional
    public void execute(AccountId id, UpdateLocaleRequestRestDto request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
        account.changeLanguage(accountModelMapper.toDomain(request.getLocale()));
        accountRepository.save(account);
    }
}
