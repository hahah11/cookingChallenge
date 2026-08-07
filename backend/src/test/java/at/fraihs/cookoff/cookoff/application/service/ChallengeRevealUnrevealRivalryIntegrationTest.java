package at.fraihs.cookoff.cookoff.application.service;

import at.fraihs.cookoff.auth.AccountLookup;
import at.fraihs.cookoff.auth.AccountSummary;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.cookoff.application.event.ChallengeRevealedRivalryUpdater;
import at.fraihs.cookoff.cookoff.application.event.ChallengeUnrevealedRivalryUpdater;
import at.fraihs.cookoff.cookoff.application.port.ChallengeRepository;
import at.fraihs.cookoff.cookoff.application.port.CookRivalryRepository;
import at.fraihs.cookoff.cookoff.application.port.ScoreSubmissionRepository;
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
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeResultRestDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

    private final List<Object> pendingEvents = new ArrayList<>();
    private ChallengeRevealedRivalryUpdater revealedUpdater;
    private ChallengeUnrevealedRivalryUpdater unrevealedUpdater;

    private RevealChallengeService revealChallengeService;
    private UnrevealChallengeService unrevealChallengeService;

    @BeforeEach
    void setUp() {
        revealedUpdater = new ChallengeRevealedRivalryUpdater(cookRivalryRepository);
        unrevealedUpdater = new ChallengeUnrevealedRivalryUpdater(cookRivalryRepository);
        // Queues events instead of dispatching inline, standing in for
        // @TransactionalEventListener(phase = AFTER_COMMIT) - which does NOT run while
        // RevealChallengeService/UnrevealChallengeService are still executing (see their own
        // in-memory CookRivalry projection, needed precisely because the real listener hasn't
        // run yet at that point). Tests call commit() to simulate the transaction committing.
        var deferredPublisher = new org.springframework.context.ApplicationEventPublisher() {
            @Override
            public void publishEvent(Object event) {
                pendingEvents.add(event);
            }
        };
        revealChallengeService = new RevealChallengeService(
                challengeRepository, scoreSubmissionRepository, cookRivalryRepository, accountLookup, deferredPublisher);
        unrevealChallengeService = new UnrevealChallengeService(
                accountLookup, challengeRepository, scoreSubmissionRepository, deferredPublisher);
    }

    private void commit() {
        for (Object event : pendingEvents) {
            if (event instanceof ChallengeRevealed revealed) {
                revealedUpdater.on(revealed);
            } else if (event instanceof ChallengeUnrevealed unrevealed) {
                unrevealedUpdater.on(unrevealed);
            }
        }
        pendingEvents.clear();
    }

    @Test
    void should_endUpWithCorrectRivalryCounters_when_revealingUnrevealingAndReRevealingWithADifferentResult() {
        when(accountLookup.canOrganize(organizerId)).thenReturn(true);
        when(accountLookup.getById(cookAId)).thenReturn(new AccountSummary(cookAId, new Email("a@x.com"), "Cook A", "Cook"));
        when(accountLookup.getById(cookBId)).thenReturn(new AccountSummary(cookBId, new Email("b@x.com"), "Cook B", "Cook"));
        Challenge challenge = Challenge.create(LocalDate.now(), "Season Finale", new DishName("Schnitzel"),
                cookAId, cookBId, List.of(guestId), organizerId);
        challengeRepository.save(challenge);
        scoreSubmissionRepository.replace(challenge.getId(), cookAWinsSubmission(challenge.getId()));

        ChallengeResultRestDto firstRevealResponse =
                revealChallengeService.execute(challenge.getId().toString());
        // The response must already reflect this reveal's own rivalry update, even though
        // ChallengeRevealedRivalryUpdater (AFTER_COMMIT) hasn't run yet - commit() below is
        // still pending. This is the specific bug this test guards against.
        assertEquals("Cook A leads Cook B 1-0", firstRevealResponse.getRivalry().getHeadline());
        commit();

        CookRivalry afterFirstReveal = cookRivalryRepository.findByPair(cookAId, cookBId).orElseThrow();
        assertEquals(1, afterFirstReveal.getCookAWins());
        assertEquals(0, afterFirstReveal.getCookBWins());
        assertEquals(0, afterFirstReveal.getDraws());
        assertEquals(1, afterFirstReveal.getTotalChallenges());

        unrevealChallengeService.execute(challenge.getId().toString(), organizerId);
        commit();

        CookRivalry afterUnreveal = cookRivalryRepository.findByPair(cookAId, cookBId).orElseThrow();
        assertEquals(0, afterUnreveal.getCookAWins());
        assertEquals(0, afterUnreveal.getCookBWins());
        assertEquals(0, afterUnreveal.getDraws());
        assertEquals(0, afterUnreveal.getTotalChallenges());

        // scores edited in between (guest resubmits, now favoring cook B)
        scoreSubmissionRepository.replace(challenge.getId(), cookBWinsSubmission(challenge.getId()));

        revealChallengeService.execute(challenge.getId().toString());
        commit();

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
        public Page<Challenge> findAll(Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()), pageable, store.size());
        }

        @Override
        public List<Challenge> findByParticipant(AccountId accountId) {
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
            // Reconstructs a detached copy, like a real JPA-backed repository mapping a fresh
            // domain object per call - returning the live stored reference would let
            // RevealChallengeService's in-memory (unsaved) recordResult() mutate persisted
            // state as a side effect, double-counting once ChallengeRevealedRivalryUpdater's
            // own recordResult() runs on commit().
            return Optional.ofNullable(store.get(key(ordered[0], ordered[1])))
                    .map(rivalry -> CookRivalry.reconstitute(rivalry.getId(), rivalry.getCookAAccountId(),
                            rivalry.getCookBAccountId(), rivalry.getCookAWins(), rivalry.getCookBWins(),
                            rivalry.getDraws(), rivalry.getTotalChallenges()));
        }

        @Override
        public Page<CookRivalry> findAll(Pageable pageable) {
            return new PageImpl<>(List.copyOf(store.values()), pageable, store.size());
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
