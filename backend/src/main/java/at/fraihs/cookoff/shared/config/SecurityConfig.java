package at.fraihs.cookoff.shared.config;

import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.shared.security.AccessLinkAuthenticationFilter;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

/**
 * Real JWT + link-token security, replacing Phase 4's permit-all placeholder — see
 * docs/cookingChallenge/plans/backend-persistence-api-security-plan.md Phase 5. Two
 * independent authentication mechanisms feed the same {@code authorizeHttpRequests} rules:
 * <ul>
 *   <li>JWT (Authorization: Bearer ...) via Spring's OAuth2 resource server support
 *   (issued by {@code POST /api/v1/auth/login}, see {@link JwtConfig}) — for organizer/admin
 *   endpoints; roles come from the token's {@code roles} claim.</li>
 *   <li>{@link AccessLinkAuthenticationFilter} — for the guest-facing link-token endpoints
 *   from docs/cookingChallenge/first-plan.md Step 3's API table.</li>
 * </ul>
 * Endpoint roles below mirror that API table exactly (not just the more coarse-grained
 * "organizer/admin-only" grouping this plan's own Phase 5 section lists) — {@code POST
 * /api/v1/accounts} is ADMIN-only there, everything else organizer-or-admin ("organizer+").
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessLinkAuthenticationFilter accessLinkAuthenticationFilter(
            AccessLinkService accessLinkService, ObjectMapper objectMapper) {
        return new AccessLinkAuthenticationFilter(accessLinkService, objectMapper);
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter, AccessLinkAuthenticationFilter accessLinkAuthenticationFilter, ObjectMapper objectMapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/invitations").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges/*/status").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/reveal").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/me/home").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/challenges/*/scores").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/challenges/*/results").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterBefore(accessLinkAuthenticationFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
