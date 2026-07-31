package at.fraihs.cookoff.shared.web;

import java.time.Instant;
import java.util.UUID;

public record ApiMeta(String requestId, Instant timestamp) {

    public static ApiMeta now() {
        return new ApiMeta(UUID.randomUUID().toString(), Instant.now());
    }
}
