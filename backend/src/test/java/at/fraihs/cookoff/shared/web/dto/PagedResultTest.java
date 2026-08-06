package at.fraihs.cookoff.shared.web.dto;

import at.fraihs.cookoff.shared.web.openapi.model.PaginationRestDto;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedResultTest {

    @Test
    void should_returnFirstPage_when_lessThanOnePageOfData() {
        Page<Integer> page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(0, 20), 3);

        PagedResult<Integer> result = PagedResult.of(page);

        assertEquals(List.of(1, 2, 3), result.content());
        PaginationRestDto pagination = result.pagination();
        assertEquals(0, pagination.getPage());
        assertEquals(3L, pagination.getTotalElements());
        assertEquals(1, pagination.getTotalPages());
        assertTrue(pagination.getFirst());
        assertTrue(pagination.getLast());
    }

    @Test
    void should_reflectMiddlePage_when_multiplePagesExist() {
        Page<Integer> page = new PageImpl<>(List.of(3, 4), PageRequest.of(1, 2), 5);

        PagedResult<Integer> result = PagedResult.of(page);

        assertEquals(List.of(3, 4), result.content());
        assertFalse(result.pagination().getFirst());
        assertFalse(result.pagination().getLast());
        assertEquals(3, result.pagination().getTotalPages());
    }

    @Test
    void should_returnEmptyFirstAndLastPage_when_noData() {
        Page<Integer> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        PagedResult<Integer> result = PagedResult.of(page);

        assertEquals(0, result.pagination().getTotalPages());
        assertTrue(result.pagination().getFirst());
        assertTrue(result.pagination().getLast());
    }
}
