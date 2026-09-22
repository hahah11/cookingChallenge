package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.PasswordResetNotification;
import at.fraihs.cookoff.auth.application.dto.PasswordResetToken;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.exception.PasswordResetNotEligibleException;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.application.port.PasswordResetNotificationPort;
import at.fraihs.cookoff.auth.application.port.PasswordResetTokenRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.tsid.TsidSupport;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-initiated password reset (docs/cookingChallenge/plans/admin-password-reset-plan.md):
 * issues a single-use, 2-hour token and mails the account a link to set a new password.
 * Issuing supersedes any earlier unused link. Admin-only is enforced in {@code SecurityConfig}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(2);
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetNotificationPort notificationPort;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public void execute(AccountId accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
        // The hash, not the role set: it is what a reset actually replaces.
        if (account.getPasswordHash() == null) {
            throw new PasswordResetNotEligibleException(accountId.toString());
        }

        tokenRepository.deleteAllByAccountId(accountId);
        String token = generateToken();
        Instant now = Instant.now();
        tokenRepository.save(new PasswordResetToken(
                TsidSupport.generate(), accountId, token, now.plus(TOKEN_VALIDITY), null, now));

        notificationPort.sendPasswordReset(new PasswordResetNotification(
                account.getEmail(), account.getFirstName(), frontendBaseUrl + "/reset-password?token=" + token));
        log.info("Password reset issued for account {}", accountId);
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
