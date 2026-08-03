package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.UnrevealChallengeCommand;
import at.fraihs.cookoff.cookoff.application.event.ChallengeRevealedRivalryUpdater;
import at.fraihs.cookoff.cookoff.application.event.ChallengeUnrevealedRivalryUpdater;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeRevealed;
import at.fraihs.cookoff.cookoff.domain.event.ChallengeUnrevealed;
import at.fraihs.cookoff.cookoff.domain.model.Category;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import at.fraihs.cookoff.cookoff.domain.model.DishLabel;
import at.fraihs.cookoff.cookoff.domain.model.DishName;
import at.fraihs.cookoff.cookoff.domain.model.Score;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmission;
import at.fraihs.cookoff.cookoff.domain.model.ScoreSubmissionId;
import at.fraihs.cookoff.cookoff.domain.repository.ChallengeRepository;
import at.fraihs.cookoff.cookoff.domain.repository.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.domain.repository.ScoreSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Exercises RevealChallengeService/UnrevealChallengeService together with the two
 * TransactionalEventListeners against in-memory repository fakes (rather than mocks), since
 * this is the one scenario the whole unreveal mechanism exists for: a reveal, an unreveal, and
 * a re-reveal with a changed result must not double- or under-count CookRivalry — see Verify
 * 7.6 in backend-persistence-api-security-plan.md. The fake ApplicationEventPublisher below
 * dispatches synchronously, standing in for Spring's real AFTER_COMMIT firing.
 */
@ExtendWith(MockitoExtension.class)
class ChallengeRevealUnrevealRivalryIntegrationTest {

    @Mock
    private AccountLookup accountLookup;

    private final AccountId cookAId = AccountId.generate();
    private final AccountId cookBId = AccountId.generate();
    private final AccountId organizerId = AccountId.generate();
    private final AccountId guestId = AccountId.generate();

    private final InMemoryChallengeRepository challengeRepository = new InMemoryChallengeRepository();
    private final InMemoryScoreSubmissionRepository scoreSubmissionRepository = new InMemoryScoreSubmissionRepository();
    private final InMemoryCookRivalryRepository cookRivalryRepository = new InMemoryCookRivalryRepository();

    private RevealChallengeService revealChallengeService;
    private UnrevealChallengeService unrevealChallengeService;

    @BeforeEach
    void setUp() {
        ChallengeRevealedRivalryUpdater revealedUpdater = new ChallengeRevealedRivalryUpdater(cookRivalryRepository);
        ChallengeUnrevealedRivalryUpdater unrevealedUpdater = new ChallengeUnrevealedRivalryUpdater(cookRivalryRepository);
        var synchronousPublisher = new org.springframework.context.ApplicationEventPublisher() {
            @Override
            public void publishEvent(Object event) {
                if (event instanceof ChallengeRevealed revealed) {
                    revealedUpdater.on(revealed);
                } else if (event instanceof ChallengeUnrevealed unrevealed) {
                    unrevealedUpdater.on(unrevealed);
                }
            }
        };
        revealChallengeService = new RevealChallengeService(challengeRepository, scoreSubmissionRepository, synchronousPublisher);
        unrevealChallengeService = new UnrevealChallengeService(accountLookup, challengeRepository, synchronousPublisher);
    }

