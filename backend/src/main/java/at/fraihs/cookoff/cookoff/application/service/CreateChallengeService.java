package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.dto.CreateChallengeCommand;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateChallengeService {

    private final AccountLookup accountLookup;
    private final ChallengeRepository challengeRepository;

    @Transactional
    public ChallengeView execute(CreateChallengeCommand command) {
        AccountId organizerId = AccountId.fromString(command.organizerAccountId());
        if (!accountLookup.canOrganize(organizerId)) {
            log.warn("Challenge creation rejected, account cannot organize: {}", organizerId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + command.organizerAccountId());
        }

        AccountId cookAAccountId = AccountId.fromString(command.cookAAccountId());
        AccountId cookBAccountId = AccountId.fromString(command.cookBAccountId());
        List<AccountId> guestAccountIds = command.guestAccountIds().stream()
                .map(AccountId::fromString)
                .toList();

        Challenge challenge = Challenge.create(
                command.date(),
                command.title(),
                new DishName(command.dishName()),
                cookAAccountId,
                cookBAccountId,
                guestAccountIds,
                organizerId);
        challengeRepository.save(challenge);
        log.info("Challenge created: {}, organizer: {}", challenge.getId(), organizerId);
        return ChallengeView.from(challenge);
    }
}
