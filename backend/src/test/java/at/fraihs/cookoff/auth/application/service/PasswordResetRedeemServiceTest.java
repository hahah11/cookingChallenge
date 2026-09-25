package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.application.dto.PasswordResetToken;
import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.application.port.PasswordResetTokenRepository;
import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.domain.model.Language;
import at.fraihs.cookoff.shared.web.openapi.model.PasswordResetRedeemRequestRestDto;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetRedeemServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetRedeemService service;

    private final AccountId accountId = AccountId.generate();

    private PasswordResetToken claimed() {
        Instant now = Instant.now();
        return new PasswordResetToken(1L, accountId, "tok", now.plusSeconds(3600), now, now);
    }

    @Test
    void should_storeTheNewPasswordHash_when_tokenIsClaimed() {
        Account account = Account.reconstitute(accountId, new Email("org@example.com"), "Olga", "Organizer",
                "old-hash", Set.of(SystemRole.ORGANIZER), Language.EN);
        when(tokenRepository.claim(eq("tok"), any(Instant.class))).thenReturn(Optional.of(claimed()));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.execute(new PasswordResetRedeemRequestRestDto("tok", "new-password"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals("new-hash", captor.getValue().getPasswordHash());
    }

    /** Stands in for the double-submit race: the loser's claim matches no row. */
    @Test
    void should_rejectWithoutTouchingTheAccount_when_claimFails() {
        when(tokenRepository.claim(eq("tok"), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredLinkException.class,
                () -> service.execute(new PasswordResetRedeemRequestRestDto("tok", "new-password")));
        verifyNoInteractions(accountRepository, passwordEncoder);
    }

    @Test
    void should_reject_when_accountVanishedAfterClaim() {
        when(tokenRepository.claim(eq("tok"), any(Instant.class))).thenReturn(Optional.of(claimed()));
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredLinkException.class,
                () -> service.execute(new PasswordResetRedeemRequestRestDto("tok", "new-password")));
        verify(accountRepository, never()).save(any());
    }
}
