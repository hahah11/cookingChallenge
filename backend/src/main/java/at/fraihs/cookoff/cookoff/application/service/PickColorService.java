package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.PickColorCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.PlateColorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PickColorService {

    private final ChallengeRepository challengeRepository;
    private final PlateColorRepository plateColorRepository;

    @Transactional
    public void execute(PickColorCommand command) {
        ChallengeId challengeId = ChallengeId.fromString(command.challengeId());
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(command.challengeId()));

        AccountId cookAccountId = AccountId.fromString(command.cookAccountId());
        boolean isCook = challenge.getCookAssignments().stream()
                .anyMatch(assignment -> assignment.accountId().equals(cookAccountId));
        if (!isCook) {
            log.warn("Color pick rejected, account {} is not a cook of challenge {}", cookAccountId, challengeId);
            throw new NotAParticipantException(command.cookAccountId(), command.challengeId());
        }

        List<PlateColor> colors = plateColorRepository.findAllActiveOrderedBySortOrder();
        if (colors.size() < 2) {
            throw new IllegalStateException("Fewer than 2 active plate colors configured");
        }
        PlateColorId first = colors.get(0).getId();
        PlateColorId second = colors.get(1).getId();
        PlateColorId chosenColorId = PlateColorId.fromString(command.colorId());
        PlateColorId otherColorId;
        if (chosenColorId.equals(first)) {
            otherColorId = second;
        } else if (chosenColorId.equals(second)) {
            otherColorId = first;
        } else {
            throw new IllegalArgumentException("colorId is not one of the available plate colors: " + command.colorId());
        }

        challenge.pickColor(cookAccountId, chosenColorId, otherColorId);
        challengeRepository.save(challenge);
        log.info("Plate color picked: challenge {}, cook {}, color {}", challengeId, cookAccountId, chosenColorId);
    }
}
