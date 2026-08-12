package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.service.CreateChallengeService;
import at.fraihs.cookoff.shared.testsupport.CapturingNotificationPort;
import at.fraihs.cookoff.shared.testsupport.CapturingNotificationPortConfig;
import at.fraihs.cookoff.shared.testsupport.GuestOnboardingTestSupport;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of access-link login (docs/cookingChallenge/plans/
 * link-login-qr-registration-test-plan.md), driven entirely through the real HTTP layer and
 * the real {@code SendChallengeInvitationsService} production path — no real email, no
 * browser. {@link SecurityIntegrationTest} already covers the "mint the token directly via
 * AccessLinkService" shortcut plus the invalid/expired-token cases; this class instead proves
 * the *actual* organizer-facing trigger (@{code POST .../invitations}) works end to end, by
 * swapping in {@link CapturingNotificationPort} to recover the token that would otherwise only
 * ever reach a mailbox.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CapturingNotificationPortConfig.class)
@Transactional
class AccessLinkLoginFlowIntegrationTest {

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

    @Autowired
    private CapturingNotificationPort capturingNotificationPort;

    @Test
    void should_loginAsGuest_when_usingTokenSentByRealSendInvitationsEndpoint() throws Exception {
        AccountRestDto guest = createAccount("guest@example.com", "Guest", "Test", SystemRoleRestDto.USER);
        ChallengeRestDto challenge = createOpenChallengeWithGuest(guest);
        String organizerJwt = login("organizer@example.com", "password123");

        mockMvc.perform(post("/api/v1/challenges/" + challenge.getId() + "/invitations")
                        .header("Authorization", "Bearer " + organizerJwt)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        String link = capturingNotificationPort.lastLinkFor("guest@example.com");
        String linkToken = CapturingNotificationPort.extractToken(link);

        String guestJwt = GuestOnboardingTestSupport.exchangeAccessLinkForJwt(mockMvc, objectMapper, linkToken);

        mockMvc.perform(get("/api/v1/me/home").header("Authorization", "Bearer " + guestJwt))
                .andExpect(status().isOk());
    }

    @Test
    void should_stayReusable_when_exchangedMultipleTimes() throws Exception {
        AccountRestDto guest = createAccount("repeat-guest@example.com", "Guest", "Test", SystemRoleRestDto.USER);
        long challengeId = TsidSupport.fromBase32(createSampleChallenge().getId());
        String linkToken = GuestOnboardingTestSupport.issueAccessLinkToken(
                accessLinkService, AccountId.fromString(guest.getId()), challengeId, Duration.ofDays(1));

        GuestOnboardingTestSupport.exchangeAccessLinkForJwt(mockMvc, objectMapper, linkToken);
        GuestOnboardingTestSupport.exchangeAccessLinkForJwt(mockMvc, objectMapper, linkToken);
        String jwt = GuestOnboardingTestSupport.exchangeAccessLinkForJwt(mockMvc, objectMapper, linkToken);

        mockMvc.perform(get("/api/v1/me/home").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void should_leaveUsedAtNull_afterExchange_becauseLinksAreDeliberatelyNotSingleUse() throws Exception {
        // Regression guard for AccessLink.markUsed(...): nothing calls it today (links are
        // reusable until expiry, per AccessLinkService's javadoc), and a future change that
        // starts single-using links on exchange should fail this test rather than silently
        // break the documented "casual access via personalized link" flow.
        AccountRestDto guest = createAccount("used-at-guest@example.com", "Guest", "Test", SystemRoleRestDto.USER);
        long challengeId = TsidSupport.fromBase32(createSampleChallenge().getId());
        String linkToken = GuestOnboardingTestSupport.issueAccessLinkToken(
                accessLinkService, AccountId.fromString(guest.getId()), challengeId, Duration.ofDays(1));

        GuestOnboardingTestSupport.exchangeAccessLinkForJwt(mockMvc, objectMapper, linkToken);

        assertNull(accessLinkService.verify(linkToken).usedAt());
    }

    private AccountRestDto createAccount(String email, String firstName, String lastName, SystemRoleRestDto role) {
        return createAccountService.execute(
                new CreateAccountRequestRestDto(email, firstName, lastName).roles(List.of(role)));
    }

    private ChallengeRestDto createOpenChallengeWithGuest(AccountRestDto guest) {
        AccountRestDto cookA = createAccount("cook-a@example.com", "Cook", "A", SystemRoleRestDto.USER);
        AccountRestDto cookB = createAccount("cook-b@example.com", "Cook", "B", SystemRoleRestDto.USER);
        AccountRestDto organizer = createAccountService.execute(
                new CreateAccountRequestRestDto("organizer@example.com", "Organizer", "Test")
                        .roles(List.of(SystemRoleRestDto.ORGANIZER)).password("password123"));
        CreateChallengeRequestRestDto request = new CreateChallengeRequestRestDto(
                LocalDate.now(), "Title", "Schnitzel", cookA.getId(), cookB.getId())
                .guestAccountIds(List.of(guest.getId()));
        return createChallengeService.execute(request, AccountId.fromString(organizer.getId()));
    }

    private ChallengeRestDto createSampleChallenge() {
        AccountRestDto cookA = createAccount("cook-a@example.com", "Cook", "A", SystemRoleRestDto.USER);
        AccountRestDto cookB = createAccount("cook-b@example.com", "Cook", "B", SystemRoleRestDto.USER);
        AccountRestDto organizer = createAccount("challenge-organizer@example.com", "Organizer", "Test", SystemRoleRestDto.ORGANIZER);
        CreateChallengeRequestRestDto request = new CreateChallengeRequestRestDto(
                LocalDate.now(), "Title", "Schnitzel", cookA.getId(), cookB.getId());
        return createChallengeService.execute(request, AccountId.fromString(organizer.getId()));
    }

    private String login(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return GuestOnboardingTestSupport.extractStringField(objectMapper, responseBody, "accessToken");
    }
}
