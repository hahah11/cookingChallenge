package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenRestDto;

import java.time.Instant;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Signs the JWTs used by every authenticated endpoint, regardless of which login flow issued
 * them ({@link LoginService}'s password login or {@link AccessLinkLoginService}'s access-link
 * exchange) — both need the same claims shape, only the expiry differs.
 */
@Component
@RequiredArgsConstructor
public class JwtIssuer {

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.issuer:cookoff}")
    private String issuer = "cookoff";

    public AuthTokenRestDto issueUntil(Account account, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
                .subject(account.getId().toString())
                .claim("roles", account.getRoles().stream().map(Enum::name).toList())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthTokenRestDto(token, expiresAt.atOffset(ZoneOffset.UTC));
    }
}
