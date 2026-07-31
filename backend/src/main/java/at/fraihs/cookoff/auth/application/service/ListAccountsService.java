package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAccountsService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<AccountView> execute() {
        return accountRepository.findAll().stream().map(AccountView::from).toList();
    }
}
