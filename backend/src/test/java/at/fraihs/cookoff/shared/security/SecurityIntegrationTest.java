package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.application.dto.CreateAccountCommand;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.dto.CreateChallengeCommand;
import at.fraihs.cookoff.cookoff.application.service.CreateChallengeService;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end Phase 5 security checks against the real filter chain (JWT resource server +
 * AccessLinkAuthenticationFilter), per docs/cookingChallenge/plans/
 * backend-persistence-api-security-plan.md Phase 5's own "Verify Phase 5" checklist — this is
 * deliberately a @SpringBootTest, not a @WebMvcTest, since the thing under test IS the
 * SecurityConfig wiring; per-controller @WebMvcTest slices disable the filter chain entirely
 * (see ChallengeControllerTest/HomeControllerTest/AccountControllerTest) and only cover
 * controller/application-service behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreateAccountService createAccountService;

    @Autowired
    private CreateChallengeService createChallengeService;

    @Autowired
    private AccessLinkService accessLinkService;

    @Test
    void should_return401_when_unauthenticatedRequestHitsOrganizerOnlyEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/challenges"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void should_return403_when_userRoleJwtHitsOrganizerOnlyEndpoint() throws Exception {
        createAccountService.execute(
                new CreateAccountCommand("user@example.com", "User", Set.of(SystemRole.USER), "password123"));
        String token = login("user@example.com", "password123");

        mockMvc.perform(get("/api/v1/challenges").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void should_return200_when_organizerRoleJwtHitsOrganizerOnlyEndpoint() throws Exception {
        createAccountService.execute(
                new CreateAccountCommand("organizer@example.com", "Organizer", Set.of(SystemRole.ORGANIZER), "password123"));
        String token = login("organizer@example.com", "password123");

        mockMvc.perform(get("/api/v1/challenges").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void should_return401_when_loginCredentialsInvalid() throws Exception {
        createAccountService.execute(
                new CreateAccountCommand("known@example.com", "Known", Set.of(SystemRole.ORGANIZER), "password123"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"known@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void should_return200_when_validLinkTokenHitsGuestEndpoint() throws Exception {
        String linkToken = issueLinkTokenForNewGuest();

        mockMvc.perform(get("/api/v1/me/home").param("token", linkToken))
                .andExpect(status().isOk());
    }

    @Test
    void should_return401_when_linkTokenInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/me/home").param("token", "not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    @Test
    void should_return401_when_linkTokenExpired() throws Exception {
        AccountId guest = AccountId.fromString(createAccountService.execute(
                new CreateAccountCommand("expired-guest@example.com", "Guest", Set.of(SystemRole.USER), null)).id());
        long challengeId = TsidSupport.fromBase32(createSampleChallenge().id());
        String expiredToken = accessLinkService.issue(guest, challengeId, Duration.ofSeconds(-1));

        mockMvc.perform(get("/api/v1/me/home").param("token", expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    private String issueLinkTokenForNewGuest() {
        AccountView guest = createAccountService.execute(
                new CreateAccountCommand("guest@example.com", "Guest", Set.of(SystemRole.USER), null));
        long challengeId = TsidSupport.fromBase32(createSampleChallenge().id());
        return accessLinkService.issue(AccountId.fromString(guest.id()), challengeId, Duration.ofDays(1));
    }

    private ChallengeView createSampleChallenge() {
        AccountView cookA = createAccountService.execute(
                new CreateAccountCommand("cook-a@example.com", "Cook A", Set.of(SystemRole.USER), null));
        AccountView cookB = createAccountService.execute(
                new CreateAccountCommand("cook-b@example.com", "Cook B", Set.of(SystemRole.USER), null));
        AccountView organizer = createAccountService.execute(
                new CreateAccountCommand("challenge-organizer@example.com", "Organizer", Set.of(SystemRole.ORGANIZER), null));
        return createChallengeService.execute(new CreateChallengeCommand(
                LocalDate.now(), "Title", "Schnitzel", cookA.id(), cookB.id(), List.of(), organizer.id()));
    }

    @SuppressWarnings("unchecked")
    private String login(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        String token = (String) data.get("accessToken");
        assertNotNull(token);
        return token;
    }
}
