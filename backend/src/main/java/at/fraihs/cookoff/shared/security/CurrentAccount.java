package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolves the authenticated caller's {@link AccountId} from the JWT's subject claim (see
 * {@code LoginService}/{@code AccessLinkLoginService}) — every request, organizer or guest,
 * is authenticated the same way. Controllers use this instead of
 * {@code @AuthenticationPrincipal AccountId}, since the resource server's principal is a
 * {@link Jwt}, not an {@code AccountId} directly.
 */
public final class CurrentAccount {

    private CurrentAccount() {
    }

    public static AccountId id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return AccountId.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Unrecognized authentication principal: " + principal);
    }
}
