package at.fraihs.cookoff.shared.security;

import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.service.CreateChallengeService;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.shared.testsupport.GuestOnboardingTestSupport;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of QR self-registration (docs/cookingChallenge/plans/
 * link-login-qr-registration-test-plan.md): organizer creates an invite, a "scanner" redeems
 * it — all over real HTTP, without ever rendering a QR image (the backend only ever returns
 * the raw token as JSON; QR rendering is a pure frontend concern, see
 * frontend/.../shared/components/qr-code/qr-code.ts).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QrRegistrationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreateAccountService createAccountService;

    @Autowired
    private CreateChallengeService createChallengeService;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Test
    void should_registerAndJoinChallenge_when_redeemingInviteFromOpenChallenge() throws Exception {
        ChallengeRestDto challenge = createOpenChallenge();
        String organizerJwt = login("organizer@example.com", "password123");

        String inviteToken = GuestOnboardingTestSupport.createRegistrationInviteToken(
                mockMvc, objectMapper, organizerJwt, challenge.getId());

        Map<String, Object> result = GuestOnboardingTestSupport.selfRegisterViaInvite(
                mockMvc, objectMapper, inviteToken, "Walk", "In", "walkin@example.com");

        assertEquals(Boolean.TRUE, result.get("joined"));
        assertNotNull(result.get("accountId"));
    }

    @Test
    void should_registerWithoutJoining_when_challengeClosedBetweenInviteCreationAndRedemption() throws Exception {
        ChallengeRestDto challengeDto = createOpenChallenge();
        String organizerJwt = login("organizer@example.com", "password123");
        String inviteToken = GuestOnboardingTestSupport.createRegistrationInviteToken(
                mockMvc, objectMapper, organizerJwt, challengeDto.getId());

        // Close the challenge the same way a real reveal would, without needing the full
        // score-submission flow — this test is about registration behavior, not reveal itself.
        Challenge challenge = challengeRepository.findById(ChallengeId.fromString(challengeDto.getId())).orElseThrow();
        challenge.reveal(null);
        challengeRepository.save(challenge);

        Map<String, Object> result = GuestOnboardingTestSupport.selfRegisterViaInvite(
                mockMvc, objectMapper, inviteToken, "Late", "Walkin", "late-walkin@example.com");

        assertEquals(Boolean.FALSE, result.get("joined"));
        assertNotNull(result.get("accountId"));
    }

    @Test
    void should_stayReusable_when_sameInviteRedeemedByMultipleWalkIns() throws Exception {
        ChallengeRestDto challenge = createOpenChallenge();
        String organizerJwt = login("organizer@example.com", "password123");

        String inviteToken = GuestOnboardingTestSupport.createRegistrationInviteToken(
                mockMvc, objectMapper, organizerJwt, challenge.getId());

        Map<String, Object> first = GuestOnboardingTestSupport.selfRegisterViaInvite(
                mockMvc, objectMapper, inviteToken, "First", "Walkin", "first-walkin@example.com");
        Map<String, Object> second = GuestOnboardingTestSupport.selfRegisterViaInvite(
                mockMvc, objectMapper, inviteToken, "Second", "Walkin", "second-walkin@example.com");

        assertEquals(Boolean.TRUE, first.get("joined"));
        assertEquals(Boolean.TRUE, second.get("joined"));
    }

    @Test
    void should_return401_when_inviteTokenIsInvalid() throws Exception {
        GuestOnboardingTestSupport.performSelfRegistration(
                        mockMvc, "not-a-real-token", "A", "B", "nobody@example.com")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    @Test
    void should_return409_when_emailAlreadyRegistered() throws Exception {
        ChallengeRestDto challenge = createOpenChallenge();
        String organizerJwt = login("organizer@example.com", "password123");
        String inviteToken = GuestOnboardingTestSupport.createRegistrationInviteToken(
                mockMvc, objectMapper, organizerJwt, challenge.getId());

        GuestOnboardingTestSupport.selfRegisterViaInvite(
                mockMvc, objectMapper, inviteToken, "Dup", "One", "duplicate@example.com");

        GuestOnboardingTestSupport.performSelfRegistration(
                        mockMvc, inviteToken, "Dup", "Two", "duplicate@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void should_return403_when_nonOrganizerRequestsInvite() throws Exception {
        ChallengeRestDto challenge = createOpenChallenge();
        createAccountService.execute(new CreateAccountRequestRestDto("plain-user@example.com", "Plain", "User")
                .roles(List.of(SystemRoleRestDto.USER)).password("password123"));
        String userJwt = login("plain-user@example.com", "password123");

        GuestOnboardingTestSupport.performCreateRegistrationInvite(mockMvc, userJwt, challenge.getId())
                .andExpect(status().isForbidden());
    }

    private ChallengeRestDto createOpenChallenge() {
        AccountRestDto cookA = createAccountService.execute(new CreateAccountRequestRestDto(
                "qr-cook-a@example.com", "Cook", "A").roles(List.of(SystemRoleRestDto.USER)));
        AccountRestDto cookB = createAccountService.execute(new CreateAccountRequestRestDto(
                "qr-cook-b@example.com", "Cook", "B").roles(List.of(SystemRoleRestDto.USER)));
        AccountRestDto organizer = createAccountService.execute(new CreateAccountRequestRestDto(
                "organizer@example.com", "Organizer", "Test")
                .roles(List.of(SystemRoleRestDto.ORGANIZER)).password("password123"));
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
