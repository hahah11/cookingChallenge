package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.mapper.AccountModelMapper;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateAccountRequestRestDto;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edits firstName/lastName/email/roles on an existing account. {@code roles} is documented as "replaces
 * the account's full role set" but is optional in the request schema and Jackson can't tell
 * an omitted field from an explicitly empty array here (no {@code JsonNullable} wrapper) -
 * an empty list is therefore treated as "roles not being changed", not "clear all roles";
 * {@code Account.revokeRole} already refuses to drop the last role anyway.
 */
@Service
@RequiredArgsConstructor
public class UpdateAccountService {

    private final AccountRepository accountRepository;
    private final AccountModelMapper accountModelMapper;

    @Transactional
    public AccountRestDto execute(AccountId id, UpdateAccountRequestRestDto request) {
        at.fraihs.cookoff.auth.domain.model.Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));

        if (request.getFirstName() != null) {
            account.renameFirst(request.getFirstName());
        }
        if (request.getLastName() != null) {
            account.renameLast(request.getLastName());
        }
        if (request.getEmail() != null) {
            changeEmail(account, request.getEmail());
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            applyRoles(account, request.getRoles());
        }

        accountRepository.save(account);
        return accountModelMapper.toGenerated(account);
    }

    private void changeEmail(at.fraihs.cookoff.auth.domain.model.Account account, String newEmailValue) {
        Email newEmail = new Email(newEmailValue);
        if (!newEmail.equals(account.getEmail()) && accountRepository.existsByEmail(newEmail)) {
            throw new AccountAlreadyExistsException(newEmailValue);
        }
        account.changeEmail(newEmail);
    }

    /** Grants every target role before revoking any dropped one, so the account never transiently holds zero roles. */
    private void applyRoles(at.fraihs.cookoff.auth.domain.model.Account account,
                             List<SystemRoleRestDto> targetGeneratedRoles) {
        Set<SystemRole> target = targetGeneratedRoles.stream()
                .map(role -> SystemRole.valueOf(role.name()))
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(SystemRole.class)));
        for (SystemRole role : target) {
            if (!account.hasRole(role)) {
                account.grantRole(role);
            }
        }
        for (SystemRole role : SystemRole.values()) {
            if (!target.contains(role) && account.hasRole(role)) {
                account.revokeRole(role);
            }
        }
    }
}
