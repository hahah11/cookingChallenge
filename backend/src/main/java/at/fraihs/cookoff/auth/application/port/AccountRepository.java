package at.fraihs.cookoff.auth.application.port;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Repository ports live in the application layer, not domain - see
 * docs/cookingChallenge/adr/0002-repository-ports-in-application-layer.md.
 */
@Repository
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    Optional<Account> findByEmail(Email email);

    boolean existsByEmail(Email email);

    /** See docs/cookingChallenge/adr/0003-spring-data-pageable-in-repository-ports.md. */
    Page<Account> findAll(Pageable pageable);

    Account save(Account account);
}
