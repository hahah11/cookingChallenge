package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.cookoff.application.service.PublicRegistrationService;
import at.fraihs.cookoff.shared.web.openapi.api.PublicApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMeta;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationRequest;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationResult;
import at.fraihs.cookoff.shared.web.openapi.model.PublicRegistrationResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicController implements PublicApi {

    private final PublicRegistrationService publicRegistrationService;

    @Override
    public ResponseEntity<PublicRegistrationResultResponse> registerPublicly(
            PublicRegistrationRequest publicRegistrationRequest) {
        PublicRegistrationResult result = publicRegistrationService.execute(publicRegistrationRequest);
        ApiMeta meta = new ApiMeta(UUID.randomUUID().toString(), OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PublicRegistrationResultResponse(result, meta));
    }
}
