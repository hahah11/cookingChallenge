package at.fraihs.cookoff.shared.web;

import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.exception.InvalidCredentialsException;
import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeImageNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotFoundException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotOpenException;
import at.fraihs.cookoff.cookoff.application.exception.ChallengeNotRevealedException;
import at.fraihs.cookoff.cookoff.application.exception.DuplicateSubmissionException;
import at.fraihs.cookoff.cookoff.application.exception.ForbiddenException;
import at.fraihs.cookoff.cookoff.application.exception.NotAParticipantException;
import at.fraihs.cookoff.cookoff.application.exception.RivalryNotFoundException;
import at.fraihs.cookoff.shared.web.dto.ApiErrorBody;
import at.fraihs.cookoff.shared.web.dto.ApiErrorDetail;
import at.fraihs.cookoff.shared.web.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Maps application-layer exceptions to the error envelope from
 * docs/shared/04-api-design.md. Domain-state-conflict exceptions (challenge not open,
 * duplicate submission, results not revealed yet) map to 409/404 rather than 400 — they
 * describe a valid request that can't be satisfied right now, not a malformed one.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountAlreadyExists(AccountAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(DuplicateSubmissionException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateSubmission(DuplicateSubmissionException ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_SUBMISSION", ex.getMessage());
    }

    @ExceptionHandler(ChallengeNotOpenException.class)
    public ResponseEntity<ApiErrorResponse> handleChallengeNotOpen(ChallengeNotOpenException ex) {
        return error(HttpStatus.CONFLICT, "CHALLENGE_NOT_OPEN", ex.getMessage());
    }

    @ExceptionHandler(NotAParticipantException.class)
    public ResponseEntity<ApiErrorResponse> handleNotAParticipant(NotAParticipantException ex) {
        return error(HttpStatus.FORBIDDEN, "NOT_A_PARTICIPANT", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(InvalidOrExpiredLinkException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrExpiredLink(InvalidOrExpiredLinkException ex) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_OR_EXPIRED_LINK", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler({AccountNotFoundException.class, ChallengeNotFoundException.class,
            ChallengeNotRevealedException.class, RivalryNotFoundException.class, ChallengeImageNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(ApiErrorBody.of("VALIDATION_ERROR", "Request validation failed", details)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(code, message));
    }
}
