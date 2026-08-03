package at.fraihs.cookoff.cookoff.application.exception;

public class RivalryNotFoundException extends RuntimeException {

    public RivalryNotFoundException(String cookAAccountId, String cookBAccountId) {
        super("No rivalry (shared challenges) found between accounts: " + cookAAccountId + ", " + cookBAccountId);
    }
}
