package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotRevealedException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.port.StoredImage;
import at.fraihs.cookoff.cookoff.application.service.ChangeChallengeImageService;
import at.fraihs.cookoff.cookoff.application.service.CreateChallengeService;
import at.fraihs.cookoff.cookoff.application.service.CreateRegistrationInviteService;
import at.fraihs.cookoff.cookoff.application.service.EditChallengeParticipantsService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeForParticipantService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeImageService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeResultsService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeStatusService;
import at.fraihs.cookoff.cookoff.application.service.ListChallengesService;
import at.fraihs.cookoff.cookoff.application.service.PickColorService;
import at.fraihs.cookoff.cookoff.application.service.RevealChallengeService;
import at.fraihs.cookoff.cookoff.application.service.SendChallengeInvitationsService;
import at.fraihs.cookoff.cookoff.application.service.SubmitScoreService;
import at.fraihs.cookoff.cookoff.application.service.UnrevealChallengeService;
import at.fraihs.cookoff.shared.config.JacksonConfig;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import at.fraihs.cookoff.shared.web.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.Category;
import at.fraihs.cookoff.shared.web.openapi.model.Challenge;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResult;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatus;
import at.fraihs.cookoff.shared.web.openapi.model.CookAssignment;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequest;
import at.fraihs.cookoff.shared.web.openapi.model.DishLabel;
import at.fraihs.cookoff.shared.web.openapi.model.InvitationsSent;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallenge;
import at.fraihs.cookoff.shared.web.openapi.model.Pagination;
import at.fraihs.cookoff.shared.web.openapi.model.PickColorRequest;
import at.fraihs.cookoff.shared.web.openapi.model.RegistrationInvite;
import at.fraihs.cookoff.shared.web.openapi.model.RivalrySummary;
import at.fraihs.cookoff.shared.web.openapi.model.ScoreEntry;
import at.fraihs.cookoff.shared.web.openapi.model.SubmissionStatus;
import at.fraihs.cookoff.shared.web.openapi.model.SubmitScoresRequest;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateParticipantsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security enforcement (JWT roles, link tokens) is covered by
 * {@code shared.security.SecurityIntegrationTest} — this slice test disables the security
 * filter chain, so {@code CurrentAccount.id()} is fed via {@link SecurityContextHolder}
 * directly rather than a real filter chain.
 */
