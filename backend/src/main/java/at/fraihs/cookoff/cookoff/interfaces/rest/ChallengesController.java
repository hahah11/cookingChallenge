package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.cookoff.application.dto.StoredImage;
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
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.api.ChallengesApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMetaRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeListResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResultResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.InvitationsSentResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PickColorRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RegistrationInviteResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ScoreSubmissionResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SendInvitationsRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SubmissionStatusResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SubmitScoresRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateParticipantsRequestRestDto;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<ChallengeResponseRestDto> createChallenge(CreateChallengeRequestRestDto createChallengeRequest) {
        ChallengeRestDto challenge = createChallengeService.execute(createChallengeRequest, CurrentAccount.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ChallengeResponseRestDto(challenge, meta()));
    }

    @Override
    public ResponseEntity<ChallengeListResponseRestDto> listChallenges(Integer page, Integer size) {
        PagedResult<ChallengeRestDto> result = listChallengesService.execute(page, size);
        return ResponseEntity.ok(new ChallengeListResponseRestDto(result.content(), result.pagination(), meta()));
    }

    @Override
    public ResponseEntity<ParticipantChallengeResponseRestDto> getChallenge(String challengeId) {
        var challenge = getChallengeForParticipantService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.ok(new ParticipantChallengeResponseRestDto(challenge, meta()));
    }

    @Override
    public ResponseEntity<SubmissionStatusResponseRestDto> getChallengeStatus(String challengeId) {
        var status = getChallengeStatusService.execute(challengeId);
        return ResponseEntity.ok(new SubmissionStatusResponseRestDto(status, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResponseRestDto> updateChallengeParticipants(
            String challengeId, UpdateParticipantsRequestRestDto updateParticipantsRequest) {
        ChallengeRestDto challenge = editChallengeParticipantsService.execute(
                challengeId, CurrentAccount.id(), updateParticipantsRequest);
        return ResponseEntity.ok(new ChallengeResponseRestDto(challenge, meta()));
    }

    @Override
    public ResponseEntity<ParticipantChallengeResponseRestDto> pickChallengeColor(
            String challengeId, PickColorRequestRestDto pickColorRequest) {
        var challenge = pickColorService.execute(challengeId, CurrentAccount.id(), pickColorRequest);
        return ResponseEntity.ok(new ParticipantChallengeResponseRestDto(challenge, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResponseRestDto> updateChallengeImage(String challengeId, MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded file", e);
        }
        ChallengeRestDto challenge = changeChallengeImageService.execute(
                challengeId, CurrentAccount.id(), bytes, file.getContentType());
        return ResponseEntity.ok(new ChallengeResponseRestDto(challenge, meta()));
    }

    @Override
    public ResponseEntity<Resource> getChallengeImage(String challengeId) {
        StoredImage image = getChallengeImageService.execute(challengeId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(new ByteArrayResource(image.bytes()));
    }

    @Override
    public ResponseEntity<RegistrationInviteResponseRestDto> createRegistrationInvite(String challengeId) {
        var invite = createRegistrationInviteService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationInviteResponseRestDto(invite, meta()));
    }

    @Override
    public ResponseEntity<InvitationsSentResponseRestDto> sendInvitations(
            String challengeId, SendInvitationsRequestRestDto sendInvitationsRequest) {
        var sent = sendChallengeInvitationsService.execute(challengeId, sendInvitationsRequest);
        return ResponseEntity.ok(new InvitationsSentResponseRestDto(sent, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResultResponseRestDto> revealChallenge(String challengeId) {
        var result = revealChallengeService.execute(challengeId);
        return ResponseEntity.ok(new ChallengeResultResponseRestDto(result, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResponseRestDto> unrevealChallenge(String challengeId) {
        ChallengeRestDto challenge = unrevealChallengeService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.ok(new ChallengeResponseRestDto(challenge, meta()));
    }

    @Override
    public ResponseEntity<ChallengeResultResponseRestDto> getChallengeResults(String challengeId) {
        var result = getChallengeResultsService.execute(challengeId, CurrentAccount.id());
        return ResponseEntity.ok(new ChallengeResultResponseRestDto(result, meta()));
    }

    @Override
    public ResponseEntity<ScoreSubmissionResponseRestDto> submitScores(String challengeId, SubmitScoresRequestRestDto submitScoresRequest) {
        SubmitScoreService.Result result = submitScoreService.execute(challengeId, CurrentAccount.id(), submitScoresRequest);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ScoreSubmissionResponseRestDto(result.data(), meta()));
    }

    private ApiMetaRestDto meta() {
        return new ApiMetaRestDto(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
