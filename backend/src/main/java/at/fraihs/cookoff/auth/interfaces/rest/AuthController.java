package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.service.AccessLinkLoginService;
import at.fraihs.cookoff.auth.application.service.LoginService;
import at.fraihs.cookoff.shared.web.openapi.api.AuthApi;
import at.fraihs.cookoff.shared.web.openapi.model.AccessLinkLoginRequest;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMeta;
import at.fraihs.cookoff.shared.web.openapi.model.AuthToken;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenResponse;
import at.fraihs.cookoff.shared.web.openapi.model.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final LoginService loginService;
    private final AccessLinkLoginService accessLinkLoginService;

    @Override
    public ResponseEntity<AuthTokenResponse> login(LoginRequest loginRequest) {
        AuthToken token = loginService.execute(loginRequest);
        return ResponseEntity.ok(new AuthTokenResponse(token, meta()));
    }

    @Override
    public ResponseEntity<AuthTokenResponse> accessLinkLogin(AccessLinkLoginRequest accessLinkLoginRequest) {
        AuthToken token = accessLinkLoginService.execute(accessLinkLoginRequest);
        return ResponseEntity.ok(new AuthTokenResponse(token, meta()));
    }

    private ApiMeta meta() {
        return new ApiMeta(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
