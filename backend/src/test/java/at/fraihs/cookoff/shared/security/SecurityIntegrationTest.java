package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.service.CreateChallengeService;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import at.fraihs.cookoff.shared.web.openapi.model.Account;
import at.fraihs.cookoff.shared.web.openapi.model.Challenge;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequest;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequest;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRole;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end security checks against the real filter chain (JWT resource server only, per
 * docs/cookingChallenge/plans/access-link-jwt-unification-plan.md) — this is deliberately a
 * @SpringBootTest, not a @WebMvcTest, since the thing under test IS the SecurityConfig
 * wiring; per-controller @WebMvcTest slices disable the filter chain entirely (see
 * ChallengeControllerTest/HomeControllerTest/AccountsControllerTest) and only cover
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
    void should_return200_when_unauthenticatedRequestHitsConfigEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return401_when_unauthenticatedRequestHitsOrganizerOnlyEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/challenges"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void should_return403_when_userRoleJwtHitsOrganizerOnlyEndpoint() throws Exception {
        createAccountService.execute(
                new CreateAccountRequest("user@example.com", "User").roles(List.of(SystemRole.USER)).password("password123"));
        String token = login("user@example.com", "password123");

        mockMvc.perform(get("/api/v1/challenges").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void should_return200_when_organizerRoleJwtHitsOrganizerOnlyEndpoint() throws Exception {
        createAccountService.execute(
                new CreateAccountRequest("organizer@example.com", "Organizer").roles(List.of(SystemRole.ORGANIZER)).password("password123"));
        String token = login("organizer@example.com", "password123");

        mockMvc.perform(get("/api/v1/challenges").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void should_return401_when_unauthenticatedRequestHitsRivalriesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/rivalries"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void should_return200_when_organizerRoleJwtHitsRivalriesEndpoint() throws Exception {
        createAccountService.execute(
                new CreateAccountRequest("rivalries-organizer@example.com", "Organizer").roles(List.of(SystemRole.ORGANIZER)).password("password123"));
        String token = login("rivalries-organizer@example.com", "password123");

        mockMvc.perform(get("/api/v1/rivalries").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void should_return201_when_adminCreatesAccountWithPasswordOverHttp() throws Exception {
        createAccountService.execute(
                new CreateAccountRequest("admin@example.com", "Admin").roles(List.of(SystemRole.ADMIN)).password("password123"));
        String token = login("admin@example.com", "password123");

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"email\":\"new-user@example.com\",\"name\":\"New User\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("new-user@example.com"));
    }

    @Test
    void should_return401_when_loginCredentialsInvalid() throws Exception {
        createAccountService.execute(
                new CreateAccountRequest("known@example.com", "Known").roles(List.of(SystemRole.ORGANIZER)).password("password123"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"known@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void should_return200_when_jwtFromAccessLinkExchangeHitsGuestEndpoint() throws Exception {
        String linkToken = issueLinkTokenForNewGuest();
        String jwt = exchangeAccessLinkForJwt(linkToken);

        mockMvc.perform(get("/api/v1/me/home").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void should_return401_when_guestEndpointHitWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/me/home"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void should_return401_when_accessLinkTokenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/access-link-login")
                        .contentType("application/json")
                        .content("{\"token\":\"not-a-real-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    @Test
    void should_return401_when_accessLinkTokenExpired() throws Exception {
        AccountId guest = AccountId.fromString(createAccountService.execute(
                new CreateAccountRequest("expired-guest@example.com", "Guest").roles(List.of(SystemRole.USER))).getId());
        long challengeId = TsidSupport.fromBase32(createSampleChallenge().getId());
        String expiredToken = accessLinkService.issue(guest, challengeId, Duration.ofSeconds(-1));

        mockMvc.perform(post("/api/v1/auth/access-link-login")
                        .contentType("application/json")
                        .content("{\"token\":\"" + expiredToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    private String issueLinkTokenForNewGuest() {
        Account guest = createAccountService.execute(
                new CreateAccountRequest("guest@example.com", "Guest").roles(List.of(SystemRole.USER)));
        long challengeId = TsidSupport.fromBase32(createSampleChallenge().getId());
        return accessLinkService.issue(AccountId.fromString(guest.getId()), challengeId, Duration.ofDays(1));
    }

    private Challenge createSampleChallenge() {
        Account cookA = createAccountService.execute(
                new CreateAccountRequest("cook-a@example.com", "Cook A").roles(List.of(SystemRole.USER)));
        Account cookB = createAccountService.execute(
                new CreateAccountRequest("cook-b@example.com", "Cook B").roles(List.of(SystemRole.USER)));
        Account organizer = createAccountService.execute(
                new CreateAccountRequest("challenge-organizer@example.com", "Organizer").roles(List.of(SystemRole.ORGANIZER)));
        CreateChallengeRequest request = new CreateChallengeRequest(
                LocalDate.now(), "Title", "Schnitzel", cookA.getId(), cookB.getId());
        return createChallengeService.execute(request, AccountId.fromString(organizer.getId()));
    }

    private String login(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractAccessToken(responseBody);
    }

    private String exchangeAccessLinkForJwt(String linkToken) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/auth/access-link-login")
                        .contentType("application/json")
                        .content("{\"token\":\"" + linkToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extractAccessToken(responseBody);
    }

    @SuppressWarnings("unchecked")
    private String extractAccessToken(String responseBody) throws Exception {
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        String token = (String) data.get("accessToken");
        assertNotNull(token);
        return token;
    }
}
