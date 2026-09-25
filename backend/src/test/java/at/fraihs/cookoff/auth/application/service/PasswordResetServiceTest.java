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
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.auth.domain.model.Language;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetNotificationPort notificationPort;

    @InjectMocks
    private PasswordResetService service;

    private final AccountId accountId = AccountId.generate();

    private Account account(String passwordHash, SystemRole role) {
        return Account.reconstitute(accountId, new Email("org@example.com"), "Olga", "Organizer",
                passwordHash, Set.of(role), Language.EN);
    }

    @Test
    void should_supersedeOlderLinksThenIssueANewOne_when_accountHasAPassword() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account("hash", SystemRole.ORGANIZER)));

        service.execute(accountId);

        InOrder order = inOrder(tokenRepository);
        order.verify(tokenRepository).deleteAllByAccountId(accountId);
        order.verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void should_issueASingleUseTwoHourToken_when_accountHasAPassword() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account("hash", SystemRole.ADMIN)));
        Instant before = Instant.now();

        service.execute(accountId);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken token = captor.getValue();
        assertEquals(accountId, token.accountId());
        assertEquals(null, token.usedAt());
        Duration validity = Duration.between(before, token.expiresAt());
        assertTrue(!validity.minus(Duration.ofHours(2)).isNegative()
                && validity.compareTo(Duration.ofHours(2).plusMinutes(1)) < 0, validity.toString());
    }

    @Test
    void should_mailTheResetLinkCarryingTheIssuedToken_when_accountHasAPassword() {
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "https://cookoff.test");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account("hash", SystemRole.ORGANIZER)));

        service.execute(accountId);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<PasswordResetNotification> mailCaptor = ArgumentCaptor.forClass(PasswordResetNotification.class);
        verify(notificationPort).sendPasswordReset(mailCaptor.capture());
        PasswordResetNotification mail = mailCaptor.getValue();
        assertEquals(new Email("org@example.com"), mail.recipient());
        assertEquals("Olga", mail.firstName());
        assertEquals("https://cookoff.test/reset-password?token=" + tokenCaptor.getValue().token(), mail.link());
    }

    @Test
    void should_mailInTheAccountsLanguage_when_accountPrefersGerman() {
        Account german = Account.reconstitute(accountId, new Email("org@example.com"), "Olga", "Organizer",
                "hash", Set.of(SystemRole.ORGANIZER), Language.DE);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(german));

        service.execute(accountId);

        ArgumentCaptor<PasswordResetNotification> mailCaptor = ArgumentCaptor.forClass(PasswordResetNotification.class);
        verify(notificationPort).sendPasswordReset(mailCaptor.capture());
        assertEquals(Locale.GERMAN, mailCaptor.getValue().locale());
    }

    @Test
    void should_rejectAndIssueNothing_when_accountHasNoPassword() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(null, SystemRole.USER)));

        assertThrows(PasswordResetNotEligibleException.class, () -> service.execute(accountId));
        verifyNoInteractions(tokenRepository, notificationPort);
    }

    @Test
    void should_throwNotFound_when_accountDoesNotExist() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.execute(accountId));
        verifyNoInteractions(tokenRepository, notificationPort);
    }
}
