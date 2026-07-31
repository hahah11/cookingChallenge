package at.fraihs.cookoff.cookoff.application.exception;

public class ChallengeNotFoundException extends RuntimeException {

    public ChallengeNotFoundException(String challengeId) {
        super("Challenge not found: " + challengeId);
    }
}
