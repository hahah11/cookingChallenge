package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.StoredImage;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotRevealedException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
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
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.CategoryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResultRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatusRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CookAssignmentRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.DishLabelRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.InvitationsSentRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PaginationRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PickColorRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RegistrationInviteRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalrySummaryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ScoreEntryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmissionRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SubmissionStatusRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SubmitScoresRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateParticipantsRequestRestDto;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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
 * Security enforcement (JWT roles) is covered by
 * {@code shared.security.SecurityIntegrationTest} — this slice test disables the security
 * filter chain, so {@code CurrentAccount.id()} is fed a {@link Jwt} principal via
 * {@link SecurityContextHolder} directly rather than a real filter chain.
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
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", accountId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));
    }

    private ChallengeRestDto sampleChallenge() {
        return new ChallengeRestDto("chal-1", LocalDate.now(), "Title", "Schnitzel", ChallengeStatusRestDto.OPEN,
                List.of(new CookAssignmentRestDto("acc-a", "Cook A", DishLabelRestDto.A, null),
                        new CookAssignmentRestDto("acc-b", "Cook B", DishLabelRestDto.B, null)),
                List.of(), "acc-org", 0, 0, false, null);
    }

    private SubmitScoresRequestRestDto sixValidScores() {
        List<ScoreEntryRestDto> scores = new ArrayList<>();
        for (DishLabelRestDto label : DishLabelRestDto.values()) {
            for (CategoryRestDto category : CategoryRestDto.values()) {
                scores.add(new ScoreEntryRestDto(label, category, 3));
            }
        }
        return new SubmitScoresRequestRestDto(scores);
    }

    private ParticipantChallengeRestDto sampleParticipantChallenge() {
        return new ParticipantChallengeRestDto("chal-1", LocalDate.now(), "Title", "Schnitzel", ChallengeStatusRestDto.OPEN,
                List.of(DishLabelRestDto.A, DishLabelRestDto.B), List.of(CategoryRestDto.MUNDGEFUEHL), List.of(),
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
                                new CreateChallengeRequestRestDto(LocalDate.now(), "Title", "Schnitzel", "acc-a", "acc-b"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dishName").value("Schnitzel"));
    }

    @Test
    void should_return200_when_listingChallenges() throws Exception {
        PaginationRestDto pagination = new PaginationRestDto(0, 20, 1L, 1, true, true);
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
        when(getChallengeStatusService.execute("chal-1")).thenReturn(new SubmissionStatusRestDto("chal-1", 2, 1, List.of()));

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
                        .content(objectMapper.writeValueAsString(new UpdateParticipantsRequestRestDto())))
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
                        .content(objectMapper.writeValueAsString(new PickColorRequestRestDto("color-1"))))
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
                .thenReturn(new RegistrationInviteRestDto("tok", URI.create("http://localhost:4200/register?token=tok")));
        authenticateAs(organizer);

        mockMvc.perform(post("/api/v1/challenges/chal-1/registration-invites"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").value("tok"));
    }

    @Test
    void should_return200_when_sendingInvitations() throws Exception {
        when(sendChallengeInvitationsService.execute(eq("chal-1"), any())).thenReturn(new InvitationsSentRestDto(3));

        mockMvc.perform(post("/api/v1/challenges/chal-1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    void should_return200_when_revealing() throws Exception {
        ChallengeResultRestDto result = new ChallengeResultRestDto("chal-1", Map.of(), List.of(), null, List.of(),
                new RivalrySummaryRestDto("acc-a", "acc-b", 0, 0, 0, 0, "headline"));
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
        ChallengeResultRestDto result = new ChallengeResultRestDto("chal-1", Map.of(), List.of(), null, List.of(),
                new RivalrySummaryRestDto("acc-a", "acc-b", 0, 0, 0, 0, "headline"));
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
        ScoreSubmissionRestDto data =
                new ScoreSubmissionRestDto(
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
        ScoreSubmissionRestDto data =
                new ScoreSubmissionRestDto(
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
