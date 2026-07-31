package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.persistence.AccountJpaEntity;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChallengeRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChallengeJpaRepository jpaRepository;

    private ChallengeRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ChallengeRepositoryImpl(jpaRepository, new ChallengeMapperImpl());
    }

    @Test
    void should_roundTripFullAggregate_when_savingThenFindingById() {
        AccountId cookA = new AccountId(persistAccount());
        AccountId cookB = new AccountId(persistAccount());
        AccountId guest1 = new AccountId(persistAccount());
        AccountId guest2 = new AccountId(persistAccount());
        AccountId organizer = new AccountId(persistAccount());
        Challenge challenge = Challenge.create(
                LocalDate.of(2026, 8, 1), "Schnitzel Showdown", new DishName("Schnitzel"),
                cookA, cookB, List.of(guest1, guest2), organizer);

        Challenge saved = repository.save(challenge);
        Optional<Challenge> found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        Challenge result = found.get();
        assertEquals(saved.getId(), result.getId());
        assertEquals(LocalDate.of(2026, 8, 1), result.getDate());
        assertEquals("Schnitzel Showdown", result.getTitle());
        assertEquals(new DishName("Schnitzel"), result.getDishName());
        assertEquals(cookA, result.cookAssignmentFor(DishLabel.A).accountId());
        assertEquals(cookB, result.cookAssignmentFor(DishLabel.B).accountId());
        assertEquals(List.of(guest1, guest2), result.getGuestAccountIds());
        assertEquals(ChallengeStatus.OPEN, result.getStatus());
        assertEquals(organizer, result.getCreatedBy());
    }

    @Test
    void should_returnAllChallenges_when_findingAll() {
        AccountId organizer = new AccountId(persistAccount());
        repository.save(Challenge.create(LocalDate.now(), null, new DishName("Goulash"),
                new AccountId(persistAccount()), new AccountId(persistAccount()), List.of(), organizer));
        repository.save(Challenge.create(LocalDate.now(), null, new DishName("Kaiserschmarrn"),
                new AccountId(persistAccount()), new AccountId(persistAccount()), List.of(), organizer));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void should_returnOpenChallengesForParticipant_when_accountIsCookOrGuest_butNotWhenRevealedOrUnrelated() {
        AccountId organizer = new AccountId(persistAccount());
        AccountId cookA = new AccountId(persistAccount());
        AccountId cookB = new AccountId(persistAccount());
        AccountId guest = new AccountId(persistAccount());
        AccountId stranger = new AccountId(persistAccount());

        Challenge openAsGuest = repository.save(Challenge.create(LocalDate.now(), null, new DishName("Goulash"),
                cookA, new AccountId(persistAccount()), List.of(guest), organizer));
        Challenge openAsCook = repository.save(Challenge.create(LocalDate.now(), null, new DishName("Kaiserschmarrn"),
                cookB, new AccountId(persistAccount()), List.of(), organizer));
        Challenge revealed = Challenge.create(LocalDate.now(), null, new DishName("Palatschinken"),
                cookA, cookB, List.of(guest), organizer);
        revealed.reveal(null);
        repository.save(revealed);

        List<Challenge> guestResult = repository.findOpenByParticipant(guest);
        assertEquals(List.of(openAsGuest.getId()), guestResult.stream().map(Challenge::getId).toList());

        List<Challenge> cookResult = repository.findOpenByParticipant(cookB);
        assertEquals(List.of(openAsCook.getId()), cookResult.stream().map(Challenge::getId).toList());

        assertTrue(repository.findOpenByParticipant(stranger).isEmpty());
    }

    private long persistAccount() {
        AccountId id = AccountId.generate();
        entityManager.persistAndFlush(new AccountJpaEntity(
                id.value(), id + "@example.com", "Account " + id, null, Set.of()));
        return id.value();
    }
}
