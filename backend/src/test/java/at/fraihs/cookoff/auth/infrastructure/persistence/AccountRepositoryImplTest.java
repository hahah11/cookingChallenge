package at.fraihs.cookoff.auth.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryImplTest {

    @Autowired
    private AccountJpaRepository jpaRepository;

    private AccountRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new AccountRepositoryImpl(jpaRepository, new AccountMapperImpl());
    }

    @Test
    void should_roundTripFullAggregate_when_savingThenFindingById() {
        Account account = Account.create(new Email("cook@example.com"), "Cook One",
                SystemRole.ORGANIZER, SystemRole.USER);

        Account saved = repository.save(account);
        Optional<Account> found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(new Email("cook@example.com"), found.get().getEmail());
        assertEquals("Cook One", found.get().getName());
        assertEquals(Set.of(SystemRole.ORGANIZER, SystemRole.USER), found.get().getRoles());
    }

    @Test
    void should_findAccount_when_lookingUpByEmail() {
        repository.save(Account.create(new Email("guest@example.com"), "Guest One"));

        Optional<Account> found = repository.findByEmail(new Email("guest@example.com"));

        assertTrue(found.isPresent());
        assertEquals("Guest One", found.get().getName());
    }

    @Test
    void should_reportExists_when_emailAlreadySaved() {
        repository.save(Account.create(new Email("taken@example.com"), "Taken"));

        assertTrue(repository.existsByEmail(new Email("taken@example.com")));
        assertFalse(repository.existsByEmail(new Email("free@example.com")));
    }
}
