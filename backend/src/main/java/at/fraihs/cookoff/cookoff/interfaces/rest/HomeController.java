package at.fraihs.cookoff.cookoff.interfaces.rest;

import at.fraihs.cookoff.auth.domain.model.AccountId;
import at.fraihs.cookoff.cookoff.application.dto.ChallengeParticipantView;
import at.fraihs.cookoff.cookoff.application.service.HomeService;
import at.fraihs.cookoff.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ApiResponse<List<ChallengeParticipantView>> home(@AuthenticationPrincipal AccountId accountId) {
        return ApiResponse.of(homeService.execute(accountId));
    }
}
