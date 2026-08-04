package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.application.service.GetAccountDetailService;
import at.fraihs.cookoff.auth.application.service.ListAccountsService;
import at.fraihs.cookoff.auth.application.service.UpdateAccountService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.web.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.api.AccountsApi;
import at.fraihs.cookoff.shared.web.openapi.model.Account;
import at.fraihs.cookoff.shared.web.openapi.model.AccountDetailResponse;
import at.fraihs.cookoff.shared.web.openapi.model.AccountListResponse;
import at.fraihs.cookoff.shared.web.openapi.model.AccountResponse;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMeta;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequest;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AccountsController implements AccountsApi {

    private final CreateAccountService createAccountService;
    private final ListAccountsService listAccountsService;
    private final GetAccountDetailService getAccountDetailService;
    private final UpdateAccountService updateAccountService;

    @Override
    public ResponseEntity<AccountResponse> createAccount(CreateAccountRequest createAccountRequest) {
        Account account = createAccountService.execute(createAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AccountResponse(account, meta()));
    }

    @Override
    public ResponseEntity<AccountDetailResponse> getAccount(String accountId) {
        Account account = getAccountDetailService.execute(AccountId.fromString(accountId));
        return ResponseEntity.ok(new AccountDetailResponse(account, meta()));
    }

    @Override
    public ResponseEntity<AccountListResponse> listAccounts(Integer page, Integer size) {
        PagedResult<Account> result = listAccountsService.execute(page, size);
        return ResponseEntity.ok(new AccountListResponse(result.content(), result.pagination(), meta()));
    }

    @Override
    public ResponseEntity<AccountResponse> updateAccount(String accountId, UpdateAccountRequest updateAccountRequest) {
        Account account = updateAccountService.execute(AccountId.fromString(accountId), updateAccountRequest);
        return ResponseEntity.ok(new AccountResponse(account, meta()));
    }

    private ApiMeta meta() {
        return new ApiMeta(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
