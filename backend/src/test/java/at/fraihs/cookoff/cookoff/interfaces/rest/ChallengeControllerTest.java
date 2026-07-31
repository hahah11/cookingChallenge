package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeResultView;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.dto.SubmissionStatusView;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotRevealedException;
import at.fraihs.cookoff.cookoff.application.exception.DuplicateSubmissionException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.service.CreateChallengeService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeForParticipantService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeResultsService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeStatusService;
import at.fraihs.cookoff.cookoff.application.service.ListChallengesService;
import at.fraihs.cookoff.cookoff.application.service.RevealChallengeService;
import at.fraihs.cookoff.cookoff.application.service.SendChallengeInvitationsService;
import at.fraihs.cookoff.cookoff.application.service.SubmitScoreService;
import at.fraihs.cookoff.shared.config.SecurityConfig;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChallengeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateChallengeService createChallengeService;
    @MockitoBean
    private ListChallengesService listChallengesService;
    @MockitoBean
    private GetChallengeForParticipantService getChallengeForParticipantService;
    @MockitoBean
    private GetChallengeStatusService getChallengeStatusService;
    @MockitoBean
    private RevealChallengeService revealChallengeService;
    @MockitoBean
    private GetChallengeResultsService getChallengeResultsService;
    @MockitoBean
    private SendChallengeInvitationsService sendChallengeInvitationsService;
    @MockitoBean
    private SubmitScoreService submitScoreService;
    @MockitoBean
    private AccessLinkService accessLinkService;

    private ChallengeView sampleView() {
        return new ChallengeView("chal-1", LocalDate.now(), "Title", "Schnitzel", "OPEN",
                List.of(new ChallengeView.CookAssignmentView("acc-a", "A")), List.of(), "acc-org");
    }

    @Test
    void should_return201_when_challengeCreated() throws Exception {
        when(createChallengeService.execute(any())).thenReturn(sampleView());

        mockMvc.perform(post("/api/v1/challenges")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateChallengeRequest(
                                LocalDate.now(), "Title", "Schnitzel", "acc-a", "acc-b", List.of(), "acc-org"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dishName").value("Schnitzel"));
    }

    @Test
    void should_return400_when_dishNameMissing() throws Exception {
        mockMvc.perform(post("/api/v1/challenges")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateChallengeRequest(
                                LocalDate.now(), "Title", "", "acc-a", "acc-b", List.of(), "acc-org"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return200_when_listingChallenges() throws Exception {
        when(listChallengesService.execute()).thenReturn(List.of(sampleView()));

        mockMvc.perform(get("/api/v1/challenges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("chal-1"));
    }

    @Test
    void should_return200_when_gettingChallengeForValidToken() throws Exception {
        AccountId accountId = AccountId.generate();
        when(accessLinkService.verify("good-token")).thenReturn(accountId);
        ChallengeParticipantView view = new ChallengeParticipantView(
                "chal-1", LocalDate.now(), "Title", "Schnitzel", "OPEN",
                List.of("A", "B"), List.of("MUNDGEFUEHL"), List.of(), null);
        when(getChallengeForParticipantService.execute(eq("chal-1"), eq(accountId))).thenReturn(view);

        mockMvc.perform(get("/api/v1/challenges/chal-1").param("token", "good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cookAssignments").value(nullValue()));
    }

    @Test
    void should_return401_when_tokenInvalid() throws Exception {
        when(accessLinkService.verify("bad-token")).thenThrow(new InvalidOrExpiredLinkException());

        mockMvc.perform(get("/api/v1/challenges/chal-1").param("token", "bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_LINK"));
    }

    @Test
    void should_return403_when_requesterNotAParticipant() throws Exception {
        AccountId accountId = AccountId.generate();
        when(accessLinkService.verify("good-token")).thenReturn(accountId);
        when(getChallengeForParticipantService.execute(anyString(), eq(accountId)))
                .thenThrow(new NotAParticipantException(accountId.toString(), "chal-1"));

        mockMvc.perform(get("/api/v1/challenges/chal-1").param("token", "good-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return200_when_gettingStatus() throws Exception {
        when(getChallengeStatusService.execute("chal-1"))
                .thenReturn(new SubmissionStatusView("chal-1", 2, 1, List.of("acc-g1")));

        mockMvc.perform(get("/api/v1/challenges/chal-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGuestCount").value(2));
    }

    @Test
    void should_return200_when_revealing() throws Exception {
        when(revealChallengeService.execute("chal-1"))
                .thenReturn(new ChallengeResultView("chal-1", Map.of(), null, List.of()));

        mockMvc.perform(post("/api/v1/challenges/chal-1/reveal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.challengeId").value("chal-1"));
    }

    @Test
    void should_return200_when_gettingResults() throws Exception {
        AccountId accountId = AccountId.generate();
        when(accessLinkService.verify("good-token")).thenReturn(accountId);
        when(getChallengeResultsService.execute("chal-1", accountId))
                .thenReturn(new ChallengeResultView("chal-1", Map.of(), null, List.of()));

        mockMvc.perform(get("/api/v1/challenges/chal-1/results").param("token", "good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return404_when_resultsNotYetRevealed() throws Exception {
        AccountId accountId = AccountId.generate();
        when(accessLinkService.verify("good-token")).thenReturn(accountId);
        when(getChallengeResultsService.execute("chal-1", accountId))
                .thenThrow(new ChallengeNotRevealedException("chal-1"));

        mockMvc.perform(get("/api/v1/challenges/chal-1/results").param("token", "good-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return200_when_sendingInvitations() throws Exception {
        when(sendChallengeInvitationsService.execute("chal-1")).thenReturn(3);

        mockMvc.perform(post("/api/v1/challenges/chal-1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    void should_return201_when_submittingScores() throws Exception {
        AccountId accountId = AccountId.generate();
        when(accessLinkService.verify("good-token")).thenReturn(accountId);

        mockMvc.perform(post("/api/v1/challenges/chal-1/scores")
                        .param("token", "good-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SubmitScoresRequest(
                                List.of(new ScoreEntryRequest("A", "GESCHMACK", 4))))))
                .andExpect(status().isCreated());
    }

    @Test
    void should_return409_when_duplicateSubmission() throws Exception {
        AccountId accountId = AccountId.generate();
        when(accessLinkService.verify("good-token")).thenReturn(accountId);
        org.mockito.Mockito.doThrow(new DuplicateSubmissionException(accountId.toString(), "chal-1"))
                .when(submitScoreService).execute(any());

        mockMvc.perform(post("/api/v1/challenges/chal-1/scores")
                        .param("token", "good-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SubmitScoresRequest(
                                List.of(new ScoreEntryRequest("A", "GESCHMACK", 4))))))
                .andExpect(status().isConflict());
    }
}
