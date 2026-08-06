package at.fraihs.cookoff.cookoff.infrastructure.image;

import at.fraihs.cookoff.cookoff.application.dto.StoredImage;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeImageNotFoundException;
import at.fraihs.cookoff.cookoff.application.port.ImageStoragePort;
import at.fraihs.cookoff.cookoff.infrastructure.image.entity.ChallengeImageJpaEntity;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Stores challenge photos as blobs in the app's own database (challenge_images table),
 * mirroring the NotificationPort -> LoggingNotificationAdapter port/stub-adapter precedent —
 * swapping in a real object-storage provider is a future, explicitly-requested task.
 */
@Component
@RequiredArgsConstructor
public class DatabaseImageStorageAdapter implements ImageStoragePort {

    private final ChallengeImageJpaRepository jpaRepository;

    @Override
    public String store(byte[] bytes, String contentType) {
        long id = TsidSupport.generate();
        jpaRepository.save(new ChallengeImageJpaEntity(id, contentType, bytes, Instant.now()));
        return TsidSupport.toBase32(id);
    }

    @Override
    public StoredImage resolve(String imageRef) {
        ChallengeImageJpaEntity entity = jpaRepository.findById(TsidSupport.fromBase32(imageRef))
                .orElseThrow(() -> new ChallengeImageNotFoundException(imageRef));
        return new StoredImage(entity.getData(), entity.getContentType());
    }

    @Override
    public void delete(String imageRef) {
        jpaRepository.deleteById(TsidSupport.fromBase32(imageRef));
    }
}
