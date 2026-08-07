package at.fraihs.cookoff.auth.interfaces.rest;

import at.fraihs.cookoff.auth.application.exception.AccountAlreadyExistsException;
import at.fraihs.cookoff.auth.application.exception.AccountNotFoundException;
import at.fraihs.cookoff.auth.application.service.CreateAccountService;
import at.fraihs.cookoff.auth.application.service.GetAccountDetailService;
import at.fraihs.cookoff.auth.application.service.ListAccountsService;
import at.fraihs.cookoff.auth.application.service.UpdateAccountService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.shared.config.JacksonConfig;
import at.fraihs.cookoff.shared.web.GlobalExceptionHandler;
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.model.AccountRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.CreateAccountRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PaginationRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRoleRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.UpdateAccountRequestRestDto;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security enforcement (JWT roles, link tokens) is covered by
 * {@code shared.security.SecurityIntegrationTest} — this slice test disables the security
 * filter chain to focus purely on controller/application-service wiring.
 */
@WebMvcTest(AccountsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonConfig.class})
class AccountsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateAccountService createAccountService;

    @MockitoBean
    private ListAccountsService listAccountsService;

    @MockitoBean
    private GetAccountDetailService getAccountDetailService;

    @MockitoBean
    private UpdateAccountService updateAccountService;

    @Test
    void should_return201_when_accountCreated() throws Exception {
        AccountRestDto account = new AccountRestDto("acc-1", "a@b.com", "Alice", "Cook", "Alice Cook", List.of(SystemRoleRestDto.USER));
        when(createAccountService.execute(any())).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateAccountRequestRestDto("a@b.com", "Alice", "Cook").roles(List.of(SystemRoleRestDto.USER)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("a@b.com"));
    }

    @Test
    void should_return400_when_emailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateAccountRequestRestDto("not-an-email", "Alice", "Cook"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return409_when_accountAlreadyExists() throws Exception {
        when(createAccountService.execute(any())).thenThrow(new AccountAlreadyExistsException("a@b.com"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateAccountRequestRestDto("a@b.com", "Alice", "Cook"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void should_return200_withAccountList_when_listCalled() throws Exception {
        AccountRestDto account = new AccountRestDto("acc-1", "a@b.com", "Alice", "Cook", "Alice Cook", List.of(SystemRoleRestDto.USER));
        PaginationRestDto pagination = new PaginationRestDto(0, 20, 1L, 1, true, true);
        when(listAccountsService.execute(0, 20)).thenReturn(new PagedResult<>(List.of(account), pagination));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("a@b.com"))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    void should_return200_withAccountDetail_when_accountExists() throws Exception {
        AccountId id = AccountId.generate();
        AccountRestDto account = new AccountRestDto(id.toString(), "a@b.com", "Alice", "Cook", "Alice Cook", List.of(SystemRoleRestDto.USER));
        when(getAccountDetailService.execute(eq(id))).thenReturn(account);

        mockMvc.perform(get("/api/v1/accounts/{accountId}", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("a@b.com"));
    }

    @Test
    void should_return404_when_accountDoesNotExist() throws Exception {
        AccountId id = AccountId.generate();
        when(getAccountDetailService.execute(eq(id))).thenThrow(new AccountNotFoundException(id.toString()));

        mockMvc.perform(get("/api/v1/accounts/{accountId}", id.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void should_return200_when_accountUpdated() throws Exception {
        AccountId id = AccountId.generate();
        AccountRestDto updated = new AccountRestDto(id.toString(), "a@b.com", "New", "Name", "New Name", List.of(SystemRoleRestDto.USER));
        when(updateAccountService.execute(eq(id), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/accounts/{accountId}", id.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateAccountRequestRestDto().firstName("New").lastName("Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));
    }
}
