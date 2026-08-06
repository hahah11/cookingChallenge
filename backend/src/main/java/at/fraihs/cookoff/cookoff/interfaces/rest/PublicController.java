package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.cookoff.application.service.PublicRegistrationService;
import at.fraihs.cookoff.shared.web.openapi.api.PublicApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMetaRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationRequestRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationResultResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationResultRestDto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicController implements PublicApi {

    private final PublicRegistrationService publicRegistrationService;

    @Override
    public ResponseEntity<PublicRegistrationResultResponseRestDto> registerPublicly(
            PublicRegistrationRequestRestDto publicRegistrationRequest) {
        PublicRegistrationResultRestDto result = publicRegistrationService.execute(publicRegistrationRequest);
        ApiMetaRestDto meta = new ApiMetaRestDto(UUID.randomUUID().toString(), OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PublicRegistrationResultResponseRestDto(result, meta));
    }
}
