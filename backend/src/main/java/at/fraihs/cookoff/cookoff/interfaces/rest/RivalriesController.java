package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.service.RivalriesListService;
import at.fraihs.cookoff.cookoff.application.service.RivalryDetailService;
import at.fraihs.cookoff.shared.web.dto.PagedResult;
import at.fraihs.cookoff.shared.web.openapi.api.RivalriesApi;
import at.fraihs.cookoff.shared.web.openapi.model.ApiMetaRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryDetailResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryDetailRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryListResponseRestDto;
import at.fraihs.cookoff.shared.web.openapi.model.RivalryRestDto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RivalriesController implements RivalriesApi {

    private final RivalriesListService rivalriesListService;
    private final RivalryDetailService rivalryDetailService;

    @Override
    public ResponseEntity<RivalryListResponseRestDto> listRivalries(Integer page, Integer size) {
        PagedResult<RivalryRestDto> result = rivalriesListService.execute(page, size);
        return ResponseEntity.ok(new RivalryListResponseRestDto(result.content(), result.pagination(), meta()));
    }

    @Override
    public ResponseEntity<RivalryDetailResponseRestDto> getRivalryDetail(String cookAAccountId, String cookBAccountId) {
        RivalryDetailRestDto detail = rivalryDetailService.execute(
                AccountId.fromString(cookAAccountId), AccountId.fromString(cookBAccountId));
        return ResponseEntity.ok(new RivalryDetailResponseRestDto(detail, meta()));
    }

    private ApiMetaRestDto meta() {
        return new ApiMetaRestDto(UUID.randomUUID().toString(), OffsetDateTime.now());
    }
}
