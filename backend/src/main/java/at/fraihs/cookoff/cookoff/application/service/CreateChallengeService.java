package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.repository.AccountRepository;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeView;
import at.fraihs.cookoff.cookoff.application.dto.CreateChallengeCommand;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateChallengeService {

    private final AccountRepository accountRepository;
    private final ChallengeRepository challengeRepository;

    @Transactional
    public ChallengeView execute(CreateChallengeCommand command) {
        AccountId organizerId = AccountId.fromString(command.organizerAccountId());
        Account organizer = accountRepository.findById(organizerId)
                .orElseThrow(() -> new AccountNotFoundException(command.organizerAccountId()));
        if (!organizer.canOrganize()) {
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
        return ChallengeView.from(challenge);
    }
}
