package at.fraihs.cookoff.cookoff.application.exception;

public class ChallengeNotOpenException extends RuntimeException {

    public ChallengeNotOpenException(String challengeId) {
        super("Challenge is not open: " + challengeId);
    }
}
