package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeResultView;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.dto.CreateChallengeCommand;
import at.fraihs.cookoff.cookoff.application.dto.ScoreInput;
import at.fraihs.cookoff.cookoff.application.dto.SubmissionStatusView;
import at.fraihs.cookoff.cookoff.application.dto.SubmitScoreCommand;
import at.fraihs.cookoff.cookoff.application.service.CreateChallengeService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeForParticipantService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeResultsService;
import at.fraihs.cookoff.cookoff.application.service.GetChallengeStatusService;
import at.fraihs.cookoff.cookoff.application.service.ListChallengesService;
import at.fraihs.cookoff.cookoff.application.service.RevealChallengeService;
import at.fraihs.cookoff.cookoff.application.service.SendChallengeInvitationsService;
import at.fraihs.cookoff.cookoff.application.service.SubmitScoreService;
import at.fraihs.cookoff.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint table from docs/cookingChallenge/first-plan.md Step 3. Link-token endpoints
 * (guest info, scoring, results) take the token as a `token` query parameter and resolve
 * it via AccessLinkService right here in the controller — Phase 5 replaces this with a
 * shared OncePerRequestFilter that sets the AccountId as the request principal instead,
 * per docs/cookingChallenge/plans/backend-persistence-api-security-plan.md Phase 5. Until
 * then, organizer-only actions below (create, list, status, reveal, invitations) have no
 * role enforcement — same pre-Phase-5 gap as RevealChallengeService already had.
 */
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final CreateChallengeService createChallengeService;
    private final ListChallengesService listChallengesService;
    private final GetChallengeForParticipantService getChallengeForParticipantService;
    private final GetChallengeStatusService getChallengeStatusService;
    private final RevealChallengeService revealChallengeService;
    private final GetChallengeResultsService getChallengeResultsService;
    private final SendChallengeInvitationsService sendChallengeInvitationsService;
    private final SubmitScoreService submitScoreService;
    private final AccessLinkService accessLinkService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChallengeView>> create(@Valid @RequestBody CreateChallengeRequest request) {
        CreateChallengeCommand command = new CreateChallengeCommand(
                request.date(), request.title(), request.dishName(),
                request.cookAAccountId(), request.cookBAccountId(),
                request.guestAccountIds() == null ? List.of() : request.guestAccountIds(),
                request.organizerAccountId());
        ChallengeView view = createChallengeService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(view));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeView>>> list() {
        return ResponseEntity.ok(ApiResponse.of(listChallengesService.execute()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChallengeParticipantView>> getForParticipant(
            @PathVariable String id, @RequestParam String token) {
        AccountId accountId = accessLinkService.verify(token);
        return ResponseEntity.ok(ApiResponse.of(getChallengeForParticipantService.execute(id, accountId)));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SubmissionStatusView>> status(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.of(getChallengeStatusService.execute(id)));
    }

    @PostMapping("/{id}/reveal")
    public ResponseEntity<ApiResponse<ChallengeResultView>> reveal(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.of(revealChallengeService.execute(id)));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse<ChallengeResultView>> results(
            @PathVariable String id, @RequestParam String token) {
        AccountId accountId = accessLinkService.verify(token);
        return ResponseEntity.ok(ApiResponse.of(getChallengeResultsService.execute(id, accountId)));
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<ApiResponse<InvitationsSentResponse>> sendInvitations(@PathVariable String id) {
        int count = sendChallengeInvitationsService.execute(id);
        return ResponseEntity.ok(ApiResponse.of(new InvitationsSentResponse(count)));
    }

    @PostMapping("/{id}/scores")
    public ResponseEntity<ApiResponse<Void>> submitScores(
            @PathVariable String id, @RequestParam String token, @Valid @RequestBody SubmitScoresRequest request) {
        AccountId accountId = accessLinkService.verify(token);
        List<ScoreInput> scores = request.scores().stream()
                .map(entry -> new ScoreInput(entry.dish(), entry.category(), entry.points()))
                .toList();
        submitScoreService.execute(new SubmitScoreCommand(id, accountId.toString(), scores));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(null));
    }
}
