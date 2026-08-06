package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateChallengeRequestRestDto;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateChallengeService {

    private final AccountLookup accountLookup;
    private final ChallengeRepository challengeRepository;

    @Transactional
    public ChallengeRestDto execute(
            CreateChallengeRequestRestDto request, AccountId organizerAccountId) {
        if (!accountLookup.canOrganize(organizerAccountId)) {
            log.warn("Challenge creation rejected, account cannot organize: {}", organizerAccountId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + organizerAccountId);
        }

        AccountId cookAAccountId = AccountId.fromString(request.getCookAAccountId());
        AccountId cookBAccountId = AccountId.fromString(request.getCookBAccountId());
        List<AccountId> guestAccountIds = request.getGuestAccountIds() == null
                ? List.of()
                : request.getGuestAccountIds().stream().map(AccountId::fromString).toList();

        Challenge challenge = Challenge.create(
                request.getDate(),
                request.getTitle(),
                new DishName(request.getDishName()),
                cookAAccountId,
                cookBAccountId,
                guestAccountIds,
                organizerAccountId);
        challengeRepository.save(challenge);
        log.info("Challenge created: {}, organizer: {}", challenge.getId(), organizerAccountId);
        return ChallengeMapping.toGenerated(challenge, 0);
    }
}
