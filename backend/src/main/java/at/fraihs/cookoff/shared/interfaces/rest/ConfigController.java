package at.fraihs.cookoff.shared.interfaces.rest;

import at.fraihs.cookoff.shared.application.service.ConfigService;
import at.fraihs.cookoff.shared.web.openapi.api.ConfigApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMetaRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.ConfigResponseRestDto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConfigController implements ConfigApi {

    private final ConfigService configService;

    @Override
    public ResponseEntity<ConfigResponseRestDto> getConfig() {
        ApiMetaRestDto meta = new ApiMetaRestDto(UUID.randomUUID().toString(), OffsetDateTime.now());
        return ResponseEntity.ok(new ConfigResponseRestDto(configService.execute(), meta));
    }
}
