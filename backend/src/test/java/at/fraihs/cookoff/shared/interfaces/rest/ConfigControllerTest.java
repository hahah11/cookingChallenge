package at.fraihs.cookoff.shared.interfaces.rest;

import at.fraihs.cookoff.shared.application.service.ConfigService;
import at.fraihs.cookoff.shared.web.openapi.model.Config;
import at.fraihs.cookoff.shared.web.openapi.model.PlateColor;
import at.fraihs.cookoff.shared.web.openapi.model.SystemRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigService configService;

    @Test
    void should_return200WithConfig_when_requested() throws Exception {
        Config config = new Config(
                List.of(SystemRole.ADMIN, SystemRole.ORGANIZER, SystemRole.USER),
                List.of(new PlateColor("color-1", "Red", "#c0392b", 0)),
                Map.of());
        when(configService.execute()).thenReturn(config);

        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableRoles.length()").value(3))
                .andExpect(jsonPath("$.data.plateColors[0].id").value("color-1"))
                .andExpect(jsonPath("$.data.plateColors[0].name").value("Red"))
                .andExpect(jsonPath("$.meta.requestId").exists())
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }
}
