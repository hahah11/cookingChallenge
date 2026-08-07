package at.fraihs.cookoff.auth.infrastructure.registrationinvite;

import at.fraihs.cookoff.auth.application.dto.RegistrationInvite;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ChallengeJpaEntity;
import at.fraihs.cookoff.shared.tsid.TsidSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RegistrationInviteRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RegistrationInviteJpaRepository jpaRepository;

    private RegistrationInviteRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RegistrationInviteRepositoryImpl(jpaRepository);
    }

    @Test
    void should_roundTripRegistrationInvite_when_savingThenFindingByToken() {
        AccountId issuedByAccountId = new AccountId(persistAccount());
        long challengeId = persistChallenge(issuedByAccountId.value());
        Instant expiresAt = Instant.now().plusSeconds(3600);
        RegistrationInvite invite = new RegistrationInvite(
                TsidSupport.generate(), issuedByAccountId, challengeId, "a-high-entropy-token", expiresAt);

        repository.save(invite);
        Optional<RegistrationInvite> found = repository.findByToken("a-high-entropy-token");

        assertTrue(found.isPresent());
        assertEquals(issuedByAccountId, found.get().issuedByAccountId());
        assertEquals(challengeId, found.get().challengeId());
        assertEquals(expiresAt, found.get().expiresAt());
    }

    @Test
    void should_returnEmpty_when_tokenDoesNotExist() {
        assertFalse(repository.findByToken("unknown-token").isPresent());
    }

    private long persistAccount() {
        AccountId id = AccountId.generate();
        entityManager.persistAndFlush(new AccountJpaEntity(
                id.value(), id + "@example.com", "Account", "" + id, null, Set.of()));
        return id.value();
    }

    private long persistChallenge(long cookAAccountId) {
        long id = TsidSupport.generate();
        long cookBAccountId = persistAccount();
        entityManager.persistAndFlush(new ChallengeJpaEntity(
                id, "Test Challenge", LocalDate.now(), "Schnitzel",
                cookAAccountId, cookBAccountId, null, null, ChallengeStatus.OPEN, cookAAccountId, List.of(), null,
                false, null));
        return id;
    }
}
