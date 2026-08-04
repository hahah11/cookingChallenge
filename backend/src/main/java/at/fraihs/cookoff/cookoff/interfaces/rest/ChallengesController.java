package at.fraihs.cookoff.cookoff.interfaces.rest;

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
import at.fraihs.cookoff.shared.security.CurrentAccount;
import at.fraihs.cookoff.shared.web.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.api.ChallengesApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMeta;
import at.fraihs.cookoff.shared.web.openapi.model.Challenge;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeListResponse;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResponse;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResultResponse;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequest;
import at.fraihs.cookoff.shared.web.openapi.model.InvitationsSentResponse;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeResponse;
import at.fraihs.cookoff.shared.web.openapi.model.PickColorRequest;
import at.fraihs.cookoff.shared.web.openapi.model.RegistrationInviteResponse;
import at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmissionResponse;
import at.fraihs.cookoff.shared.web.openapi.model.SendInvitationsRequest;
import at.fraihs.cookoff.shared.web.openapi.model.SubmissionStatusResponse;
import at.fraihs.cookoff.shared.web.openapi.model.SubmitScoresRequest;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateParticipantsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Implements every operation under the {@code Challenges} tag - the generated interface
 * bundles all 14 into one, so this controller is the single seam where they all land, per
 * openapi-first-api-plan.md's Phase 5 Challenges-group note.
 */
@RestController
@RequiredArgsConstructor
public class ChallengesController implements ChallengesApi {

    private final CreateChallengeService createChallengeService;
    private final ListChallengesService listChallengesService;
    private final GetChallengeForParticipantService getChallengeForParticipantService;
    private final GetChallengeStatusService getChallengeStatusService;
    private final EditChallengeParticipantsService editChallengeParticipantsService;
    private final PickColorService pickColorService;
    private final ChangeChallengeImageService changeChallengeImageService;
    private final GetChallengeImageService getChallengeImageService;
    private final CreateRegistrationInviteService createRegistrationInviteService;
    private final SendChallengeInvitationsService sendChallengeInvitationsService;
    private final RevealChallengeService revealChallengeService;
    private final UnrevealChallengeService unrevealChallengeService;
    private final GetChallengeResultsService getChallengeResultsService;
    private final SubmitScoreService submitScoreService;

    @Override
    public ResponseEntity<ChallengeResponse> createChallenge(CreateChallengeRequest createChallengeRequest) {
        Challenge challenge = createChallengeService.execute(createChallengeRequest, CurrentAccount.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ChallengeResponse(challenge, meta()));
    }

    @Override
    public ResponseEntity<ChallengeListResponse> listChallenges(Integer page, Integer size) {
        PagedResult<Challenge> result = listChallengesService.execute(page, size);
        return ResponseEntity.ok(new ChallengeListResponse(result.content(), result.pagination(), meta()));
    }

    @Override
    public ResponseEntity<ParticipantChallengeResponse> getChallenge(String challengeId) {
        var challenge = getChallengeForParticipantService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.ok(new ParticipantChallengeResponse(challenge, meta()));
    }

    @Override
    public ResponseEntity<SubmissionStatusResponse> getChallengeStatus(String challengeId) {
        var status = getChallengeStatusService.execute(challengeId);
        return ResponseEntity.ok(new SubmissionStatusResponse(status, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResponse> updateChallengeParticipants(
            String challengeId, UpdateParticipantsRequest updateParticipantsRequest) {
        Challenge challenge = editChallengeParticipantsService.execute(
                challengeId, CurrentAccount.id(), updateParticipantsRequest);
        return ResponseEntity.ok(new ChallengeResponse(challenge, meta()));
    }

    @Override
    public ResponseEntity<ParticipantChallengeResponse> pickChallengeColor(
            String challengeId, PickColorRequest pickColorRequest) {
        var challenge = pickColorService.execute(challengeId, CurrentAccount.id(), pickColorRequest);
        return ResponseEntity.ok(new ParticipantChallengeResponse(challenge, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResponse> updateChallengeImage(String challengeId, MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded file", e);
        }
        Challenge challenge = changeChallengeImageService.execute(
                challengeId, CurrentAccount.id(), bytes, file.getContentType());
        return ResponseEntity.ok(new ChallengeResponse(challenge, meta()));
    }

    @Override
    public ResponseEntity<Resource> getChallengeImage(String challengeId) {
        StoredImage image = getChallengeImageService.execute(challengeId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(new ByteArrayResource(image.bytes()));
    }

    @Override
    public ResponseEntity<RegistrationInviteResponse> createRegistrationInvite(String challengeId) {
        var invite = createRegistrationInviteService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationInviteResponse(invite, meta()));
    }

    @Override
    public ResponseEntity<InvitationsSentResponse> sendInvitations(
            String challengeId, SendInvitationsRequest sendInvitationsRequest) {
        var sent = sendChallengeInvitationsService.execute(challengeId, sendInvitationsRequest);
        return ResponseEntity.ok(new InvitationsSentResponse(sent, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResultResponse> revealChallenge(String challengeId) {
        var result = revealChallengeService.execute(challengeId);
        return ResponseEntity.ok(new ChallengeResultResponse(result, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResponse> unrevealChallenge(String challengeId) {
        Challenge challenge = unrevealChallengeService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.ok(new ChallengeResponse(challenge, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResultResponse> getChallengeResults(String challengeId) {
        var result = getChallengeResultsService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.ok(new ChallengeResultResponse(result, meta()));
    }

    @Override
    public ResponseEntity<ScoreSubmissionResponse> submitScores(String challengeId, SubmitScoresRequest submitScoresRequest) {
        SubmitScoreService.Result result = submitScoreService.execute(challengeId, CurrentAccount.id(), submitScoresRequest);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ScoreSubmissionResponse(result.data(), meta()));
    }

    private ApiMeta meta() {
        return new ApiMeta(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
