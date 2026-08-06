package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateParticipantsRequestRestDto;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditChallengeParticipantsService {

    private final AccountLookup accountLookup;
    private final ChallengeRepository challengeRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional
    public ChallengeRestDto execute(
            String challengeIdString, AccountId organizerAccountId, UpdateParticipantsRequestRestDto request) {
        if (!accountLookup.canOrganize(organizerAccountId)) {
            log.warn("Edit participants rejected, account cannot organize: {}", organizerAccountId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + organizerAccountId);
        }

        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        AccountId newCookAAccountId = request.getCookAAccountId() != null
                ? AccountId.fromString(request.getCookAAccountId()) : null;
        AccountId newCookBAccountId = request.getCookBAccountId() != null
                ? AccountId.fromString(request.getCookBAccountId()) : null;
        List<AccountId> guestIdsToAdd = request.getAddGuestAccountIds() == null
                ? List.of() : request.getAddGuestAccountIds().stream().map(AccountId::fromString).toList();
        List<AccountId> guestIdsToRemove = request.getRemoveGuestAccountIds() == null
                ? List.of() : request.getRemoveGuestAccountIds().stream().map(AccountId::fromString).toList();

        challenge.editParticipants(newCookAAccountId, newCookBAccountId, guestIdsToAdd, guestIdsToRemove);
        challengeRepository.save(challenge);
        log.info("Challenge participants edited: {}", challengeId);
        return ChallengeMapping.toGenerated(
                challenge, ChallengeMapping.submittedGuestCount(challenge, scoreSubmissionRepository));
    }
}
