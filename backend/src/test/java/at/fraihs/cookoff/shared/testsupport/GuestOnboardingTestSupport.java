package at.fraihs.cookoff.shared.testsupport;

import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;

import java.time.Duration;
import java.util.Map;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared "issue token -> redeem token" helpers for the two guest-onboarding flows (access-link
 * login, QR self-registration) — see
 * docs/cookingChallenge/plans/link-login-qr-registration-test-plan.md. Every helper stays at
 * the token/JSON level: no QR image is ever rendered and no email is ever sent, matching how
 * each token actually reaches its caller in production — the registration-invite token comes
 * back as plain JSON to the organizer's own authenticated call, and the access-link token can
 * be minted directly via {@link AccessLinkService} instead of parsed out of a sent email.
 */
public final class GuestOnboardingTestSupport {

    private GuestOnboardingTestSupport() {
    }

    // --- access-link login ---

    public static String issueAccessLinkToken(
            AccessLinkService accessLinkService, AccountId guestAccountId, long challengeId, Duration validFor) {
        return accessLinkService.issue(guestAccountId, challengeId, validFor);
    }

    public static ResultActions performAccessLinkLogin(MockMvc mockMvc, String linkToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/access-link-login")
                .contentType("application/json")
                .content("{\"token\":\"" + linkToken + "\"}"));
    }

    public static String exchangeAccessLinkForJwt(MockMvc mockMvc, ObjectMapper objectMapper, String linkToken) throws Exception {
        String responseBody = performAccessLinkLogin(mockMvc, linkToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractStringField(objectMapper, responseBody, "accessToken");
    }

    // --- QR self-registration ---

    public static ResultActions performCreateRegistrationInvite(MockMvc mockMvc, String organizerJwt, String challengeId) throws Exception {
        return mockMvc.perform(post("/api/v1/challenges/" + challengeId + "/registration-invites")
                .header("Authorization", "Bearer " + organizerJwt));
    }

    public static String createRegistrationInviteToken(
            MockMvc mockMvc, ObjectMapper objectMapper, String organizerJwt, String challengeId) throws Exception {
        String responseBody = performCreateRegistrationInvite(mockMvc, organizerJwt, challengeId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extractStringField(objectMapper, responseBody, "token");
    }

    public static ResultActions performSelfRegistration(
            MockMvc mockMvc, String inviteToken, String firstName, String lastName, String email) throws Exception {
        return mockMvc.perform(post("/api/v1/public/registrations")
                .contentType("application/json")
                .content("{\"token\":\"" + inviteToken + "\",\"firstName\":\"" + firstName
                        + "\",\"lastName\":\"" + lastName + "\",\"email\":\"" + email + "\"}"));
    }

    public static Map<String, Object> selfRegisterViaInvite(
            MockMvc mockMvc, ObjectMapper objectMapper, String inviteToken,
            String firstName, String lastName, String email) throws Exception {
        String responseBody = performSelfRegistration(mockMvc, inviteToken, firstName, lastName, email)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return dataOf(objectMapper, responseBody);
    }

    // --- shared response-envelope parsing ({ data: {...}, meta: {...} }) ---

    @SuppressWarnings("unchecked")
    public static Map<String, Object> dataOf(ObjectMapper objectMapper, String responseBody) throws Exception {
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        return (Map<String, Object>) body.get("data");
    }

    public static String extractStringField(ObjectMapper objectMapper, String responseBody, String field) throws Exception {
        Map<String, Object> data = dataOf(objectMapper, responseBody);
        String value = (String) data.get(field);
        assertNotNull(value, "expected \"" + field + "\" in response data: " + responseBody);
        return value;
    }
}
