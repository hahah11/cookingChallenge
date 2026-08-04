package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.exception.InvalidCredentialsException;
import at.fraihs.cookoff.auth.application.service.LoginService;
import at.fraihs.cookoff.shared.config.JacksonConfig;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import at.fraihs.cookoff.shared.web.openapi.model.AuthToken;
import at.fraihs.cookoff.shared.web.openapi.model.LoginRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security enforcement (login is permitAll) is covered by
 * {@code shared.security.SecurityIntegrationTest} — this slice test disables the security
 * filter chain to focus purely on controller/application-service wiring.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoginService loginService;

    @Test
    void should_return200_when_credentialsValid() throws Exception {
        AuthToken token = new AuthToken("signed-jwt", OffsetDateTime.parse("2026-08-04T12:00:00Z"));
        when(loginService.execute(any())).thenReturn(token);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest("a@b.com", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("signed-jwt"));
    }

    @Test
    void should_return400_when_emailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest("not-an-email", "secret"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return401_when_credentialsInvalid() throws Exception {
        when(loginService.execute(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginRequest("a@b.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }
}
