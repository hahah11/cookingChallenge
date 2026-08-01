package at.fraihs.cookoff.auth.domain.repository;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    Optional<Account> findByEmail(Email email);

    boolean existsByEmail(Email email);

    List<Account> findAll();

    Account save(Account account);
}
