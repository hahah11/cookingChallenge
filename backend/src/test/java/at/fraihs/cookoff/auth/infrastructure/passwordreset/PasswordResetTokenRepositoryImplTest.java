package at.fraihs.cookoff.auth.infrastructure.passwordreset;

import at.fraihs.cookoff.auth.application.dto.PasswordResetToken;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordResetTokenRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordResetTokenJpaRepository jpaRepository;

    private PasswordResetTokenRepositoryImpl repository;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @BeforeEach
    void setUp() {
        repository = new PasswordResetTokenRepositoryImpl(jpaRepository);
    }

    private PasswordResetToken token(AccountId accountId, String value, Instant expiresAt) {
        return new PasswordResetToken(TsidSupport.generate(), accountId, value, expiresAt, null, now);
    }

    @Test
    void should_roundTripToken_when_savingThenFindingByToken() {
        AccountId accountId = persistAccount();
        repository.save(token(accountId, "reset-token", now.plusSeconds(7200)));

        Optional<PasswordResetToken> found = repository.findByToken("reset-token");

        assertTrue(found.isPresent());
        assertEquals(accountId, found.get().accountId());
        assertEquals(now.plusSeconds(7200), found.get().expiresAt());
    }

    @Test
    void should_rejectADuplicateTokenValue_when_saving() {
        AccountId accountId = persistAccount();
        repository.save(token(accountId, "same-token", now.plusSeconds(7200)));

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(token(accountId, "same-token", now.plusSeconds(7200)));
            jpaRepository.flush();
        });
    }

    @Test
    void should_claimOnceOnly_when_claimedTwice() {
        AccountId accountId = persistAccount();
        repository.save(token(accountId, "once", now.plusSeconds(7200)));

        Optional<PasswordResetToken> first = repository.claim("once", now);
        Optional<PasswordResetToken> second = repository.claim("once", now.plusSeconds(1));

        assertTrue(first.isPresent());
        assertNotNull(first.get().usedAt());
        assertFalse(second.isPresent());
    }

    @Test
    void should_notClaim_when_tokenHasExpired() {
        AccountId accountId = persistAccount();
        repository.save(token(accountId, "stale", now.minusSeconds(1)));

        assertFalse(repository.claim("stale", now).isPresent());
    }

    @Test
    void should_notClaim_when_tokenIsUnknown() {
        assertFalse(repository.claim("nope", now).isPresent());
    }

    @Test
    void should_deleteOnlyThatAccountsTokens_when_deletingByAccount() {
        AccountId target = persistAccount();
        AccountId other = persistAccount();
        repository.save(token(target, "target-1", now.plusSeconds(7200)));
        repository.save(token(target, "target-2", now.plusSeconds(7200)));
        repository.save(token(other, "other", now.plusSeconds(7200)));

        repository.deleteAllByAccountId(target);

        assertFalse(repository.findByToken("target-1").isPresent());
        assertFalse(repository.findByToken("target-2").isPresent());
        assertTrue(repository.findByToken("other").isPresent());
    }

    private AccountId persistAccount() {
        AccountId id = AccountId.generate();
        entityManager.persistAndFlush(new AccountJpaEntity(
                id.value(), id + "@example.com", "Account", "" + id, "hash", Set.of()));
        return id;
    }
}
