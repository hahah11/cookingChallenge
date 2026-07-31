package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.dto.AuthTokenView;
import at.fraihs.cookoff.auth.application.dto.LoginCommand;
import at.fraihs.cookoff.auth.application.service.LoginService;
import at.fraihs.cookoff.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenView>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenView token = loginService.execute(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(ApiResponse.of(token));
    }
}
