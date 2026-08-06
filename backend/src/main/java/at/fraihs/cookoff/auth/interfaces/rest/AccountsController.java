package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.application.service.GetAccountDetailService;
import at.fraihs.cookoff.auth.application.service.ListAccountsService;
import at.fraihs.cookoff.auth.application.service.UpdateAccountService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.api.AccountsApi;
import at.fraihs.cookoff.shared.web.openapi.model.AccountDetailResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AccountListResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AccountResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMetaRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateAccountRequestRestDto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountsController implements AccountsApi {

    private final CreateAccountService createAccountService;
    private final ListAccountsService listAccountsService;
    private final GetAccountDetailService getAccountDetailService;
    private final UpdateAccountService updateAccountService;

    @Override
    public ResponseEntity<AccountResponseRestDto> createAccount(CreateAccountRequestRestDto createAccountRequest) {
        AccountRestDto account = createAccountService.execute(createAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AccountResponseRestDto(account, meta()));
    }

    @Override
    public ResponseEntity<AccountDetailResponseRestDto> getAccount(String accountId) {
        AccountRestDto account = getAccountDetailService.execute(AccountId.fromString(accountId));
        return ResponseEntity.ok(new AccountDetailResponseRestDto(account, meta()));
    }

    @Override
    public ResponseEntity<AccountListResponseRestDto> listAccounts(Integer page, Integer size) {
        PagedResult<AccountRestDto> result = listAccountsService.execute(page, size);
        return ResponseEntity.ok(new AccountListResponseRestDto(result.content(), result.pagination(), meta()));
    }

    @Override
    public ResponseEntity<AccountResponseRestDto> updateAccount(String accountId, UpdateAccountRequestRestDto updateAccountRequest) {
        AccountRestDto account = updateAccountService.execute(AccountId.fromString(accountId), updateAccountRequest);
        return ResponseEntity.ok(new AccountResponseRestDto(account, meta()));
    }

    private ApiMetaRestDto meta() {
        return new ApiMetaRestDto(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