@WebMvcTest(ChallengesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonConfig.class})
class ChallengesControllerTest {

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
    private EditChallengeParticipantsService editChallengeParticipantsService;
    @MockitoBean
    private PickColorService pickColorService;
    @MockitoBean
    private ChangeChallengeImageService changeChallengeImageService;
    @MockitoBean
    private GetChallengeImageService getChallengeImageService;
    @MockitoBean
    private CreateRegistrationInviteService createRegistrationInviteService;
    @MockitoBean
    private SendChallengeInvitationsService sendChallengeInvitationsService;
    @MockitoBean
    private RevealChallengeService revealChallengeService;
    @MockitoBean
    private UnrevealChallengeService unrevealChallengeService;
    @MockitoBean
    private GetChallengeResultsService getChallengeResultsService;
    @MockitoBean
    private SubmitScoreService submitScoreService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(AccountId accountId) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(accountId, null));
    }

    private Challenge sampleChallenge() {
        return new Challenge("chal-1", LocalDate.now(), "Title", "Schnitzel", ChallengeStatus.OPEN,
                List.of(new CookAssignment("acc-a", DishLabel.A, null), new CookAssignment("acc-b", DishLabel.B, null)),
                List.of(), "acc-org", 0, 0, false, null);
    }

    private SubmitScoresRequest sixValidScores() {
        List<ScoreEntry> scores = new ArrayList<>();
        for (DishLabel label : DishLabel.values()) {
            for (Category category : Category.values()) {
                scores.add(new ScoreEntry(label, category, 3));
            }
        }
        return new SubmitScoresRequest(scores);
    }

    private ParticipantChallenge sampleParticipantChallenge() {
        return new ParticipantChallenge("chal-1", LocalDate.now(), "Title", "Schnitzel", ChallengeStatus.OPEN,
                List.of(DishLabel.A, DishLabel.B), List.of(Category.MUNDGEFUEHL), List.of(),
                false, false, null, null, true, false);
    }

    @Test
    void should_return201_when_challengeCreated() throws Exception {
        AccountId organizer = AccountId.generate();
        when(createChallengeService.execute(any(), eq(organizer))).thenReturn(sampleChallenge());
        authenticateAs(organizer);

        mockMvc.perform(post("/api/v1/challenges")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateChallengeRequest(LocalDate.now(), "Title", "Schnitzel", "acc-a", "acc-b"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dishName").value("Schnitzel"));
    }

    @Test
    void should_return200_when_listingChallenges() throws Exception {
        Pagination pagination = new Pagination(0, 20, 1L, 1, true, true);
        when(listChallengesService.execute(0, 20)).thenReturn(new PagedResult<>(List.of(sampleChallenge()), pagination));

        mockMvc.perform(get("/api/v1/challenges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("chal-1"));
    }

    @Test
    void should_return200_when_gettingChallengeForValidToken() throws Exception {
        AccountId accountId = AccountId.generate();
        when(getChallengeForParticipantService.execute(eq("chal-1"), eq(accountId))).thenReturn(sampleParticipantChallenge());
        authenticateAs(accountId);

        mockMvc.perform(get("/api/v1/challenges/chal-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("chal-1"));
    }

    @Test
    void should_return403_when_requesterNotAParticipant() throws Exception {
        AccountId accountId = AccountId.generate();
        when(getChallengeForParticipantService.execute(eq("chal-1"), eq(accountId)))
                .thenThrow(new NotAParticipantException(accountId.toString(), "chal-1"));
        authenticateAs(accountId);

        mockMvc.perform(get("/api/v1/challenges/chal-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return200_when_gettingStatus() throws Exception {
        when(getChallengeStatusService.execute("chal-1")).thenReturn(new SubmissionStatus("chal-1", 2, 1, List.of()));

        mockMvc.perform(get("/api/v1/challenges/chal-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGuestCount").value(2));
    }

    @Test
    void should_return200_when_updatingParticipants() throws Exception {
        AccountId organizer = AccountId.generate();
        when(editChallengeParticipantsService.execute(eq("chal-1"), eq(organizer), any())).thenReturn(sampleChallenge());
        authenticateAs(organizer);

        mockMvc.perform(patch("/api/v1/challenges/chal-1/participants")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateParticipantsRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("chal-1"));
    }

    @Test
    void should_return200_when_pickingColor() throws Exception {
        AccountId cook = AccountId.generate();
        when(pickColorService.execute(eq("chal-1"), eq(cook), any())).thenReturn(sampleParticipantChallenge());
        authenticateAs(cook);

        mockMvc.perform(post("/api/v1/challenges/chal-1/color-pick")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new PickColorRequest("color-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("chal-1"));
    }

    @Test
    void should_return200_when_uploadingImage() throws Exception {
        AccountId organizer = AccountId.generate();
        when(changeChallengeImageService.execute(eq("chal-1"), eq(organizer), any(), any())).thenReturn(sampleChallenge());
        authenticateAs(organizer);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/challenges/chal-1/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("chal-1"));
    }

    @Test
    void should_return200_withImageBytes_when_gettingImage() throws Exception {
        when(getChallengeImageService.execute("chal-1")).thenReturn(new StoredImage(new byte[]{1, 2, 3}, "image/png"));

        mockMvc.perform(get("/api/v1/challenges/chal-1/image"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return201_when_creatingRegistrationInvite() throws Exception {
        AccountId organizer = AccountId.generate();
        when(createRegistrationInviteService.execute("chal-1", organizer))
                .thenReturn(new RegistrationInvite("tok", URI.create("http://localhost:4200/register?token=tok")));
        authenticateAs(organizer);

        mockMvc.perform(post("/api/v1/challenges/chal-1/registration-invites"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").value("tok"));
    }

    @Test
    void should_return200_when_sendingInvitations() throws Exception {
        when(sendChallengeInvitationsService.execute(eq("chal-1"), any())).thenReturn(new InvitationsSent(3));

        mockMvc.perform(post("/api/v1/challenges/chal-1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    void should_return200_when_revealing() throws Exception {
        ChallengeResult result = new ChallengeResult("chal-1", Map.of(), List.of(), null, List.of(),
                new RivalrySummary("acc-a", "acc-b", 0, 0, 0, 0, "headline"));
        when(revealChallengeService.execute("chal-1")).thenReturn(result);

        mockMvc.perform(post("/api/v1/challenges/chal-1/reveal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.challengeId").value("chal-1"));
    }

    @Test
    void should_return200_when_unrevealing() throws Exception {
        AccountId organizer = AccountId.generate();
        when(unrevealChallengeService.execute("chal-1", organizer)).thenReturn(sampleChallenge());
        authenticateAs(organizer);

        mockMvc.perform(post("/api/v1/challenges/chal-1/unreveal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("chal-1"));
    }

    @Test
    void should_return200_when_gettingResults() throws Exception {
        AccountId accountId = AccountId.generate();
        ChallengeResult result = new ChallengeResult("chal-1", Map.of(), List.of(), null, List.of(),
                new RivalrySummary("acc-a", "acc-b", 0, 0, 0, 0, "headline"));
        when(getChallengeResultsService.execute("chal-1", accountId)).thenReturn(result);
        authenticateAs(accountId);

        mockMvc.perform(get("/api/v1/challenges/chal-1/results"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return404_when_resultsNotYetRevealed() throws Exception {
        AccountId accountId = AccountId.generate();
        when(getChallengeResultsService.execute("chal-1", accountId))
                .thenThrow(new ChallengeNotRevealedException("chal-1"));
        authenticateAs(accountId);

        mockMvc.perform(get("/api/v1/challenges/chal-1/results"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return201_when_submittingScoresFirstTime() throws Exception {
        AccountId accountId = AccountId.generate();
        at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmission data =
                new at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmission(
                        "chal-1", accountId.toString(), List.of(), OffsetDateTime.now());
        when(submitScoreService.execute(eq("chal-1"), eq(accountId), any()))
                .thenReturn(new SubmitScoreService.Result(data, true));
        authenticateAs(accountId);

        mockMvc.perform(post("/api/v1/challenges/chal-1/scores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sixValidScores())))
                .andExpect(status().isCreated());
    }

    @Test
    void should_return200_when_resubmittingScores() throws Exception {
        AccountId accountId = AccountId.generate();
        at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmission data =
                new at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmission(
                        "chal-1", accountId.toString(), List.of(), OffsetDateTime.now());
        when(submitScoreService.execute(eq("chal-1"), eq(accountId), any()))
                .thenReturn(new SubmitScoreService.Result(data, false));
        authenticateAs(accountId);

        mockMvc.perform(post("/api/v1/challenges/chal-1/scores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sixValidScores())))
                .andExpect(status().isOk());
    }
}
