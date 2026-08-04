package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.service.RivalriesListService;
import at.fraihs.cookoff.cookoff.application.service.RivalryDetailService;
import at.fraihs.cookoff.shared.web.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.api.RivalriesApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMeta;
import at.fraihs.cookoff.shared.web.openapi.model.Rivalry;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryDetail;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryDetailResponse;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RivalriesController implements RivalriesApi {

    private final RivalriesListService rivalriesListService;
    private final RivalryDetailService rivalryDetailService;

    @Override
    public ResponseEntity<RivalryListResponse> listRivalries(Integer page, Integer size) {
        PagedResult<Rivalry> result = rivalriesListService.execute(page, size);
        return ResponseEntity.ok(new RivalryListResponse(result.content(), result.pagination(), meta()));
    }

    @Override
    public ResponseEntity<RivalryDetailResponse> getRivalryDetail(String cookAAccountId, String cookBAccountId) {
        RivalryDetail detail = rivalryDetailService.execute(
                AccountId.fromString(cookAAccountId), AccountId.fromString(cookBAccountId));
        return ResponseEntity.ok(new RivalryDetailResponse(detail, meta()));
    }

    private ApiMeta meta() {
        return new ApiMeta(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
