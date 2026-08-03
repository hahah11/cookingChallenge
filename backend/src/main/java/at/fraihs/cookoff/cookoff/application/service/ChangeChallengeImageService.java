package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChangeChallengeImageCommand;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.port.ImageStoragePort;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
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

    @Transactional
    public void execute(ChangeChallengeImageCommand command, byte[] imageBytes) {
        AccountId organizerId = AccountId.fromString(command.organizerAccountId());
        if (!accountLookup.canOrganize(organizerId)) {
            log.warn("Change image rejected, account cannot organize: {}", organizerId);
            throw new ForbiddenException("Account is not allowed to organize challenges: " + command.organizerAccountId());
        }

        ChallengeId challengeId = ChallengeId.fromString(command.challengeId());
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(command.challengeId()));

        String oldImageRef = challenge.getImageRef();
        String newImageRef = imageStoragePort.store(imageBytes, command.contentType());
        challenge.changeImage(newImageRef);
        challengeRepository.save(challenge);

        if (oldImageRef != null) {
            imageStoragePort.delete(oldImageRef);
        }
        log.info("Challenge image changed: {}", challengeId);
    }
}
