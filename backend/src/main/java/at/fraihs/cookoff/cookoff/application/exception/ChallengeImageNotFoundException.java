package at.fraihs.cookoff.cookoff.application.exception;

public class ChallengeImageNotFoundException extends RuntimeException {

    public ChallengeImageNotFoundException(String imageRef) {
        super("Challenge image not found: " + imageRef);
    }
}