    @Test
    void should_endUpWithCorrectRivalryCounters_when_revealingUnrevealingAndReRevealingWithADifferentResult() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        Challenge challenge = Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(guestId), organizerId);
        challengeRepository.save(challenge);
        scoreSubmissionRepository.replace(challenge.getId(), cookAWinsSubmission(challenge.getId()));

        revealChallengeService.execute(challenge.getId().toString());

        CookRivalry afterFirstReveal = cookRivalryRepository.findByPair(cookAId, cookBId).orElseThrow();
        assertEquals(1, afterFirstReveal.getCookAWins());
        assertEquals(0, afterFirstReveal.getCookBWins());
        assertEquals(0, afterFirstReveal.getDraws());
        assertEquals(1, afterFirstReveal.getTotalChallenges());

        unrevealChallengeService.execute(new UnrevealChallengeCommand(challenge.getId().toString(), organizerId.toString()));

        CookRivalry afterUnreveal = cookRivalryRepository.findByPair(cookAId, cookBId).orElseThrow();
        assertEquals(0, afterUnreveal.getCookAWins());
        assertEquals(0, afterUnreveal.getCookBWins());
        assertEquals(0, afterUnreveal.getDraws());
        assertEquals(0, afterUnreveal.getTotalChallenges());

        // scores edited in between (guest resubmits, now favoring cook B)
        scoreSubmissionRepository.replace(challenge.getId(), cookBWinsSubmission(challenge.getId()));

        revealChallengeService.execute(challenge.getId().toString());

        CookRivalry afterSecondReveal = cookRivalryRepository.findByPair(cookAId, cookBId).orElseThrow();
        assertEquals(0, afterSecondReveal.getCookAWins());
        assertEquals(1, afterSecondReveal.getCookBWins());
        assertEquals(0, afterSecondReveal.getDraws());
        assertEquals(1, afterSecondReveal.getTotalChallenges());
    }

    private ScoreSubmission cookAWinsSubmission(ChallengeId challengeId) {
        return ScoreSubmission.submit(challengeId, guestId, List.of(
                new Score(DishLabel.A, Category.MUNDGEFUEHL, 5),
                new Score(DishLabel.A, Category.TELLERSPRACHE, 5),
                new Score(DishLabel.A, Category.GESCHMACK, 5),
                new Score(DishLabel.B, Category.MUNDGEFUEHL, 1),
                new Score(DishLabel.B, Category.TELLERSPRACHE, 1),
                new Score(DishLabel.B, Category.GESCHMACK, 1)
        ), Instant.now());
    }

    private ScoreSubmission cookBWinsSubmission(ChallengeId challengeId) {
        return ScoreSubmission.submit(challengeId, guestId, List.of(
                new Score(DishLabel.A, Category.MUNDGEFUEHL, 1),
                new Score(DishLabel.A, Category.TELLERSPRACHE, 1),
                new Score(DishLabel.A, Category.GESCHMACK, 1),
                new Score(DishLabel.B, Category.MUNDGEFUEHL, 5),
                new Score(DishLabel.B, Category.TELLERSPRACHE, 5),
                new Score(DishLabel.B, Category.GESCHMACK, 5)
        ), Instant.now());
    }

    private static class InMemoryChallengeRepository implements ChallengeRepository {
        private final Map<ChallengeId, Challenge> store = new HashMap<>();

        @Override
        public Optional<Challenge> findById(ChallengeId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Challenge> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public List<Challenge> findOpenByParticipant(AccountId accountId) {
            return List.of();
        }

        @Override
        public List<Challenge> findByCookPair(AccountId firstAccountId, AccountId secondAccountId) {
            return List.of();
        }

        @Override
        public Challenge save(Challenge challenge) {
            store.put(challenge.getId(), challenge);
            return challenge;
        }
    }

    private static class InMemoryScoreSubmissionRepository implements ScoreSubmissionRepository {
        private final Map<ChallengeId, List<ScoreSubmission>> byChallenge = new HashMap<>();

        @Override
        public Optional<ScoreSubmission> findById(ScoreSubmissionId id) {
            return byChallenge.values().stream().flatMap(List::stream)
                    .filter(submission -> submission.getId().equals(id))
                    .findFirst();
        }

        @Override
        public List<ScoreSubmission> findByChallengeId(ChallengeId challengeId) {
            return List.copyOf(byChallenge.getOrDefault(challengeId, List.of()));
        }

        @Override
        public Optional<ScoreSubmission> findByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId) {
            return findByChallengeId(challengeId).stream()
                    .filter(submission -> submission.getGuestAccountId().equals(guestAccountId))
                    .findFirst();
        }

        @Override
        public boolean existsByChallengeIdAndGuestAccountId(ChallengeId challengeId, AccountId guestAccountId) {
            return findByChallengeId(challengeId).stream()
                    .anyMatch(submission -> submission.getGuestAccountId().equals(guestAccountId));
        }

        @Override
        public ScoreSubmission save(ScoreSubmission submission) {
            byChallenge.computeIfAbsent(submission.getChallengeId(), key -> new ArrayList<>()).add(submission);
            return submission;
        }

        void replace(ChallengeId challengeId, ScoreSubmission submission) {
            byChallenge.put(challengeId, new ArrayList<>(List.of(submission)));
        }
    }

    private static class InMemoryCookRivalryRepository implements CookRivalryRepository {
        private final Map<String, CookRivalry> store = new HashMap<>();

        @Override
        public Optional<CookRivalry> findByPair(AccountId firstAccountId, AccountId secondAccountId) {
            AccountId[] ordered = CookRivalry.orderPair(firstAccountId, secondAccountId);
            return Optional.ofNullable(store.get(key(ordered[0], ordered[1])));
        }

        @Override
        public List<CookRivalry> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public CookRivalry save(CookRivalry rivalry) {
            store.put(key(rivalry.getCookAAccountId(), rivalry.getCookBAccountId()), rivalry);
            return rivalry;
        }

        private static String key(AccountId first, AccountId second) {
            return first.value() + ":" + second.value();
        }
    }
}
