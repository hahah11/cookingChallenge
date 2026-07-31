package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.repository.AccountRepository;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.NotificationPort;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * "Send links" action from docs/cookingChallenge/first-plan.md's Invite flow: issues one
 * reusable-until-expiry access link (AccessLinkService, Phase 3) per participant — both
 * cooks plus every guest, deduplicated in case an account fills more than one slot — and
 * emails it via NotificationPort.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendChallengeInvitationsService {

    private static final Duration LINK_VALIDITY = Duration.ofDays(30);

    private final ChallengeRepository challengeRepository;
    private final AccountRepository accountRepository;
    private final AccessLinkService accessLinkService;
    private final NotificationPort notificationPort;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public int execute(String challengeIdString) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        Set<AccountId> participantIds = new LinkedHashSet<>();
        challenge.getCookAssignments().forEach(assignment -> participantIds.add(assignment.accountId()));
        participantIds.addAll(challenge.getGuestAccountIds());

        for (AccountId participantId : participantIds) {
            Account account = accountRepository.findById(participantId)
                    .orElseThrow(() -> new AccountNotFoundException(participantId.toString()));
            String token = accessLinkService.issue(participantId, challengeId.value(), LINK_VALIDITY);
            notificationPort.sendAccessLink(account.getEmail(), frontendBaseUrl + "/home?token=" + token);
        }

        log.info("Sent {} invitation(s) for challenge {}", participantIds.size(), challengeId);
        return participantIds.size();
    }
}
