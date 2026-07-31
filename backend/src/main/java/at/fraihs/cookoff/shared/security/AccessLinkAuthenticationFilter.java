package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.web.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Resolves the {@code token} query param on link-token endpoints (docs/cookingChallenge/
 * plans/backend-persistence-api-security-plan.md Phase 5) into an authenticated
 * {@link AccountId} principal, replacing the per-controller {@code AccessLinkService.verify()}
 * calls Phase 4 used as a stopgap. Only runs against the exact endpoints the API table marks
 * "link token" — every other request passes through untouched and is left to the JWT resource
 * server / {@code authorizeHttpRequests} rules in SecurityConfig.
 */
@RequiredArgsConstructor
public class AccessLinkAuthenticationFilter extends OncePerRequestFilter {

    private static final RequestMatcher LINK_TOKEN_ENDPOINTS = new OrRequestMatcher(List.of(
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/me/home"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/challenges/{id}"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/challenges/{id}/scores"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/challenges/{id}/results")));

    private final AccessLinkService accessLinkService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!LINK_TOKEN_ENDPOINTS.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getParameter("token");
        try {
            if (token == null || token.isBlank()) {
                throw new InvalidOrExpiredLinkException();
            }
            AccountId accountId = accessLinkService.verify(token);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    accountId, null, List.of(new SimpleGrantedAuthority("ROLE_LINK")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidOrExpiredLinkException ex) {
            writeUnauthorized(response, ex);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, InvalidOrExpiredLinkException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of("INVALID_OR_EXPIRED_LINK", ex.getMessage()));
    }
}
