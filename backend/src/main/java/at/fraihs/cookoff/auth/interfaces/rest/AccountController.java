package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.application.dto.CreateAccountCommand;
import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.application.service.ListAccountsService;
import at.fraihs.cookoff.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Host-side account management (create/list). Both actions are organizer/admin-only per
 * docs/cookingChallenge/first-plan.md's API table; JWT-based enforcement is Phase 5 —
 * not yet applied here.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final CreateAccountService createAccountService;
    private final ListAccountsService listAccountsService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountView>> create(@Valid @RequestBody CreateAccountRequest request) {
        CreateAccountCommand command = new CreateAccountCommand(request.email(), request.name(), request.roles());
        AccountView view = createAccountService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(view));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountView>>> list() {
        return ResponseEntity.ok(ApiResponse.of(listAccountsService.execute()));
    }
}
