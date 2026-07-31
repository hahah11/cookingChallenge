package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.application.exception.InvalidOrExpiredLinkException;
import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.service.HomeService;
import at.fraihs.cookoff.shared.config.SecurityConfig;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessLinkService accessLinkService;

    @MockitoBean
    private HomeService homeService;

    @Test
    void should_return200_withOpenChallenges_when_tokenValid() throws Exception {
        AccountId accountId = AccountId.generate();
        when(accessLinkService.verify("good-token")).thenReturn(accountId);
        ChallengeParticipantView view = new ChallengeParticipantView(
                "chal-1", LocalDate.now(), "Title", "Schnitzel", "OPEN",
                List.of("A", "B"), List.of("MUNDGEFUEHL"), List.of(), null);
        when(homeService.execute(accountId)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/me/home").param("token", "good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("chal-1"));
    }

    @Test
    void should_return401_when_tokenInvalid() throws Exception {
        when(accessLinkService.verify("bad-token")).thenThrow(new InvalidOrExpiredLinkException());

        mockMvc.perform(get("/api/v1/me/home").param("token", "bad-token"))
                .andExpect(status().isUnauthorized());
    }
}
