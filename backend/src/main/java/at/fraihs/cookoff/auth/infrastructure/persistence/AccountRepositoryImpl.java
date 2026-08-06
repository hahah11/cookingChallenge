package at.fraihs.cookoff.auth.infrastructure.persistence;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.application.port.AccountRepository;
import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import at.fraihs.cookoff.auth.infrastructure.persistence.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository jpaRepository;
    private final AccountMapper mapper;

    @Override
    public Optional<Account> findById(AccountId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    @Override
    public Page<Account> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity saved = jpaRepository.save(mapper.toEntity(account));
        return mapper.toDomain(saved);
    }
}
