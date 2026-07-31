package at.fraihs.cookoff.auth.application.exception;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException(String email) {
        super("Account already exists for email: " + email);
    }
}
