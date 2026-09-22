package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.application.dto.PasswordResetNotification;
import at.fraihs.cookoff.auth.application.port.PasswordResetNotificationPort;
import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.shared.testsupport.CapturingNotificationPort;
import at.fraihs.cookoff.shared.testsupport.GuestOnboardingTestSupport;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin-initiated password reset end to end, against the real filter chain and database
 * (docs/cookingChallenge/plans/admin-password-reset-plan.md). The notification port is mocked
 * only to recover the link that would otherwise reach a mailbox.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreateAccountService createAccountService;

    @MockitoBean
    private PasswordResetNotificationPort notificationPort;

    @Test
    void should_letTheOrganizerLogInWithTheNewPasswordOnly_when_resetRedeemed() throws Exception {
        String admin = adminToken();
        String organizerId = createAccount("olga@example.com", SystemRoleRestDto.ORGANIZER, "old-password");

        trigger(admin, organizerId).andExpect(status().isAccepted());
        redeem(lastIssuedToken(), "brand-new-password").andExpect(status().isNoContent());

        login("olga@example.com", "brand-new-password").andExpect(status().isOk());
        login("olga@example.com", "old-password").andExpect(status().isUnauthorized());
    }

    @Test
    void should_rejectTheSameLinkTwice_when_alreadyRedeemed() throws Exception {
        String admin = adminToken();
        String organizerId = createAccount("once@example.com", SystemRoleRestDto.ORGANIZER, "old-password");
        trigger(admin, organizerId).andExpect(status().isAccepted());
        String token = lastIssuedToken();

        redeem(token, "first-new-password").andExpect(status().isNoContent());

        redeem(token, "second-new-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    @Test
    void should_killTheEarlierLink_when_aSecondResetIsIssued() throws Exception {
        String admin = adminToken();
        String organizerId = createAccount("twice@example.com", SystemRoleRestDto.ORGANIZER, "old-password");
        trigger(admin, organizerId).andExpect(status().isAccepted());
        String first = lastIssuedToken();

        trigger(admin, organizerId).andExpect(status().isAccepted());

        redeem(first, "brand-new-password").andExpect(status().isUnauthorized());
        redeem(lastIssuedToken(), "brand-new-password").andExpect(status().isNoContent());
    }

    @Test
    void should_return409_when_adminResetsAGuest() throws Exception {
        String admin = adminToken();
        String guestId = createAccount("guest@example.com", SystemRoleRestDto.USER, null);

        trigger(admin, guestId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_NOT_ELIGIBLE"));
    }

    @Test
    void should_return403_when_anOrganizerTriggersAReset() throws Exception {
        createAccount("boss@example.com", SystemRoleRestDto.ORGANIZER, "password123");
        String organizer = tokenFor("boss@example.com", "password123");
        String targetId = createAccount("target@example.com", SystemRoleRestDto.ADMIN, "password123");

        trigger(organizer, targetId).andExpect(status().isForbidden());
    }

    @Test
    void should_return401_when_anonymousTriggersAReset() throws Exception {
        String targetId = createAccount("anon-target@example.com", SystemRoleRestDto.ORGANIZER, "password123");

        mockMvc.perform(post("/api/v1/accounts/" + targetId + "/password-reset"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reachRedeemWithoutAuthorizationHeader_when_tokenUnknown() throws Exception {
        // 401 from the handler with the link error code, not the filter chain's UNAUTHENTICATED.
        redeem("never-issued", "brand-new-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    private String adminToken() throws Exception {
        createAccount("reset-admin@example.com", SystemRoleRestDto.ADMIN, "password123");
        return tokenFor("reset-admin@example.com", "password123");
    }

    private String createAccount(String email, SystemRoleRestDto role, String password) {
        CreateAccountRequestRestDto request = new CreateAccountRequestRestDto(email, "First", "Last").roles(List.of(role));
        if (password != null) {
            request.password(password);
        }
        return createAccountService.execute(request).getId();
    }

    private ResultActions trigger(String bearer, String accountId) throws Exception {
        return mockMvc.perform(post("/api/v1/accounts/" + accountId + "/password-reset")
                .header("Authorization", "Bearer " + bearer));
    }

    private ResultActions redeem(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password-reset")
                .contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + newPassword + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String tokenFor(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return GuestOnboardingTestSupport.extractStringField(objectMapper, body, "accessToken");
    }

    private String lastIssuedToken() {
        ArgumentCaptor<PasswordResetNotification> captor = ArgumentCaptor.forClass(PasswordResetNotification.class);
        verify(notificationPort, atLeastOnce()).sendPasswordReset(captor.capture());
        return CapturingNotificationPort.extractToken(captor.getValue().link());
    }
}
