package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.service.AccessLinkLoginService;
import at.fraihs.cookoff.auth.application.service.LoginService;
import at.fraihs.cookoff.shared.web.openapi.api.AuthApi;
import at.fraihs.cookoff.shared.web.openapi.model.AccessLinkLoginRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMetaRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.LoginRequestRestDto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final LoginService loginService;
    private final AccessLinkLoginService accessLinkLoginService;

    @Override
    public ResponseEntity<AuthTokenResponseRestDto> login(LoginRequestRestDto loginRequest) {
        AuthTokenRestDto token = loginService.execute(loginRequest);
        return ResponseEntity.ok(new AuthTokenResponseRestDto(token, meta()));
    }

    @Override
    public ResponseEntity<AuthTokenResponseRestDto> accessLinkLogin(AccessLinkLoginRequestRestDto accessLinkLoginRequest) {
        AuthTokenRestDto token = accessLinkLoginService.execute(accessLinkLoginRequest);
        return ResponseEntity.ok(new AuthTokenResponseRestDto(token, meta()));
    }

    private ApiMetaRestDto meta() {
        return new ApiMetaRestDto(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
