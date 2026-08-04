package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.service.HomeService;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import at.fraihs.cookoff.shared.web.openapi.model.Category;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatus;
import at.fraihs.cookoff.shared.web.openapi.model.DishLabel;
import at.fraihs.cookoff.shared.web.openapi.model.GuestHome;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallenge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security enforcement (JWT roles, link tokens) is covered by
 * {@code shared.security.SecurityIntegrationTest} — this slice test disables the security
 * filter chain (@AutoConfigureMockMvc(addFilters = false)). With the filter chain disabled,
 * {@code SecurityContextHolderFilter} never runs, so setting the AccountId principal is done
 * directly via {@link SecurityContextHolder} (MockMvc's single-threaded dispatch still honors
 * it) — {@code CurrentAccount.id()} reads it back the same way regardless.
 */
@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeService homeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_return200_withHomeBuckets_when_authenticated() throws Exception {
        AccountId accountId = AccountId.generate();
        ParticipantChallenge view = new ParticipantChallenge(
                "chal-1", LocalDate.now(), "Title", "Schnitzel", ChallengeStatus.OPEN,
                List.of(DishLabel.A, DishLabel.B), List.of(Category.MUNDGEFUEHL), List.of(),
                false, false, null, null, true, false);
        when(homeService.execute(accountId)).thenReturn(new GuestHome(List.of(view), List.of()));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(accountId, null));

        mockMvc.perform(get("/api/v1/me/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open[0].id").value("chal-1"));
    }
}
