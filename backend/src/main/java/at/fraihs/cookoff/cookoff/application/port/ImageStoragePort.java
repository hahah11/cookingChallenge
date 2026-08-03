package at.fraihs.cookoff.cookoff.application.port;

/**
 * Storage for challenge photos. {@code store}'s returned ref is opaque to callers — it's
 * whatever {@code Challenge.imageRef} should hold, not necessarily a raw id. The initial
 * adapter is the app's own database (see infrastructure.image); swapping in a real
 * object-storage provider (S3-compatible) is a future, explicitly-requested task.
 */
public interface ImageStoragePort {

    String store(byte[] bytes, String contentType);

    /** @throws at.fraihs.cookoff.cookoff.application.exception.ChallengeImageNotFoundException if imageRef is unknown */
    StoredImage resolve(String imageRef);

    void delete(String imageRef);
}
