package at.fraihs.cookoff.shared.config;

import at.fraihs.cookoff.shared.security.RestAccessDeniedHandler;
import at.fraihs.cookoff.shared.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

/**
 * JWT-only security — see docs/cookingChallenge/plans/access-link-jwt-unification-plan.md.
 * Every authenticated endpoint uses the same mechanism: Spring's OAuth2 resource server
 * support validates a bearer JWT (issued by either {@code POST /api/v1/auth/login} for
 * organizer/admin password login, or {@code POST /api/v1/auth/access-link-login} for guests
 * exchanging a personalized access-link token — see {@link JwtConfig}); roles come from the
 * token's {@code roles} claim.
 * <p>
 * Endpoint roles below mirror docs/cookingChallenge/first-plan.md Step 3's API table exactly
 * (not just the more coarse-grained "organizer/admin-only" grouping) — {@code POST
 * /api/v1/accounts} is ADMIN-only there, everything else organizer-or-admin ("organizer+").
 * Guest-facing endpoints (challenge detail, scores, color-pick, results, image, home) are
 * just {@code .authenticated()} — per-challenge authorization (is this account actually a
 * participant/cook of this specific challenge) is enforced at the application-service layer,
 * not here, since it depends on domain state a URL-pattern-based rule can't see.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter, ObjectMapper objectMapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/access-link-login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/config").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/registrations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts/*").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/accounts/*").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/rivalries").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/rivalries/*/*").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/invitations").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges/*/status").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/reveal").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/unreveal").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/challenges/*/participants").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/challenges/*/image").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/registration-invites").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/me/home").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/scores").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges/*/results").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges/*/image").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/color-pick").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
