package at.fraihs.cookoff.auth.application.exception;

/** The account holds no password — a guest, who logs in through access links instead. */
public class PasswordResetNotEligibleException extends RuntimeException {

    public PasswordResetNotEligibleException(String accountId) {
        super("Account has no password to reset: " + accountId);
    }
}
