package at.fraihs.cookoff.auth.infrastructure.persistence.mapper;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.infrastructure.persistence.entity.AccountJpaEntity;
import org.mapstruct.Mapper;

/**
 * Account is immutable with no public constructor (only create/reconstitute factories), so
 * this mapper delegates to Account.reconstitute(...) instead of MapStruct's generated
 * field-by-field mapping, per docs/backend/03-code-style.md#mapper-usage-mapstruct.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    default Account toDomain(AccountJpaEntity entity) {
        return Account.reconstitute(
                new AccountId(entity.getId()),
                new Email(entity.getEmail()),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPasswordHash(),
                entity.getRoles());
    }

    default AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(
                account.getId().value(),
                account.getEmail().value(),
                account.getFirstName(),
                account.getLastName(),
                account.getPasswordHash(),
                account.getRoles());
    }
}
