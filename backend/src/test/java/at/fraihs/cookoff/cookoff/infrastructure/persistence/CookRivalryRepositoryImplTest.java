package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalryId;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.CookRivalryJpaEntity;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.AccountIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.CookRivalryIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.CookRivalryMapper;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CookRivalryRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CookRivalryJpaRepository jpaRepository;

    private CookRivalryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CookRivalryRepositoryImpl(jpaRepository,
                new CookRivalryMapper(new CookRivalryIdMapperImpl(), new AccountIdMapperImpl()));
    }

    @Test
    void should_roundTripFullAggregate_when_savingThenFindingByPair() {
        AccountId cookA = new AccountId(persistAccount());
        AccountId cookB = new AccountId(persistAccount());
        CookRivalry rivalry = CookRivalry.start(cookA, cookB);
        rivalry.recordResult(cookA);

        repository.save(rivalry);

        Optional<CookRivalry> found = repository.findByPair(cookA, cookB);
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getCookAWins());
        assertEquals(1, found.get().getTotalChallenges());
    }

    @Test
    void should_findRivalry_when_queriedWithArgumentsInReverseOrder() {
        AccountId first = new AccountId(persistAccount());
        AccountId second = new AccountId(persistAccount());
        repository.save(CookRivalry.start(first, second));

        AccountId[] ordered = CookRivalry.orderPair(first, second);
        Optional<CookRivalry> found = repository.findByPair(ordered[1], ordered[0]);

        assertTrue(found.isPresent());
    }

    @Test
    void should_returnAllRivalries_when_findingAll() {
        AccountId a = new AccountId(persistAccount());
        AccountId b = new AccountId(persistAccount());
        AccountId c = new AccountId(persistAccount());
        repository.save(CookRivalry.start(a, b));
        repository.save(CookRivalry.start(a, c));

        Page<CookRivalry> found = repository.findAll(PageRequest.of(0, 20));

        assertEquals(2, found.getContent().size());
        assertEquals(2L, found.getTotalElements());
    }

    @Test
    void should_rejectUnorderedPair_when_insertedDirectlyBypassingDomainNormalization() {
        long lowerId = persistAccount();
        long higherId = persistAccount();
        long ordered = Math.min(lowerId, higherId);
        long unordered = Math.max(lowerId, higherId);
        CookRivalryJpaEntity entity = new CookRivalryJpaEntity(
                CookRivalryId.generate().value(), unordered, ordered, 0, 0, 0, 0);

        assertThrows(PersistenceException.class, () -> entityManager.persistAndFlush(entity));
    }

    private long persistAccount() {
        AccountId id = AccountId.generate();
        entityManager.persistAndFlush(new AccountJpaEntity(
                id.value(), id + "@example.com", "Account", "" + id, null, Set.of()));
        return id.value();
    }
}
