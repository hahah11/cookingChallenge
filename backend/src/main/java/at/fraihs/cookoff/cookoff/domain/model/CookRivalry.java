package at.fraihs.cookoff.cookoff.domain.model;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

/** Running head-to-head record for one specific pair of cooks, updated on each ChallengeRevealed. */
@AggregateRoot
public class CookRivalry {

    @Identity
    private final CookRivalryId id;
    private final AccountId cookAAccountId;
    private final AccountId cookBAccountId;
    private int cookAWins;
    private int cookBWins;
    private int draws;
    private int totalChallenges;

    private CookRivalry(CookRivalryId id, AccountId cookAAccountId, AccountId cookBAccountId,
                         int cookAWins, int cookBWins, int draws, int totalChallenges) {
        this.id = id;
        this.cookAAccountId = cookAAccountId;
        this.cookBAccountId = cookBAccountId;
        this.cookAWins = cookAWins;
        this.cookBWins = cookBWins;
        this.draws = draws;
        this.totalChallenges = totalChallenges;
    }

    /** Canonical ordering so the pair (X, Y) always maps to one rivalry, regardless of A/B label in any one challenge. */
    public static CookRivalry start(AccountId firstAccountId, AccountId secondAccountId) {
        AccountId[] ordered = orderPair(firstAccountId, secondAccountId);
        return new CookRivalry(CookRivalryId.generate(), ordered[0], ordered[1], 0, 0, 0, 0);
    }

    public static CookRivalry reconstitute(CookRivalryId id, AccountId cookAAccountId, AccountId cookBAccountId,
                                            int cookAWins, int cookBWins, int draws, int totalChallenges) {
        return new CookRivalry(id, cookAAccountId, cookBAccountId, cookAWins, cookBWins, draws, totalChallenges);
    }

    public static AccountId[] orderPair(AccountId first, AccountId second) {
        return first.value() <= second.value()
                ? new AccountId[]{first, second}
                : new AccountId[]{second, first};
    }

    public void recordResult(AccountId winnerAccountId) {
        totalChallenges++;
        if (winnerAccountId == null) {
            draws++;
        } else if (winnerAccountId.equals(cookAAccountId)) {
            cookAWins++;
        } else if (winnerAccountId.equals(cookBAccountId)) {
            cookBWins++;
        } else {
            throw new IllegalArgumentException("Winner is not part of this rivalry: " + winnerAccountId);
        }
    }

    public CookRivalryId getId() {
        return id;
    }

    public AccountId getCookAAccountId() {
        return cookAAccountId;
    }

    public AccountId getCookBAccountId() {
        return cookBAccountId;
    }

    public int getCookAWins() {
        return cookAWins;
    }

    public int getCookBWins() {
        return cookBWins;
    }

    public int getDraws() {
        return draws;
    }

    public int getTotalChallenges() {
        return totalChallenges;
    }
}
