package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.service.HomeService;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
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
 * it), per docs/cookingChallenge/plans/backend-persistence-api-security-plan.md Phase 5.
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
    void should_return200_withOpenChallenges_when_authenticated() throws Exception {
        AccountId accountId = AccountId.generate();
        ChallengeParticipantView view = new ChallengeParticipantView(
                "chal-1", LocalDate.now(), "Title", "Schnitzel", "OPEN",
                List.of("A", "B"), List.of("MUNDGEFUEHL"), List.of(), null);
        when(homeService.execute(accountId)).thenReturn(List.of(view));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(accountId, null));

        mockMvc.perform(get("/api/v1/me/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("chal-1"));
    }
}
