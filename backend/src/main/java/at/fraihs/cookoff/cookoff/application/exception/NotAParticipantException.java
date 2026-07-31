package at.fraihs.cookoff.cookoff.application.exception;

public class NotAParticipantException extends RuntimeException {

    public NotAParticipantException(String accountId, String challengeId) {
        super("Account " + accountId + " is not a participant of challenge " + challengeId);
    }
}
