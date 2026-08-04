package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.cookoff.application.service.HomeService;
import at.fraihs.cookoff.shared.security.CurrentAccount;
import at.fraihs.cookoff.shared.web.openapi.api.HomeApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMeta;
import at.fraihs.cookoff.shared.web.openapi.model.GuestHomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class HomeController implements HomeApi {

    private final HomeService homeService;

    @Override
    public ResponseEntity<GuestHomeResponse> getMyHome() {
        var home = homeService.execute(CurrentAccount.id());
        return ResponseEntity.ok(new GuestHomeResponse(home, new ApiMeta(UUID.randomUUID().toString(), OffsetDateTime.now())));
    }
}
