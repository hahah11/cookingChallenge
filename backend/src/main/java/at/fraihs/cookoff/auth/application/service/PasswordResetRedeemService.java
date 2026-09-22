package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.PasswordResetToken;
import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.application.port.PasswordResetTokenRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.shared.web.openapi.model.PasswordResetRedeemRequestRestDto;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redeems a password-reset token. The token is claimed atomically <em>before</em> the account is
 * touched, so a double submit changes the password once and the loser gets the same
 * {@link InvalidOrExpiredLinkException} as an unknown or expired token.
 *
 * <p>Known limitation: JWTs are stateless, so sessions already open for this account survive the
 * reset until they expire. See the plan's "existing sessions survive a reset" section.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetRedeemService {

    private final PasswordResetTokenRepository tokenRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(PasswordResetRedeemRequestRestDto request) {
        PasswordResetToken claimed = tokenRepository.claim(request.getToken(), Instant.now())
                .orElseThrow(InvalidOrExpiredLinkException::new);
        Account account = accountRepository.findById(claimed.accountId())
                .orElseThrow(InvalidOrExpiredLinkException::new);

        account.changePasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        log.info("Password reset redeemed for account {}", account.getId());
    }
}
