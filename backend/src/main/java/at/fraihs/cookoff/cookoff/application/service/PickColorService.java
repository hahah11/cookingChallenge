package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.mapper.ChallengeModelMapper;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.PlateColorRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.PlateColor;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PickColorRequestRestDto;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PickColorService {

    private final ChallengeRepository challengeRepository;
    private final PlateColorRepository plateColorRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final AccountLookup accountLookup;

    @Transactional
    public ParticipantChallengeRestDto execute(
            String challengeIdString, AccountId cookAccountId, PickColorRequestRestDto request) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        boolean isCook = challenge.getCookAssignments().stream()
                .anyMatch(assignment -> assignment.accountId().equals(cookAccountId));
        if (!isCook) {
            log.warn("Color pick rejected, account {} is not a cook of challenge {}", cookAccountId, challengeId);
            throw new NotAParticipantException(cookAccountId.toString(), challengeIdString);
        }

        List<PlateColor> colors = plateColorRepository.findAllActiveOrderedBySortOrder();
        if (colors.size() < 2) {
            throw new IllegalStateException("Fewer than 2 active plate colors configured");
        }
        PlateColorId first = colors.get(0).getId();
        PlateColorId second = colors.get(1).getId();
        PlateColorId chosenColorId = PlateColorId.fromString(request.getColorId());
        PlateColorId otherColorId;
        if (chosenColorId.equals(first)) {
            otherColorId = second;
        } else if (chosenColorId.equals(second)) {
            otherColorId = first;
        } else {
            throw new IllegalArgumentException("colorId is not one of the available plate colors: " + request.getColorId());
        }

        challenge.pickColor(cookAccountId, chosenColorId, otherColorId);
        challengeRepository.save(challenge);
        log.info("Plate color picked: challenge {}, cook {}, color {}", challengeId, cookAccountId, chosenColorId);

        ScoreSubmission mySubmission = scoreSubmissionRepository
                .findByChallengeIdAndGuestAccountId(challengeId, cookAccountId)
                .orElse(null);
        return ChallengeModelMapper.toParticipantChallenge(challenge, mySubmission, cookAccountId, accountLookup);
    }
}
