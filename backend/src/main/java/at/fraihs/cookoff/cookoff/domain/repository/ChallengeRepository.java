package at.fraihs.cookoff.cookoff.domain.repository;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.Challenge;
import at.fraihs.cookoff.cookoff.domain.model.ChallengeId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository {

    Optional<Challenge> findById(ChallengeId id);

    List<Challenge> findAll();

    /** OPEN challenges where the account is a cook or a pre-added guest — backs GET /me/home. */
    List<Challenge> findOpenByParticipant(AccountId accountId);

    /**
     * Every challenge where these two accounts cooked against each other, regardless of
     * which one was cook A/B in any given challenge — backs the rivalry detail screen.
     */
    List<Challenge> findByCookPair(AccountId firstAccountId, AccountId secondAccountId);

    Challenge save(Challenge challenge);
}
