package at.fraihs.cookoff.shared.interfaces.rest;

import at.fraihs.cookoff.shared.application.service.ConfigService;
import at.fraihs.cookoff.shared.web.openapi.api.ConfigApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMeta;
import at.fraihs.cookoff.shared.web.openapi.model.ConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ConfigController implements ConfigApi {

    private final ConfigService configService;

    @Override
    public ResponseEntity<ConfigResponse> getConfig() {
        ApiMeta meta = new ApiMeta(UUID.randomUUID().toString(), OffsetDateTime.now());
        return ResponseEntity.ok(new ConfigResponse(configService.execute(), meta));
    }
}
