package at.fraihs.cookoff.auth.application.mapper;

import at.fraihs.cookoff.auth.domain.model.Account;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.auth.domain.model.Email;
import at.fraihs.cookoff.auth.domain.model.Language;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.LocaleRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;

import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Domain -> generated-OpenAPI-model mapping, shared by every account use case that returns
 * the generated {@code AccountRestDto} model. Unlike infrastructure/persistence/mapper's
 * AccountMapper (which hand-writes both directions because the domain Account has no public
 * constructor),
 * the generated model has a plain public constructor/setters, so MapStruct generates
 * {@link #toGenerated} itself; only the typed-VO/enum conversions it can't infer are
 * hand-written, per docs/backend/03-code-style.md's Mapper Usage section.
 */
@Mapper(componentModel = "spring")
public interface AccountModelMapper {

    @Mapping(target = "locale", source = "language")
    AccountRestDto toGenerated(Account account);

    default LocaleRestDto map(Language language) {
        return LocaleRestDto.valueOf(language.name());
    }

    default Language toDomain(LocaleRestDto locale) {
        return Language.valueOf(locale.name());
    }

    default String map(AccountId id) {
        return id.toString();
    }

    default String map(Email email) {
        return email.toString();
    }

    /** Account#getRoles()'s Set.copyOf(...) doesn't guarantee iteration order - sort explicitly for a deterministic response. */
    default List<SystemRoleRestDto> mapRoles(Set<SystemRole> roles) {
        return roles.stream()
                .map(role -> SystemRoleRestDto.valueOf(role.name()))
                .sorted()
                .toList();
    }
}
