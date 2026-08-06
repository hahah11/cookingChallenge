package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.exception.RivalryNotFoundException;
import at.fraihs.cookoff.cookoff.application.service.RivalriesListService;
import at.fraihs.cookoff.cookoff.application.service.RivalryDetailService;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.PaginationRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryDetailRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryRestDto;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security enforcement (JWT roles) is covered by {@code shared.security.SecurityIntegrationTest}
 * — this slice test disables the security filter chain to focus purely on
 * controller/application-service wiring.
 */
@WebMvcTest(RivalriesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RivalriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RivalriesListService rivalriesListService;

    @MockitoBean
    private RivalryDetailService rivalryDetailService;

    @Test
    void should_return200_withRivalryList_when_listCalled() throws Exception {
        RivalryRestDto rivalry = new RivalryRestDto("acc-a", "Alice", "acc-b", "Bob", 3, 1, 1, 5, "Alice leads Bob 3-1 (1 draw)");
        PaginationRestDto pagination = new PaginationRestDto(0, 20, 1L, 1, true, true);
        when(rivalriesListService.execute(0, 20)).thenReturn(new PagedResult<>(List.of(rivalry), pagination));

        mockMvc.perform(get("/api/v1/rivalries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].cookAName").value("Alice"))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    void should_return200_withRivalryDetail_when_pairExists() throws Exception {
        AccountId aliceId = AccountId.generate();
        AccountId bobId = AccountId.generate();
        RivalryDetailRestDto detail = new RivalryDetailRestDto(aliceId.toString(), "Alice", bobId.toString(), "Bob",
                3, 1, 1, 5, "Alice leads Bob 3-1 (1 draw)", List.of());
        when(rivalryDetailService.execute(aliceId, bobId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/rivalries/{a}/{b}", aliceId.toString(), bobId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headline").value("Alice leads Bob 3-1 (1 draw)"));
    }

    @Test
    void should_return404_when_pairHasNeverPlayed() throws Exception {
        AccountId aliceId = AccountId.generate();
        AccountId bobId = AccountId.generate();
        when(rivalryDetailService.execute(aliceId, bobId))
                .thenThrow(new RivalryNotFoundException(aliceId.toString(), bobId.toString()));

        mockMvc.perform(get("/api/v1/rivalries/{a}/{b}", aliceId.toString(), bobId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
