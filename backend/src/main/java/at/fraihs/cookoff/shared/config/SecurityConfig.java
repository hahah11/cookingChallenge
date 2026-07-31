package at.fraihs.cookoff.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Placeholder while Phase 5 (JWT + link-token auth) isn't built yet — permits every
 * request so Phase 4's controllers are reachable, since spring-boot-starter-security on
 * the classpath otherwise auto-locks every endpoint behind generated-password HTTP
 * Basic. Phase 5 replaces this wholesale with real JWT/link-token filters and
 * per-endpoint role rules; nothing here should be assumed to still exist after that.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
