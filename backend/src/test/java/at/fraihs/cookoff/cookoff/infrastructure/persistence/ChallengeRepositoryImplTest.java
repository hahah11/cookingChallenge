package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.PlateColorId;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.PlateColorJpaEntity;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.AccountIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ChallengeIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ChallengeMapper;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.CookAssignmentMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.DishNameMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.PlateColorIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.RevealResultMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        repository = new ChallengeRepositoryImpl(jpaRepository, new ChallengeMapper(
                new ChallengeIdMapperImpl(), new AccountIdMapperImpl(), new PlateColorIdMapperImpl(),
                new DishNameMapperImpl(), new CookAssignmentMapperImpl(), new RevealResultMapperImpl()));
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

        assertEquals(2, repository.findAll(Pageable.unpaged()).getTotalElements());
    }

    @Test
    void should_returnEveryChallengeForParticipant_regardlessOfStatus_butNotUnrelatedChallenges() {
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
        Challenge savedRevealed = repository.save(revealed);

        List<Challenge> guestResult = repository.findByParticipant(guest);
        assertEquals(Set.of(openAsGuest.getId(), savedRevealed.getId()),
                guestResult.stream().map(Challenge::getId).collect(Collectors.toSet()));

        List<Challenge> cookResult = repository.findByParticipant(cookB);
        assertEquals(Set.of(openAsCook.getId(), savedRevealed.getId()),
                cookResult.stream().map(Challenge::getId).collect(Collectors.toSet()));

        assertTrue(repository.findByParticipant(stranger).isEmpty());
    }

    @Test
    void should_returnChallengesBetweenCookPair_regardlessOfWhichCookWasAOrB_when_findingByCookPair() {
        AccountId organizer = new AccountId(persistAccount());
        AccountId cookX = new AccountId(persistAccount());
        AccountId cookY = new AccountId(persistAccount());
        AccountId stranger = new AccountId(persistAccount());

        Challenge xThenY = repository.save(Challenge.create(LocalDate.now(), null, new DishName("Goulash"),
                cookX, cookY, List.of(), organizer));
        Challenge yThenX = repository.save(Challenge.create(LocalDate.now(), null, new DishName("Kaiserschmarrn"),
                cookY, cookX, List.of(), organizer));
        repository.save(Challenge.create(LocalDate.now(), null, new DishName("Palatschinken"),
                cookX, stranger, List.of(), organizer));

        List<Challenge> found = repository.findByCookPair(cookX, cookY);

        assertEquals(Set.of(xThenY.getId(), yThenX.getId()), found.stream().map(Challenge::getId).collect(Collectors.toSet()));
    }

    @Test
    void should_roundTripPickedColors_when_savingThenFindingById() {
        AccountId cookA = new AccountId(persistAccount());
        AccountId cookB = new AccountId(persistAccount());
        AccountId organizer = new AccountId(persistAccount());
        PlateColorId red = persistPlateColor();
        PlateColorId yellow = persistPlateColor();
        Challenge challenge = Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookA, cookB, List.of(), organizer);
        challenge.pickColor(cookA, red, yellow);

        Challenge saved = repository.save(challenge);
        Challenge found = repository.findById(saved.getId()).orElseThrow();

        assertEquals(red, found.cookAssignmentFor(DishLabel.A).colorId());
        assertEquals(yellow, found.cookAssignmentFor(DishLabel.B).colorId());
    }

    @Test
    void should_roundTripImageRef_when_savingThenFindingById() {
        AccountId cookA = new AccountId(persistAccount());
        AccountId cookB = new AccountId(persistAccount());
        AccountId organizer = new AccountId(persistAccount());
        Challenge challenge = Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookA, cookB, List.of(), organizer);
        challenge.changeImage("image-ref-1");

        Challenge saved = repository.save(challenge);
        Challenge found = repository.findById(saved.getId()).orElseThrow();

        assertEquals("image-ref-1", found.getImageRef());
    }

    @Test
    void should_roundTripRevealResult_when_revealedThenUnrevealedThenFindingById() {
        AccountId cookA = new AccountId(persistAccount());
        AccountId cookB = new AccountId(persistAccount());
        AccountId organizer = new AccountId(persistAccount());
        Challenge challenge = Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookA, cookB, List.of(), organizer);
        challenge.reveal(cookA);

        Challenge savedRevealed = repository.save(challenge);
        Challenge foundRevealed = repository.findById(savedRevealed.getId()).orElseThrow();
        assertEquals(ChallengeStatus.REVEALED, foundRevealed.getStatus());
        assertEquals(cookA, foundRevealed.getLastRevealResult().winnerAccountId());

        foundRevealed.unreveal();
        Challenge savedUnrevealed = repository.save(foundRevealed);
        Challenge foundUnrevealed = repository.findById(savedUnrevealed.getId()).orElseThrow();
        assertEquals(ChallengeStatus.OPEN, foundUnrevealed.getStatus());
        assertEquals(null, foundUnrevealed.getLastRevealResult());
    }

    private PlateColorId persistPlateColor() {
        PlateColorId id = PlateColorId.generate();
        entityManager.persistAndFlush(new PlateColorJpaEntity(id.value(), "Color " + id, "#000000", 1, true));
        return id;
    }

    private long persistAccount() {
        AccountId id = AccountId.generate();
        entityManager.persistAndFlush(new AccountJpaEntity(
                id.value(), id + "@example.com", "Account", "" + id, null, Set.of()));
        return id.value();
    }
}
