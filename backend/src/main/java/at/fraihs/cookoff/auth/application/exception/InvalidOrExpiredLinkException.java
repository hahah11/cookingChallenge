package at.fraihs.cookoff.auth.application.exception;

public class InvalidOrExpiredLinkException extends RuntimeException {

    public InvalidOrExpiredLinkException() {
        super("Access link is invalid or expired");
    }
}
