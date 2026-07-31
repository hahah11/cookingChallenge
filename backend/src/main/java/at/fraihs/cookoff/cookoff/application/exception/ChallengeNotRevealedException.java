package at.fraihs.cookoff.cookoff.application.exception;

public class ChallengeNotRevealedException extends RuntimeException {

    public ChallengeNotRevealedException(String challengeId) {
        super("Challenge results are not available yet: " + challengeId);
    }
}
