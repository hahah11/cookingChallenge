package at.fraihs.cookoff.shared.web;

import at.fraihs.cookoff.shared.web.openapi.model.Pagination;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps a Spring Data {@link Page} into the generated {@link Pagination} block, per
 * docs/shared/04-api-design.md's pagination convention (zero-indexed page, default size
 * 20, max 100 - enforced by the generated request parameters, not here). See
 * docs/cookingChallenge/adr/0003-spring-data-pageable-in-repository-ports.md for why the
 * repository ports themselves already do the real DB-level paging.
 */
public record PagedResult<T>(List<T> content, Pagination pagination) {

    public static <T> PagedResult<T> of(Page<T> page) {
        Pagination pagination = new Pagination(page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
        return new PagedResult<>(page.getContent(), pagination);
    }
}
