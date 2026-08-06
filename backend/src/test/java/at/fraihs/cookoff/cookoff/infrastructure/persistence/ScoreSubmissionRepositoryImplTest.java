package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import at.fraihs.cookoff.cookoff.application.exception.DuplicateSubmissionException;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeStatus;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ChallengeJpaEntity;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ScoreEmbeddable;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.entity.ScoreSubmissionJpaEntity;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.AccountIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ChallengeIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ScoreMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ScoreSubmissionIdMapperImpl;
import at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper.ScoreSubmissionMapper;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScoreSubmissionRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ScoreSubmissionJpaRepository jpaRepository;

    private ScoreSubmissionRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ScoreSubmissionRepositoryImpl(jpaRepository, new ScoreSubmissionMapper(
                new ScoreSubmissionIdMapperImpl(), new ChallengeIdMapperImpl(), new AccountIdMapperImpl(), new ScoreMapperImpl()));
    }

    @Test
    void should_roundTripFullAggregate_when_savingThenFindingByChallengeId() {
        long guestId = persistAccount();
        long challengeId = persistChallenge();

        ScoreSubmission submission = ScoreSubmission.submit(
                new ChallengeId(challengeId), new AccountId(guestId), buildScores(), Instant.now());
        repository.save(submission);

        List<ScoreSubmission> found = repository.findByChallengeId(new ChallengeId(challengeId));

        assertEquals(1, found.size());
        assertEquals(6, found.get(0).getScores().size());
        assertEquals(new AccountId(guestId), found.get(0).getGuestAccountId());
    }

    @Test
    void should_reportExists_when_challengeAndGuestAlreadySubmitted() {
        long guestId = persistAccount();
        long challengeId = persistChallenge();
        repository.save(ScoreSubmission.submit(
                new ChallengeId(challengeId), new AccountId(guestId), buildScores(), Instant.now()));

        boolean exists = repository.existsByChallengeIdAndGuestAccountId(
                new ChallengeId(challengeId), new AccountId(guestId));

        assertEquals(true, exists);
    }

    @Test
    void should_findSubmission_when_queriedByChallengeIdAndGuestAccountId() {
        long guestId = persistAccount();
        long challengeId = persistChallenge();
        repository.save(ScoreSubmission.submit(
                new ChallengeId(challengeId), new AccountId(guestId), buildScores(), Instant.now()));

        Optional<ScoreSubmission> found = repository.findByChallengeIdAndGuestAccountId(
                new ChallengeId(challengeId), new AccountId(guestId));

        assertTrue(found.isPresent());
        assertEquals(new AccountId(guestId), found.get().getGuestAccountId());
    }

    @Test
    void should_returnEmpty_when_noSubmissionExistsForChallengeAndGuestAccountId() {
        long guestId = persistAccount();
        long challengeId = persistChallenge();

        Optional<ScoreSubmission> found = repository.findByChallengeIdAndGuestAccountId(
                new ChallengeId(challengeId), new AccountId(guestId));

        assertTrue(found.isEmpty());
    }

    @Test
    void should_updateInPlace_when_savingAResubmittedScoreSubmission() {
        long guestId = persistAccount();
        long challengeId = persistChallenge();
        ChallengeId cId = new ChallengeId(challengeId);
        AccountId guestAccountId = new AccountId(guestId);
        ScoreSubmission submission = ScoreSubmission.submit(cId, guestAccountId, buildScores(), Instant.now());
        repository.save(submission);

        ScoreSubmission existing = repository.findByChallengeIdAndGuestAccountId(cId, guestAccountId).orElseThrow();
        List<Score> revisedScores = new ArrayList<>();
        for (DishLabel label : DishLabel.values()) {
            for (Category category : Category.values()) {
                revisedScores.add(new Score(label, category, 5));
            }
        }
        existing.update(revisedScores, Instant.now());
        repository.save(existing);

        List<ScoreSubmission> allForChallenge = repository.findByChallengeId(cId);
        assertEquals(1, allForChallenge.size());
        assertEquals(revisedScores, allForChallenge.get(0).getScores());
    }

    @Test
    void should_rejectDuplicateSubmission_when_sameChallengeAndGuestSubmitTwice() {
        long guestId = persistAccount();
        long challengeId = persistChallenge();
        ChallengeId cId = new ChallengeId(challengeId);
        AccountId guestAccountId = new AccountId(guestId);
        repository.save(ScoreSubmission.submit(cId, guestAccountId, buildScores(), Instant.now()));

        ScoreSubmission secondSubmission = ScoreSubmission.submit(cId, guestAccountId, buildScores(), Instant.now());

        assertThrows(DuplicateSubmissionException.class, () -> repository.save(secondSubmission));
    }

    @Test
    void should_rejectZeroPoints_when_insertedDirectlyBypassingDomainGuard() {
        long guestId = persistAccount();
        long challengeId = persistChallenge();
        ScoreSubmissionJpaEntity entity = new ScoreSubmissionJpaEntity();
        entity.setId(ScoreSubmissionId.generate().value());
        entity.setChallengeId(challengeId);
        entity.setGuestAccountId(guestId);
        entity.setSubmittedAt(Instant.now());
        entity.setScores(List.of(new ScoreEmbeddable(DishLabel.A, Category.GESCHMACK, 0)));

        assertThrows(PersistenceException.class, () -> entityManager.persistAndFlush(entity));
    }

    private long persistAccount() {
        AccountId id = AccountId.generate();
        entityManager.persistAndFlush(new AccountJpaEntity(
                id.value(), id + "@example.com", "Account " + id, null, Set.of()));
        return id.value();
    }

    private long persistChallenge() {
        long cookA = persistAccount();
        long cookB = persistAccount();
        long organizer = persistAccount();
        ChallengeId id = ChallengeId.generate();
        entityManager.persistAndFlush(new ChallengeJpaEntity(
                id.value(), "Test Challenge", LocalDate.now(), "Schnitzel", cookA, cookB, null, null,
                ChallengeStatus.OPEN, organizer, new ArrayList<>(), null, false, null));
        return id.value();
    }

    private List<Score> buildScores() {
        List<Score> scores = new ArrayList<>();
        for (DishLabel label : DishLabel.values()) {
            for (Category category : Category.values()) {
                scores.add(new Score(label, category, 3));
            }
        }
        return scores;
    }
}
