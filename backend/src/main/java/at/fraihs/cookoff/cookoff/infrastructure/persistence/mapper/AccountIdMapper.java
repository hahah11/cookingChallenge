package at.fraihs.cookoff.cookoff.infrastructure.persistence.mapper;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountIdMapper {

    default AccountId toDomain(Long raw) {
        return raw == null ? null : new AccountId(raw);
    }

    default Long toRaw(AccountId accountId) {
        return accountId == null ? null : accountId.value();
    }
}
