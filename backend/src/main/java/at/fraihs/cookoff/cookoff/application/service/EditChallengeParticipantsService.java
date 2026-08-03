package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.EditChallengeParticipantsCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
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

    @Transactional
    public void execute(EditChallengeParticipantsCommand command) {
        AccountId organizerId = AccountId.fromString(command.organizerAccountId());
        if (!accountLookup.canOrganize(organizerId)) {
            log.warn("Edit participants rejected, account cannot organize: {}", organizerId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + command.organizerAccountId());
        }

        ChallengeId challengeId = ChallengeId.fromString(command.challengeId());
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(command.challengeId()));

        AccountId newCookAAccountId = command.newCookAAccountId() != null
                ? AccountId.fromString(command.newCookAAccountId()) : null;
        AccountId newCookBAccountId = command.newCookBAccountId() != null
                ? AccountId.fromString(command.newCookBAccountId()) : null;
        var guestIdsToAdd = command.guestIdsToAdd().stream().map(AccountId::fromString).toList();
        var guestIdsToRemove = command.guestIdsToRemove().stream().map(AccountId::fromString).toList();

        challenge.editParticipants(newCookAAccountId, newCookBAccountId, guestIdsToAdd, guestIdsToRemove);
        challengeRepository.save(challenge);
        log.info("Challenge participants edited: {}", challengeId);
    }
}
