package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolves the authenticated caller's {@link AccountId} regardless of which of the two
 * authentication mechanisms handled the request: {@link AccessLinkAuthenticationFilter} sets
 * the principal directly to an {@code AccountId}, while the JWT resource server sets it to a
 * {@link Jwt} whose subject claim is the account id (see {@code LoginService}). Controllers
 * use this instead of {@code @AuthenticationPrincipal AccountId}, which only binds correctly
 * for the link-token case.
 */
public final class CurrentAccount {

    private CurrentAccount() {
    }

    public static AccountId id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof AccountId accountId) {
            return accountId;
        }
        if (principal instanceof Jwt jwt) {
            return AccountId.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Unrecognized authentication principal: " + principal);
    }
}
