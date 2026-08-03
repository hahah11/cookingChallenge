package at.fraihs.cookoff.cookoff.domain.repository;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CookRivalryRepository {

    Optional<CookRivalry> findByPair(AccountId firstAccountId, AccountId secondAccountId);

    /** Backs the rivalries list screen. */
    List<CookRivalry> findAll();

    CookRivalry save(CookRivalry rivalry);
}
