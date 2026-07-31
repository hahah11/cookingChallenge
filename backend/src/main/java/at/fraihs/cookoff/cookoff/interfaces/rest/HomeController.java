package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.application.service.AccessLinkService;
import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.service.HomeService;
import at.fraihs.cookoff.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/home")
@RequiredArgsConstructor
public class HomeController {

    private final AccessLinkService accessLinkService;
    private final HomeService homeService;

    @GetMapping
    public ApiResponse<List<ChallengeParticipantView>> home(@RequestParam String token) {
        AccountId accountId = accessLinkService.verify(token);
        return ApiResponse.of(homeService.execute(accountId));
    }
}
