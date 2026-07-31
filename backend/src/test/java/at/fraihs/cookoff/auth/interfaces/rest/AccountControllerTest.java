package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.dto.AccountView;
import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.application.service.ListAccountsService;
import at.fraihs.cookoff.auth.domain.model.SystemRole;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security enforcement (JWT roles, link tokens) is covered by
 * {@code shared.security.SecurityIntegrationTest} — this slice test disables the security
 * filter chain (@AutoConfigureMockMvc(addFilters = false)) to focus purely on
 * controller/application-service wiring, per
 * docs/cookingChallenge/plans/backend-persistence-api-security-plan.md Phase 5.
 */
@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateAccountService createAccountService;

    @MockitoBean
    private ListAccountsService listAccountsService;

    @Test
    void should_return201_when_accountCreated() throws Exception {
        AccountView view = new AccountView("acc-1", "a@b.com", "Alice", Set.of(SystemRole.USER));
        when(createAccountService.execute(any())).thenReturn(view);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest("a@b.com", "Alice", Set.of(SystemRole.USER), null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("a@b.com"));
    }

    @Test
    void should_return400_when_emailBlank() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest("", "Alice", Set.of(), null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return409_when_accountAlreadyExists() throws Exception {
        when(createAccountService.execute(any())).thenThrow(new AccountAlreadyExistsException("a@b.com"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest("a@b.com", "Alice", Set.of(), null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void should_return200_withAccountList_when_listCalled() throws Exception {
        AccountView view = new AccountView("acc-1", "a@b.com", "Alice", Set.of(SystemRole.USER));
        when(listAccountsService.execute()).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("a@b.com"));
    }
}
