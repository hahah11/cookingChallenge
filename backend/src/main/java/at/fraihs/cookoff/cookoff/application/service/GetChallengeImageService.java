package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.cookoff.application.exception.ChallengeImageNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.ImageStoragePort;
import at.fraihs.cookoff.cookoff.application.port.StoredImage;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Streams a challenge's photo bytes. Visibility (organizer bearer token or a valid
 * access-link token) is enforced by the security filter chain, not here — unlike the
 * participant-facing read model, there's no per-requester field to hide.
 */
@Service
@RequiredArgsConstructor
public class GetChallengeImageService {

    private final ChallengeRepository challengeRepository;
    private final ImageStoragePort imageStoragePort;

    @Transactional(readOnly = true)
    public StoredImage execute(String challengeIdString) {
        ChallengeId challengeId = ChallengeId.fromString(challengeIdString);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeIdString));
        if (challenge.getImageRef() == null) {
            throw new ChallengeImageNotFoundException(challengeIdString);
        }
        return imageStoragePort.resolve(challenge.getImageRef());
    }
}
