package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.service.AccessLinkLoginService;
import at.fraihs.cookoff.auth.application.service.ChangeAccountLocaleService;
import at.fraihs.cookoff.auth.application.service.LoginService;
import at.fraihs.cookoff.auth.application.service.PasswordResetRedeemService;
import at.fraihs.cookoff.shared.security.CurrentAccount;
import at.fraihs.cookoff.shared.web.openapi.api.AuthApi;
import at.fraihs.cookoff.shared.web.openapi.model.AccessLinkLoginRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMetaRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.AuthTokenRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.LoginRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PasswordResetRedeemRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateLocaleRequestRestDto;

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
    private final PasswordResetRedeemService passwordResetRedeemService;
    private final ChangeAccountLocaleService changeAccountLocaleService;

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

    @Override
    public ResponseEntity<Void> redeemPasswordReset(PasswordResetRedeemRequestRestDto passwordResetRedeemRequest) {
        passwordResetRedeemService.execute(passwordResetRedeemRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> updateMyLocale(UpdateLocaleRequestRestDto updateLocaleRequest) {
        changeAccountLocaleService.execute(CurrentAccount.id(), updateLocaleRequest);
        return ResponseEntity.noContent().build();
    }

    private ApiMetaRestDto meta() {
        return new ApiMetaRestDto(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
