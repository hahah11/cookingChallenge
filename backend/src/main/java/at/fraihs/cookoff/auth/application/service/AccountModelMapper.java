package at.fraihs.cookoff.auth.application.service;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

/**
 * Domain -> generated-OpenAPI-model mapping, shared by every account use case that returns
 * the generated {@code Account} model. Unlike infrastructure/persistence's AccountMapper
 * (which hand-writes both directions because the domain Account has no public constructor),
 * the generated Account model has a plain public constructor/setters, so MapStruct generates
 * {@link #toGenerated} itself; only the typed-VO/enum conversions it can't infer are
 * hand-written, per docs/backend/03-code-style.md's Mapper Usage section.
 */
@Mapper(componentModel = "spring")
public interface AccountModelMapper {

    at.fraihs.cookoff.shared.web.openapi.model.Account toGenerated(Account account);

    default String map(AccountId id) {
        return id.toString();
    }

    default String map(Email email) {
        return email.toString();
    }

    /** Account#getRoles()'s Set.copyOf(...) doesn't guarantee iteration order - sort explicitly for a deterministic response. */
    default List<at.fraihs.cookoff.shared.web.openapi.model.SystemRole> mapRoles(Set<SystemRole> roles) {
        return roles.stream()
                .map(role -> at.fraihs.cookoff.shared.web.openapi.model.SystemRole.valueOf(role.name()))
                .sorted()
                .toList();
    }
}
