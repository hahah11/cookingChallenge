package at.fraihs.cookoff.cookoff.domain.repository;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
public interface CookRivalryRepository {

    Optional<CookRivalry> findByPair(AccountId firstAccountId, AccountId secondAccountId);

    CookRivalry save(CookRivalry rivalry);
}
