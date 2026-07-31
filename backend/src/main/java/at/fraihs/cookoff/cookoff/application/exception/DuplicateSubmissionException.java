package at.fraihs.cookoff.cookoff.application.exception;

public class DuplicateSubmissionException extends RuntimeException {

    public DuplicateSubmissionException(String accountId, String challengeId) {
        super("Account " + accountId + " has already submitted scores for challenge " + challengeId);
    }
}
