package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.InvitationsSentRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SendInvitationsRequestRestDto;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Send links" action from docs/cookingChallenge/first-plan.md's Invite flow: issues one
 * reusable-until-expiry access link (AccessLinkService, Phase 3) to every targeted guest and
 * emails it via NotificationPort. Scoped to guests only, per the generated request's
 * {@code guestAccountIds} field and the spec's "Send (or resend) guest access-link
 * invitations" summary — an explicit id list targets exactly those guests; omitting it
 * targets every guest who hasn't submitted yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendChallengeInvitationsService {

    private static final Duration LINK_VALIDITY = Duration.ofDays(30);

    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final AccountLookup accountLookup;
    private final AccessLinkService accessLinkService;
    private final NotificationPort notificationPort;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public InvitationsSentRestDto execute(
            String challengeIdString, AccountId requesterAccountId, SendInvitationsRequestRestDto request) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (!challenge.isOwnedBy(requesterAccountId) && !accountLookup.isAdmin(requesterAccountId)) {
            log.warn("Send invitations rejected, account {} does not own challenge {}", requesterAccountId, challengeId);
            throw new ForbiddenException("Account is not allowed to manage this challenge: " + requesterAccountId);
        }

        List<AccountId> targets = resolveTargets(challenge, challengeId, request);
        for (AccountId guestAccountId : targets) {
            AccountSummary account = accountLookup.getById(guestAccountId);
            String token = accessLinkService.issue(guestAccountId, challengeId.value(), LINK_VALIDITY);
            notificationPort.sendAccessLink(account.email(), frontendBaseUrl + "/home?token=" + token);
        }

        log.info("Sent {} invitation(s) for challenge {}", targets.size(), challengeId);
        return new InvitationsSentRestDto(targets.size());
    }

    private List<AccountId> resolveTargets(Challenge challenge, ChallengeId challengeId, SendInvitationsRequestRestDto request) {
        List<String> requestedIds = request == null ? null : request.getGuestAccountIds();
        if (requestedIds != null && !requestedIds.isEmpty()) {
            Set<AccountId> guestAccountIds = Set.copyOf(challenge.getGuestAccountIds());
            return requestedIds.stream()
                    .map(AccountId::fromString)
                    .filter(guestAccountIds::contains)
                    .distinct()
                    .toList();
        }
        Set<AccountId> alreadySubmitted = scoreSubmissionRepository.findByChallengeId(challengeId).stream()
                .map(ScoreSubmission::getGuestAccountId)
                .collect(Collectors.toSet());
        return challenge.getGuestAccountIds().stream()
                .filter(guestAccountId -> !alreadySubmitted.contains(guestAccountId))
                .toList();
    }
}
