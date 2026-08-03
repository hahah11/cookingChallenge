package at.fraihs.cookoff.cookoff.application.port;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.domain.model.CookRivalry;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Repository ports live in the application layer, not domain - see
 * docs/cookingChallenge/adr/0002-repository-ports-in-application-layer.md.
 */
@Repository
public interface CookRivalryRepository {

    Optional<CookRivalry> findByPair(AccountId firstAccountId, AccountId secondAccountId);

    /** Backs the rivalries list screen. See
     * docs/cookingChallenge/adr/0003-spring-data-pageable-in-repository-ports.md. */
    Page<CookRivalry> findAll(Pageable pageable);

    CookRivalry save(CookRivalry rivalry);
}
