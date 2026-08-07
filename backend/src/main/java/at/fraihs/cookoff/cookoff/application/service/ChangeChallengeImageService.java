package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.mapper.ChallengeModelMapper;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ImageStoragePort;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeRestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeChallengeImageService {

    private final AccountLookup accountLookup;
    private final ChallengeRepository challengeRepository;
    private final ImageStoragePort imageStoragePort;
    private final ScoreSubmissionRepository scoreSubmissionRepository;

    @Transactional
    public ChallengeRestDto execute(
            String challengeIdString, AccountId organizerAccountId, byte[] imageBytes, String contentType) {
        if (!accountLookup.canOrganize(organizerAccountId)) {
            log.warn("Change image rejected, account cannot organize: {}", organizerAccountId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + organizerAccountId);
        }

        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));

        String oldImageRef = challenge.getImageRef();
        String newImageRef = imageStoragePort.store(imageBytes, contentType);
        challenge.changeImage(newImageRef);
        challengeRepository.save(challenge);

        if (oldImageRef != null) {
            imageStoragePort.delete(oldImageRef);
        }
        log.info("Challenge image changed: {}", challengeId);
        return ChallengeModelMapper.toGenerated(
                challenge, ChallengeModelMapper.submittedGuestCount(challenge, scoreSubmissionRepository),
                accountLookup);
    }
}
