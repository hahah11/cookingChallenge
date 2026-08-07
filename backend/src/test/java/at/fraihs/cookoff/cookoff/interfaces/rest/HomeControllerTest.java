package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.service.HomeService;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import at.fraihs.cookoff.shared.web.openapi.model.CategoryRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ChallengeStatusRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.DishLabelRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.GuestHomeRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ParticipantChallengeRestDto;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security enforcement (JWT roles, link tokens) is covered by
 * {@code shared.security.SecurityIntegrationTest} — this slice test disables the security
 * filter chain (@AutoConfigureMockMvc(addFilters = false)). With the filter chain disabled,
 * {@code SecurityContextHolderFilter} never runs, so setting the {@link Jwt} principal is done
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
        ParticipantChallengeRestDto view = new ParticipantChallengeRestDto(
                "chal-1", LocalDate.now(), "Title", "Schnitzel", ChallengeStatusRestDto.OPEN,
                List.of(DishLabelRestDto.A, DishLabelRestDto.B), List.of(CategoryRestDto.MUNDGEFUEHL), List.of(),
                false, false, null, null, true, false);
        when(homeService.execute(accountId)).thenReturn(new GuestHomeRestDto("Felix", List.of(view), List.of()));
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", accountId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        mockMvc.perform(get("/api/v1/me/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open[0].id").value("chal-1"));
    }
}
