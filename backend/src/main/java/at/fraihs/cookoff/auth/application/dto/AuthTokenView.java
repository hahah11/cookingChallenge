package at.fraihs.cookoff.auth.application.dto;

import java.time.Instant;

public record AuthTokenView(String accessToken, Instant expiresAt) {
}
